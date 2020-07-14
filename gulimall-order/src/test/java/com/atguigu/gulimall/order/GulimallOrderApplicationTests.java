package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallOrderApplicationTests {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     *
     * 如果发送的消息是对象，会使用序列化机制将对象写出去，所以要求对象必须实现 Serializable 接口
     */
    @Test
    public void test5() {
        OrderReturnReasonEntity orderReturnReason = new OrderReturnReasonEntity();
        orderReturnReason.setId(1L);
        orderReturnReason.setCreateTime(new Date());
        orderReturnReason.setName("OrderReturnReason");
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderReturnReason);
    }

    /**
     * 发送消息
     */
    @Test
    public void test4() {
        String message = "hello world";
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", message);
    }

    /**
     * 创建 绑定 (Binding) - 基于路由键 将 交换器 和 消息队列 连接起来
     */
    @Test
    public void test3() {
        // 绑定 - 基于路由键 将 交换器 和 消息队列 连接起来
        // 第一个参数 : destination (目的地)
        // 第二个参数 : destinationType (目的地类型)
        // 第三个参数 : exchange (交换器)
        // 第四个参数 : routingKey (路由键)
        // 第五个参数 : arguments (参数)
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE,
                "hello-java-exchange", "hello.java", null);
        amqpAdmin.declareBinding(binding);
    }

    /**
     * 创建 队列 (Queue)
     */
    @Test
    public void test2() {
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
    }

    /**
     * 创建 交换器 (Exchange)
     */
    @Test
    public void test() {
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
    }
}
