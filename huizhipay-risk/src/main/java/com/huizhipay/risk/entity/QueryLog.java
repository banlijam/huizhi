package com.huizhipay.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** API 查询日志（对应表 t_query_log） */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_query_log")
public class QueryLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 查询流水号（业务唯一键，对应账本流水中的 bizId） */
    private String queryNo;
    /** 发起查询的商户ID */
    private String merchantId;
    /** API 产品ID（关联定价表） */
    private String productId;
    /** 本次查询成本（主币单位，正数，与账本扣费金额绝对值一致） */
    private BigDecimal costAmount;
    /** 查询请求参数（JSON 字符串），在 Service 层 JSON.toJSONString() 后注入 */
    private String queryParams;
    /** 第三方 API 返回的原始内容（或脱敏摘要） */
    private String thirdPartyResponse;
    /** 查询状态 */
    private QueryStatus status;
    /** 失败时的错误信息 */
    private String errorMessage;
    /** 创建时间（由数据库默认值填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间（由数据库默认值填充，更新时自动刷新） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 查询状态：处理中 / 成功 / 失败(已回滚) / 已退款 */
    public enum QueryStatus { PROCESSING, SUCCESS, FAIL, REFUNDED }
}
