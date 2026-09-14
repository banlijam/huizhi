package com.huizhipay.bootstrap;

import com.huizhipay.acquiring.service.TransFiCheckoutWebhookService;
import com.huizhipay.acquiring.service.MerchantWebhookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named="HUIZHIPAY_LOCAL_PG_TEST",matches="true")
@SpringBootTest(classes=Main.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={
        "spring.profiles.active=local",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:15432/huizhipay_local",
        "spring.datasource.username=huizhipay",
        "spring.datasource.password=",
        "huizhipay.transfi.checkout.webhook-enabled=true",
        "huizhipay.transfi.checkout.webhook-secret=task3-local-secret",
        "huizhipay.webhooks.master-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "huizhipay.webhooks.worker-delay-ms=3600000",
        "huizhipay.transfi.checkout.webhook-recovery-delay-ms=3600000"
})
class TransFiWebhookTestEnvironmentPostgresqlIntegrationTest {
    private static final String MERCHANT="M-TASK3-TEST";
    @Autowired JdbcTemplate jdbc;
    @Autowired TransFiCheckoutWebhookService service;
    @Autowired MerchantWebhookService merchantWebhooks;

    @AfterEach void cleanup(){
        jdbc.update("delete from t_merchant_webhook_attempt where delivery_id in(select id from t_merchant_webhook_delivery where merchant_id=?)",MERCHANT);
        jdbc.update("delete from t_merchant_webhook_delivery where merchant_id=?",MERCHANT);
        jdbc.update("delete from t_merchant_webhook_config where merchant_id=?",MERCHANT);
        jdbc.update("delete from t_transfi_checkout_webhook where event_id like 'EV-TASK3-%'");
        jdbc.update("delete from t_payment_event_log where merchant_id=?",MERCHANT);
        jdbc.update("delete from t_ledger_entry where biz_id like 'TFI-TASK3-%'");
        jdbc.update("delete from t_payment_order where merchant_id=?",MERCHANT);
        jdbc.update("delete from t_account where merchant_id in (?, '__PLATFORM__') and account_no like 'TASK3-%'",MERCHANT);
    }

