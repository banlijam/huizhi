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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static manifold.science.util.CoercionConstants.bd;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final LedgerBookingService ledgerBookingService;
    private final AccountMapper accountMapper;
    private final AsyncQueryExecutor asyncQueryExecutor;
    private final QueryLogMapper queryLogMapper;

    private static final String PLATFORM_MERCHANT_ID = "__PLATFORM__";
    private static final BigDecimal SUPPLIER_COST = 0.8bd;

    @Transactional(rollbackFor = Exception.class)
    public String doQuery(String merchantId, QueryRequest request) {
        String queryNo = "Q" + System.currentTimeMillis();

        Account platformCostAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, PLATFORM_MERCHANT_ID)
                .eq(Account::getAccountType, AccountTypeEnum.PLATFORM_COST));

        Account platformIncomeAccount = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getMerchantId, PLATFORM_MERCHANT_ID)
                .eq(Account::getAccountType, AccountTypeEnum.PLATFORM_INCOME));

        AccountEntryDetail costDetail = new AccountEntryDetail()
                .setAccountNo(platformCostAccount.getAccountNo())
                .setMerchantId(PLATFORM_MERCHANT_ID)
                .setAmount(SUPPLIER_COST)
                .setBizType(BizTypeEnum.QUERY_COST)
                .setChannel("EXTERNAL_API")
                .setRemark("外部查询供应商成本");

        AccountEntryDetail incomeDetail = new AccountEntryDetail()
                .setAccountNo(platformIncomeAccount.getAccountNo())
                .setMerchantId(PLATFORM_MERCHANT_ID)
                .setAmount(-SUPPLIER_COST)
                .setBizType(BizTypeEnum.QUERY_COST)
                .setChannel("EXTERNAL_API")
                .setRemark("平台收入支付供应商成本");

        ledgerBookingService.doubleEntryBooking(
                PLATFORM_MERCHANT_ID,
                queryNo,
                List.of(costDetail, incomeDetail)
        );

        QueryLog logEntry = new QueryLog();
        logEntry.setQueryNo(queryNo);
        logEntry.setMerchantId(merchantId);
        logEntry.setCostAmount(SUPPLIER_COST);
        logEntry.setQueryParams(request.params);
        logEntry.setStatus(QueryLog.QueryStatus.PROCESSING);
        logEntry.setCreatedAt(LocalDateTime.now());
        queryLogMapper.insert(logEntry);

        asyncQueryExecutor.executeExternalQuery(merchantId, queryNo, request.params);

        return queryNo;
    }

    public record QueryRequest(String params) {
    }
}
