package com.atguigu.gulimall.member.feign;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * 这是一个声明式的远程调用
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

}
