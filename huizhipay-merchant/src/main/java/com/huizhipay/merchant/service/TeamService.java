package com.huizhipay.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.merchant.dto.InviteMemberRequest;
import com.huizhipay.merchant.dto.TeamMemberResponse;
import com.huizhipay.merchant.entity.MerchantTeam;
import com.huizhipay.merchant.entity.MerchantTeam.TeamInviteStatus;
import com.huizhipay.merchant.entity.MerchantTeam.TeamRole;
import com.huizhipay.merchant.mapper.MerchantTeamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 商户团队成员服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final MerchantTeamMapper merchantTeamMapper;

    public List<TeamMemberResponse> listMembers(String merchantId) {
        log.debug("[Team] 列出成员 merchantId={}", merchantId);
        if (merchantId == null) {
            return List.of();
        }
        List<MerchantTeam> list = merchantTeamMapper.selectList(
                new QueryWrapper<MerchantTeam>()
                        .eq("merchant_id", merchantId)
                        .orderByDesc("created_at"));
        log.debug("[Team] 成员数量 merchantId={}, count={}", merchantId, list.size());
        return list.stream().map(TeamMemberResponse::from).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void invite(String merchantId, InviteMemberRequest req) {
        log.info("[Team] 邀请成员 merchantId={}, email={}, role={}", merchantId, req.getEmail(), req.getRole());
        Long count = merchantTeamMapper.selectCount(
                new QueryWrapper<MerchantTeam>()
                        .eq("merchant_id", merchantId)
                        .eq("email", req.getEmail()));
        if (count != null && count > 0) {
            log.warn("[Team] 邀请重复 merchantId={}, email={}", merchantId, req.getEmail());
            throw new BizException(409, I18nUtils.get("team.invite.duplicate"));
        }
        MerchantTeam member = new MerchantTeam()
                .setMerchantId(merchantId)
                .setEmail(req.getEmail())
                .setRole(TeamRole.valueOf(req.getRole().toUpperCase()))
                .setStatus(TeamInviteStatus.PENDING);
        merchantTeamMapper.insert(member);
        log.info("[Team] 邀请已发送 id={}, email={}", member.getId(), req.getEmail());
    }
}
