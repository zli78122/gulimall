package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品的所有属性分组信息以及每个属性分组下所有属性信息
 */
@Data
public class SpuItemAttrGroupVo {
    // 分组名称
    private String groupName;
    // 当前分组下的所有基本属性
    private List<Attr> attrs;
}
