package com.atguigu.gulimall.order.to;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {
    // 订单
    private OrderEntity order;
    // 订单项
    private List<OrderItemEntity> items;
    // 应付价格
    private BigDecimal payPrice;
    // 运费
    private BigDecimal fare;
}
