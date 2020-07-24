package com.atguigu.gulimall.product.feign.fallback;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.feign.SecKillFeignService;
import org.springframework.stereotype.Component;

/**
 * 熔断 - 远程调用秒杀服务失败，触发熔断机制
 */
@Component
public class SecKillFeignServiceFallBack implements SecKillFeignService {

    @Override
    public R getSkuSecKillInfo(Long skuId) {
        System.out.println("SecKillFeignServiceFallBack - getSkuSecKillInfo() 熔断...");
        return R.error(BizCodeEnum.TOO_MANY_REQUEST_EXCEPTION.getCode(), BizCodeEnum.TOO_MANY_REQUEST_EXCEPTION.getMsg());
    }
}
