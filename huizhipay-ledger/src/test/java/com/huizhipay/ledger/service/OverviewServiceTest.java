package com.huizhipay.ledger.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.huizhipay.common.port.SettlementCountdownPort;
import com.huizhipay.ledger.dto.LedgerResponse;
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
        when(ledgerEntryMapper.sumPaymentGross("M-A")).thenReturn(new BigDecimal("100.000"));
        when(ledgerEntryMapper.countPaymentGross("M-A")).thenReturn(1L);

        LedgerResponse resp = service.getLedger("M-A");

        assertThat(resp.getRows()).hasSize(1);
        assertThat(resp.getRows().getFirst().getGross()).isEqualByComparingTo("100.000");
        assertThat(resp.getRows().getFirst().getFee()).isEqualByComparingTo("7.000");
        assertThat(resp.getRows().getFirst().getNet()).isEqualByComparingTo("93.000");
        assertThat(resp.getTotalGross()).isEqualByComparingTo("100.000");
        assertThat(resp.getTotalFee()).isEqualByComparingTo("7.000");
        assertThat(resp.getTotalNet()).isEqualByComparingTo("93.000");
        assertThat(resp.getTotalCount()).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<LedgerEntry>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(ledgerEntryMapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("merchant_id", "amount <");
    }

    @Test
    void ledgerTotalUsesFullCumulativeAggregateIndependentOfDetailRows() {
        LedgerEntry recent = new LedgerEntry()
                .setMerchantId("M-A")
                .setBizId("DUMMY-NEW")
                .setAmount(new BigDecimal("-10.000"))
                .setEntryStatus(LedgerEntry.EntryStatusEnum.SETTLED)
                .setCreatedAt(LocalDateTime.now());
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(recent));
        when(ledgerEntryMapper.sumPaymentGross("M-A")).thenReturn(new BigDecimal("10000.000"));
        when(ledgerEntryMapper.countPaymentGross("M-A")).thenReturn(120L);

        LedgerResponse resp = service.getLedger("M-A");

        // 总额来自全量聚合，明细行仍只取最近记录，二者解耦
        assertThat(resp.getTotalGross()).isEqualByComparingTo("10000.000");
        assertThat(resp.getTotalFee()).isEqualByComparingTo("700.000");
        assertThat(resp.getTotalNet()).isEqualByComparingTo("9300.000");
        assertThat(resp.getTotalCount()).isEqualTo(120L);
        assertThat(resp.getRows()).hasSize(1);
        assertThat(resp.getRows().getFirst().getGross()).isEqualByComparingTo("10.000");
    }
}
