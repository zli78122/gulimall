package com.atguigu.gulimall.auth.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("gulimall-third-party")
public interface ThirdPartyFeignService {

    // 发送验证码
    @GetMapping("/sms/sendcode")
    R sendSMSCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
