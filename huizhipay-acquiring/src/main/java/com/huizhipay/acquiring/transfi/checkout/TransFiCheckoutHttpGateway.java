package com.huizhipay.acquiring.transfi.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TransFiCheckoutHttpGateway implements TransFiCheckoutGateway {
    static final String PATH = "/checkout/payment-link/invoice";
    private final ObjectMapper objectMapper;
    private final RestClient client;
    private final Clock clock;
    private final boolean enabled;
    private final String publicKey;
    private final String secretKey;
    private final String paymentLinkId;

    @Autowired
    public TransFiCheckoutHttpGateway(ObjectMapper objectMapper,
            @Value("${huizhipay.transfi.checkout.base-url:https://checkout-server.transfi.com}") String baseUrl,
            @Value("${huizhipay.transfi.checkout.outbound-enabled:false}") boolean enabled,
            @Value("${huizhipay.transfi.checkout.public-key:}") String publicKey,
            @Value("${huizhipay.transfi.checkout.secret-key:}") String secretKey,
            @Value("${huizhipay.transfi.checkout.payment-link-id:}") String paymentLinkId) {
        this(objectMapper, RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build(), Clock.systemUTC(), enabled,
                publicKey, secretKey, paymentLinkId);
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return factory;
    }

    TransFiCheckoutHttpGateway(ObjectMapper objectMapper, RestClient client, Clock clock, boolean enabled,
                              String publicKey, String secretKey, String paymentLinkId) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.clock = clock;
        this.enabled = enabled;
        this.publicKey = publicKey;
        this.secretKey = secretKey;
        this.paymentLinkId = paymentLinkId;
    }

    @Override
    public Result createInvoice(Command command) {
        if (!enabled) throw new IllegalStateException("TransFi Checkout outbound calls are disabled");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("paymentLinkId", paymentLinkId);
            body.put("amount", command.amount().stripTrailingZeros().toPlainString());
            body.put("currency", command.currency());
            body.put("productDetails", Map.of("name", command.productName(), "reference", command.platformOrderNo()));
            body.put("individual", Map.of());
            body.put("successRedirectUrl", command.successRedirectUrl());
            body.put("failureRedirectUrl", command.failureRedirectUrl());
            body.put("customerOrderId", command.merchantOrderNo());
            String rawBody = objectMapper.writeValueAsString(body);
            String timestamp = Long.toString(clock.millis());
            String signature = hmac("POST" + PATH + timestamp + rawBody, secretKey);
            String rawResponse = client.post().uri(PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", publicKey)
                    .header("x-timestamp", timestamp)
                    .header("x-signature", signature)
                    .header("X-Api-Version", "v1")
                    .body(rawBody).retrieve().body(String.class);
            JsonNode response = objectMapper.readTree(rawResponse);
            String paymentUrl = firstText(response, "paymentUrl", "paymentLink", "checkoutUrl", "url");
            String channelOrderId = firstText(response, "invoiceId", "transactionId", "orderId", "id");
            if (paymentUrl == null || !paymentUrl.startsWith("https://")) {
                throw new IllegalStateException("TransFi Checkout response omitted an HTTPS payment URL");
            }
            return new Result(channelOrderId, paymentUrl);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("TransFi Checkout request failed", e);
        }
    }

    private String firstText(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode found = root.findValue(name);
            if (found != null && found.isTextual() && !found.textValue().isBlank()) return found.textValue();
        }
        return null;
    }

    private String hmac(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }
}
