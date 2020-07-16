package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单提交的数据
 */
@Data
public class OrderSubmitVo {
    // 收货地址id
    private Long addrId;

    // 支付方式
    private Integer payType;

    // 防重令牌
    private String orderToken;

    // 应付总额 (需要验价)
    private BigDecimal payPrice;

    // 订单备注信息
    private String note;

    // 优惠信息

    // 发票信息

    // 无需提交需要购买的商品，去购物车再获取一次
    // 用户相关信息，session中取出登录的用户
}
