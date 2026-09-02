package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.ledger.entity.Account;
import com.huizhipay.ledger.entity.AccountEntryDetail;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.AccountMapper;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerBookingServiceTest {
    @Mock private AccountMapper accountMapper;
    @Mock private LedgerEntryMapper ledgerEntryMapper;
    @InjectMocks private LedgerBookingService service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Account.class);
    }

    @Test
    void paymentAllowsCustodyLiabilityToBecomeNegativeAndBalancesToZero() {
        Account asset = account("ASSET", Account.AccountTypeEnum.ASSET_AVAILABLE);
        Account income = account("INCOME", Account.AccountTypeEnum.PLATFORM_INCOME);
        Account liability = account("LIABILITY", Account.AccountTypeEnum.LIABILITY_CUSTODY);
        when(accountMapper.selectOne(any())).thenReturn(asset, income, liability);
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);

        service.doubleEntryBooking("M-A", "DUMMY-100", List.of(
                detail("ASSET", "93.000"),
                detail("INCOME", "7.000"),
                detail("LIABILITY", "-100.000")));

        assertThat(asset.getBalance()).isEqualByComparingTo("93.000");
        assertThat(income.getBalance()).isEqualByComparingTo("7.000");
        assertThat(liability.getBalance()).isEqualByComparingTo("-100.000");
    }

    @Test
    void unbalancedEntriesFailBeforeAnyAccountMutation() {
        assertThatThrownBy(() -> service.doubleEntryBooking("M-A", "BAD", List.of(
                detail("ASSET", "93.000"), detail("LIABILITY", "-100.000"))))
                .isInstanceOf(BizException.class).extracting("code").isEqualTo(500);
        verifyNoInteractions(accountMapper, ledgerEntryMapper);
    }

    private Account account(String no, Account.AccountTypeEnum type) {
        Account account = new Account();
        account.setAccountNo(no);
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setVersion(0);
        return account;
    }

    private AccountEntryDetail detail(String accountNo, String amount) {
        return new AccountEntryDetail().setAccountNo(accountNo).setMerchantId("M-A")
                .setAmount(new BigDecimal(amount)).setBizType(LedgerEntry.BizTypeEnum.PAYMENT)
                .setChannel("DUMMY");
    }
}
