package com.huizhipay.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 商户结算钱包绑定 */
@Data
@Accessors(chain = true)
@TableName("t_merchant_wallet")
public class MerchantWallet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    /** 钱包类型 */
    private WalletTypeEnum walletType;
    /** 网络 */
    private WalletNetworkEnum network;
    private String address;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime boundAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 钱包类型：METAMASK / STELLAR */
    public enum WalletTypeEnum {
        METAMASK,
        STELLAR
    }

    /** 网络：POLYGON / STELLAR / TESTNET */
    public enum WalletNetworkEnum {
        POLYGON,
        STELLAR,
        TESTNET
    }
}
