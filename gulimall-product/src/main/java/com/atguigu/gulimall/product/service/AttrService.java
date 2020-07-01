package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    // 分页条件查询
    PageUtils queryBaseAttrPage(String attrType, Long catelogId, Map<String, Object> params);

    // 根据 id 查询
    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    // 根据 attrGroupId 查询 Attr对象
    List<AttrEntity> getRelationAttr(Long attrGroupId);

    // 批量删除 Attr对象 和 AttrGroup对象 的 关联关系
    void deleteRelation(AttrGroupRelationVo[] attrGroupRelationVos);

    // 查询 可以跟 当前AttrGroup对象 关联的 所有Attr对象
    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrGroupId);

    // 根据 属性id 查询 search_type = 1 的基本属性 (search_type 表示 属性是否可以被检索)
    List<Long> selectSearchAttrIds(List<Long> attrIds);
}
