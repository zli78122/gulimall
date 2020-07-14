package com.atguigu.gulimall.order.receiver;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RabbitListener(queues = {"hello-java-queue"}) //声明需要监听的队列
public class RabbitMQReceiver {

    /*
     * 1.同一个 Queue 可以被多个客户端监听，但 Queue 中的每一条消息只能被一个客户端消费 - 多个客户端竞争同一条消息，谁抢到算谁的
     *
     * 2.消费端确认 - Ack机制 (保证每条消息都能被正确消费)
     *     默认是 消费端自动确认 : 只要接收到消息，客户端就会自动确认，然后 Broker 会从服务端移除这条消息
     *     消费端自动确认 存在问题 : 当客户端收到多条消息，在处理这些消息的过程中，客户端宕机，这样会造成消息丢失，因为服务器中不会保存那些剩余还未被处理的消息
     *     解决方式 : 消费端手动确认
     *         只要客户端没有明确告诉服务器收到消息，消息就不能从RabbitMQ服务器中删除
     *         Consumer宕机，未被确认的消息的状态会从 Unacked 变为 Ready
     *         Consumer恢复(可以继续从 Queue 中读取消息了)，未被确认的消息的状态会从 Ready 变为 Unacked
     *
     *         签收消息 : channel.basicAck(deliveryTag, false);
     *         拒收消息 : channel.basicNack(deliveryTag, false, true);
     *
     * Note: 客户端 == Consumer
     *       服务端 == RabbitMQ服务端
     */

    /**
     * 接收消息
     *
     * @param message           消息对象 (可以通过 消息对象 获取 消息详细信息)
     * @param orderReturnReason 消息内容
     * @param channel           传输当前消息的信道
     */
    @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity orderReturnReason, Channel channel) throws IOException {
        System.out.println("Message Content: " + orderReturnReason);

        // 消息头
        byte[] body = message.getBody();
        // 消息属性信息
        MessageProperties properties = message.getMessageProperties();

        // deliveryTag : 同一信道(Channel)内 按顺序自增 (可以根据 deliveryTag 找到 信道内的当前消息)
        long deliveryTag = properties.getDeliveryTag();
        try {
            // 签收消息
            //   deliveryTag : 同一信道(Channel)内 按顺序自增 (可以根据 deliveryTag 找到 信道内的当前消息)
            //   multiple    : 是否批量签收。multiple = false 表示只签收当前消息 (非批量模式签收消息)。通常 multiple = false
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            // 出现异常 (网络中断)
            e.printStackTrace();

            // 拒收消息
            //   deliveryTag : 同一信道(Channel)内 按顺序自增 (可以根据 deliveryTag 找到 信道内的当前消息)
            //   multiple    : 是否批量拒收。multiple = false 表示只拒收当前消息 (非批量模式拒收消息)。通常 multiple = false
            //   requeue     : 被拒收的当前消息是否重新入队。requeue = true : 发回服务器重新入队。requeue = false : 丢弃当前消息
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 接收消息
     *
     * @param message 消息对象 (可以通过 消息对象 获取 消息详细信息)
     * @param order   消息内容
     * @param channel 传输当前消息的信道
     */
    @RabbitHandler
    public void receiveMessage(Message message, OrderEntity order, Channel channel) throws IOException {
        System.out.println("Message Content: " + order);

        // 消息属性信息
        MessageProperties properties = message.getMessageProperties();

        // deliveryTag : 同一信道(Channel)内 按顺序自增 (可以根据 deliveryTag 找到 信道内的当前消息)
        long deliveryTag = properties.getDeliveryTag();
        try {
            // 签收消息
            //   deliveryTag : 同一信道(Channel)内 按顺序自增 (可以根据 deliveryTag 找到 信道内的当前消息)
            //   multiple    : 是否批量签收。multiple = false 表示只签收当前消息 (非批量模式签收消息)。通常 multiple = false
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            // 出现异常 (网络中断)
            e.printStackTrace();

            // 拒收消息
            //   deliveryTag : 同一信道(Channel)内 按顺序自增 (可以根据 deliveryTag 找到 信道内的当前消息)
            //   multiple    : 是否批量拒收。multiple = false 表示只拒收当前消息 (非批量模式拒收消息)。通常 multiple = false
            //   requeue     : 被拒收的当前消息是否重新入队。requeue = true : 发回服务器重新入队。requeue = false : 丢弃当前消息
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
