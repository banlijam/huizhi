package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 上传发票响应数据
 */
@Data
public class UploadInvoiceResponse {
    /** 上传成功后返回的发票 ID，用于在创建 Payin/Payout 订单时关联 */
    private String invoiceId;
}
