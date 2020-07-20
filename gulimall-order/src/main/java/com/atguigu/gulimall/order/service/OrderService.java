package com.atguigu.gulimall.order.service;

import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:20:14
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 封装 OrderConfirmVo对象
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    // 提交订单 (下单)
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    // 根据 订单号 查询 订单信息
    OrderEntity getOrderByOrderSn(String orderSn);

    // 判断订单状态是否为 "待付款"，如果是，就取消订单
    void closeOrder(OrderEntity entity);

    // 根据 订单号 获取 PayVo对象
    PayVo getOrderPay(String orderSn);

    // 分页查询当前登录用户的所有订单信息
    PageUtils queryPageWithItem(Map<String, Object> params);

    // 支付完成 -> 保存支付信息 & 修改订单状态
    String handPayResult(PayAsyncVo vo);
}
