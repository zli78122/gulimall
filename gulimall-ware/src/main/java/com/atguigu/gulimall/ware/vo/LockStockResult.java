package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResult {
    // 商品id
    private Long skuId;
    // 锁了几件该商品
    private Integer num;
    // 是否锁定成功
    private Boolean locked;
}
