package com.atguigu.gulimall.ware.service;

import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:25:27
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 查询 未领取的采购单
    PageUtils queryPageUnreceivePurchase(Map<String, Object> params);

    // 合并 采购单 和 采购项 - 把多个 采购项 添加到一个 采购单 中
    void mergePurchase(MergeVo mergeVo);

    // 领取采购单
    void received(List<Long> ids);

    // 完成采购
    void done(PurchaseDoneVo doneVo);
}
