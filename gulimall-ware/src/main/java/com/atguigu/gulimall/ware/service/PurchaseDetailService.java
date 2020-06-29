package com.atguigu.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:25:27
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    // 分页条件查询
    PageUtils queryPage(Map<String, Object> params);

    List<PurchaseDetailEntity> listDetailByPurchaseId(Long id);
}
