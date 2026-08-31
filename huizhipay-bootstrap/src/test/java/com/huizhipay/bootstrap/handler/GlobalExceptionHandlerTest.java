package com.huizhipay.bootstrap.handler;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.model.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(mock(MessageSource.class));
        MDC.put("traceId", "request_12345678");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void businessAuthorizationFailureUsesRealHttp403AndCarriesTraceId() {
        ResponseEntity<R<?>> response = handler.handleBiz(new BizException(403, "Forbidden"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
        assertThat(response.getBody().getTraceId()).isEqualTo("request_12345678");
    }

    @Test
    void badCredentialsUseHttp401InsteadOfTransportSuccess() {
        ResponseEntity<R<?>> response = handler.handleBadCredentials(new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getCode()).isEqualTo(401);
    }
}
