package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class PurchaseDoneVo {

    /**
     * 采购单id
     */
    @NotNull
    private Long id;

    /**
     * 采购单 中的 采购项集合
     */
    private List<PurchaseItemDoneVo> items;
}
