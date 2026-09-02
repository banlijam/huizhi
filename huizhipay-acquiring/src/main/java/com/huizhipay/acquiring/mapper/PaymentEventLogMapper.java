package com.huizhipay.acquiring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huizhipay.acquiring.entity.PaymentEventLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentEventLogMapper extends BaseMapper<PaymentEventLog> {
}
