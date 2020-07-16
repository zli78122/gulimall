package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    // 会员收货地址
    private MemberAddressVo address;
    // 运费
    private BigDecimal fare;
}
