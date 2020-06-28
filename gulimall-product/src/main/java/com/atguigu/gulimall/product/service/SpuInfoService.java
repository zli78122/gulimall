package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:03:41
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 新增商品
    void saveSpuInfo(SpuSaveVo spuSaveVo);

    // 保存spu基本信息 - 对应 pms_spu_info 数据表
    void saveBaseSpuInfo(SpuInfoEntity infoEntity);

    // 分页条件查询
    PageUtils queryPageByCondition(Map<String, Object> params);
}
