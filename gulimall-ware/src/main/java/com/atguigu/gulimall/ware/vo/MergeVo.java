package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVo {
    // 采购单id
    private Long purchaseId;
    // 采购项id 集合
    private List<Long> items;
}
