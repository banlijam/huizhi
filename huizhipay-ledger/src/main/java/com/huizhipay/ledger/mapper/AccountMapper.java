package com.huizhipay.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huizhipay.ledger.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    /**
     * 原子增加余额（乐观锁实现）
     * 返回影响行数：1 表示成功，0 表示版本冲突或账户不存在
     */
    @Update("UPDATE t_account SET " +
            "balance = balance + #{amount}, " +
            "version = version + 1, " +
            "updated_at = NOW() " +
            "WHERE account_no = #{accountNo} " +
            "AND version = #{oldVersion}")
    int increaseBalance(@Param("accountNo") String accountNo,
                        @Param("amount") BigDecimal amount,
                        @Param("oldVersion") Integer oldVersion);

    /**
     * 原子扣减余额（充值场景用不到，但收单/结算场景会用到，提前预备）
     * 额外加了 balance >= #{amount} 防超扣
     */
    @Update("UPDATE t_account SET " +
            "balance = balance - #{amount}, " +
            "version = version + 1, " +
            "updated_at = NOW() " +
            "WHERE account_no = #{accountNo} " +
            "AND version = #{oldVersion} " +
            "AND balance >= #{amount}")
    int decreaseBalance(@Param("accountNo") String accountNo,
                        @Param("amount") BigDecimal amount,
                        @Param("oldVersion") Integer oldVersion);
}