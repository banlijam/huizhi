package com.huizhipay.merchant.controller;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantResolver;
import com.huizhipay.merchant.dto.InviteMemberRequest;
import com.huizhipay.merchant.dto.TeamMemberResponse;
import com.huizhipay.merchant.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户团队成员管理。
 */
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final MerchantResolver merchantResolver;
    private final TeamService teamService;

    @GetMapping("/members")
    public R<List<TeamMemberResponse>> members() {
        return R.ok(teamService.listMembers(merchantResolver.getCurrentMerchantId()));
    }

    @PostMapping("/invite")
    public R<Void> invite(@Valid @RequestBody InviteMemberRequest req) {
        String merchantId = merchantResolver.getCurrentMerchantId();
        if (merchantId == null) {
            throw new BizException(403, I18nUtils.get("team.merchant.not_ready"));
        }
        teamService.invite(merchantId, req);
        return R.ok(I18nUtils.get("team.invite.created"));
    }
}
