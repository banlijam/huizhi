package com.huizhipay.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * 定义全局 API 文档元信息（替代原来在 Docket 中配置的 apiInfo）
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("绘智付 (HuiZhiPay) API 文档")
                        .version("1.0.0")
                        .description("聚合支付系统接口文档，涵盖收单、账本、物流、结算四大领域")
                        .contact(new Contact()
                                .name("技术团队")
                                .email("dev@huizhipay.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                );
    }

    @Bean
    public GroupedOpenApi allModulesApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")          // 分组名称，在 Swagger UI 下拉栏显示
                .packagesToScan("com.huizhipay") // 扫描该包及所有子包下的 @Controller/@RestController
                .build();
    }
}