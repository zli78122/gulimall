package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:25:27
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    // 更新 商品库存记录
    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    // 查询 当前sku 的 总库存量
    Long getSkuStock(Long skuId);

    // 根据 skuId 查询 该商品在哪些仓库中有库存
    List<Long> listWareIdHasSkuStock(@Param("skuId") Long skuId);

    // 锁定库存 (根据 商品id、仓库id、需要锁定的商品件数 锁定库存)
    Long lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);

    // 解锁库存 (根据 商品id、仓库id、需要解锁的商品件数 解锁库存)
    void unLockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);
}
