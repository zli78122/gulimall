package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Autowired
    private AttrGroupDao attrGroupDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private CategoryService categoryService;

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity); //属性复制

        // 1.更新 基本信息
        this.updateById(attrEntity);

        // 只有 基本属性 有 分组信息，销售属性 不需要 分组信息   =>   Attr 和 AttrGroup 的 中间表 不需要保存 销售属性
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 2.更新 Attr 和 AttrGroup 的 中间表
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());
            // 根据 attrId 查询 Attr 和 AttrGroup 的 中间表 是否有数据
            //    如果有数据 -> 执行更新操作
            //    如果没有数据 -> 执行插入操作
            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if (count > 0) {
                // 更新操作
                relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            } else {
                // 插入操作
                relationDao.insert(relationEntity);
            }
        }
    }

    // 根据 id 查询
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        // 1.查询 基本信息
        AttrEntity attrEntity = this.getById(attrId);

        AttrRespVo respVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, respVo); //属性复制

        // 只有 基本属性 有 分组信息，销售属性 不需要 分组信息
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 2.查询 分组名称
            // 根据 attrId 查询 Attr 和 AttrGroup 的 中间表
            AttrAttrgroupRelationEntity attrgroupRelation = relationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId)
            );
            if (attrgroupRelation != null) {
                // attrGroupId
                respVo.setAttrGroupId(attrgroupRelation.getAttrGroupId());
                // 根据 attrGroupId 查询 AttrGroup 表
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupRelation.getAttrGroupId());
                if (attrGroupEntity != null) {
                    // 分组名称
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 3.查询 分类名称 & 分类路径
        // 分类id
        Long catelogId = attrEntity.getCatelogId();
        // 分类路径
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        respVo.setCatelogPath(catelogPath);
        // 根据 catelogId 查询 Category 表
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null) {
            // 分类名称
            respVo.setCatelogName(categoryEntity.getName());
        }

        return respVo;
    }

    // 分页条件查询
    @Override
    public PageUtils queryBaseAttrPage(String attrType, Long catelogId, Map<String, Object> params) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<>();

        if ("base".equalsIgnoreCase(attrType)) {
            // 查询 基本属性 (attr_type = 1)
            queryWrapper.eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        } else {
            // 查询 销售属性 (attr_type = 0)
            queryWrapper.eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        }

        // 查询条件 分类id
        if (catelogId != 0) {
            queryWrapper.eq("catelog_id", catelogId);
        }

        // 查询条件 关键字
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        // 分页查询
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        PageUtils pageUtils = new PageUtils(page);

        // 查询结果
        List<AttrEntity> records = page.getRecords();

        // 把 List<AttrEntity> 转换为 List<AttrRespVo>
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo); //属性复制

            // 只有 基本属性 有 分组信息，销售属性 不需要 分组信息
            if ("base".equalsIgnoreCase(attrType)) {
                // 根据 attrId 查询 Attr 和 AttrGroup 的 中间表
                AttrAttrgroupRelationEntity relationEntity = relationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId())
                );

                if (relationEntity != null && relationEntity.getAttrGroupId() != null) {
                    // 根据 attrGroupId 查询 AttrGroup 表
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                    // 获取 分组名称
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }

            // 根据 catelogId 查询 Category 表
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                // 获取 分类名称
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);

        return pageUtils;
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);

        // 保存自身
        this.save(attrEntity);

        // 只有 基本属性 有 分组信息，销售属性 不需要 分组信息   =>   Attr 和 AttrGroup 的 中间表 不需要保存 销售属性
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 保存 Attr 和 AttrGroup 的 中间表
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }
}
