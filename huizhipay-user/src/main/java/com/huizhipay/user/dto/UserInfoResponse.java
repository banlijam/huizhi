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
}