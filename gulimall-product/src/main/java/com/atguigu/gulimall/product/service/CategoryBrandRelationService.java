package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 新增 品牌与分类的关联关系
    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    // 更新 品牌与分类的关联关系 中的 品牌信息
    void updateBrand(Long brandId, String name);

    // 更新 品牌与分类的关联关系 中的 分类信息
    void updateCategory(Long catId, String name);

    // 获取 与指定分类相关联的品牌 (根据 分类id 查询 品牌)
    List<BrandEntity> getBrandsByCatId(Long catId);
}
