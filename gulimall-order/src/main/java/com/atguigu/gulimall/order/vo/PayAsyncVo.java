package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 支付结果异步通知VO
 *
 * 文档 : https://opendocs.alipay.com/open/270/105902
 */
@ToString
@Data
public class PayAsyncVo {
    // 交易创建时间
    private String gmt_create;
    // 编码格式
    private String charset;
    // 交易付款时间
    private String gmt_payment;
    // 通知时间
    private Date notify_time;
    // 订单标题
    private String subject;
    // 签名
    private String sign;
    // 买家支付宝用户号
    private String buyer_id;
    // 商品描述
    private String body;
    // 开票金额
    private String invoice_amount;
    // 接口版本
    private String version;
    // 通知校验ID
    private String notify_id;
    // 支付金额信息
    private String fund_bill_list;
    // 通知类型
    private String notify_type;
    // 商户订单号
    private String out_trade_no;
    // 订单金额
    private String total_amount;
    // 交易状态
    private String trade_status;
    // 支付宝交易号
    private String trade_no;
    // 授权方的app_id
    private String auth_app_id;
    // 实收金额
    private String receipt_amount;
    // 集分宝金额
    private String point_amount;
    // 开发者的app_id
    private String app_id;
    // 付款金额
    private String buyer_pay_amount;
    // 签名类型
    private String sign_type;
    // 卖家支付宝用户号
    private String seller_id;
}
