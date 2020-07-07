package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
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

    // 根据 属性id 查询 search_type = 1 的基本属性 (search_type 表示 属性是否可以被检索)
    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
        return this.baseMapper.selectSearchAttrIds(attrIds);
    }

    /**
     * 查询 可以跟 当前AttrGroup对象 关联的 所有Attr对象
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrGroupId) {
        // 1.当前 AttrGroup对象 只能关联 与自己分类相同的 Attr对象
        // 2.当前 AttrGroup对象 只能关联 还没有跟其他 AttrGroup对象 关联的 Attr对象
        // 因此，当前 AttrGroup对象 只能关联 与自己分类相同 && 还没有跟其他AttrGroup对象关联 的 Attr对象

        // 根据 attrGroupId 查询 AttrGroup对象
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
        // 获取 分类id
        Long catelogId = attrGroupEntity.getCatelogId();

        // 查询 当前分类下 所有AttrGroup对象 (根据 分类id 查询 AttrGroup对象)
        // e.g. 查询到 手机分类下 所有AttrGroup对象 : 主体、基本信息、主芯片、屏幕、后置摄像头、前置摄像头、电池信息 ...
        List<AttrGroupEntity> group = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId)
        );
        // 收集 attrGroupId
        List<Long> attrGroupIds = group.stream().map(item -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        // 根据 attrGroupIds 查询 Attr 和 AttrGroup 的 中间表 => 可以查询到 当前分类下 所有已经跟 AttrGroup对象 关联了的 Attr对象
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds)
        );
        // 收集 attrIds (获取 当前分类下 所有已经跟 AttrGroup对象 关联了的 Attr对象的id)
        List<Long> attrIds = relationEntities.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        // 查询条件 : ①.指定分类id   ②.属性类别 为 基本属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());

        // 查询条件 : attr_id != attrId in attrIds
        // 当前分类下 所有Attr对象 - 当前分类下 已经跟 AttrGroup对象 关联了的 Attr对象 = 当前分类下 还没有跟 AttrGroup对象 关联的 Attr对象
        if (attrIds != null && attrIds.size() > 0) {
            wrapper.notIn("attr_id", attrIds);
        }

        // 关键字 条件查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        // 分页条件查询
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

    // 批量删除 Attr对象 和 AttrGroup对象 的 关联关系
    @Override
    public void deleteRelation(AttrGroupRelationVo[] attrGroupRelationVos) {
        List<AttrAttrgroupRelationEntity> relationEntities = Arrays.asList(attrGroupRelationVos).stream().map((item) -> {
            // 把 AttrGroupRelationVo 转换为 AttrAttrgroupRelationEntity
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            // 属性赋值
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());

        // 批量删除 Attr对象 和 AttrGroup对象 的 关联关系 - 批量删除 Attr 和 AttrGroup 的 中间表
        relationDao.deleteBatchRelation(relationEntities);
    }

    // 根据 attrGroupId 查询 Attr对象
    @Override
    public List<AttrEntity> getRelationAttr(Long attrGroupId) {
        // 根据 attrGroupId 查询 Attr 和 AttrGroup 的 中间表
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId)
        );

        // 获取 目标Attr对象 的 attrId集合
        List<Long> attrIds = relationEntities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if (attrIds == null || attrIds.size() == 0) {
            return null;
        }

        // 根据 attrId集合 查询 Attr对象
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);

        return (List<AttrEntity>) attrEntities;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity); //属性复制

        // 1.更新 基本信息
        this.updateById(attrEntity);

        // 只有 基本属性 有 分组信息，销售属性 不需要 分组信息   =>   Attr 和 AttrGroup 的 中间表 不需要保存 销售属性
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) {
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
    @Cacheable(value = "attr", key = "'attrinfo:' + #root.args[0]")
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
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) {
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
