package com.atguigu.gulimall.order.controller.web;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    /**
     * 提交订单 (下单)
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            if (responseVo.getCode() == 0) {
                // 下单成功
                model.addAttribute("submitOrderResp", responseVo);
                // 跳转到支付页面
                return "pay";
            } else {
                // 下单失败 -> 返回订单确认页面
                String msg = "下单失败：";
                switch (responseVo.getCode()) {
                    case 1:
                        msg += "订单信息过期，请刷新后再次提交";
                        break;
                    case 2:
                        msg += "订单商品价格发生变化，请确认后再次提交";
                        break;
                    case 3:
                        msg += "库存锁定失败，商品库存不足";
                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                // 跳转到订单确认页面
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e) {
            // 抛出异常 -> 返回订单确认页面
            e.printStackTrace();
            if (e instanceof NoStockException) {
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }

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
