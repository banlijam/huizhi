package com.huizhipay.merchant.dto;

import com.huizhipay.settlement.entity.MerchantWallet;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WalletResponse {
    private boolean bound;
    private String type;
    private String network;
    private String address;

    public static WalletResponse from(MerchantWallet wallet) {
        WalletResponse r = new WalletResponse();
        if (wallet != null) {
            r.bound = true;
            r.type = wallet.getWalletType() == null ? null : wallet.getWalletType().name();
            r.network = wallet.getNetwork() == null ? null : wallet.getNetwork().name();
            r.address = wallet.getAddress();
        }
        return r;
    }
}
