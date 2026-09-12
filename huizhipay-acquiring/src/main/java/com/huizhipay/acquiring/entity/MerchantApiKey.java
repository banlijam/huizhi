package com.huizhipay.acquiring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("t_merchant_api_key")
public class MerchantApiKey {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    private String keyPrefix;
    private String keyHash;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime disabledAt;
}
