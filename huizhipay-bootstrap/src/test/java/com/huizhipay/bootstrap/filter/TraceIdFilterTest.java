package com.huizhipay.bootstrap.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void safeCallerTraceIdIsReturnedAndAvailableDuringRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "checkout_12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> traceDuringRequest.set(MDC.get(TraceIdFilter.TRACE_ID_KEY)));

        assertThat(traceDuringRequest).hasValue("checkout_12345678");
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("checkout_12345678");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
    }

    @Test
    void unsafeTraceIdIsReplacedWithServerGeneratedValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "bad value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER))
                .matches("[a-f0-9]{32}")
                .isNotEqualTo("bad value with spaces");
    }
}
