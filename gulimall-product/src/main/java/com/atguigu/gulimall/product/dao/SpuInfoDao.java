package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    // 更新 当前spu 的 发布状态
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
