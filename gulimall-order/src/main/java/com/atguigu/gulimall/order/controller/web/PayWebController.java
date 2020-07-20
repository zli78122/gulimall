package com.atguigu.gulimall.order.controller.web;

import com.alipay.api.AlipayApiException;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * 获取 PayVo对象，跳转到 支付宝的收银台页面(支付页面)
     *
     * produces = "text/html" : 响应数据格式为"text/html"
     */
    @ResponseBody
    @GetMapping(value = "/payOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        // 根据 订单号 获取 PayVo对象
        PayVo payVo = orderService.getOrderPay(orderSn);
        // 会收到支付宝的响应，响应是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
