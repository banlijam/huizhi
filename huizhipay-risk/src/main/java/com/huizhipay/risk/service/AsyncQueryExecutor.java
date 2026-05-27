package com.huizhipay.risk.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.ledger.service.LedgerBatchService;
import com.huizhipay.ledger.service.LedgerBatchService.AccountEntryDetail;
import com.huizhipay.risk.entity.QueryLog;
import com.huizhipay.risk.mapper.QueryLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncQueryExecutor {

    private final ThirdPartyApiClient thirdPartyApiClient;
    private final QueryLogMapper queryLogMapper;
    private final LedgerBatchService ledgerBatchService; // 账本批量接口

    // 平台归集账户（与之前保持一致）
    private static final String PLATFORM_INCOME_ACCOUNT = "PLATFORM_INCOME";
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("100");
    private static final BigDecimal SUPPLIER_COST = new BigDecimal("80");
    private static final BigDecimal PLATFORM_FEE = new BigDecimal("20");

    /**
     * 异步执行外部查询（由主 Service 触发）
     *
     * @param merchantId 用户ID
     * @param queryNo    查询流水号
     * @param params     请求参数
     */
    @Async("taskExecutor") // 指定线程池
    public void executeExternalQuery(String merchantId, String queryNo, String params) {
        log.info("异步任务开始执行, queryNo: {}", queryNo);
        String rawResult = null;
        try {
            // 1. 调用外部API（阻塞操作在此处进行，但不占用数据库事务）
            rawResult = thirdPartyApiClient.fetchData(params);

            // 2. 外部调用成功 -> 更新日志为 SUCCESS
            QueryLog logEntry = queryLogMapper.selectOne(Wrappers.<QueryLog>lambdaQuery()
                                                                 .eq(QueryLog::getQueryNo, queryNo));
            logEntry.setStatus("SUCCESS");
            logEntry.setThirdPartyResponse(rawResult.substring(0, Math.min(100, rawResult.length())));
            logEntry.setUpdatedAt(LocalDateTime.now());
            queryLogMapper.updateById(logEntry);

            // 3. 将结果存入 Redis（供前端轮询），此处略
            log.info("异步查询成功, queryNo: {}", queryNo);

        } catch (Exception e) {
            // 4. 外部调用失败 -> 执行补偿退款（非常重要！）
            log.error("异步查询失败，开始执行补偿退款, queryNo: {}", queryNo, e);
            try {
                // 4.1 更新日志为 FAILED
                QueryLog logEntry = queryLogMapper.selectOne(Wrappers.<QueryLog>lambdaQuery()
                                                                     .eq(QueryLog::getQueryNo, queryNo));
                logEntry.setStatus("FAILED");
                logEntry.setUpdatedAt(LocalDateTime.now());
                queryLogMapper.updateById(logEntry);

                // 4.2 补偿记账（反向操作：平台减钱，用户加钱）
                // 构造反向流水：用户 +100，平台 -80，平台 -20 （总和为0）
                AccountEntryDetail userRefund = new AccountEntryDetail()
                        .setAccountNo(getUserAccountNo(merchantId))
                        .setMerchantId(merchantId)
                        .setAmount(TOTAL_PRICE)  // +100 退款
                        .setBizType("QUERY_REFUND")
                        .setRemark("外部接口异常，自动退款");

                AccountEntryDetail platformCostRefund = new AccountEntryDetail()
                        .setAccountNo(PLATFORM_INCOME_ACCOUNT)
                        .setMerchantId("PLATFORM")
                        .setAmount(SUPPLIER_COST.negate()) // -80
                        .setBizType("REFUND_COST");

                AccountEntryDetail platformFeeRefund = new AccountEntryDetail()
                        .setAccountNo(PLATFORM_INCOME_ACCOUNT)
                        .setMerchantId("PLATFORM")
                        .setAmount(PLATFORM_FEE.negate()) // -20
                        .setBizType("REFUND_FEE");

                // 执行补偿记账（此处需确保补偿事务独立，防止失败后无法退款）
                ledgerBatchService.batchRecordWithDoubleEntry(
                        merchantId,
                        PLATFORM_INCOME_ACCOUNT,
                        queryNo + "_REFUND",
                        Arrays.asList(userRefund, platformCostRefund, platformFeeRefund)
                );
                log.info("补偿退款成功, queryNo: {}", queryNo);

            } catch (Exception refundEx) {
                // 极端情况：补偿退款也失败了 -> 必须告警，走人工介入
                log.error("补偿退款失败，请人工处理！queryNo: {}", queryNo, refundEx);
                // 发送钉钉/邮件告警
            }
        }
    }

    private String getUserAccountNo(String merchantId) {
        return "ACC_AVAILABLE_" + merchantId;
    }
}