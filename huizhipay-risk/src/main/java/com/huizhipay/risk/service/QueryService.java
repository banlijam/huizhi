package com.huizhipay.risk.service;

import com.huizhipay.ledger.service.LedgerBatchService;
import com.huizhipay.ledger.service.LedgerBatchService.AccountEntryDetail;
import com.huizhipay.risk.entity.QueryLog;
import com.huizhipay.risk.mapper.QueryLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static manifold.science.util.CoercionConstants.bd;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {
    private final LedgerBatchService ledgerBatchService;
    private final AsyncQueryExecutor asyncQueryExecutor;
    private final QueryLogMapper queryLogMapper;

    // 定价策略（可从配置中心读取）
    private static final BigDecimal TOTAL_PRICE = 100bd;   // 用户扣1元 (单位:分)
    private static final BigDecimal SUPPLIER_COST = 80bd;  // 外部成本0.8元
    private static final BigDecimal PLATFORM_FEE = 20bd;   // 平台利润0.2元

    // 平台内部归集账户（需提前在 t_account 表中初始化一条记录）
    private static final String PLATFORM_INCOME_ACCOUNT = "PLATFORM_INCOME";

    @Transactional(rollbackFor = Exception.class)
    public String doQuery(String merchantId, QueryRequest request) {
        String queryNo = "Q" + System.currentTimeMillis();

        // ----- 1. 双记账：构造 3 条流水（借贷必相等） -----
        // 明细1：用户账户扣减 -100（贷方/减少）
        AccountEntryDetail userDebit = new AccountEntryDetail()
                .setAccountNo(getUserAccountNo(merchantId)) // 例如 "ACC_AVAILABLE_123"
                .setMerchantId(merchantId)
                .setAmount(TOTAL_PRICE.negate())            // -100
                .setBizType("QUERY_FEE")
                .setChannel("EXTERNAL_API")
                .setRemark("用户查询扣费");

        // 明细2：平台归集账户增加 +80（用于支付外部供应商）
        AccountEntryDetail platformCreditCost = new AccountEntryDetail()
                .setAccountNo(PLATFORM_INCOME_ACCOUNT)
                .setMerchantId("PLATFORM")                  // 平台自己的ID
                .setAmount(SUPPLIER_COST)                   // +80
                .setBizType("SUPPLIER_COST")
                .setChannel("EXTERNAL_API")
                .setRemark("代收外部供应商成本");

        // 明细3：平台归集账户增加 +20（平台手续费收入）
        AccountEntryDetail platformCreditFee = new AccountEntryDetail()
                .setAccountNo(PLATFORM_INCOME_ACCOUNT)
                .setMerchantId("PLATFORM")
                .setAmount(PLATFORM_FEE)                    // +20
                .setBizType("PLATFORM_FEE")
                .setChannel("EXTERNAL_API")
                .setRemark("平台服务手续费");

        // 执行批量记账（事务内，余额+流水同时落库）
        ledgerBatchService.batchRecordWithDoubleEntry(
                merchantId,
                PLATFORM_INCOME_ACCOUNT,
                queryNo,
                Arrays.asList(userDebit, platformCreditCost, platformCreditFee)
        );

        // ----- 2. 调用第三方外部 API（必须放在记账之后） -----
        String rawResult = "";
        try {
//            rawResult = thirdPartyClient.fetchData(request.getParams());
        } catch (Exception e) {
            // 外部失败：抛出 RuntimeException，触发 @Transactional 回滚
            // 此时用户余额没扣，平台余额没加，所有流水都不落库（完美回滚）
            log.error("第三方API调用失败，事务回滚, queryNo: {}", queryNo, e);
            throw new RuntimeException("外部数据源不可用，已自动回滚扣费", e);
        }

        // ----- 3. 记录查询日志（非必须事务，但可放在同一个事务中保持一致性） -----
        QueryLog logEntry = new QueryLog();
        logEntry.setQueryNo(queryNo);
        logEntry.setMerchantId(merchantId);
        logEntry.setCostAmount(TOTAL_PRICE);
        logEntry.setQueryParams(request.params);
        logEntry.setStatus("PROCESSING");
        logEntry.setCreatedAt(LocalDateTime.now());
        queryLogMapper.insert(logEntry);

        asyncQueryExecutor.executeExternalQuery(merchantId, queryNo, request.params);

        return queryNo;
    }

    // 根据商户ID获取其可用余额账户号（实际可从 t_account 表查询）
    private String getUserAccountNo(String merchantId) {
        return "ACC_AVAILABLE_" + merchantId;
    }

    public record QueryRequest(String params) {
    }
}