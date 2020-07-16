package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 下单返回数据VO
 */
@Data
public class SubmitOrderResponseVo {
    // 订单
    private OrderEntity order;
    // 状态码 (0 : 成功)
    private Integer code;
}
