package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物项VO
 */
@Data
public class OrderItemVo {

    private Long skuId;

    private String title;

    private String image;

    // 销售属性
    private List<String> skuAttr;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

    // 商品重量
    private BigDecimal weight;
}
