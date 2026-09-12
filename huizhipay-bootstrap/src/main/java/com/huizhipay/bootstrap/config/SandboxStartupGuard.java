package com.huizhipay.bootstrap.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Fails closed when the sandbox profile is pointed outside the local sandbox boundary. */
@Component
@Profile("sandbox")
public class SandboxStartupGuard implements ApplicationRunner {
    private final Environment environment;

    public SandboxStartupGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String serverAddress = required("server.address").toLowerCase(Locale.ROOT);
        if (!serverAddress.equals("127.0.0.1") && !serverAddress.equals("localhost")) {
            throw new IllegalStateException("Task 1 sandbox must listen on loopback only");
        }
        String datasourceUrl = required("spring.datasource.url").toLowerCase(Locale.ROOT);
        if (!datasourceUrl.matches("^jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/[^?]*sandbox(?:[?].*)?$")) {
            throw new IllegalStateException("Sandbox profile requires a loopback PostgreSQL database whose name ends with sandbox");
        }
        String mailHost = required("spring.mail.host").toLowerCase(Locale.ROOT);
        if (!mailHost.equals("127.0.0.1") && !mailHost.equals("localhost")) {
            throw new IllegalStateException("Sandbox profile requires a loopback SMTP capture service");
        }
        String transfiUrl = required("client.transfi.url").toLowerCase(Locale.ROOT);
        if (!transfiUrl.startsWith("https://sandbox-api.transfi.com/")) {
            throw new IllegalStateException("Sandbox profile requires the TransFi sandbox endpoint");
        }
        if (environment.getProperty("huizhipay.transfi.outbound-enabled", Boolean.class, true)) {
            throw new IllegalStateException("Task 1 sandbox requires TransFi outbound calls to remain disabled");
        }
    }

    private String required(String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Sandbox profile requires property: " + name);
        }
        return value.trim();
    }
}
