package com.huizhipay.acquiring.transfi;

import com.huizhipay.acquiring.transfi.dto.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange
public interface TransFiClient {

    // ==================== Users ====================

    @GetExchange("/users/individual")
    TransFiResponse<List<TransFiUser>> queryUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone);

    /**
     * 创建个人用户
     */
    @PostExchange("/users/individual")
    TransFiResponse<CreateUserResponse> createIndividualUser(@RequestBody CreateIndividualUserRequest request);

    /**
     * 更新个人用户（修正 firstName、lastName、dob）
     */
    @PatchExchange("/users/individual")
    TransFiResponse<UpdateUserResponse> updateIndividualUser(@RequestBody UpdateIndividualUserRequest request);

    /**
     * 获取企业用户列表
     */
    @GetExchange("/users/business")
    TransFiResponse<List<TransFiUser>> listBusinessUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone);

    /**
     * 创建企业用户
     */
    @PostExchange("/users/business")
    TransFiResponse<CreateUserResponse> createBusinessUser(@RequestBody CreateBusinessUserRequest request);

    /**
     * 获取用户账户类型（SENDER / RECIPIENT）
     */
    @GetExchange("/users/account-type")
    TransFiResponse<AccountTypeData> getAccountType(@RequestParam String userId);

    // ==================== Orders ====================

    /**
     * 获取订单列表
     */
    @GetExchange("/orders")
    TransFiResponse<OrderListData> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String currency);

    /**
     * 创建订单
     */
    @PostExchange("/orders")
    TransFiResponse<TransFiOrder> createOrder(@RequestBody CreateOrderRequest request);

    /**
     * 获取订单详情
     */
    @GetExchange("/orders/{orderId}")
    TransFiResponse<TransFiOrder> getOrder(@PathVariable String orderId);

    /**
     * 取消订单（仅 offramp 类型且未收到加密货币存款时可取消）
     */
    @PostExchange("/orders/cancel")
    TransFiResponse<CancelOrderResponse> cancelOrder(@RequestBody CancelOrderRequest request);
}
