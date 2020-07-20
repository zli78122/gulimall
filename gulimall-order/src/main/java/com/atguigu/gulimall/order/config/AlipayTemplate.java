package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class AlipayTemplate {

    // 应用ID，您的APPID，收款账号即是您的APPID对应的支付宝账号（使用沙箱应用的APPID）
    private String app_id = "2016102200735219";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDaMBbclg0HOhHIrg8pfWZGnbZiuC3Q3C7rE/W1XMwuyJfCwr7VapO7P2Ksigbd8pxcMWvIOJrcOgc/S+YbXvJVBTidLsezRDuW7ClMLVflcOh0NReFVgk6K6+InSHOynzGltoTsH+QeRMa51HBsRTCwZ0uEytTH4+BXhvpRngfrxAi4jsqyGXeMfcpUNCbB+CE5VJjslAsaqREZPSy+yV2zXA99HaKeDNwFf4Tu0RDpDXqZPvFGQJeDUZESYFWmw9T2txLJVkJqpjaPqkzee3Xe8aSm0hgIEu+gKZJNPY3Uq+ERkW/gh8M41qcP/ejh5Bwnn06PUpIWIGz9UnaFpIzAgMBAAECggEBAKuwP60gXewb47aYUNIDHSHgJI6WA2dge4L719L4zKrbqZ/WAriI/urPO+QtUK4BpMxfD0MkV6eH+f/yx4UuN15zl1MIZphHzuQ5vV12KL4hFKmsxW9kfipKCfxkOSAx8fGK7tujfR3ASCpZb9oMcBu4ouiIqQTlxtSzqx7rFsdGVFyEZ3p9LxMMG0XA4Rr8g56qW/qzUQY3X5sXV6iPNN1YQZR5iNpGDPyxkofEY/VdzhCks8bSzaILgdMlUZGkMCg8HhlHDurPtDBe/R2J0PtnfsfNPVrw0m6g+jcKVUP5cf93OlH7ypokYoNzyDqo2s+Rm+DLI3lJ0+8S5AdKr9ECgYEA7/T8M19ED8FpbYjM0/jY+oQ+xqu0fYOGCuk47iN0nsOMvGHtl5Q9GxUL6jMQiwvI1VzNXeeM1wcxEWNhwo17N21BlF9aeQkGr8CwUonh6Hxj2AcR957+w+5A4JMtegjmKH3/j93OwQN3qx3ktiR1vLRBpoYSAEeAXmQwVHXd9q0CgYEA6MaDCS413ZomxmYHCRgaWpgWGK9JtLVhd7JyUF4l4y3ccUborCt7Q94R1eECgq76L/ZFnfymXiDGe/Ofz7+OBKe92t9EZ+GXb4m1qrjtqNGGdk63P2s2A/+wcaDrRwOXBT9v38NPSaJzKIKGXaupdsP5vNQpretLJAgpp8WBKF8CgYEAqLEI8DCkD5OAQ6HtpYOHPq7j1xV1INvqyxbkj/3bCZYeBX2u42YygyyLoMzfb3EMcnr4YXDcQXrr0vaUDosaOYAUn3NdjkN7MOl8y7nlWGTCaGeoCPX80s9XSsoNKYLCE2to08IyjNycL28nj6kYIF503NXaJ9occ79pfqcSMU0CgYBIaTBsE673/7MeNrDrmE7ibbs59JVmHKJ0XNOIwrDgieywZFGO9XvD8fn0YampQ4taji675dNPJrz35CKLutgQBKpfwuKBukCklt8ne52azYsZlIUWumA0QD8AKgHmtmwtWr2Piy9IBbvzFx97ViJCatZuckWA79gRHmTu5ta12wKBgGlITfv6j6+T5+txbkan/+dkqSICjNM3RJp3X5t4BzG1qZHY/qZiF06E3KeosHeEpZTnZGWPr5bo1TuCmzHLEpR+yQzMLIzIkriuwTKlNyxwKr7Wz3/9nyIVfgMSFk7vJfEXjCCLpGCkCfBsvafOc62g593Y1q//YTUQ3ZKMjCYE";

    // 支付宝公钥，查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArfwqP3gHGHXWCOqUy7oB+zP/2oIiCU4wfmBBbDvG+K/oq/2YreWSprGc2KW05scv+STfU4k/TFlmAYLboZr7PVSzAOo3rRoXXEnnQQtqvaEl7WQ+4S8cKMCgo6fSA8Wr++v/FOfNNyFG9NT6LxF6H74eQdSz5VDd6Ylclf6talznpdO3aNyHG+nPn1AGksgoJ4TJGlK+pcJifztG3QzGZDgCv8yiUE5aM7EuIS13w4z7mr5q3YuuK3041vxKu2C4xbPNUqHOkFYd8d0w/ULwATO4PfwWbSxoKGiiZsA2ODRE0M/YEKWRzPXJn8kgWJsKI/hEsu3hmCce43SI7uRE0QIDAQAB";

    // 服务器异步通知请求路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 使用 内网穿透客户端(NATAPP) 提供的域名
    private String notify_url = "http://6xrb8k.natappfree.cc/payed/notify";

    // 本地浏览器同步通知请求路径 需http://格式的完整路径，不能加?id=123这类自定义参数
    private String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    // 订单支付超时时间
    private String timeout = "30m";

    // 支付宝网关（沙箱环境）
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {
        // 1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl, app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        // 2、创建一个支付请求，设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        // 商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        // 付款金额，必填
        String total_amount = vo.getTotal_amount();
        // 订单名称，必填
        String subject = vo.getSubject();
        // 商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        // 会收到支付宝的响应，响应是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        System.out.println("支付宝的响应：" + result);

        return result;
    }
}
