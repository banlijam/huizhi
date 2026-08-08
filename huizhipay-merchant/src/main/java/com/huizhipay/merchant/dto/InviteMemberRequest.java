package com.huizhipay.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank(message = "{merchant.onboarding.field_required}")
    @Email(message = "{validate.email.format}")
    private String email;
    /** admin / analyst / readonly */
    private String role = "analyst";
}
