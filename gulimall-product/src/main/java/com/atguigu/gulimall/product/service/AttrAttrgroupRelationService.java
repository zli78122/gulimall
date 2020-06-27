package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:42
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 批量添加 AttrGroup对象 和 Attr对象 的 关联关系
    void saveBatch(List<AttrGroupRelationVo> attrGroupRelationVos);
}
