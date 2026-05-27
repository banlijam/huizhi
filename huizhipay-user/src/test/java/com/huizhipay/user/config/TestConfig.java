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
@MapperScan("com.huizhipay")
@EnableAsync
public class TestConfig {
}