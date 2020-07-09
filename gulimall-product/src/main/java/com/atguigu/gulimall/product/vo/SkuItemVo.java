package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    // sku基本信息
    SkuInfoEntity info;

    // sku图片信息
    List<SkuImagesEntity> images;

    // 商品的销售属性信息
    List<SkuItemSaleAttrVo> saleAttr;

    // 商品的描述信息
    SpuInfoDescEntity desp;

    // 商品的所有属性分组信息以及每个属性分组下所有属性信息
    List<SpuItemAttrGroupVo> groupAttrs;

    // 是否有货
    Boolean hasStock = true;
}
