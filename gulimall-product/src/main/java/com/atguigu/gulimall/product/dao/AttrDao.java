package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    // 根据 属性id 查询 search_type = 1 的基本属性 (search_type 表示 属性是否可以被检索)
    List<Long> selectSearchAttrIds(@Param("attrIds") List<Long> attrIds);
}
