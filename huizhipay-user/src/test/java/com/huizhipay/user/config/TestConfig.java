package com.huizhipay.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 测试配置类
 * 用于EmailService测试的Spring Boot配置
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan("com.huizhipay")
@MapperScan({
        "com.huizhipay.acquiring.mapper",
        "com.huizhipay.ledger.mapper",
        "com.huizhipay.merchant.mapper",
        "com.huizhipay.risk.mapper",
        "com.huizhipay.settlement.mapper",
        "com.huizhipay.user.mapper"
})
@EnableAsync
public class TestConfig {
}