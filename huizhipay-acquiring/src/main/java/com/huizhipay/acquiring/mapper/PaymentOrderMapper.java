package com.huizhipay.acquiring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huizhipay.acquiring.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}