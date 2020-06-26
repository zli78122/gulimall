package com.atguigu.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrRespVo extends AttrVo {

    // 所属分组名称
    private String groupName;

    // 所属分类名称
    private String catelogName;

    // 分类路径 (从祖先节点到自身 的路径)
    private Long[] catelogPath;
}
