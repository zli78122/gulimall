package com.atguigu.gulimall.order.vo;

import lombok.Data;

/**
 * 商品库存VO
 */
@Data
public class SkuStockVo {
    // 商品id
    private Long skuId;
    // 商品是否有货
    private Boolean hasStock;
}
