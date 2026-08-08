package com.huizhipay.user.controller;

import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.user.dto.UserProfileResponse;
import com.huizhipay.user.security.UserPrincipal;
import com.huizhipay.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户头部用户资料。
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final MerchantResolver merchantResolver;
    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public R<UserProfileResponse> profile(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setBalance(userProfileService.getBalance(merchantResolver.getCurrentMerchantId()));
        if (principal != null) {
            resp.setEmail(principal.getEmail());
        }
        return R.ok(resp);
    }
}
