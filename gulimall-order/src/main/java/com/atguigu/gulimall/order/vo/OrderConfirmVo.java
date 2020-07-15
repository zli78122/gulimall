package com.atguigu.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页VO
 */
public class OrderConfirmVo {

    // 会员收货地址
    @Setter
    @Getter
    private List<MemberAddressVo> address;

    // 所有选中的购物项
    @Setter
    @Getter
    private List<OrderItemVo> items;

    // 发票信息
    // ...

    // 优惠劵信息 (会员积分)
    @Setter
    @Getter
    private Integer integration;

    // 商品库存状态 (商品是否有货)
    @Setter
    @Getter
    private Map<Long, Boolean> stocks;

    // 订单防重令牌，防止重复提交订单
    @Setter
    @Getter
    private String orderToken;

    // 商品总件数 (所有购物项商品件数之和)
    private Integer count;

    public Integer getCount() {
        Integer i = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

    // 订单总额
    private BigDecimal total;

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    // 应付价格
    private BigDecimal payPrice;

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
