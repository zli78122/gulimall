package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页分类的数据
 * 2级分类Vo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catelog2VO {

    private String catalog1Id;//1级父分类id

    private List<Catelog3VO> catalog3List;//3级子分类id

    private String id;//当前2级分类的id

    private String name;//当前2级分类的name

    /**
     * 3级分类Vo
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catelog3VO {

        private String catalog2Id;

        private String id;

        private String name;
    }
}
