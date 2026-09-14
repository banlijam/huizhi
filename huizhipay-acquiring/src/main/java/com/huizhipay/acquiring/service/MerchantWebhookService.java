package com.huizhipay.acquiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huizhipay.common.exceptions.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantWebhookService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final WebhookSecretCipher cipher;
    private final WebhookEndpointPolicy endpointPolicy;
    private final PlatformTransactionManager transactionManager;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public ConfigView getConfig(String merchantId) {
        List<ConfigView> rows = jdbc.query("select endpoint_url,enabled,created_at,updated_at from t_merchant_webhook_config where merchant_id=?",
                (rs,n) -> new ConfigView(rs.getString(1), rs.getBoolean(2), true, null, rs.getTimestamp(3).toLocalDateTime(), rs.getTimestamp(4).toLocalDateTime()), merchantId);
        return rows.isEmpty() ? new ConfigView(null, false, false, null, null, null) : rows.getFirst();
    }

    public ConfigView save(String merchantId, String endpointUrl, boolean enabled) {
        endpointPolicy.validateConfiguration(endpointUrl);
        if (!cipher.configured()) throw new BizException(503, "Webhook encryption master key is not configured");
        String current = jdbc.query("select endpoint_url from t_merchant_webhook_config where merchant_id=?", rs -> rs.next() ? rs.getString(1) : null, merchantId);
        if (current != null) {
            Integer pending = jdbc.queryForObject("select count(*) from t_merchant_webhook_delivery where merchant_id=? and status in ('PENDING','RETRY','SENDING')", Integer.class, merchantId);
            if (pending != null && pending > 0) throw new BizException(409, "Webhook configuration cannot rotate while deliveries are pending");
        }
        String oneTimeSecret = "whsec_" + randomHex(32);
        String encrypted = cipher.encrypt(oneTimeSecret);
        jdbc.update("insert into t_merchant_webhook_config(merchant_id,endpoint_url,secret_ciphertext,enabled) values(?,?,?,?) " +
                        "on conflict(merchant_id) do update set endpoint_url=excluded.endpoint_url,secret_ciphertext=excluded.secret_ciphertext,enabled=excluded.enabled,updated_at=current_timestamp",
                merchantId, endpointUrl, encrypted, enabled);
        ConfigView saved = getConfig(merchantId);
        return new ConfigView(saved.endpointUrl(), saved.enabled(), true, oneTimeSecret, saved.createdAt(), saved.updatedAt());
    }

    public String enqueue(String merchantId, String orderNo, String eventType, Map<String,Object> payload) {
        ConfigRow config = config(merchantId);
        if (config == null || !config.enabled()) return null;
        String eventId = "evt_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Map<String,Object> envelope = Map.of("eventId", eventId, "type", eventType, "sandbox", true,
                    "createdAt", Instant.now().toString(), "data", payload);
            jdbc.update("insert into t_merchant_webhook_delivery(event_id,merchant_id,order_no,event_type,payload_json,endpoint_url) values(?,?,?,?,?::jsonb,?)",
                    eventId, merchantId, orderNo, eventType, objectMapper.writeValueAsString(envelope), config.endpointUrl());
            return eventId;
        } catch (Exception e) { throw new IllegalStateException("Unable to create webhook outbox event", e); }
    }

    public String enqueueTest(String merchantId) {
        return enqueue(merchantId, null, "webhook.test", Map.of("message", "HuizhiPay test notification"));
    }

    public List<Map<String,Object>> list(String merchantId) {
        return jdbc.queryForList("select event_id,event_type,order_no,status,attempt_count,next_attempt_at,last_error,created_at,updated_at from t_merchant_webhook_delivery where merchant_id=? order by created_at desc limit 100", merchantId);
    }

    public List<Map<String,Object>> attempts(String merchantId, String eventId) {
        return jdbc.queryForList("select a.attempt_no,a.response_status,a.outcome,a.error_message,a.attempted_at from t_merchant_webhook_attempt a join t_merchant_webhook_delivery d on d.id=a.delivery_id where d.merchant_id=? and d.event_id=? order by a.attempt_no", merchantId, eventId);
    }

    public void retry(String merchantId, String eventId) {
        int updated = jdbc.update("update t_merchant_webhook_delivery set status='RETRY',next_attempt_at=current_timestamp,lease_until=null,last_error=null,updated_at=current_timestamp where merchant_id=? and event_id=? and status='FAILED'", merchantId, eventId);
        if (updated != 1) throw new BizException(409, "Only a failed merchant delivery can be retried");
    }

    @Scheduled(fixedDelayString = "${huizhipay.webhooks.worker-delay-ms:5000}")
    public void deliverDue() {
        for (int i=0; i<10; i++) {
            Delivery row = new TransactionTemplate(transactionManager).execute(s -> claimOne());
            if (row == null) return;
            send(row);
        }
    }

    private Delivery claimOne() {
        List<Delivery> rows = jdbc.query("select id,event_id,merchant_id,payload_json::text,endpoint_url,attempt_count from t_merchant_webhook_delivery " +
                        "where status in ('PENDING','RETRY','SENDING') and next_attempt_at<=current_timestamp and (lease_until is null or lease_until<current_timestamp) order by next_attempt_at for update skip locked limit 1",
                (rs,n)->new Delivery(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6)));
        if (rows.isEmpty()) return null;
        Delivery row=rows.getFirst();
        jdbc.update("update t_merchant_webhook_delivery set status='SENDING',lease_until=current_timestamp+interval '30 seconds',updated_at=current_timestamp where id=?",row.id());
        return row;
    }

    private void send(Delivery row) {
        int attempt=row.attemptCount()+1, status=0; String error=null; boolean success=false;
        try {
            URI uri=endpointPolicy.validateForSend(row.endpointUrl());
            ConfigRow config=config(row.merchantId());
            if(config==null) throw new IllegalStateException("Webhook configuration was removed");
            String timestamp=Long.toString(Instant.now().getEpochSecond());
            String signature=hmac(timestamp+"."+row.payload(),cipher.decrypt(config.encryptedSecret()));
            HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).header("Content-Type","application/json")
                    .header("X-HuizhiPay-Event-Id",row.eventId()).header("X-HuizhiPay-Timestamp",timestamp)
                    .header("X-HuizhiPay-Signature","v1="+signature).POST(HttpRequest.BodyPublishers.ofString(row.payload())).build();
            HttpResponse<Void> response=http.send(request,HttpResponse.BodyHandlers.discarding()); status=response.statusCode();
            success=status>=200&&status<300;
            if(!success) error="HTTP "+status;
        } catch(Exception e){ error=safeError(e); }
        final int responseStatus=status; final String finalError=error; final boolean delivered=success;
        new TransactionTemplate(transactionManager).executeWithoutResult(s->{
            jdbc.update("insert into t_merchant_webhook_attempt(delivery_id,attempt_no,response_status,outcome,error_message) values(?,?,?,?,?)",
                    row.id(),attempt,responseStatus==0?null:responseStatus,delivered?"DELIVERED":"FAILED",finalError);
            if(delivered) jdbc.update("update t_merchant_webhook_delivery set status='DELIVERED',attempt_count=?,lease_until=null,last_error=null,updated_at=current_timestamp where id=?",attempt,row.id());
            else if(attempt>=4) jdbc.update("update t_merchant_webhook_delivery set status='FAILED',attempt_count=?,lease_until=null,last_error=?,updated_at=current_timestamp where id=?",attempt,finalError,row.id());
            else jdbc.update("update t_merchant_webhook_delivery set status='RETRY',attempt_count=?,lease_until=null,last_error=?,next_attempt_at=current_timestamp+(case ? when 1 then interval '10 seconds' when 2 then interval '1 minute' else interval '5 minutes' end),updated_at=current_timestamp where id=?",attempt,finalError,attempt,row.id());
        });
    }

    private ConfigRow config(String merchantId){
        List<ConfigRow> r=jdbc.query("select endpoint_url,secret_ciphertext,enabled from t_merchant_webhook_config where merchant_id=?",(rs,n)->new ConfigRow(rs.getString(1),rs.getString(2),rs.getBoolean(3)),merchantId);
        return r.isEmpty()?null:r.getFirst();
    }
    private String hmac(String value,String secret)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
    private String randomHex(int bytes){byte[] v=new byte[bytes];new SecureRandom().nextBytes(v);return HexFormat.of().formatHex(v);}
    private String safeError(Exception e){String value=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();return value.replaceAll("[\\r\\n]"," ").substring(0,Math.min(240,value.length()));}
    public record ConfigView(String endpointUrl,boolean enabled,boolean configured,String signingSecret,java.time.LocalDateTime createdAt,java.time.LocalDateTime updatedAt){}
    private record ConfigRow(String endpointUrl,String encryptedSecret,boolean enabled){}
    private record Delivery(long id,String eventId,String merchantId,String payload,String endpointUrl,int attemptCount){}
}
