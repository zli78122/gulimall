package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * spu属性值
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 批量保存
    void saveProductAttr(List<ProductAttrValueEntity> productAttrValueEntities);

    // 查询 spu 的 规格参数 (基本属性)
    List<ProductAttrValueEntity> baseAttrlistforspu(Long spuId);

    // 修改 spu 的 规格参数 (基本属性)
    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}
