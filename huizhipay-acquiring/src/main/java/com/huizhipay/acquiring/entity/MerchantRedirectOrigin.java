package com.huizhipay.acquiring.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
@Data @Accessors(chain=true) @TableName("t_merchant_redirect_origin")
public class MerchantRedirectOrigin {
 @TableId(type=IdType.AUTO) private Long id; private String merchantId; private String environment; private String origin;
 private Boolean verified; private String verificationTokenHash; private String createdBy; private LocalDateTime verifiedAt;
 private LocalDateTime disabledAt; private LocalDateTime createdAt;
}
