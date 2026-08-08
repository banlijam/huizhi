package com.huizhipay.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stellar.sdk.*;
import org.stellar.sdk.operations.PathPaymentStrictReceiveOperation;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;

import java.math.BigDecimal;

import static manifold.science.util.CoercionConstants.bd;

@Slf4j
@Service
public class StellarSettlementService {
    @Value("huizhipay.stellar.horizon-endpoint:https://horizon-testnet.stellar.org")
    private String HORIZON_TESTNET;

    public String dispatchFundsViaPathPayment(String sourceSecret, String destinationAccountId, BigDecimal amount) {
        log.info("[Stellar] 发起链上清算 dest={}, amount={}", destinationAccountId, amount);
        Server server = new Server(HORIZON_TESTNET);
        KeyPair sourceKey = KeyPair.fromSecretSeed(sourceSecret);
        log.debug("[Stellar] 加载源账户 horizon={}", HORIZON_TESTNET);
        AccountResponse sourceAccount = server.accounts().account(sourceKey.getAccountId());

        // 1. 使用 new Asset() 构造非原生资产
        Asset sourceAsset = Asset.create("USD:GBX...");  // 替换为真实发行方
        Asset destAsset = Asset.create("HKD:GCA...");    // 替换为真实发行方
        log.debug("[Stellar] 资产映射 source={}, dest={}", sourceAsset, destAsset);

        // 2. 构建 Path Payment 操作
        PathPaymentStrictReceiveOperation.PathPaymentStrictReceiveOperationBuilder<?, ?> operationBuilder =
                PathPaymentStrictReceiveOperation.builder();

        PathPaymentStrictReceiveOperation operation = operationBuilder
                .sendAsset(sourceAsset)                // 源资产
                .sendMax(105.00bd) // 最大可支付金额 (滑点上限)
                .destination(destinationAccountId)     // 收款账户
                .destAsset(destAsset)                  // 目标资产
                .destAmount(amount)                 // 收款金额
                .path(new Asset[]{Asset.create("native")})     // 支付路径经过 XLM
                .build();
        log.debug("[Stellar] PathPayment 构建完成 destAmount={}, sendMax={}", amount, 105.00bd);

        // 3. 使用 TransactionBuilder 构建交易
        Transaction transaction = new TransactionBuilder(sourceAccount, Network.TESTNET)
                .addOperation(operation)
                .setTimeout(180)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build();

        transaction.sign(sourceKey);
        log.debug("[Stellar] 交易已签名，提交到链上...");
        TransactionResponse response = server.submitTransaction(transaction);

        if (!response.getSuccessful()) {
            log.error("[Stellar] 链上执行失败 dest={}, amount={}", destinationAccountId, amount);
            throw new RuntimeException("Stellar execution aborted on-chain.");
        }
        log.info("[Stellar] 清算完成 txHash={}", response.getHash());
        return response.getHash();
    }
}
