package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品二级分类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catelog2VO {

    // 一级父分类id
    private String catalog1Id;

    // 三级子分类
    private List<Catelog3VO> catalog3List;

    // 二级分类id
    private String id;

    // 二级分类名称
    private String name;

    /**
     * 三级分类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catelog3VO {
        // 二级父分类id
        private String catalog2Id;
        // 三级分类id
        private String id;
        // 三级分类名称
        private String name;
    }
}
