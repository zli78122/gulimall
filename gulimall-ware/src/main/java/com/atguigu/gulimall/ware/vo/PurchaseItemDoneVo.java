package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {
    // 采购项id
    private Long itemId;
    // 采购项状态
    private Integer status;
    // 采购失败原因
    private String reason;
}
