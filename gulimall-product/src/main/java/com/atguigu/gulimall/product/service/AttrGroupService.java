package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 根据 商品分类id、关键字 分页查询
    PageUtils queryPage(Map<String, Object> params, Long categoryId);

    // 查询 当前分类下 所有 属性分组(AttrGroup) 以及 每个属性分组下 所有 属性(Attr)
    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId);
}
