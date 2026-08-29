-- Checkout 公开令牌与完成后返回地址。
-- 旧订单使用不可逆的 legacy token 回填；新订单由应用生成随机 ct_* token。
alter table t_payment_order add column checkout_token varchar(80);
alter table t_payment_order add column return_url varchar(1024);

update t_payment_order
set checkout_token = 'legacy_' || md5(order_no),
    return_url = '/merchant'
where checkout_token is null;

alter table t_payment_order alter column checkout_token set not null;
alter table t_payment_order alter column return_url set default '/merchant';
alter table t_payment_order alter column return_url set not null;

create unique index uk_payment_order_checkout_token on t_payment_order(checkout_token);
