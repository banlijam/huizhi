package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.acquiring.transfi.checkout.TransFiCheckoutGateway;
import com.huizhipay.common.exceptions.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestPaymentService {
    public static final String CHANNEL = "TRANSFI_CHECKOUT";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000.00");
    private final PaymentOrderMapper paymentOrderMapper;
    private final TransFiCheckoutGateway checkoutGateway;
    private final MerchantRedirectOriginService redirectOrigins;
    private final DummyPaymentPolicy dummyPaymentPolicy;
    @Value("${app.frontend.url:http://127.0.0.1:3000}")
    private String frontendUrl;

    public PaymentView create(String merchantId, CreateCommand command) {
        validate(merchantId, command);
        String merchantOrderNo = command.merchantOrderNo().trim();
        BigDecimal amount = command.amount().setScale(2, RoundingMode.UNNECESSARY);
        String fingerprint = fingerprint(command, amount);
        PaymentOrder existing = find(merchantId, merchantOrderNo);
        if (existing != null) return sameRequestOrConflict(existing, fingerprint);

        LocalDateTime now = LocalDateTime.now();
        PaymentOrder order = new PaymentOrder()
                .setOrderNo("TFI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT))
                .setCheckoutToken("ct_" + UUID.randomUUID().toString().replace("-", ""))
                .setReturnUrl(command.successRedirectUrl())
                .setMerchantId(merchantId)
                .setMerchantOrderNo(merchantOrderNo)
                .setAmount(amount)
                .setCurrency("USD")
                .setChannel(dummyPaymentPolicy.isEnabled() ? "DUMMY" : CHANNEL)
                .setFingerprint(fingerprint)
                .setStatus(PaymentOrder.PaymentStatus.PENDING)
                .setChannelStatus(dummyPaymentPolicy.isEnabled() ? "INITIATED" : "CREATING")
                .setRemark(dummyPaymentPolicy.isEnabled() ? "Dummy checkout created through Test API" : "TransFi Checkout invoice creation started")
                .setCreatedAt(now).setUpdatedAt(now);
        try {
            paymentOrderMapper.insert(order);
        } catch (DuplicateKeyException race) {
            PaymentOrder winner = find(merchantId, merchantOrderNo);
            if (winner == null) throw race;
            return sameRequestOrConflict(winner, fingerprint);
        }

        if (dummyPaymentPolicy.isEnabled()) {
            String hppBase = frontendUrl == null ? "http://127.0.0.1:3000" : frontendUrl.replaceAll("/+$", "");
            order.setPaymentUrl(hppBase + "/pay/?checkoutToken=" + order.getCheckoutToken());
            paymentOrderMapper.updateById(order);
            return toView(order);
        }

        try {
            TransFiCheckoutGateway.Result result = checkoutGateway.createInvoice(new TransFiCheckoutGateway.Command(
                    order.getOrderNo(), merchantOrderNo, amount, "USD", command.successRedirectUrl(),
                    command.failureRedirectUrl(), command.productName().trim()));
            order.setChannelTradeNo(result.channelOrderId())
                    .setPaymentUrl(result.paymentUrl())
                    .setChannelStatus("INITIATED")
                    .setRemark("TransFi Checkout invoice created")
                    .setUpdatedAt(LocalDateTime.now());
            paymentOrderMapper.updateById(order);
        } catch (RuntimeException uncertain) {
            order.setChannelStatus("PENDING_CONFIRMATION")
                    .setRemark("Channel result uncertain; do not retry automatically")
                    .setUpdatedAt(LocalDateTime.now());
            paymentOrderMapper.updateById(order);
        }
        return toView(order);
    }

    public PaymentView get(String merchantId, String merchantOrderNo) {
        PaymentOrder order = find(merchantId, merchantOrderNo);
        if (order == null) throw new BizException(404, "Test payment not found");
        return toView(order);
    }

    private PaymentView sameRequestOrConflict(PaymentOrder existing, String fingerprint) {
        if (!fingerprint.equals(existing.getFingerprint())) {
            throw new BizException(409, "merchantOrderNo was already used with different payment details");
        }
        return toView(existing);
    }

    private PaymentOrder find(String merchantId, String merchantOrderNo) {
        return paymentOrderMapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
                .eq(PaymentOrder::getMerchantId, merchantId)
                .eq(PaymentOrder::getMerchantOrderNo, merchantOrderNo));
    }

    private void validate(String merchantId, CreateCommand command) {
        if (command == null || command.merchantOrderNo() == null
                || !command.merchantOrderNo().trim().matches("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$")) {
            throw new BizException(400, "merchantOrderNo must be 1-64 safe characters");
        }
        if (command.amount() == null || command.amount().signum() <= 0
                || command.amount().compareTo(MAX_AMOUNT) > 0 || command.amount().scale() > 2) {
            throw new BizException(400, "amount must be USD 0.01-10000.00 with at most 2 decimals");
        }
        if (!"USD".equalsIgnoreCase(command.currency())) {
            throw new BizException(400, "TransFi Checkout test integration currently supports USD only");
        }
        if (command.productName() == null || command.productName().isBlank() || command.productName().length() > 120) {
            throw new BizException(400, "productName is required and limited to 120 characters");
        }
        redirectOrigins.requireAllowed(merchantId, "TEST", command.successRedirectUrl());
        redirectOrigins.requireAllowed(merchantId, "TEST", command.failureRedirectUrl());
    }

    private String fingerprint(CreateCommand command, BigDecimal amount) {
        String canonical = amount.toPlainString() + "|USD|" + command.productName().trim() + "|"
                + command.successRedirectUrl() + "|" + command.failureRedirectUrl();
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private PaymentView toView(PaymentOrder order) {
        return new PaymentView(order.getOrderNo(), order.getMerchantOrderNo(), order.getAmount(), order.getCurrency(),
                order.getStatus().name(), order.getChannelStatus(), order.getChannelTradeNo(), order.getPaymentUrl(),
                order.getCreatedAt(), order.getUpdatedAt());
    }

    public record CreateCommand(String merchantOrderNo, BigDecimal amount, String currency,
                                String productName, String successRedirectUrl, String failureRedirectUrl) {}
    public record PaymentView(String platformOrderNo, String merchantOrderNo, BigDecimal amount, String currency,
                              String status, String channelStatus, String channelOrderId, String paymentUrl,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {}
}
