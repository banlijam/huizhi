package com.huizhipay.acquiring.service;

import com.huizhipay.common.exceptions.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WebhookEndpointPolicy {
    private final Set<String> allowedHosts;

    public WebhookEndpointPolicy(@Value("${huizhipay.webhooks.allowed-hosts:merchant-sandbox.example.test}") String hosts) {
        allowedHosts = Arrays.stream(hosts.split(",")).map(String::trim).map(String::toLowerCase)
                .filter(s -> !s.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    public URI validateConfiguration(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !allowedHosts.contains(uri.getHost().toLowerCase())) throw new IllegalArgumentException();
            return uri;
        } catch (RuntimeException e) { throw new BizException(400, "Webhook endpoint must be an approved HTTPS host on port 443"); }
    }

    public URI validateForSend(String value) {
        URI uri = validateConfiguration(value);
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress() || isMetadata(address)) {
                    throw new IllegalStateException("Webhook endpoint resolved to a forbidden network");
                }
            }
            return uri;
        } catch (java.net.UnknownHostException e) { throw new IllegalStateException("Webhook endpoint DNS resolution failed"); }
    }

    private boolean isMetadata(InetAddress address) {
        byte[] b = address.getAddress();
        return b.length == 4 && (b[0] & 255) == 169 && (b[1] & 255) == 254;
    }
}
