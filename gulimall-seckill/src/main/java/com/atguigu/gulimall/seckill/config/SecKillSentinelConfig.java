package com.atguigu.gulimall.seckill.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class SecKillSentinelConfig {

    /**
     * 自定义流控异常
     */
//    @Bean
//    public BlockExceptionHandler blockExceptionHandler() {
//        return new BlockExceptionHandler() {
//            @Override
//            public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
//                R error = R.error(BizCodeEnum.TOO_MANY_REQUEST_EXCEPTION.getCode(), BizCodeEnum.TOO_MANY_REQUEST_EXCEPTION.getMsg());
//                response.setCharacterEncoding("UTF-8");
//                response.setContentType("application/json");
//                response.getWriter().write(JSON.toJSONString(error));
//            }
//        };
//    }
}
