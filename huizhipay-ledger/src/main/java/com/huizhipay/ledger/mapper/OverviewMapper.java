package com.huizhipay.ledger.mapper;

import com.huizhipay.ledger.dto.DailyStat;
import com.huizhipay.ledger.dto.OverviewRangeStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指挥中心聚合查询（读模型）：直接查 t_payment_order 做统计，避免 ledger → acquiring 反向依赖。
 */
@Mapper
public interface OverviewMapper {

    /**
     * 统计某时间区间内：成功笔数 / 总笔数 / 成功交易额。
     * 使用 PostgreSQL 的 aggregate filter 语法。
     */
    @Select("select count(*) filter (where status = 'SUCCESS') as success_count, "
            + "count(*) as total_count, "
            + "coalesce(sum(amount) filter (where status = 'SUCCESS'), 0) as success_volume "
            + "from t_payment_order "
            + "where merchant_id = #{merchantId} and created_at >= #{start} and created_at < #{end} and deleted = 0")
    OverviewRangeStat statsForRange(@Param("merchantId") String merchantId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /**
     * 最近 7 天按天统计（仅返回有数据的日期）。
     */
    @Select("select date(created_at) as d, "
            + "count(*) filter (where status = 'SUCCESS') as success_count, "
            + "count(*) as total_count "
            + "from t_payment_order "
            + "where merchant_id = #{merchantId} and created_at >= #{start} and deleted = 0 "
            + "group by d order by d")
    List<DailyStat> dailyStats(@Param("merchantId") String merchantId,
                               @Param("start") LocalDateTime start);
}
