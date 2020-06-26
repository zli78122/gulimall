package com.atguigu.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;

/**
 * 商品三级分类
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-22 23:41:46
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询所有分类，以树形结构组装起来
     */
    @RequestMapping("/list/tree")
    //@RequiresPermissions("product:category:list")
    public R listTree() {
        List<CategoryEntity> entities = categoryService.listWithTree();

        return R.ok().put("data", entities);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:category:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = categoryService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    //@RequiresPermissions("product:category:info")
    public R info(@PathVariable("catId") Long catId) {
        CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("data", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:category:save")
    public R save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:category:update")
    public R update(@RequestBody CategoryEntity category) {
        // 级联更新
        categoryService.updateCascade(category);

        return R.ok();
    }

    /**
     * 批量修改
     */
    @RequestMapping("/batch/update")
    //@RequiresPermissions("product:category:update")
    public R batchUpdate(@RequestBody CategoryEntity[] categories) {
        categoryService.updateBatchById(Arrays.asList(categories));

        return R.ok();
    }

    /**
     * 批量删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:category:delete")
    public R delete(@RequestBody Long[] catIds) {
        categoryService.removeCategoryByIds(Arrays.asList(catIds));

        return R.ok();
    }
}
