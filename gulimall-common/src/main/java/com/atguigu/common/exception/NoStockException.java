package com.atguigu.common.exception;

/**
 * 商品库存不足 异常
 */
public class NoStockException extends RuntimeException {

    private Long skuId;

    public NoStockException(Long skuId) {
        super("skuId 为 " + skuId + " 的商品没有足够的库存了");
    }

    public NoStockException(String msg) {
        super(msg);
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
