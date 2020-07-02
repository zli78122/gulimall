package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.Catelog2VO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 查询所有分类，以树形结构组装起来
    List<CategoryEntity> listWithTree();

    // 批量删除
    void removeCategoryByIds(List<Long> asList);

    // 查询 商品分类 的 catelogPath - 从祖先节点到自身 的路径
    Long[] findCatelogPath(Long categoryId);

    // 级联更新
    void updateCascade(CategoryEntity category);

    // 查询所有一级分类
    List<CategoryEntity> getLevelOneCategories();

    // 查询商品分类信息，封装成固定格式，用于前台首页显示
    Map<String, List<Catelog2VO>> getCatelogJson();
}
