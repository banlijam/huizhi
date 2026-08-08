package com.huizhipay.ledger.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 透明分账账本一行（7:93 拆解）。
 */
@Data
@NoArgsConstructor
public class LedgerRowResponse {
    private String orderId;
    private BigDecimal gross;
    private BigDecimal fee;
    private BigDecimal net;
    private String status;
    private LocalDateTime time;
}
