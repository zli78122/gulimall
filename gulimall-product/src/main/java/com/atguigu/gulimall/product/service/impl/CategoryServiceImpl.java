package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    // 查询商品分类信息，封装成固定格式，用于前台首页显示
    @Override
    public Map<String, List<Catelog2VO>> getCatelogJson() {
        // 查询所有类别
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 查询所有一级分类
        List<CategoryEntity> levelOneCategorys = getParent_cid(selectList, 0L);

        // 封装 结果Map
        //   key : 一级分类id
        //   value : List<Catelog2VO>>
        Map<String, List<Catelog2VO>> result = levelOneCategorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 查询 一级分类 下的 二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

            List<Catelog2VO> catelog2VOS = null;
            if (!CollectionUtils.isEmpty(categoryEntities)) {
                // 遍历 二级分类
                catelog2VOS = categoryEntities.stream().map(l2 -> {
                    // 封装 二级分类
                    Catelog2VO catelog2VO = new Catelog2VO(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    // 查询 二级分类 下的 三级分类
                    List<CategoryEntity> category3List = getParent_cid(selectList, l2.getCatId());

                    if (!CollectionUtils.isEmpty(category3List)) {
                        // 遍历 三级分类
                        List<Catelog2VO.Catelog3VO> collect = category3List.stream().map(l3 -> {
                            // 封装 三级分类
                            Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3VO;
                        }).collect(Collectors.toList());

                        catelog2VO.setCatalog3List(collect);
                    }

                    return catelog2VO;
                }).collect(Collectors.toList());
            }

            return catelog2VOS;
        }));

        return result;
    }

    // 根据 parent_cid 查询 分类信息
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid().longValue() == parent_cid.longValue();
        }).collect(Collectors.toList());

        return collect;
    }

    // 查询所有一级分类
    @Override
    public List<CategoryEntity> getLevelOneCategories() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(
                new QueryWrapper<CategoryEntity>().eq("parent_cid", 0)
        );
        return categoryEntities;
    }

    // 级联更新
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        // 更新自身
        this.updateById(category);

        // 同步更新其他关联表中的数据 (级联更新)
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    // 查询 商品分类 的 catelogPath - 从祖先节点到自身 的路径
    @Override
    public Long[] findCatelogPath(Long categoryId) {
        List<Long> paths = new ArrayList<>();

        // 递归 查询 商品分类 的 catelogPath - 从祖先节点到自身 的路径
        List<Long> parentPath = findParentPath(categoryId, paths);

        // 反转列表
        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }

    // 递归 查询 商品分类 的 catelogPath - 从祖先节点到自身 的路径
    private List<Long> findParentPath(Long categoryId, List<Long> paths) {
        paths.add(categoryId);
        CategoryEntity category = this.getById(categoryId);
        if (category.getParentCid() != 0) {
            findParentPath(category.getParentCid(), paths);
        }
        return paths;
    }

    // 批量删除
    @Override
    public void removeCategoryByIds(List<Long> asList) {
        // TODO 检查当前删除的Categories 是否被其他地方引用，如果被其他地方引用，则不能删除

        // 逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    // 查询所有分类，以树形结构组装起来
    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> allCategories = baseMapper.selectList(null);

        //2、以树形结构组装起来
        List<CategoryEntity> level1Categories;

        //2-1 找到所有一级分类
        //2-2 递归查找每个一级分类的子分类
        //2-3 按照 CategoryEntity对象的sort属性值 排序
        //2-4 返回List集合
        level1Categories = allCategories.stream().filter(currCategory ->
                currCategory.getParentCid() == 0
        ).map(currCategory -> {
            currCategory.setChildren(getChildren(currCategory, allCategories));
            return currCategory;
        }).sorted((category1, category2) -> {
            return (category1.getSort() == null ? 0 : category1.getSort()) - (category2.getSort() == null ? 0 : category2.getSort());
        }).collect(Collectors.toList());

        return level1Categories;
    }

    /**
     * 递归查找 目标分类(root) 的子分类
     *
     * @param root 目标分类
     * @param all  所有分类
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children;

        // 1.找到 root 的所有子分类
        // 2.递归查找每个子分类的子分类
        // 3.按照 CategoryEntity对象的sort属性值 排序
        // 4.返回List集合
        children = all.stream().filter(currCategory ->
                currCategory.getParentCid().equals(root.getCatId())
        ).map(currCategory -> {
            currCategory.setChildren(getChildren(currCategory, all));
            return currCategory;
        }).sorted((category1, category2) -> {
            return (category1.getSort() == null ? 0 : category1.getSort()) - (category2.getSort() == null ? 0 : category2.getSort());
        }).collect(Collectors.toList());

        return children;
    }
}
