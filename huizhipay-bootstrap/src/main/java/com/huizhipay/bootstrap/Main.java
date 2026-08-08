package com.huizhipay.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.huizhipay")
@EnableScheduling
@EnableAsync
@MapperScan({
        "com.huizhipay.acquiring.mapper",
        "com.huizhipay.ledger.mapper",
        "com.huizhipay.merchant.mapper",
        "com.huizhipay.risk.mapper",
        "com.huizhipay.settlement.mapper",
        "com.huizhipay.user.mapper"
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}