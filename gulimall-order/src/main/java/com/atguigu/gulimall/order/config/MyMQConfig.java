package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建 RabbitMQ 队列、交换器、绑定
 */
@Configuration
public class MyMQConfig {

    // 创建 延时队列 (死信队列)
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 死信路由
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        // 死信路由键
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        // 过期时间 (1分钟)
        arguments.put("x-message-ttl", 60000);
        // 创建队列
        Queue queue = new Queue("order.delay.queue", true, false, false, arguments);
        return queue;
    }

    // 创建 队列 (Queue)
    @Bean
    public Queue orderReleaseOrderQueue() {
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }

    // 创建 交换器 (Exchange)
    @Bean
    public Exchange orderEventExchange() {
        TopicExchange topicExchange = new TopicExchange("order-event-exchange", true, false);
        return topicExchange;
    }

    // 创建 绑定 (Binding) - 基于路由键 将 交换器 和 消息队列 连接起来
    @Bean
    public Binding orderCreateOrderBinding() {
        // 绑定 - 基于路由键 将 交换器 和 消息队列 连接起来
        // 第一个参数 : destination (目的地)
        // 第二个参数 : destinationType (目的地类型)
        // 第三个参数 : exchange (交换器)
        // 第四个参数 : routingKey (路由键)
        // 第五个参数 : arguments (参数)
        Binding binding = new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
        return binding;
    }

    // 创建 绑定 (Binding) - 基于路由键 将 交换器 和 消息队列 连接起来
    @Bean
    public Binding orderReleaseOrderBinding() {
        // 绑定 - 基于路由键 将 交换器 和 消息队列 连接起来
        // 第一个参数 : destination (目的地)
        // 第二个参数 : destinationType (目的地类型)
        // 第三个参数 : exchange (交换器)
        // 第四个参数 : routingKey (路由键)
        // 第五个参数 : arguments (参数)
        Binding binding = new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
        return binding;
    }
}
