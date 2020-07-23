package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SecKillService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品定时上架
 */
@Service
public class SecKillSkuScheduled {

    private final String upload_lock = "seckill:upload:lock";

    @Autowired
    private SecKillService seckillService;

    @Autowired
    private RedissonClient redissonClient;

    // 定时任务 : 将 最近三天的商品秒杀活动信息 缓存到Redis中
    @Scheduled(cron = "0 * * * * ?")
    public void uploadSecKillSkuLatest3Days() {
        System.out.println("定时任务执行...");

        /*
         * 分布式锁
         *
         * 分布式环境下，定时任务面临的问题
         *   当前微服务有多个，每个都会定时执行定时任务
         *   但是，业务要求同一个定时任务在多个相同的微服务中执行一次即可，不需要每个微服务都执行一遍 -> 要求满足 定时任务的幂等性
         *
         * 使用分布式锁实现
         *   得到锁的微服务才能执行，没有得到锁的要等得到锁的微服务释放锁之后，看看有没有执行成功
         *   如果执行成功了，就不需要再执行了
         *   如果没有执行成功，大家再去争抢锁，抢到的微服务执行
         *   这样保证了 多个相同的微服务中，每次只有一个执行定时任务 (满足 定时任务的幂等性)
         */

        // 获取 分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        // 加锁
        lock.lock(10, TimeUnit.SECONDS);
        try {
            // 将 最近三天的商品秒杀活动信息 缓存到Redis中
            seckillService.uploadSecKillSkuLatest3Days();
        } finally {
            // 解锁
            lock.unlock();
        }
    }
}
