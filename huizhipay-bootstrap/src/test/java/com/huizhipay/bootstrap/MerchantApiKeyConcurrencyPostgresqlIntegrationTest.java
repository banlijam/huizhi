package com.huizhipay.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes=Main.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={"spring.profiles.active=test","jwt.secret=test_dummy_jwt_secret_key_with_at_least_32_chars","spring.mail.host=localhost"})
class MerchantApiKeyConcurrencyPostgresqlIntegrationTest {
 @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("huizhipay_test").withUsername("huizhipay").withPassword("test");
 @DynamicPropertySource static void datasource(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired JdbcTemplate jdbc;
 @Test void databaseAllowsOnlyOneActiveKeyPerMerchantAndEnvironment()throws Exception{
  jdbc.update("delete from t_merchant_api_key where merchant_id='M-CONCURRENT'");
  ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
  Callable<Boolean> insert=()->{start.await();try{jdbc.update("insert into t_merchant_api_key(merchant_id,key_prefix,key_hash,environment,status,key_name,scopes,enabled,created_at) values('M-CONCURRENT',?,?,'TEST','ACTIVE','Concurrent','payments:write',true,current_timestamp)","hzp_test_concurrent",java.util.UUID.randomUUID().toString().replace("-","")+"0".repeat(32));return true;}catch(Exception e){return false;}};
  Future<Boolean> a=pool.submit(insert),b=pool.submit(insert);start.countDown();
  assertThat(java.util.List.of(a.get(),b.get())).containsExactlyInAnyOrder(true,false);
  assertThat(jdbc.queryForObject("select count(*) from t_merchant_api_key where merchant_id='M-CONCURRENT' and environment='TEST' and enabled=true",Integer.class)).isEqualTo(1);
  pool.shutdownNow();
 }
}
