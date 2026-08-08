package com.huizhipay.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindWalletRequest {
    /** metamask / stellar */
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String type;
    @NotBlank(message = "{merchant.onboarding.field_required}")
    private String address;
    private String network;
}
