package com.huizhipay.acquiring.transfi.dto;

import lombok.Data;

/**
 * 订单发送者姓名
 */
@Data
public class SenderName {
    /** 发送者名 */
    private String firstName;
    /** 发送者姓 */
    private String lastName;
}
