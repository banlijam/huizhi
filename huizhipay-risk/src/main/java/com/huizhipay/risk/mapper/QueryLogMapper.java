package com.huizhipay.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huizhipay.risk.dto.DeveloperQueryLogResponse;
import com.huizhipay.risk.entity.QueryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QueryLogMapper extends BaseMapper<QueryLog> {

    @Select("""
            select query_no as queryNo,
                   product_id as productId,
                   cost_amount as costAmount,
                   status,
                   error_message as errorMessage,
                   created_at as createdAt
              from t_query_log
             where merchant_id = #{merchantId}
             order by created_at desc, id desc
             limit 100
            """)
    List<DeveloperQueryLogResponse> selectLatestForMerchant(@Param("merchantId") String merchantId);
}
