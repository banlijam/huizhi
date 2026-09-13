package com.huizhipay.acquiring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huizhipay.ledger.service.LedgerTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransFiCheckoutWebhookService {
    private static final String CHANNEL="TRANSFI_CHECKOUT";
    private static final Set<String> SUCCESS_TERMINALS=Set.of("FUND_SETTLED","ASSET_SETTLED","SUCCEEDED","SUCCESS","COMPLETED","PAID","PAYMENT_SUCCEEDED");
    private static final Set<String> FAILURE_TERMINALS=Set.of("FUND_FAILED","FAILED","CANCELLED","CANCELED","EXPIRED","PAYMENT_FAILED");
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final LedgerTransferService ledger;
    private final MerchantWebhookService merchantWebhooks;
    @Value("${huizhipay.transfi.checkout.webhook-secret:}") private String webhookSecret;
    @Value("${huizhipay.transfi.checkout.webhook-enabled:false}") private boolean enabled;

    public boolean signatureValid(byte[] rawBody,String signature){
        if(!enabled||webhookSecret==null||webhookSecret.isBlank()||signature==null||signature.isBlank()) return false;
        try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            byte[] expected=mac.doFinal(rawBody),provided=HexFormat.of().parseHex(signature.trim());return MessageDigest.isEqual(expected,provided);
        }catch(Exception e){return false;}
    }
    public boolean enabled(){return enabled&&webhookSecret!=null&&!webhookSecret.isBlank();}

    @Transactional(rollbackFor=Exception.class)
    public Result receive(byte[] rawBody) throws Exception {
        JsonNode root=objectMapper.readTree(rawBody);
        String eventId=text(root,"eventId","event_id","id");
        if(eventId==null||eventId.isBlank()||eventId.length()>128) return Result.INVALID;
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawBody));
        String prior=jdbc.query("select payload_sha256 from t_transfi_checkout_webhook where event_id=?",rs->rs.next()?rs.getString(1):null,eventId);
        if(prior!=null){
            if(prior.equals(hash)) return Result.DUPLICATE;
            jdbc.update("update t_transfi_checkout_webhook set status='CONFLICT',diagnostic='same event id with different payload',updated_at=current_timestamp where event_id=?",eventId);
            return Result.CONFLICT;
        }
        String eventType=text(root,"eventType","event_type","type");
        JsonNode data=root.has("data")?root.path("data"):root;
        String channelOrder=text(data,"invoiceId","transactionId","paymentId","orderId","entityId","id");
        String platformOrder=text(data,"platformOrderNo","platform_order_no","reference");
        if(platformOrder==null) platformOrder=text(root,"platformOrderNo","reference");
        String merchantOrder=text(data,"customerOrderId","merchantOrderNo","orderNo");
        String status=text(data,"status","paymentStatus","state"); if(status==null)status=text(root,"status");
        BigDecimal amount=decimal(data,"amount","orderAmount","fiatAmount","depositAmount");
        String currency=text(data,"currency","fiatCurrency","depositCurrency");
        jdbc.update("insert into t_transfi_checkout_webhook(event_id,payload_sha256,event_type,channel_order_id,platform_order_no,merchant_order_no,amount,currency,channel_status,status,diagnostic) values(?,?,?,?,?,?,?,?,?,?,?)",
                eventId,hash,eventType,channelOrder,platformOrder,merchantOrder,amount,currency,status,"RECEIVED",null);
        return apply(eventId,eventType,channelOrder,platformOrder,merchantOrder,status,amount,currency);
    }

    private Result apply(String eventId,String eventType,String channelOrder,String platformOrder,String merchantOrder,String channelStatus,BigDecimal amount,String currency){
        List<Order> rows=jdbc.query("select id,order_no,merchant_order_no,merchant_id,amount,currency,status,channel_trade_no from t_payment_order where channel=? and deleted=0 and (channel_trade_no=? or order_no=? or merchant_order_no=?)",
                (rs,n)->new Order(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBigDecimal(5),rs.getString(6),rs.getString(7),rs.getString(8)),CHANNEL,channelOrder,platformOrder,merchantOrder);
        if(rows.size()!=1){mark(eventId,rows.isEmpty()?"WAITING_LINK":"MANUAL_REVIEW",rows.isEmpty()?"order association not yet available":"ambiguous order association");return rows.isEmpty()?Result.PENDING:Result.MANUAL_REVIEW;}
        Order order=rows.getFirst();
        if((platformOrder!=null&&!platformOrder.equals(order.orderNo()))||(merchantOrder!=null&&!merchantOrder.equals(order.merchantOrderNo()))
                ||(channelOrder!=null&&order.channelTradeNo()!=null&&!channelOrder.equals(order.channelTradeNo()))){mark(eventId,"MANUAL_REVIEW","order identifier mismatch");return Result.MANUAL_REVIEW;}
        if(amount==null||currency==null||amount.compareTo(order.amount())!=0||!currency.equalsIgnoreCase(order.currency())){mark(eventId,"MANUAL_REVIEW","amount or currency mismatch");return Result.MANUAL_REVIEW;}
        Terminal terminal=terminal(eventType,channelStatus);
        if(terminal==null){mark(eventId,"MANUAL_REVIEW","unrecognized Checkout event or non-terminal state");return Result.MANUAL_REVIEW;}
        if("SUCCESS".equals(order.status())){
            if(terminal==Terminal.SUCCESS){mark(eventId,"PROCESSED","duplicate successful terminal state");return Result.DUPLICATE;}
            mark(eventId,"MANUAL_REVIEW","terminal state conflicts with successful order");return Result.MANUAL_REVIEW;
        }
        if("FAILED".equals(order.status())){
            if(terminal==Terminal.FAILED){mark(eventId,"PROCESSED","duplicate failed terminal state");return Result.DUPLICATE;}
            mark(eventId,"MANUAL_REVIEW","terminal state conflicts with failed order");return Result.MANUAL_REVIEW;
        }
        String newStatus=terminal==Terminal.SUCCESS?"SUCCESS":"FAILED",outType=terminal==Terminal.SUCCESS?"payment.succeeded":"payment.failed";
        int changed=jdbc.update("update t_payment_order set status=?,channel_status=?,channel_trade_no=coalesce(channel_trade_no,?),remark=?,updated_at=current_timestamp,version=version+1 where id=? and status='PENDING'",
                newStatus,channelStatus,channelOrder,"Authoritative TransFi Checkout webhook",order.id());
        if(changed!=1){mark(eventId,"WAITING_RETRY","concurrent order update");return Result.PENDING;}
        if(terminal==Terminal.SUCCESS) ledger.payment(order.merchantId(),order.currency(),order.amount(),order.orderNo(),CHANNEL,channelOrder==null?eventId:channelOrder);
        jdbc.update("insert into t_payment_event_log(order_no,merchant_id,event_type,transaction_id) values(?,?,?,?) on conflict(order_no,event_type) do nothing",order.orderNo(),order.merchantId(),outType,channelOrder);
        merchantWebhooks.enqueue(order.merchantId(),order.orderNo(),outType,Map.of("platformOrderNo",order.orderNo(),"merchantOrderNo",order.merchantOrderNo(),"amount",order.amount(),"currency",order.currency(),"status",newStatus));
        mark(eventId,"PROCESSED","order and outbox committed"); return Result.PROCESSED;
    }

    @Scheduled(fixedDelayString="${huizhipay.transfi.checkout.webhook-recovery-delay-ms:30000}")
    @Transactional(rollbackFor=Exception.class)
    public void recoverUnlinked(){
        jdbc.query("select event_id,event_type,channel_order_id,platform_order_no,merchant_order_no,channel_status,amount,currency from t_transfi_checkout_webhook where status in ('WAITING_LINK','WAITING_RETRY') and updated_at<current_timestamp-interval '5 seconds' order by received_at limit 20",
                rs->{while(rs.next()){String id=rs.getString(1),type=rs.getString(2),channel=rs.getString(3),platform=rs.getString(4),merchant=rs.getString(5),state=rs.getString(6),currency=rs.getString(8);BigDecimal amount=rs.getBigDecimal(7);apply(id,type,channel,platform,merchant,state,amount,currency);}return null;});
    }
    private void mark(String id,String status,String diagnostic){jdbc.update("update t_transfi_checkout_webhook set status=?,diagnostic=?,processed_at=case when ?='PROCESSED' then current_timestamp else processed_at end,updated_at=current_timestamp where event_id=?",status,diagnostic,status,id);}
    private String text(JsonNode node,String...names){for(String name:names){JsonNode v=node.findValue(name);if(v!=null&&v.isValueNode()&&!v.asText().isBlank())return v.asText();}return null;}
    private BigDecimal decimal(JsonNode node,String...names){String value=text(node,names);try{return value==null?null:new BigDecimal(value);}catch(Exception e){return null;}}
    private Terminal terminal(String eventType,String status){String type=normalize(eventType),state=normalize(status);if(SUCCESS_TERMINALS.contains(state)||SUCCESS_TERMINALS.contains(type))return Terminal.SUCCESS;if(FAILURE_TERMINALS.contains(state)||FAILURE_TERMINALS.contains(type))return Terminal.FAILED;return null;}
    private String normalize(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replace('.','_').replace('-','_');}
    private enum Terminal{SUCCESS,FAILED}
    private record Order(long id,String orderNo,String merchantOrderNo,String merchantId,BigDecimal amount,String currency,String status,String channelTradeNo){}
    public enum Result{PROCESSED,DUPLICATE,PENDING,MANUAL_REVIEW,CONFLICT,INVALID}
}
