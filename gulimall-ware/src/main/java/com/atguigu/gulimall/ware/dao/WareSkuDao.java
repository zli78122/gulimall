package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
