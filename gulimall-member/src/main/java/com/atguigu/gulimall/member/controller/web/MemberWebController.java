package com.atguigu.gulimall.member.controller.web;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Autowired
    private OrderFeignService orderFeignService;

    /**
     * 分页查询当前登录用户的所有订单信息
     */
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Model model) {
        Map<String, Object> page = new HashMap<>();
        page.put("page", pageNum.toString());
        // 分页查询当前登录用户的所有订单信息
        R orderR = orderFeignService.listWithItem(page);
        model.addAttribute("orders", orderR);
        return "orderList";
    }
}
