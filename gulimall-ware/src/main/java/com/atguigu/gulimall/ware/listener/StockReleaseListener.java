package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 监听 "stock.release.stock.queue"队列
     * 收到消息(库存锁定已满2分钟) -> 执行"解锁库存"的业务逻辑
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {
        System.out.println("库存工作单详情id = " + stockLockedTo.getDetailTo().getId() + "。" +
                "库存锁定已满2分钟，执行'解锁库存'的业务逻辑。");

        try {
            // 根据 StockLockedTo对象 解锁库存
            wareSkuService.unlockStock(stockLockedTo);
            // 签收消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 拒收消息 -> 消息重新入队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    /**
     * 监听 "stock.release.stock.queue"队列
     * 收到消息(订单已取消) -> 执行"解锁库存"的业务逻辑
     */
    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单号[" + orderTo.getOrderSn() + "]: " + "订单已取消，执行'解锁库存'的业务逻辑。");
        try {
            // 根据 OrderTo对象 解锁库存
            wareSkuService.unlockStock(orderTo);
            // 签收消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 拒收消息 -> 消息重新入队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
