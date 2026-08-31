package com.huizhipay.merchant.controller;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.merchant.dto.BindWalletRequest;
import com.huizhipay.merchant.dto.OnboardingStatusResponse;
import com.huizhipay.merchant.dto.SubmitOnboardingRequest;
import com.huizhipay.merchant.dto.WalletResponse;
import com.huizhipay.merchant.service.MerchantService;
import com.huizhipay.settlement.entity.MerchantWallet;
import com.huizhipay.settlement.service.SettlementWalletService;
import com.huizhipay.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.huizhipay.common.security.MerchantAccessGuard.ADMIN;
import static com.huizhipay.common.security.MerchantAccessGuard.OWNER;

/**
 * 入驻与合规：KYB 表单 + 结算钱包绑定。
 * 钱包绑定逻辑委托给 huizhipay-settlement 的 SettlementWalletService。
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final MerchantResolver merchantResolver;
    private final MerchantAccessGuard merchantAccessGuard;
    private final MerchantService merchantService;
    private final SettlementWalletService settlementWalletService;

    @GetMapping("/status")
    public R<OnboardingStatusResponse> status() {
        return R.ok(merchantService.getStatus(merchantResolver.getCurrentMerchantId()));
    }

    @PostMapping("/submit")
    public R<Void> submit(@Valid @RequestBody SubmitOnboardingRequest req,
                          @AuthenticationPrincipal UserPrincipal principal) {
        merchantService.submit(merchantResolver.getCurrentMerchantId(),
                principal == null ? null : principal.getId(), req);
        return R.ok(I18nUtils.get("merchant.onboarding.submitted"));
    }

    @GetMapping("/wallet")
    public R<WalletResponse> wallet() {
        String merchantId = requireMerchant();
        return R.ok(WalletResponse.from(settlementWalletService.getWallet(merchantId)));
    }

    @PostMapping("/wallet/bind")
    public R<WalletResponse> bindWallet(@Valid @RequestBody BindWalletRequest req) {
        String merchantId = merchantAccessGuard.requireAnyRole(OWNER, ADMIN).merchantId();
        MerchantWallet.WalletTypeEnum type = MerchantWallet.WalletTypeEnum.valueOf(req.getType().toUpperCase());
        MerchantWallet.WalletNetworkEnum network;
        if (type == MerchantWallet.WalletTypeEnum.STELLAR) {
            if (req.getAddress() == null || !req.getAddress().matches("^G[A-Z0-9]{55}$")) {
                throw new BizException(400, I18nUtils.get("merchant.wallet.invalid"));
            }
            network = MerchantWallet.WalletNetworkEnum.STELLAR;
        } else if (req.getNetwork() != null) {
            network = MerchantWallet.WalletNetworkEnum.valueOf(req.getNetwork().toUpperCase());
        } else {
            network = MerchantWallet.WalletNetworkEnum.POLYGON;
        }
        return R.ok(WalletResponse.from(
                settlementWalletService.bindWallet(merchantId, type, req.getAddress(), network)));
    }

    private String requireMerchant() {
        String merchantId = merchantResolver.getCurrentMerchantId();
        if (merchantId == null) {
            throw new BizException(403, I18nUtils.get("merchant.context.not_found"));
        }
        return merchantId;
    }
}
