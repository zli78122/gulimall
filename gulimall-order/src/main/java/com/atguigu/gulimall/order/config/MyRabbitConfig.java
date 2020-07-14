package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * RabbitMQ 配置
 */
@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 配置 消息转换器 - 使用 Json序列化机制 进行消息转换
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制 RabbitTemplate
     *   confirmCallback (回调方法) - 消息只要被 Broker 接收到就会触发 confirmCallback
     *   returnCallback  (回调方法) - 消息未被成功投递到 Queue 时会触发 returnCallback (如果成功投递到 Queue，就不会触发 returnCallback)
     */
    @PostConstruct //MyRabbitConfig类对象 创建完成之后执行 initRabbitTemplate()
    public void initRabbitTemplate() {
        // confirmCallback 确认模式
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 消息只要被 Broker 接收到就会触发 confirm()
             *
             * @param correlationData 当前消息的唯一关联数据，通过 correlationData 可以获得当前消息的 id
             * @param ack             Broker 是否成功收到消息
             * @param cause           Broker 没有收到消息的 失败原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("correlationData=" + correlationData + ", ack=" + ack + ", cause=" + cause);
            }
        });

        // returnCallback 未投递到 Queue 退回模式
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 消息未被成功投递到 Queue 时会触发 returnedMessage()
             * i.e. 如果成功投递到 Queue，就不会触发 returnedMessage()
             *
             * @param message    消息对象 (可以通过 消息对象 获取 消息详细信息)
             * @param replyCode  消息投递失败的 回复状态码
             * @param replyText  消息投递失败的 回复文本内容
             * @param exchange   接收消息的交换器
             * @param routingKey 消息的路由键
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                // 消息投递失败 -> 修改数据库当前消息的状态
            }
        });
    }
}
