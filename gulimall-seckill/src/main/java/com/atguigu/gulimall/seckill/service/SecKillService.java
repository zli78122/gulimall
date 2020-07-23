package com.atguigu.gulimall.seckill.service;

import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;

import java.util.List;

public interface SecKillService {

    // 将 最近三天的商品秒杀活动信息 缓存到Redis中
    void uploadSecKillSkuLatest3Days();

    // 获取 当前时间正在参与秒杀的所有商品
    List<SecKillSkuRedisTo> getCurrentSecKillSku();

    // 根据 skuId 获取 商品秒杀信息
    SecKillSkuRedisTo getSkuSecKillInfo(Long skuId);

    // 秒杀
    String secKill(String killId, String key, Integer num);
}
