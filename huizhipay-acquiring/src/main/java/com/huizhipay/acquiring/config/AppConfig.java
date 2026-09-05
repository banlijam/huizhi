package com.huizhipay.acquiring.config;

import com.huizhipay.acquiring.transfi.TransFiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    @Value("${client.transfi.url}")
    private String transfiUrl;

    @Value("${client.transfi.mid}")
    private String transfiMid;

    @Value("${client.transfi.authorization}")
    private String transfiAuthorization;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(transfiUrl)
                .defaultHeader("MID", transfiMid)
                .defaultHeader("accept", "application/json")
                .defaultHeader("authorization", transfiAuthorization)
                .build();
    }

    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory(RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter).build();
    }

    @Bean
    public TransFiClient transFiClient(HttpServiceProxyFactory factory) {
        return factory.createClient(TransFiClient.class);
    }
}
