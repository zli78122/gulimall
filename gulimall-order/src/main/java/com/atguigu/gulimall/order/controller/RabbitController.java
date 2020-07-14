package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
public class RabbitController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     *
     * 如果发送的消息是对象，会使用序列化机制将对象写出去，所以要求对象必须实现 Serializable 接口
     */
    @GetMapping("/sendMessage")
    public String sendMessage(@RequestParam(value = "num", defaultValue = "10") Integer num) {
        for (int i = 0; i < num; i++) {
            if (i % 2 == 0) {
                OrderReturnReasonEntity orderReturnReason = new OrderReturnReasonEntity();
                orderReturnReason.setId((long) i);
                orderReturnReason.setCreateTime(new Date());
                orderReturnReason.setName("orderReturnReason" + i);
                // 发送消息
                // CorrelationData : 当前消息的唯一关联数据，通过 CorrelationData 可以获得当前消息的 id
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderReturnReason, new CorrelationData(UUID.randomUUID().toString()));
            } else {
                OrderEntity order = new OrderEntity();
                order.setId((long) i);
                order.setOrderSn(UUID.randomUUID().toString());
                // 发送消息
                // CorrelationData : 当前消息的唯一关联数据，通过 CorrelationData 可以获得当前消息的 id
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", order, new CorrelationData(UUID.randomUUID().toString()));
            }
        }
        return "ok";
    }
}
