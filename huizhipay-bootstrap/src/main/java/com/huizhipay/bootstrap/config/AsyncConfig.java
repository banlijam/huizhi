package com.huizhipay.bootstrap.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Application-wide async executor. It lives in bootstrap because it affects every module.
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("HuizhiPay-Async-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::logUncaughtAsyncFailure;
    }

    private void logUncaughtAsyncFailure(Throwable error, Method method, Object... params) {
        log.error("Uncaught async failure method={}, traceId={}",
                method.getDeclaringClass().getSimpleName() + "." + method.getName(),
                MDC.get("traceId"),
                error);
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> callerContext = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();
                try {
                    if (callerContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(callerContext);
                    }
                    runnable.run();
                } finally {
                    if (previousContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContext);
                    }
                }
            };
        };
    }
}
