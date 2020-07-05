package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传过来的查询条件
 */
@Data
public class SearchParam {

    /**
     * 全文匹配关键字
     */
    private String keyword;

    /**
     * 三级分类id
     */
    private Long catalog3Id;

    /**
     * 排序条件
     * sort=saleCount_asc/saleCount_desc   -   销量排序
     * sort=skuPrice_asc/skuPrice_desc     -   价格排序
     * sort=hotScore_asc/hotScore_desc     -   热度评分排序
     */
    private String sort;

    /**
     * 过滤条件
     * hasStock(是否有货)、skuPrice(价格区间)、brandId、catalog3Id，attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     */
    // 是否有货 : 0-无库存   1-有库存
    private Integer hasStock;

    // 价格区间 : skuPrice=1_500/_500/500_
    private String skuPrice;

    // 品牌 (可以多选) : brandId=1&brandId=2
    private List<Long> brandId;

    /**
     * 按属性筛选 (可以多选)
     *
     * attrs=[属性id]_属性值1:属性值2&attrs=[属性id]_属性值1:属性值2
     * e.g.   attrs=1_5寸:6寸&attrs=2_16G:8G
     */
    private List<String> attrs;

    // 页码
    private Integer pageNum = 1;

    // 原生的所有查询字符串
    private String _queryString;
}
