package com.atguigu.gulimall.coupon.service;

import com.atguigu.common.to.SkuReductionTo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:03:13
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 保存sku的 商品会员价格、商品满减信息、商品折扣信息
    void saveSkuReduction(SkuReductionTo reductionTo);
}
