package com.huizhipay.settlement.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huizhipay.settlement.entity.MerchantWallet;
import com.huizhipay.settlement.mapper.MerchantWalletMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算钱包绑定服务（MetaMask Polygon / Stellar）。
 * 每个商户仅绑定一个结算地址（t_merchant_wallet.merchant_id 唯一）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementWalletService {

    private final MerchantWalletMapper merchantWalletMapper;

    public MerchantWallet getWallet(String merchantId) {
        log.debug("[SettlementWallet] 查询绑定钱包 merchantId={}", merchantId);
        if (merchantId == null) {
            return null;
        }
        MerchantWallet wallet = merchantWalletMapper.selectOne(
                new QueryWrapper<MerchantWallet>().eq("merchant_id", merchantId));
        log.debug("[SettlementWallet] 查询结果 merchantId={}, 存在={}, type={}",
                merchantId, wallet != null, wallet != null ? wallet.getWalletType() : null);
        return wallet;
    }

    @Transactional(rollbackFor = Exception.class)
    public MerchantWallet bindWallet(String merchantId, MerchantWallet.WalletTypeEnum type, String address, MerchantWallet.WalletNetworkEnum network) {
        log.info("[SettlementWallet] 绑定结算钱包 merchantId={}, type={}, network={}, address={}",
                merchantId, type, network, maskAddress(address));
        // 先清除旧绑定，再写入新地址（保证一商户一钱包）
        int deleted = merchantWalletMapper.delete(
                new QueryWrapper<MerchantWallet>().eq("merchant_id", merchantId));
        if (deleted > 0) {
            log.debug("[SettlementWallet] 已清除旧绑定 merchantId={}, 删除条数={}", merchantId, deleted);
        }
        MerchantWallet wallet = new MerchantWallet()
                .setMerchantId(merchantId)
                .setWalletType(type)
                .setNetwork(network)
                .setAddress(address);
        merchantWalletMapper.insert(wallet);
        log.info("[SettlementWallet] 钱包绑定完成 merchantId={}, id={}", merchantId, wallet.getId());
        return wallet;
    }

    private static String maskAddress(String address) {
        if (address == null || address.length() <= 8) return address;
        return address.substring(0, 4) + "..." + address.substring(address.length() - 4);
    }
}
