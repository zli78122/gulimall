package com.atguigu.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 支付完成后 支付宝异步回调
 */
@RestController
public class OrderPayedListener {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * 支付完成后 支付宝异步回调
     */
    @PostMapping("/payed/notify")
    public String handAlipayAsyncNotify(PayAsyncVo payAsyncVo, HttpServletRequest request) throws AlipayApiException, UnsupportedEncodingException {
        // 验证签名 (验证本次请求是不是支付宝发出的，防止数据篡改和伪造)
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(), alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名

        if (signVerified) {
            // 验签成功
            // 开发者的app_id
            String appId = payAsyncVo.getApp_id();
            // 根据 订单号 查询 订单信息
            OrderEntity orderEntity = orderService.getOrderByOrderSn(payAsyncVo.getOut_trade_no());
            if (orderEntity != null && alipayTemplate.getApp_id().equals(appId)) {
                // 支付完成 -> 保存支付信息 & 修改订单状态
                String result = orderService.handPayResult(payAsyncVo);
                return result;
            } else {
                return "error";
            }
        } else {
            // 验签失败
            return "error";
        }
    }
}
