package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * 创建订单请求（onramp / offramp / payin / payout / fiat_prefund / crypto_prefund / swap / gaming）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderRequest {
    /** 合作伙伴交易 ID（可选；用于将自己的交易 ID 与 TransFi 订单 ID 关联） */
    private String partnerId;
    /** 用户 ID（UX- 开头，onramp / offramp / payin / payout / swap / gaming 必填；第三方 offramp 可省略，改用 source.userId + destination.userId） */
    private String userId;
    /** 发票 ID（IN- 开头，payin / payout 场景使用） */
    private String invoiceId;
    /** 客户元数据（原样透传，可选） */
    private Map<String, Object> customerMetaData;
    /** 结算账户 ID，仅 payin 和 gaming 适用；结算账户币种需与 destination.currency 一致 */
    private String settlementAccountId;
    /** 支付目的代码（purposeCode），必填 */
    private String purposeCode;
    /** 订单类型：onramp / offramp / payin / payout / fiat_prefund / crypto_prefund / swap / gaming */
    private String orderType;
    /** 商户来源页面 URL（https 协议） */
    private String sourceUrl;
    /** 用户支付成功后的回跳 URL（https 协议；未在 dashboard 配置默认 redirectUrl 时必填） */
    private String successRedirectUrl;
    /** 用户支付失败后的回跳 URL（https 协议；未在 dashboard 配置默认 redirectUrl 时必填） */
    private String failureRedirectUrl;
    /** 无头模式，默认 false；为 true 时用户不跳转到支付页面，需在 payload 中提供全部支付细节 */
    private Boolean headlessMode;
    /** 源（付款方），必填 */
    private OrderSource source;
    /** 目标（收款方），必填 */
    private OrderDestination destination;
    /** 设备详情（可选；用于风控） */
    private DeviceDetails deviceDetails;
    /** 支付页面定制化设置（可选） */
    private Customization customization;

    /** 设备详情 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceDetails {
        /** 浏览器 User-Agent */
        private String userAgent;
        /** IP 信息 */
        private IpInfo ipInfo;
    }

    /** IP 信息 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IpInfo {
        /** 客户端 IP 地址（IPv4 或 IPv6） */
        private String ip;
        /** 3 位国家代码；不传时服务端会根据 IP 自动解析 */
        private String countryCode3;
    }

    /** 支付页面定制化设置 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customization {
        /** 页面语言：en / id / zh / pt / es / vi / tl / bn */
        private String locale;
    }
}
