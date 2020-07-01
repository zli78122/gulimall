package com.atguigu.gulimall.ware.service;

import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:25:27
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    // 分页条件查询
    PageUtils queryPage(Map<String, Object> params);

    // 将 成功采购的商品 入库
    void addStock(Long skuId, Long wareId, Integer skuNum);

    // 查询sku是否有库存
    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);
}
