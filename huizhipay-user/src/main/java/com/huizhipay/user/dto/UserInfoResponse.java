package com.huizhipay.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String email;
    private String nickname;
    private Boolean emailVerified;
    private Boolean totpEnabled;
    private Integer status;  // 1启用 0禁用
    private String merchantId;
    private String merchantRole;

    public UserInfoResponse(Long id, String email, String nickname, Boolean emailVerified,
                            Boolean totpEnabled, Integer status) {
        this(id, email, nickname, emailVerified, totpEnabled, status, null, null);
    }
}