    @Test void successCommitsOrderLedgerInboundEventAndOutboxOnceAndRejectsRegression() throws Exception {
        accounts();
        jdbc.update("insert into t_merchant_webhook_config(merchant_id,endpoint_url,secret_ciphertext,enabled) values(?, 'https://merchant-test.example.test/webhooks/huizhipay', 'unused-in-this-test', true)",MERCHANT);
        jdbc.update("insert into t_payment_order(order_no,checkout_token,merchant_order_no,merchant_id,amount,currency,channel,channel_trade_no,status,created_at,updated_at) values('TFI-TASK3-OK','ct_task3_ok','SHOP-TASK3-OK',?,12.00,'TST','TRANSFI_CHECKOUT','OR-TASK3-OK','PENDING',current_timestamp,current_timestamp)",MERCHANT);
        byte[] success=payload("EV-TASK3-OK","fund_settled","OR-TASK3-OK","SHOP-TASK3-OK","12.00");
        assertThat(service.receive(success)).isEqualTo(TransFiCheckoutWebhookService.Result.PROCESSED);
        assertThat(service.receive(success)).isEqualTo(TransFiCheckoutWebhookService.Result.DUPLICATE);
        assertThat(jdbc.queryForObject("select status from t_payment_order where order_no='TFI-TASK3-OK'",String.class)).isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("select count(*) from t_ledger_entry where biz_id='TFI-TASK3-OK'",Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from t_payment_event_log where order_no='TFI-TASK3-OK'",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from t_merchant_webhook_delivery where merchant_id=?",Integer.class,MERCHANT)).isEqualTo(1);
        assertThat(service.receive(payload("EV-TASK3-REGRESSION","fund_failed","OR-TASK3-OK","SHOP-TASK3-OK","12.00"))).isEqualTo(TransFiCheckoutWebhookService.Result.MANUAL_REVIEW);
        assertThat(jdbc.queryForObject("select status from t_payment_order where order_no='TFI-TASK3-OK'",String.class)).isEqualTo("SUCCESS");
    }

    @Test void mismatchAndUnknownOrderPersistForReviewWithoutCrossMerchantMutation() throws Exception {
        jdbc.update("insert into t_merchant_webhook_config(merchant_id,endpoint_url,secret_ciphertext,enabled) values(?, 'https://merchant-test.example.test/webhooks/huizhipay', 'unused-in-this-test', true)",MERCHANT);
        jdbc.update("insert into t_payment_order(order_no,checkout_token,merchant_order_no,merchant_id,amount,currency,channel,channel_trade_no,status,created_at,updated_at) values('TFI-TASK3-MISMATCH','ct_task3_mismatch','SHOP-TASK3-MISMATCH',?,12.00,'TST','TRANSFI_CHECKOUT','OR-TASK3-MISMATCH','PENDING',current_timestamp,current_timestamp)",MERCHANT);
        jdbc.update("insert into t_payment_order(order_no,checkout_token,merchant_order_no,merchant_id,amount,currency,channel,channel_trade_no,status,created_at,updated_at) values('TFI-TASK3-FAIL','ct_task3_fail','SHOP-TASK3-FAIL',?,12.00,'TST','TRANSFI_CHECKOUT','OR-TASK3-FAIL','PENDING',current_timestamp,current_timestamp)",MERCHANT);
        assertThat(service.receive(payload("EV-TASK3-MISMATCH","fund_settled","OR-TASK3-MISMATCH","SHOP-TASK3-MISMATCH","13.00"))).isEqualTo(TransFiCheckoutWebhookService.Result.MANUAL_REVIEW);
        assertThat(service.receive(payload("EV-TASK3-EARLY","fund_settled","OR-NOT-LINKED","SHOP-NOT-LINKED","12.00"))).isEqualTo(TransFiCheckoutWebhookService.Result.PENDING);
        assertThat(jdbc.queryForObject("select status from t_payment_order where order_no='TFI-TASK3-MISMATCH'",String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select status from t_transfi_checkout_webhook where event_id='EV-TASK3-EARLY'",String.class)).isEqualTo("WAITING_LINK");
        assertThat(service.receive(payload("EV-TASK3-FAIL","fund_failed","OR-TASK3-FAIL","SHOP-TASK3-FAIL","12.00"))).isEqualTo(TransFiCheckoutWebhookService.Result.PROCESSED);
        assertThat(jdbc.queryForObject("select status from t_payment_order where order_no='TFI-TASK3-FAIL'",String.class)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select count(*) from t_ledger_entry where biz_id='TFI-TASK3-FAIL'",Integer.class)).isZero();
    }

    @Test void failedDeliveryKeepsStableEventAndExpiredLeaseIsRecovered() {
        jdbc.update("insert into t_merchant_webhook_config(merchant_id,endpoint_url,secret_ciphertext,enabled) values(?, 'https://merchant-test.example.test/webhooks/huizhipay', 'not-reached-because-test-domain-has-no-dns', true)",MERCHANT);
        String eventId=merchantWebhooks.enqueueTest(MERCHANT);
        merchantWebhooks.deliverDue();
        assertThat(jdbc.queryForObject("select status from t_merchant_webhook_delivery where event_id=?",String.class,eventId)).isEqualTo("RETRY");
        assertThat(jdbc.queryForObject("select attempt_count from t_merchant_webhook_delivery where event_id=?",Integer.class,eventId)).isEqualTo(1);
        jdbc.update("update t_merchant_webhook_delivery set status='SENDING',lease_until=current_timestamp-interval '1 second',next_attempt_at=current_timestamp where event_id=?",eventId);
        merchantWebhooks.deliverDue();
        assertThat(jdbc.queryForObject("select count(*) from t_merchant_webhook_attempt a join t_merchant_webhook_delivery d on d.id=a.delivery_id where d.event_id=?",Integer.class,eventId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select event_id from t_merchant_webhook_delivery where event_id=?",String.class,eventId)).isEqualTo(eventId);
    }

    private byte[] payload(String eventId,String status,String channelOrder,String merchantOrder,String amount){return ("{\"eventId\":\""+eventId+"\",\"entityId\":\""+channelOrder+"\",\"entityType\":\"order\",\"status\":\""+status+"\",\"order\":{\"orderId\":\""+channelOrder+"\",\"customerOrderId\":\""+merchantOrder+"\",\"depositAmount\":"+amount+",\"depositCurrency\":\"TST\"}}").getBytes(StandardCharsets.UTF_8);}
    private void accounts(){
        jdbc.update("insert into t_account(account_no,merchant_id,account_type,currency,balance) values('TASK3-ASSET',?,'ASSET_AVAILABLE','TST',0),('TASK3-LIABILITY',?,'LIABILITY_CUSTODY','TST',0),('TASK3-PLATFORM','__PLATFORM__','PLATFORM_INCOME','TST',0)",MERCHANT,MERCHANT);
    }
}
