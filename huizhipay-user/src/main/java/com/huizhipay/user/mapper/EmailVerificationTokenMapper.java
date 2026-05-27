package com.huizhipay.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huizhipay.user.entity.EmailVerificationToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailVerificationTokenMapper extends BaseMapper<EmailVerificationToken> {
}