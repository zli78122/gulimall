package com.atguigu.gulimall.ware.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @RabbitHandler
    public void handleStockLockedRelease(Message message, Channel channel) throws IOException {

    }
}
