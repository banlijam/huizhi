package com.huizhipay.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 清算倒计时信息（供指挥中心环形进度条展示）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCountdown {
    /** 距离下一笔 T+1 清算剩余小时数（保留 1 位小数） */
    private double hoursRemaining;
    /** 一轮清算总时长（小时），默认 24 */
    private int totalHours;
    /** 预计到账时间（UTC） */
    private LocalDateTime expectedAt;
}
