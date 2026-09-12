package com.huizhipay.acquiring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huizhipay.acquiring.transfi.TransFiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper transFiWebhookObjectMapper() {
        return new ObjectMapper();
    }

    @Value("${client.transfi.url}")
    private String transfiUrl;

    @Value("${client.transfi.mid}")
    private String transfiMid;

    @Value("${client.transfi.authorization}")
    private String transfiAuthorization;

    @Value("${huizhipay.transfi.outbound-enabled:true}")
    private boolean transfiOutboundEnabled;

    @Bean
    public RestClient restClient() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(transfiUrl)
                .defaultHeader("MID", transfiMid)
                .defaultHeader("accept", "application/json")
                .defaultHeader("authorization", transfiAuthorization);
        if (!transfiOutboundEnabled) {
            builder.requestInterceptor((request, body, execution) -> {
                throw new IllegalStateException("TransFi outbound calls are disabled for this environment");
            });
        }
        return builder.build();
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
