package com.huizhipay.bootstrap;

import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.service.DummyPaymentCompletionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "jwt.secret=test_dummy_jwt_secret_key_with_at_least_32_chars",
                "spring.mail.host=localhost"
        })
class DummyPaymentSandboxPostgresqlIntegrationTest {
    private static final String MERCHANT_ID = "M-20260801-DEMOHZ";

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("huizhipay_test")
            .withUsername("huizhipay")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private DummyPaymentCompletionService completionService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void successCreatesExactlyOneEventAndThreeLedgerEntriesEvenWhenReplayed() {
        String orderNo = "TEST-DUMMY-SUCCESS";
        String checkoutToken = "ct_test_dummy_success";
        insertPendingOrder(orderNo, checkoutToken);

        PaymentOrder completed = completionService.complete(checkoutToken, "SUCCESS");
        assertThat(completed.getStatus()).isEqualTo(PaymentOrder.PaymentStatus.SUCCESS);
        assertThat(count("t_ledger_entry", "biz_id", orderNo)).isEqualTo(3);
        assertThat(count("t_payment_event_log", "order_no", orderNo)).isEqualTo(1);

        completionService.complete(checkoutToken, "SUCCESS");
        assertThat(count("t_ledger_entry", "biz_id", orderNo)).isEqualTo(3);
        assertThat(count("t_payment_event_log", "order_no", orderNo)).isEqualTo(1);
    }

    @Test
    void eventConflictRollsBackOrderAndLedgerTogether() {
        String orderNo = "TEST-DUMMY-ROLLBACK";
        String checkoutToken = "ct_test_dummy_rollback";
        insertPendingOrder(orderNo, checkoutToken);
        jdbcTemplate.update("""
                insert into t_payment_event_log(order_no, merchant_id, event_type, created_at)
                values (?, ?, 'payment.succeeded', current_timestamp)
                """, orderNo, MERCHANT_ID);

        assertThatThrownBy(() -> completionService.complete(checkoutToken, "SUCCESS"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "select status from t_payment_order where order_no = ?", String.class, orderNo))
                .isEqualTo("PENDING");
        assertThat(count("t_ledger_entry", "biz_id", orderNo)).isZero();
        assertThat(count("t_payment_event_log", "order_no", orderNo)).isEqualTo(1);
    }

    private void insertPendingOrder(String orderNo, String checkoutToken) {
        jdbcTemplate.update("""
                insert into t_payment_order(
                    order_no, merchant_id, amount, currency, channel, status,
                    checkout_token, return_url, created_at, updated_at)
                values (?, ?, 100.000, 'USD', 'DUMMY', 'PENDING', ?, '/merchant',
                        current_timestamp, current_timestamp)
                """, orderNo, MERCHANT_ID, checkoutToken);
    }

    private int count(String table, String column, String value) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?", Integer.class, value);
    }
}
