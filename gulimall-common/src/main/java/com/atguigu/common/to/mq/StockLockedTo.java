package com.atguigu.common.to.mq;

import lombok.Data;

/**
 * 库存工作单TO
 */
@Data
public class StockLockedTo {
    // 库存工作单id
    private Long id;
    // 库存工作单详情
    private StockDetailTo detailTo;
}
