package com.atguigu.gulimall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池 配置
 */
@Component
@ConfigurationProperties(prefix = "gulimall.thread")
@Data
public class ThreadPoolConfigProperties {
    // 核心线程数
    private Integer coreSize;
    // 最大线程数
    private Integer maxSize;
    // 休眠时长
    private Integer keepAliveTime;
}
