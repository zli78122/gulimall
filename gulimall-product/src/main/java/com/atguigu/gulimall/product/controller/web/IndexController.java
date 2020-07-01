package com.atguigu.gulimall.product.controller.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping({"/", "/index", "/index.html"})
    public String indexPage(Model model) {
        // 查询所有一级分类
        List<CategoryEntity> categories = categoryService.getLevelOneCategories();
        model.addAttribute("categories", categories);

        return "index";
    }

    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2VO>> getCatelogJson() {
        return null;
    }
}
