package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 商品的销售属性信息
 *
 * SkuItemSaleAttrVo(
 *     attrId=21, attrName=颜色, attrValues=[AttrValueWithSkuIdVo(attrValue=亮黑色, skuIds=1,2,3),
 *                                          AttrValueWithSkuIdVo(attrValue=星河银, skuIds=4,5,6),
 *                                          AttrValueWithSkuIdVo(attrValue=翡冷翠, skuIds=7,8,9)])
 */
@ToString
@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
