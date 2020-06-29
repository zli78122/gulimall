package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService detailService;

    @Autowired
    private WareSkuService wareSkuService;

    // 完成采购
    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        // 采购单id
        Long id = doneVo.getId();

        // 修改 采购单 和 采购项 的 状态，如果有一个采购项的状态为失败，那么这个采购项所在的采购单的状态为失败

        // 标志 采购单转态 是否失败
        boolean flag = true;
        // 采购项集合
        List<PurchaseItemDoneVo> purchaseItems = doneVo.getItems();
        List<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
        for (PurchaseItemDoneVo item : purchaseItems) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            // 如果有一个采购项的状态为失败，那么这个采购项所在的采购单的状态为失败
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
            } else {
                // 当前采购项 采购成功，修改 采购项状态 为 已完成
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                // 根据 采购项id 获取 采购项对象
                PurchaseDetailEntity entity = detailService.getById(item.getItemId());
                // 将 成功采购的商品 入库 - 对应 wms_ware_sku 数据表
                wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            purchaseDetailEntities.add(detailEntity);
        }
        // 批量保存 采购项数据
        detailService.updateBatchById(purchaseDetailEntities);

        // 保存 采购单数据
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    // 领取采购单
    @Transactional
    @Override
    public void received(List<Long> ids) {
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            // 根据 采购单id 获取 采购单对象
            PurchaseEntity purchaseEntity = this.getById(id);
            return purchaseEntity;
        }).filter(item -> {
            // 保证当前 采购单状态 是 新建状态 或者 已分配状态，否则不能领取
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {
            // 修改 采购单状态 为 已领取状态
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        // 批量修改 采购单数据
        this.updateBatchById(collect);

        // 修改 采购项数据
        collect.forEach((item) -> {
            List<PurchaseDetailEntity> entities = detailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                purchaseDetailEntity.setId(entity.getId());
                // 修改 采购项状态 为 正在采购状态
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return purchaseDetailEntity;
            }).collect(Collectors.toList());
            // 修改 采购项数据
            detailService.updateBatchById(detailEntities);
        });
    }

    // 合并 采购单 和 采购项 - 把多个 采购项 添加到一个 采购单 中
    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        // 如果 采购单id 为空，则 创建并保存一个新的 采购单对象
        // 如果 采购单id 不为空，则 只有当 采购单状态值 = 0 or 1 时，才可以 合并 采购单 和 采购项
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            // 获取 采购单id
            purchaseId = purchaseEntity.getId();
        } else {
            PurchaseEntity purchaseEntity = this.getById(purchaseId);
            if (purchaseEntity.getStatus() != 0 && purchaseEntity.getStatus() != 1) {
                return;
            }
        }

        // 保存 采购项
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(item -> {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            detailEntity.setId(item);
            detailEntity.setPurchaseId(finalPurchaseId);
            detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return detailEntity;
        }).collect(Collectors.toList());
        detailService.updateBatchById(collect);

        // 更新 采购单
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    // 查询 未领取的采购单
    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }
}
