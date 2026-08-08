package com.huizhipay.risk.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.entity.Account.AccountTypeEnum;
import com.huizhipay.ledger.entity.AccountEntryDetail;
import com.huizhipay.ledger.entity.LedgerEntry.BizTypeEnum;
import com.huizhipay.ledger.mapper.AccountMapper;
import com.huizhipay.ledger.service.LedgerBookingService;
import com.huizhipay.risk.entity.QueryLog;
import com.huizhipay.risk.mapper.QueryLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static manifold.science.util.CoercionConstants.bd;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncQueryExecutor {

    private final ThirdPartyApiClient thirdPartyApiClient;
    private final QueryLogMapper queryLogMapper;
    private final LedgerBookingService ledgerBookingService;
    private final AccountMapper accountMapper;

    private static final String PLATFORM_MERCHANT_ID = "__PLATFORM__";
    private static final BigDecimal SUPPLIER_COST = 0.8bd;

    @Async("taskExecutor")
    public void executeExternalQuery(String merchantId, String queryNo, String params) {
        log.info("异步任务开始执行, queryNo: {}", queryNo);
        String rawResult = null;
        try {
            rawResult = thirdPartyApiClient.fetchData(params);

            QueryLog logEntry = queryLogMapper.selectOne(Wrappers.<QueryLog>lambdaQuery()
                    .eq(QueryLog::getQueryNo, queryNo));
            logEntry.setStatus(QueryLog.QueryStatus.SUCCESS);
            logEntry.setThirdPartyResponse(rawResult.substring(0, Math.min(100, rawResult.length())));
            logEntry.setUpdatedAt(LocalDateTime.now());
            queryLogMapper.updateById(logEntry);

            log.info("异步查询成功, queryNo: {}", queryNo);

        } catch (Exception e) {
            log.error("异步查询失败，开始执行补偿退款, queryNo: {}", queryNo, e);
            try {
                QueryLog logEntry = queryLogMapper.selectOne(Wrappers.<QueryLog>lambdaQuery()
                        .eq(QueryLog::getQueryNo, queryNo));
                logEntry.setStatus(QueryLog.QueryStatus.FAIL);
                logEntry.setUpdatedAt(LocalDateTime.now());
                queryLogMapper.updateById(logEntry);

                Account platformCostAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                        .eq(Account::getMerchantId, PLATFORM_MERCHANT_ID)
                        .eq(Account::getAccountType, AccountTypeEnum.PLATFORM_COST));

                Account platformIncomeAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                        .eq(Account::getMerchantId, PLATFORM_MERCHANT_ID)
                        .eq(Account::getAccountType, AccountTypeEnum.PLATFORM_INCOME));

                AccountEntryDetail costRefund = new AccountEntryDetail()
                        .setAccountNo(platformCostAccount.getAccountNo())
                        .setMerchantId(PLATFORM_MERCHANT_ID)
                        .setAmount(-SUPPLIER_COST)
                        .setBizType(BizTypeEnum.QUERY_REFUND)
                        .setChannel("EXTERNAL_API")
                        .setRemark("外部查询失败，退回供应商成本");

                AccountEntryDetail incomeRefund = new AccountEntryDetail()
                        .setAccountNo(platformIncomeAccount.getAccountNo())
                        .setMerchantId(PLATFORM_MERCHANT_ID)
                        .setAmount(SUPPLIER_COST)
                        .setBizType(BizTypeEnum.QUERY_REFUND)
                        .setChannel("EXTERNAL_API")
                        .setRemark("外部查询失败，归还平台收入");

                ledgerBookingService.doubleEntryBooking(
                        PLATFORM_MERCHANT_ID,
                        queryNo + "_REFUND",
                        List.of(costRefund, incomeRefund)
                );
                log.info("补偿退款成功, queryNo: {}", queryNo);

            } catch (Exception refundEx) {
                log.error("补偿退款失败，请人工处理！queryNo: {}", queryNo, refundEx);
            }
        }
    }
}
