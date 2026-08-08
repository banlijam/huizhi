package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import com.huizhipay.ledger.service.LedgerTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcquiringWebhookService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentOrderMapper paymentOrderMapper;
    private final LedgerTransferService ledgerTransferService;

    @Transactional(rollbackFor = Exception.class)
    public void processPaymentSuccess(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("type").asText();

            // 只处理支付成功事件
            if (!"payment_intent.succeeded".equals(eventType)) {
                return;
            }

            JsonNode data = root.path("data");
            String paymentIntentId = data.path("id").asString();
            String merchantOrderNo = data.path("metadata").path("order_no").asString(); // 创建时自定义的元数据
            BigDecimal amount = new BigDecimal(data.path("amount").toString());
            String currency = data.path("currency").asString();

            // ----- 查库 + 幂等控制（关键） -----
            PaymentOrder existingOrder = paymentOrderMapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
                    .eq(PaymentOrder::getOrderNo, merchantOrderNo));
            if (existingOrder == null) {
                log.error("本地订单不存在, orderNo: {}", merchantOrderNo);
                return;
            }

            // 如果已经成功，直接ACK（防止重复回调）
            if (PaymentOrder.PaymentStatus.SUCCESS == existingOrder.getStatus()) {
                log.info("订单已处理完成，幂等丢弃, orderNo: {}", merchantOrderNo);
                return;
            }

            // ----- 加余额（委托账本模块）-----
            // 1. 先更新本地订单状态为成功
            existingOrder.setStatus(PaymentOrder.PaymentStatus.SUCCESS);
            existingOrder.setChannelTradeNo(paymentIntentId);
            paymentOrderMapper.updateById(existingOrder);

            // 2. 调用账本服务增加可用余额（这一步在同一个事务里，保证强一致性）
            //    如果账本模块是独立微服务，这里改为发送RPC或领域事件；
            //    当前是单体多模块，直接注入Service调用。
            ledgerTransferService.payment(
                    existingOrder.getMerchantId(),
                    existingOrder.getCurrency(),
                    amount,
                    merchantOrderNo,
                    "AIRWALLEX",
                    paymentIntentId
            );

            log.info("Airwallex 充值成功, orderNo: {}, amount: {}", merchantOrderNo, amount);

        } catch (Exception e) {
            log.error("Webhook 处理异常", e);
            throw new RuntimeException(e); // 事务回滚，返回非200给Airwallex触发重试
        }
    }
}