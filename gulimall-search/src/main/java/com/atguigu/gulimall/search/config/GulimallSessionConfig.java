package com.atguigu.gulimall.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session 配置
 */
@Configuration
public class GulimallSessionConfig {

    /**
     * Cookie Serializer
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        // 放大Cookie的作用域 - 设置Cookie的作用域为 父域(gulimall.com)
        cookieSerializer.setDomainName("gulimall.com");
        // 设置 Cookie 名称
        cookieSerializer.setCookieName("GULISESSION");
        return cookieSerializer;
    }

    /**
     * Redis Serializer - Json
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
