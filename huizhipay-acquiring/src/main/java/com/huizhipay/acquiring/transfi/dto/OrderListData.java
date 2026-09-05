package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 订单列表响应 data 体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderListData {
    /** API 实际返回的字段名为 transfers（不是 orders） */
    private List<TransFiOrder> transfers;
}
