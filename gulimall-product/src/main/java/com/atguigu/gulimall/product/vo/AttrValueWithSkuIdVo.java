package com.atguigu.gulimall.product.vo;

import lombok.Data;

/**
 * 销售属性的属性值 以及 与该属性值关联的所有skuId
 *
 * AttrValueWithSkuIdVo(attrValue=亮黑色, skuIds=1,2,3)
 */
@Data
public class AttrValueWithSkuIdVo {
    private String attrValue;
    private String skuIds;
}
