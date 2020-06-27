package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:42
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    // 批量删除 Attr对象 和 AttrGroup对象 的 关联关系 - 批量删除 Attr 和 AttrGroup 的 中间表
    void deleteBatchRelation(@Param("relationEntities") List<AttrAttrgroupRelationEntity> relationEntities);
}
