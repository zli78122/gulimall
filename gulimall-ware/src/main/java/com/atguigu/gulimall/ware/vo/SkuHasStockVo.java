package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class SkuHasStockVo {
    // 商品id
    private Long skuId;
    // 商品是否有货
    private Boolean hasStock;
}
