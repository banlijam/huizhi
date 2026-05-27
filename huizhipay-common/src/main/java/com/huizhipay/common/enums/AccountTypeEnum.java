package com.huizhipay.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountTypeEnum {
    ASSET_AVAILABLE("ASSET_AVAILABLE", "商户可用资产（余额）"),
    LIABILITY_CUSTODY("LIABILITY_CUSTODY", "托管应付负债（客户资金）"),
    ;

    @EnumValue
    private final String code;
    private final String desc;
}