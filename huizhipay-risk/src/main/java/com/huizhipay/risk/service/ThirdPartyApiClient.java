package com.huizhipay.risk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ThirdPartyApiClient {
//    private final RestTemplate restTemplate;

    @Value("${third.api.url:test}")
    private String apiUrl;

    // 纯粹的同步HTTP调用（会被异步线程池包装）
    public String fetchData(String params) {
        log.info("开始调用外部API，参数：{}", params);
//        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, params, String.class);
//        if (!response.getStatusCode().is2xxSuccessful()) {
//            throw new RuntimeException("外部API返回异常状态码：" + response.getStatusCode());
//        }
//        return response.getBody();
        return "third party";
    }
}