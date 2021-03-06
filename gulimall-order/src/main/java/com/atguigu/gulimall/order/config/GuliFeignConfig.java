package com.atguigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class GuliFeignConfig {

    /*
     * Feign远程调用丢失请求头
     *   出现这个问题的原因 :
     *     Feign远程调用请求 和 客户端发送给浏览器的请求(原始请求) 是 两个不同的请求
     *     Feign远程调用请求 不可能自动拥有 原始请求的请求头数据
     *
     *   这个问题引发的结果 :
     *     Feign远程调用其他微服务，其他微服务无法从Feign请求中获取 原始请求的相关数据 (比如 : Cookie 请求头)
     *     导致远程微服务无法获取 原始请求的Cookie数据，即不能判断当前用户是否已登录
     *     所以会默认用户未登录，导致逻辑判断出错，进而引发了一系列错误
     *
     *   解决方案 :
     *     创建一个 Feign远程调用的请求拦截器 (远程调用请求在发出之前，会被该拦截器拦截)
     *     该拦截器会获取 原始请求的请求头数据，并将请求头数据同步到 Feign远程调用请求 中，以此来解决 Feign远程调用丢失请求头 的问题
     */

    /**
     * Feign远程调用的请求拦截器 : 远程调用请求在发出之前，会被该拦截器拦截
     *
     * 该拦截器会获取 原始请求的请求头数据，并将请求头数据同步到 Feign远程调用请求 中，以此来解决 Feign远程调用丢失请求头 的问题
     */
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    // 获取原始请求对象
                    HttpServletRequest request = attributes.getRequest();
                    if (request != null) {
                        // 同步请求头数据 (主要同步 Cookie 请求头)
                        // 获取原始请求的 Cookie 请求头
                        String cookie = request.getHeader("Cookie");
                        // 将 Cookie 请求头 设置到 远程调用请求 中
                        requestTemplate.header("Cookie", cookie);
                    }
                }
            }
        };
    }
}
