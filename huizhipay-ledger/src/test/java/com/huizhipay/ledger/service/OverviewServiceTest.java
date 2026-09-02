package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.huizhipay.common.port.SettlementCountdownPort;
import com.huizhipay.ledger.dto.LedgerRowResponse;
import com.huizhipay.ledger.entity.LedgerEntry;
import com.huizhipay.ledger.mapper.LedgerEntryMapper;
import com.huizhipay.ledger.mapper.OverviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverviewServiceTest {
    @Mock private OverviewMapper overviewMapper;
    @Mock private LedgerEntryMapper ledgerEntryMapper;
    @Mock private SettlementCountdownPort settlementCountdownPort;
    @InjectMocks private OverviewService service;

    @Test
    void ledgerUsesCustodyLiabilityAsGrossAndReturnsSevenNinetyThreeSplit() {
        LedgerEntry custody = new LedgerEntry()
                .setMerchantId("M-A")
                .setBizId("DUMMY-100")
                .setAmount(new BigDecimal("-100.000"))
                .setEntryStatus(LedgerEntry.EntryStatusEnum.SETTLED)
                .setCreatedAt(LocalDateTime.now());
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(custody));

        List<LedgerRowResponse> rows = service.getLedger("M-A");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getGross()).isEqualByComparingTo("100.000");
        assertThat(rows.getFirst().getFee()).isEqualByComparingTo("7.000");
        assertThat(rows.getFirst().getNet()).isEqualByComparingTo("93.000");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<LedgerEntry>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(ledgerEntryMapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("merchant_id", "amount <");
    }
}
