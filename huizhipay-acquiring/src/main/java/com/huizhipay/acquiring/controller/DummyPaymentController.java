package com.huizhipay.acquiring.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dummy/orders")
@RequiredArgsConstructor
public class DummyPaymentController {
    private static final String DUMMY_MERCHANT = "M-DUMMY";
    private static final String DUMMY_CHANNEL = "DUMMY";
    private static final int PAGE_SIZE = 7;
    private final PaymentOrderMapper paymentOrderMapper;

    @PostMapping
    public R<OrderView> create(@RequestBody CreateOrderRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BizException(400, "Dummy amount must be greater than zero");
        }
        BigDecimal amount = request.amount();
        String currency = request.currency() == null || request.currency().isBlank()
                ? "USD" : request.currency().trim().toUpperCase(Locale.ROOT);
        String orderNo = "DUMMY-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        PaymentOrder order = new PaymentOrder()
                .setOrderNo(orderNo)
                .setMerchantId(DUMMY_MERCHANT)
                .setAmount(amount)
                .setCurrency(currency)
                .setChannel(DUMMY_CHANNEL)
                .setStatus(PaymentOrder.PaymentStatus.PENDING)
                .setRemark("Dummy order created")
                .setCreatedAt(now)
                .setUpdatedAt(now);
        paymentOrderMapper.insert(order);
        return R.ok(toView(order));
    }

    @GetMapping("/{orderNo}")
    public R<OrderView> get(@PathVariable String orderNo) {
        return R.ok(toView(requireOrder(orderNo)));
    }

    @PostMapping("/{orderNo}/result")
    public R<OrderView> result(@PathVariable String orderNo, @RequestBody ResultRequest request) {
        PaymentOrder order = requireOrder(orderNo);
        String requestedResult = request.result() == null ? "" : request.result().trim().toUpperCase(Locale.ROOT);
        if (!"SUCCESS".equals(requestedResult) && !"FAILED".equals(requestedResult)) {
            throw new BizException(400, "Dummy result must be SUCCESS or FAILED");
        }
        if (order.getStatus() != PaymentOrder.PaymentStatus.PENDING) {
            return R.ok(toView(order));
        }
        PaymentOrder.PaymentStatus status = PaymentOrder.PaymentStatus.valueOf(requestedResult);
        order.setStatus(status)
                .setChannelTradeNo("DUMMY_TXN_" + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 10).toUpperCase(Locale.ROOT))
                .setRemark(status == PaymentOrder.PaymentStatus.SUCCESS
                        ? "Dummy payment succeeded" : "Dummy payment failed")
                .setUpdatedAt(LocalDateTime.now());
        paymentOrderMapper.updateById(order);
        return R.ok(toView(order));
    }

    @GetMapping
    public R<?> list(@RequestParam(name = "page", required = false) Integer page) {
        if (page != null) {
            if (page < 1) throw new BizException(400, "Page must be greater than zero");
            long total = paymentOrderMapper.selectCount(
                    Wrappers.<PaymentOrder>lambdaQuery()
                            .eq(PaymentOrder::getChannel, DUMMY_CHANNEL));
            int totalPages = (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE);
            List<OrderView> items = paymentOrderMapper.selectList(
                            Wrappers.<PaymentOrder>lambdaQuery()
                                    .eq(PaymentOrder::getChannel, DUMMY_CHANNEL)
                                    .orderByDesc(PaymentOrder::getCreatedAt, PaymentOrder::getId)
                                    .last("limit " + PAGE_SIZE + " offset " + ((page - 1) * PAGE_SIZE)))
                    .stream().map(this::toView).toList();
            return R.ok(new OrderPage(items, total, page, PAGE_SIZE, totalPages));
        }
        List<OrderView> orders = paymentOrderMapper.selectList(
                        Wrappers.<PaymentOrder>lambdaQuery()
                                .eq(PaymentOrder::getChannel, DUMMY_CHANNEL)
                                .orderByDesc(PaymentOrder::getCreatedAt, PaymentOrder::getId)
                                .last("limit 50"))
                .stream().map(this::toView).toList();
        return R.ok(orders);
    }

    private PaymentOrder requireOrder(String orderNo) {
        PaymentOrder order = paymentOrderMapper.selectOne(
                Wrappers.<PaymentOrder>lambdaQuery()
                        .eq(PaymentOrder::getOrderNo, orderNo)
                        .eq(PaymentOrder::getChannel, DUMMY_CHANNEL));
        if (order == null) throw new BizException(404, "Dummy order not found: " + orderNo);
        return order;
    }

    private OrderView toView(PaymentOrder order) {
        return new OrderView(order.getOrderNo(), order.getAmount(), order.getCurrency(), order.getStatus().name(),
                order.getChannelTradeNo(), order.getRemark(), order.getCreatedAt(), order.getUpdatedAt(),
                "/pay/?orderNo=" + order.getOrderNo());
    }

    public record CreateOrderRequest(BigDecimal amount, String currency) {}
    public record ResultRequest(String result) {}
    public record OrderPage(List<OrderView> items, long total, int page, int size, int totalPages) {}
    public record OrderView(String orderNo, BigDecimal amount, String currency, String status,
                            String transactionId, String result, LocalDateTime createdAt,
                            LocalDateTime updatedAt, String paymentUrl) {}
}
