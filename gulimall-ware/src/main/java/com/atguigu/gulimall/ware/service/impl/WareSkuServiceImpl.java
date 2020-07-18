package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderFeignService orderFeignService;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 根据 OrderTo对象 解锁库存
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        // 获取 订单号
        String orderSn = orderTo.getOrderSn();
        // 根据 订单号 查询 库存工作单
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        // 获取 库存工作单id
        Long taskId = taskEntity.getId();
        // 根据 库存工作单id 查询所有 "lockStatus = 1"的库存工作单详情
        //   lockStatus = 1 : 库存已锁定
        //   lockStatus = 2 : 库存已解锁
        //   lockStatus = 3 : 库存已扣减
        QueryWrapper<WareOrderTaskDetailEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        wrapper.eq("lock_status", 1);
        List<WareOrderTaskDetailEntity> wareOrderTaskDetailEntities = wareOrderTaskDetailService.list(wrapper);

        // 遍历"库存工作单详情"集合，解锁库存
        for (WareOrderTaskDetailEntity entity : wareOrderTaskDetailEntities) {
            // 解锁库存 & 更新库存工作单详情
            unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }

    // 根据 StockLockedTo对象 解锁库存
    @Override
    public void unlockStock(StockLockedTo stockLockedTo) {
        // 获取 库存工作单详情
        StockDetailTo detail = stockLockedTo.getDetailTo();
        // 获取 库存工作单详情id
        Long detailId = detail.getId();

        /*
         * 根据 库存工作单详情id 查询 最新的库存工作单详情 (对应 wms_ware_order_task_detail 数据表)
         *     能查到记录 : 库存锁定成功 ("库存工作单详情"是在"锁定库存"时创建的，能查到"库存工作单详情"记录，说明库存锁定成功)
         *     查不到记录 : 库存锁定失败 ("库存工作单详情"是在"锁定库存"时创建的，查不到"库存工作单详情"记录，说明库存锁定失败，事务回滚)
         *
         *     1.能查到记录 : 库存锁定成功 -> 根据 订单状态 & 库存工作单详情的锁定状态 决定是否需要 解锁库存
         *         a.订单不存在 : 解锁库存
         *             库存服务在"锁定库存"完成后，订单服务没有收到库存服务的响应 OR 订单服务"生成订单"的后续操作出现问题
         *             订单服务就会抛出异常，然后订单服务事务回滚
         *             因为库存服务已经执行结束，所以库存服务的事务不会回滚
         *             所以数据库中没有订单服务相关的数据记录，但有库存服务相关的数据记录
         *             这种情况(订单创建失败) 必须要 解锁库存
         *         b.订单状态为"已取消" & 库存工作单详情的锁定状态为"库存已锁定" : 订单已经取消，但库存还在锁定商品 -> 解锁库存
         *     2.查不到记录 : 库存锁定失败，事务回滚 -> 无需解锁
         */

        // 根据 库存工作单详情id 查询 最新的库存工作单详情
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = wareOrderTaskDetailService.getById(detailId);
        if (wareOrderTaskDetailEntity != null) {
            // 能查到"库存工作单详情"记录 : 库存锁定成功 -> 根据 订单状态 & 库存工作单详情的锁定状态 决定是否需要 解锁库存
            // 库存工作单id
            Long id = stockLockedTo.getId();
            // 根据 库存工作单id 获取 库存工作单
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            // 从 库存工作单 中获取 订单号
            String orderSn = taskEntity.getOrderSn();
            // 根据 订单号 查询 订单信息
            R orderVoR = orderFeignService.getOrderByOrderSn(orderSn);
            if (orderVoR.getCode() == 0) {
                // 远程服务调用成功
                // 从 orderVoR 中获取 orderVo(订单信息)
                OrderVo orderVo = orderVoR.getData(new TypeReference<OrderVo>() {
                });
                // orderVo == null          : 订单不存在
                // orderVo.getStatus() == 4 : 订单状态为"已取消"
                // 订单不存在 : 解锁库存
                // 订单状态为"已取消" & 库存工作单详情的锁定状态为"库存已锁定" : 订单已经取消，但库存还在锁定商品 -> 解锁库存
                if (orderVo == null || orderVo.getStatus() == 4) {
                    // 获取 库存工作单详情的锁定状态
                    //   lockStatus = 1 : 库存已锁定
                    //   lockStatus = 2 : 库存已解锁
                    //   lockStatus = 3 : 库存已扣减
                    // 只有当 lockStatus = 1 时，才需要 解锁库存 (库存已锁定，但需要解锁库存)
                    if (wareOrderTaskDetailEntity.getLockStatus() == 1) {
                        // 解锁库存 & 更新库存工作单详情
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                // 远程服务调用失败 -> 抛出异常 -> 消息监听器 handleStockLockedRelease() 捕获到异常 -> 拒收消息 -> 消息重新入队
                throw new RuntimeException("远程服务调用失败");
            }
        } else {
            // 查不到"库存工作单详情"记录 : 库存锁定失败，事务回滚 -> 无需解锁
        }
    }

    // 解锁库存 & 更新库存工作单详情
    public void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 解锁库存 (根据 商品id、仓库id、需要解锁的商品件数 解锁库存)
        baseMapper.unLockStock(skuId, wareId, num);

        // 更新 库存工作单详情
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        // 设置 库存工作单详情id
        entity.setId(taskDetailId);
        // 设置 锁定状态 为 已解锁
        entity.setLockStatus(2);
        // 更新 库存工作单详情
        wareOrderTaskDetailService.updateById(entity);
    }

    /**
     * 锁定库存 (所有订单项都锁定成功才算锁定成功，只要有一个订单项锁定失败那就是锁定失败)
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo lockVo) {
        /*
         * 库存工作单 & 库存工作单详情
         *
         * 一个订单 对应 一个库存工作单
         * 一个订单 有 n个订单项
         * 一个订单项 对应 一个库存工作单详情 (n个订单项 对应 n个库存工作单详情)
         *
         * 库存工作单 保存了 订单号
         * 库存工作单详情 保存了 每一个订单项的相关信息，包括 库存工作单id、商品id、锁定的商品件数、仓库id、锁定状态
         *
         * 库存工作单 & 库存工作单详情 作用是什么?
         *   解锁库存时，通过查询 库存工作单 & 库存工作单详情
         *   可以获取 每一个订单项的相关信息，包括 库存工作单id、商品id、锁定的商品件数、仓库id、锁定状态
         *   需要通过这些信息，来解锁库存
         *
         * 对于 库存工作单，只要执行当前方法，先在数据库中 保存"库存工作单"信息
         * 对于 库存工作单详情，每锁定成功一个订单项，在数据库中 保存一份"库存工作单详情"信息
         */

        // 保存 库存工作单
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(lockVo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        // 给 每一个订单项 都封装一个 SkuWareHasStock对象 (查询 每件商品在哪些仓库中有库存)
        List<OrderItemVo> locks = lockVo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 根据 skuId 查询 该商品在哪些仓库中有库存
            List<Long> wareIds = baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有当前商品 -> 当前商品锁定失败 -> 全局锁定失败 -> 抛出 NoStockException异常
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                // 锁定库存 (根据 商品id、库存id、需要锁定的商品件数 锁定库存)
                //   锁定成功 : 返回1
                //   锁定失败 : 返回0
                Long count = baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    // 当前商品 锁定成功
                    skuStocked = true;

                    // 保存 库存工作单详情
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
                    taskDetailEntity.setSkuId(skuId);
                    taskDetailEntity.setSkuName("");
                    taskDetailEntity.setSkuNum(hasStock.getNum());
                    taskDetailEntity.setTaskId(taskEntity.getId());
                    taskDetailEntity.setWareId(wareId);
                    taskDetailEntity.setLockStatus(1);
                    wareOrderTaskDetailService.save(taskDetailEntity);

                    // 封装 StockLockedTo对象 (库存工作单TO)
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);

                    /*
                     * 一个订单 对应 一个库存工作单
                     * 一个订单 有 n个订单项
                     * 一个订单项 对应 一个库存工作单详情 (n个订单项 对应 n个库存工作单详情)
                     *
                     * 所有订单项都锁定成功，数据库中会保存 一条"库存工作单"记录 和 n条"库存工作单详情"记录
                     * 同时，RabbitMQ 会接收到 n条消息，每一条消息的内容都是一个 StockLockedTo对象
                     *
                     * 只要有一个订单项锁定失败，系统就会抛出 NoStockException异常，事务回滚
                     * 数据库中不会有"库存工作单"和"库存工作单详情"记录
                     * 但 RabbitMQ 会接收到 在这次锁定失败之前 所有锁定成功的订单项发出的消息
                     * 对于已经发出的消息，2分钟后，它们会被转发到 "stock.release.stock.queue"队列
                     * "stock.release.stock.queue" 的消费者在获取到这些消息之后，会执行"解锁库存"的业务逻辑
                     * 判断得知 这些消息对应的"库存工作单"和"库存工作单详情"记录不存在，所以 无需解锁
                     */

                    // 发送延时消息
                    // "stock-event-exchange"交换机 会将消息派送到 "stock.delay.queue"队列
                    // "stock.delay.queue"队列 为一个 延时队列，也叫 死信队列 ("stock.delay.queue"队列 没有消费者)
                    // 2分钟后，延时队列 会将消息转发到 "stock.release.stock.queue"队列
                    // "stock.release.stock.queue" 的消费者在获取消息之后，会执行"解锁库存"的业务逻辑
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);

                    break;
                } else {
                    // 当前仓库 锁定 当前商品 失败
                    // 如果还有下一个仓库，尝试使用下一个仓库 锁定 当前商品
                }
            }
            if (!skuStocked) {
                // 所有仓库 锁定 当前商品 失败 -> 当前商品锁定失败 -> 全局锁定失败 -> 抛出 NoStockException异常
                throw new NoStockException(skuId);
            }
        }
        // 全部商品锁定成功 -> 全局锁定成功
        return true;
    }

    // 查询sku是否有库存
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            // 查询 当前sku 的 总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    // 将 成功采购的商品 入库
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 如果仓库中还没有这件商品的记录 -> 新增
        // 如果仓库中已经有了这件商品的记录 -> 更新
        List<WareSkuEntity> entities = wareSkuDao.selectList(
                new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId)
        );
        if (entities == null || entities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);

            // 远程调用 商品微服务，查询 商品名称
            // 如果查询失败，事务不回滚！！！因为没必要因为一个不重要的字段没查出来，就把整个事务都回滚
            // 所以，如果 捕获到异常，不需要把异常抛出，直接自己把异常吞下即可 -> catch{} 中 除了打印异常信息外 其余什么都不做！
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 添加 商品库存记录
            wareSkuDao.insert(skuEntity);
        } else {
            // 更新 商品库存记录
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    // 分页条件查询
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }

    @Data
    class SkuWareHasStock {
        // 商品id
        private Long skuId;
        // 需要锁定的商品件数
        private Integer num;
        // 拥有该商品的仓库集合 (该商品在哪些仓库中有库存)
        private List<Long> wareId;
    }
}
