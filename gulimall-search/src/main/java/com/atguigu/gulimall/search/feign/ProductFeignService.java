package com.atguigu.gulimall.search.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    // 根据 attrId 查询 属性详细信息
    @GetMapping("/product/attr/info/{attrId}")
    R info(@PathVariable("attrId") Long attrId);

    // 根据 brandIds 获取 品牌信息
    @GetMapping("/product/brand/infos")
    R getBrands(@RequestParam("brandIds") List<Long> brandIds);
}
