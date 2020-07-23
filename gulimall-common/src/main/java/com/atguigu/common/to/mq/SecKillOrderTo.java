package com.atguigu.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀订单TO
 */
@Data
public class SecKillOrderTo {

    /**
     * 订单号
     */
    private String orderSn;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 购买数量
     */
    private Integer num;
    /**
     * 会员id
     */
    private Long memberId;
}
