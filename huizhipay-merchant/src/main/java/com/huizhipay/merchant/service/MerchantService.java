package com.huizhipay.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.merchant.dto.OnboardingStatusResponse;
import com.huizhipay.merchant.dto.SubmitOnboardingRequest;
import com.huizhipay.merchant.entity.Merchant;
import com.huizhipay.merchant.entity.Merchant.KybStatus;
import com.huizhipay.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 商户入驻与 KYB 服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private static final int TOTAL_STEPS = 4;

    private final MerchantMapper merchantMapper;

    /**
     * 获取入驻状态。未入驻时返回草稿态（currentStep=1）。
     */
    public OnboardingStatusResponse getStatus(String merchantId) {
        log.debug("[Merchant] 获取入驻状态 merchantId={}", merchantId);
        if (merchantId == null) {
            return OnboardingStatusResponse.draft();
        }
        Merchant merchant = merchantMapper.selectOne(
                new QueryWrapper<Merchant>().eq("merchant_id", merchantId));
        if (merchant == null) {
            return OnboardingStatusResponse.draft();
        }
        log.debug("[Merchant] 入驻状态 merchantId={}, kybStatus={}, step={}",
                merchantId, merchant.getKybStatus(), merchant.getCurrentStep());
        return new OnboardingStatusResponse(merchant);
    }

    /**
     * 提交 KYB 资料：不存在则新建商户主体，存在则更新并置为 PENDING。
     */
    @Transactional(rollbackFor = Exception.class)
    public Merchant submit(String merchantId, Long ownerUserId, SubmitOnboardingRequest req) {
        log.info("[Merchant] 提交KYB ownerUserId={}, company={}, country={}, settlementPref={}",
                ownerUserId, req.getCompany(), req.getCountry(), req.getSettlementPref());
        Merchant merchant = null;
        if (ownerUserId != null) {
            merchant = merchantMapper.selectOne(
                    new QueryWrapper<Merchant>().eq("owner_user_id", ownerUserId));
        }
        boolean isNew = merchant == null;
        if (isNew) {
            merchant = new Merchant();
            merchant.setMerchantId(generateMerchantId());
            merchant.setOwnerUserId(ownerUserId);
            merchant.setKybStatus(KybStatus.DRAFT);
            merchant.setCurrentStep((short) 1);
            log.info("[Merchant] 新建商户主体 merchantId={}, ownerUserId={}", merchant.getMerchantId(), ownerUserId);
        }
        merchant.applyKybFields(req)
                .setCurrentStep((short) TOTAL_STEPS)
                .setKybStatus(KybStatus.PENDING)
                .setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));

        if (isNew) {
            merchantMapper.insert(merchant);
        } else {
            merchantMapper.updateById(merchant);
        }
        log.info("[Merchant] KYB提交完成 merchantId={}, isNew={}, kybStatus=PENDING", merchant.getMerchantId(), isNew);
        return merchant;
    }

    public Merchant getByOwnerUserId(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        Merchant merchant = merchantMapper.selectOne(
                new QueryWrapper<Merchant>().eq("owner_user_id", ownerUserId));
        log.debug("[Merchant] 根据owner查询 merchant={}, ownerUserId={}",
                merchant != null ? merchant.getMerchantId() : null, ownerUserId);
        return merchant;
    }

    private String generateMerchantId() {
        String date = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
        return "M-" + date + "-" + suffix;
    }
}
