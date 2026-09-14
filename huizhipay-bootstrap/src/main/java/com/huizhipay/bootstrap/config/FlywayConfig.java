package com.huizhipay.bootstrap.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway 配置类
 * 通过手动注册 Flyway Bean 确保应用启动时执行数据库迁移
 * Spring Boot 自动配置的 DataSource（HikariCP）会自动注入
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        FluentConfiguration config = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .schemas("public")
                .table("flyway_schema_history")
                .validateOnMigrate(false)
                .cleanDisabled(false)
                .outOfOrder(false);
        
        return config.load();
    }
}
