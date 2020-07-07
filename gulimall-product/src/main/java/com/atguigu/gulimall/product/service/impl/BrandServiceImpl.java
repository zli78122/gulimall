package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    // 根据 brandIds 获取 品牌信息
    @Override
    public List<BrandEntity> getBrandsByIds(List<Long> brandIds) {
        QueryWrapper<BrandEntity> wrapper = new QueryWrapper<>();
        wrapper.in("brand_id", brandIds);
        List<BrandEntity> list = baseMapper.selectList(wrapper);
        return list;
    }

    // 级联更新
    @Transactional
    @Override
    public void updateDetail(BrandEntity brand) {
        // 更新自身
        this.updateById(brand);

        if (!StringUtils.isEmpty(brand.getName())) {
            // 同步更新其他关联表中的数据 (级联更新)
            categoryBrandRelationService.updateBrand(brand.getBrandId(), brand.getName());
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq("brand_id", key).or().like("name", key);
        }

        IPage<BrandEntity> page = this.page(new Query<BrandEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }
}
