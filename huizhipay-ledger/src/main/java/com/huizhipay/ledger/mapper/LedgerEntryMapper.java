package com.huizhipay.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huizhipay.ledger.entity.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface LedgerEntryMapper extends BaseMapper<LedgerEntry> {

    /**
     * 全量累计成功付款的毛额：对负向（负债）流水的金额取绝对值求和，
     * 与 getLedger 明细查询使用同一过滤条件（biz_type=PAYMENT 且 amount<0）。
     */
    @Select("select coalesce(sum(abs(amount)), 0) from t_ledger_entry "
            + "where merchant_id = #{merchantId} and biz_type = 'PAYMENT' and amount < 0")
    BigDecimal sumPaymentGross(@Param("merchantId") String merchantId);

    /**
     * 同条件流水笔数，用于账本 KPI 副标题展示真实全量笔数。
     */
    @Select("select count(*) from t_ledger_entry "
            + "where merchant_id = #{merchantId} and biz_type = 'PAYMENT' and amount < 0")
    long countPaymentGross(@Param("merchantId") String merchantId);
}
