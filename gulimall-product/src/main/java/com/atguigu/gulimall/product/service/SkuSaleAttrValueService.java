package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 根据 spuId 获取 商品的销售属性信息
    List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId);

    // 根据 skuId 获取 商品的所有销售属性的属性值
    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}
