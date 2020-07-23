package com.atguigu.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Redisson 配置
 */
@Configuration
public class MyRedissonConfig {

    /**
     * 所有对 Redisson 的操作都是通过 RedissonClient对象
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        Config config = new Config();

        // 集群模式
        // config.useClusterServers().addNodeAddress("127.0.0.1:7001", "127.0.0.1:7002");

        // 单节点模式
        config.useSingleServer().setAddress("redis://192.168.56.200:6379");

        RedissonClient redissonClient = Redisson.create(config);

        return redissonClient;
    }
}
