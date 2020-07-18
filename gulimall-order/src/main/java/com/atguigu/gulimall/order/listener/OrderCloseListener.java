package com.atguigu.gulimall.order.listener;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RabbitListener(queues = "order.release.order.queue")
@Component
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    /**
     * 监听 "order.release.order.queue"队列
     * 收到消息(订单创建已满1分钟) -> 判断订单状态是否为 "待付款"，如果是，就取消订单
     */
    @RabbitHandler
    public void listener(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        System.out.println("订单号[" + orderEntity.getOrderSn() + "] " + "订单创建已满1分钟，判断订单状态是否为 '待付款'，如果是，就取消订单");
        try {
            // 判断订单状态是否为 "待付款"，如果是，就取消订单
            orderService.closeOrder(orderEntity);
            // 签收消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 拒收消息 -> 消息重新入队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
