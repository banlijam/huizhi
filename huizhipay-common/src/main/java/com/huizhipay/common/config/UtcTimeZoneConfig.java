package com.huizhipay.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * 统一时区为 UTC：
 *  - JVM 默认时区设为 UTC，保证 LocalDateTime / Date 处理一致
 *  - 配合数据库 timestamptz 列，所有接口时间均以 UTC 输出
 */
@Configuration
public class UtcTimeZoneConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
