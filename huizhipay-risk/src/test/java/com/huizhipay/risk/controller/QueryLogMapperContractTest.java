package com.huizhipay.risk.controller;

import com.huizhipay.risk.mapper.QueryLogMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class QueryLogMapperContractTest {

    @Test
    void developerQueryIsMerchantBoundAndDoesNotSelectSensitivePayloads() throws Exception {
        Method method = QueryLogMapper.class.getMethod("selectLatestForMerchant", String.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value())
                .replaceAll("\\s+", " ")
                .toLowerCase();

        assertThat(sql).contains("where merchant_id = #{merchantid}");
        assertThat(sql).contains("limit 100");
        assertThat(sql).doesNotContain("query_params", "third_party_response");
    }
}
