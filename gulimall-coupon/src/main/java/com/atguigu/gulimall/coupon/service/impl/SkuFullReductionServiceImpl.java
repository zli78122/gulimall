package com.atguigu.gulimall.coupon.service.impl;

import com.atguigu.common.to.MemberPrice;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.gulimall.coupon.entity.MemberPriceEntity;
import com.atguigu.gulimall.coupon.entity.SkuLadderEntity;
import com.atguigu.gulimall.coupon.service.MemberPriceService;
import com.atguigu.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.coupon.dao.SkuFullReductionDao;
import com.atguigu.gulimall.coupon.entity.SkuFullReductionEntity;
import com.atguigu.gulimall.coupon.service.SkuFullReductionService;

@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    private SkuLadderService skuLadderService;

    @Autowired
    private MemberPriceService memberPriceService;

    // 保存sku的 商品会员价格、商品满减信息、商品折扣信息
    @Override
    public void saveSkuReduction(SkuReductionTo reductionTo) {
        // 6-4 保存sku的 商品会员价格、商品满减信息、商品折扣信息
        //   商品会员价格 对应 gulimall_sms 数据库 的 sms_member_price 数据表
        //   商品满减信息 对应 gulimall_sms 数据库 的 sms_sku_full_reduction 数据表
        //   商品折扣信息 对应 gulimall_sms 数据库 的 sms_sku_ladder 数据表


        // 1.保存sku的商品折扣信息 - 对应 gulimall_sms 数据库 的 sms_sku_ladder 数据表
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(reductionTo.getSkuId());
        skuLadderEntity.setFullCount(reductionTo.getFullCount());
        skuLadderEntity.setDiscount(reductionTo.getDiscount());
        skuLadderEntity.setAddOther(reductionTo.getCountStatus());
        if (reductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        // 2.保存sku的商品满减信息 - 对应 gulimall_sms 数据库 的 sms_sku_full_reduction 数据表
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(reductionTo, reductionEntity);
        if (reductionEntity.getFullPrice().compareTo(new BigDecimal("0")) > 0) {
            this.save(reductionEntity);
        }

        // 3.保存sku的商品会员价格 - 对应 gulimall_sms 数据库 的 sms_member_price 数据表
        List<MemberPrice> memberPrice = reductionTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
            MemberPriceEntity priceEntity = new MemberPriceEntity();
            priceEntity.setSkuId(reductionTo.getSkuId());
            priceEntity.setMemberLevelId(item.getId());
            priceEntity.setMemberLevelName(item.getName());
            priceEntity.setMemberPrice(item.getPrice());
            priceEntity.setAddOther(reductionTo.getPriceStatus());
            return priceEntity;
        }).filter(item -> {
            return item.getMemberPrice().compareTo(new BigDecimal("0")) > 0;
        }).collect(Collectors.toList());
        memberPriceService.saveBatch(collect);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }
}
