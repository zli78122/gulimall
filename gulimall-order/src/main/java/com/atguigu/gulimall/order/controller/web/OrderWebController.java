package com.atguigu.gulimall.order.controller.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    /**
     * 封装 OrderConfirmVo对象，并跳转到 订单确认页面
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        // 封装 OrderConfirmVo对象
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("data", confirmVo);
        return "confirm";
    }
}
