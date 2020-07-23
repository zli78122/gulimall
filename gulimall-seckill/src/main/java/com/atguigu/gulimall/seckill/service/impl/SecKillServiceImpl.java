package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SecKillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVO;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SecKillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SecKillSessionWithSku;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SecKillServiceImpl implements SecKillService {

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 秒杀
     *
     * 整个秒杀流程执行的非常快，因为我们一直在缓存中操作，从头到尾没有操作过数据库
     * 最终，在请求获取到信号量之后，服务器直接给RabbitMQ发送了一个"创建秒杀订单"的消息，消息发送成功之后，不等订单创建完成，直接给客户端返回秒杀成功的结果
     * 订单服务的消息消费者在接收到"创建秒杀订单"的消息后，慢慢创建秒杀订单
     * 对于一个请求(一个线程)，可能只需要花费 10ms 就能执行完整个秒杀流程 (包括 校验合法性、尝试获取信号量、给MQ发送消息)
     * 这样的话，一个单线程一秒钟就可以处理100个请求
     * 一个Tomcat的并发能力在500左右，所以一个Tomcat一秒钟就可以处理 50000个请求
     * 我们使用20个Tomcat组成的集群就可以处理百万请求了
     * 这是一个非常了不起的结果！！！
     *
     * 对于传统模式 (不使用RabbitMQ发送消息，而是直接操作数据库，创建秒杀订单，秒杀订单创建完成后，再给客户端响应秒杀结果)
     * 处理一个请求可能需要花费一秒的时间
     * 一个Tomcat的并发能力在500左右，所以一个Tomcat一秒钟只能处理 500个请求
     *
     * 使用RabbitMQ消息队列 : 一个Tomcat一秒钟处理 50000个请求
     * 传统模式 : 一个Tomcat一秒钟处理 500个请求
     * 100倍的差距！！！
     *
     * @param killId 秒杀Id = sessionId_skuId
     * @param code   秒杀随机码
     * @param num    购买商品数量
     */
    @Override
    public String secKill(String killId, String code, Integer num) {
        // 从 ThreadLocal 中获取 用户登录信息
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        // 获取 BoundHashOperations对象，用于操作存储在Redis中的秒杀相关数据
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        // 根据 killId 从Redis中获取 当前秒杀商品的详细信息 (获取的数据格式是 JSON字符串)
        String secKillSkuJSONString = hashOps.get(killId);
        if (StringUtils.isEmpty(secKillSkuJSONString)) {
            return null;
        } else {
            // 将 String字符串 转换为 SecKillSkuRedisTo对象
            SecKillSkuRedisTo redisTo = JSON.parseObject(secKillSkuJSONString, SecKillSkuRedisTo.class);
            // 秒杀开始时间
            Long startTime = redisTo.getStartTime();
            // 秒杀结束时间
            Long endTime = redisTo.getEndTime();
            // 秒杀时长
            long ttl = endTime - startTime;

            // 秒杀随机码
            String randomCode = redisTo.getRandomCode();
            // sessionId_skuId
            String rKey = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
            // 校验 秒杀随机码 和 killId
            if (code.equals(randomCode) && killId.equals(rKey)) {
                // 每人限购数量
                Integer secKillLimit = redisTo.getSeckillLimit();
                // 校验 购买数量 是否合理 (用户购买数量不能超过限购数量)
                if (num <= secKillLimit) {

                    /*
                     * 校验 当前用户之前是否已经成功秒杀过当前商品 (保证幂等性 : 每个用户只能秒杀同一件商品一次)
                     *
                     * 用户每次参与秒杀时，都会尝试从Redis中获取 key = "userId_sessionId_skuId" 的数据
                     *   如果能获取到，说明 这已经不是用户第一次秒杀该商品 -> 不能再次秒杀
                     *   如果获取不到，说明 这是用户第一次秒杀该商品 -> 允许用户秒杀 (如果用户能抢到信号量，就秒杀成功)
                     * 如果 这是用户第一次秒杀该商品 (获取不到 "userId_sessionId_skuId") -> 在Redis中保存 userId_sessionId_skuId
                     * 如果 这已经不是用户第一次秒杀该商品 (能获取到 "userId_sessionId_skuId") -> 直接释放请求
                     *
                     * 以上的描述可以使用 setIfAbsent()方法 来一步实现，它是一个原子性操作
                     * setIfAbsent() : key不存在就占位，key已经存在则不做任何操作，最终返回 是否占位成功
                     *                 占位成功说明 这是用户第一次秒杀该商品
                     *                 占位失败说明 这已经不是用户第一次秒杀该商品
                     */

                    // 校验 当前用户之前是否已经成功秒杀过当前商品 (保证幂等性 : 每个用户只能秒杀同一件商品一次)
                    // isBuyKey = "userId_sessionId_skuId"
                    String isBuyKey = memberResponseVO.getId() + "_" + rKey;
                    // 尝试占位 : 占位成功，返回true；占位失败，返回false
                    Boolean isBuy = redisTemplate.opsForValue().setIfAbsent(isBuyKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                    if (isBuy) {
                        // 占位成功 : 说明 Redis中不存在 "userId_sessionId_skuId" -> 这是用户第一次秒杀该商品

                        // 信号量对象
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                        // 尝试获取信号量
                        // tryAcquire() : 尝试获取，获取不到就直接释放请求 (非阻塞方法)
                        boolean hasSemaphore = semaphore.tryAcquire(num);
                        if (hasSemaphore) {
                            // 成功获取信号量 (秒杀成功) -> 发送消息，创建 秒杀订单

                            // 封装 SecKillOrderTo对象
                            String order_sn = IdWorker.getTimeId();
                            SecKillOrderTo orderTo = new SecKillOrderTo();
                            orderTo.setOrderSn(order_sn);
                            orderTo.setMemberId(memberResponseVO.getId());
                            orderTo.setNum(num);
                            orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                            orderTo.setSkuId(redisTo.getSkuId());
                            orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                            // 发送消息
                            // "order-event-exchange"交换机 会将消息派送到 "order.seckill.order.queue"队列
                            // "order.seckill.order.queue" 的消费者在获取消息之后，会执行"创建 秒杀订单"的业务逻辑
                            rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                            // 返回 订单号
                            return order_sn;
                        }
                        return null;
                    } else {
                        // 占位失败 : 说明 Redis中已经存在 "userId_sessionId_skuId" -> 这已经不是用户第一次秒杀该商品
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

    // 根据 skuId 获取 商品秒杀信息
    @Override
    public SecKillSkuRedisTo getSkuSecKillInfo(Long skuId) {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的秒杀相关数据
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        // 从 Redis 中获取 "seckill:skus:" 的所有 key
        //   key   = sessionId_skuId
        //   value = SecKillSkuRedisTo对象
        // 获取所有 key -> 获取所有 "sessionId_skuId"
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            // 正则表达式
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                // 正则匹配 : 匹配成功，说明 key 对应的 value 为 目标商品的秒杀信息
                if (Pattern.matches(regx, key)) {
                    // 根据 key 从Redis中获取 商品秒杀信息 (获取的数据格式是 JSON字符串)
                    String skuSecKillInfoString = hashOps.get(key);
                    // 将 String字符串 转换为 SecKillSkuRedisTo对象
                    SecKillSkuRedisTo skuRedisTo = JSON.parseObject(skuSecKillInfoString, SecKillSkuRedisTo.class);

                    // 秒杀还未开始 -> 屏蔽 秒杀随机码 (保证秒杀的公平性)
                    // 当前正在秒杀期间 -> 不需要屏蔽 秒杀随机码
                    // skuRedisTo.setRandomCode(null);

                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    // 获取 当前时间正在参与秒杀的所有商品
    @Override
    public List<SecKillSkuRedisTo> getCurrentSecKillSku() {
        // 获取 当前时间 (时间戳)
        long time = new Date().getTime();
        // 获取Redis中的所有秒杀场次
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            // key = seckill:sessions:1595347200000_1595376000000
            String replace = key.replace("seckill:sessions:", "");
            String[] s = replace.split("_");
            // 活动开始时间
            Long startTime = Long.parseLong(s[0]);
            // 活动结束时间
            Long endTime = Long.parseLong(s[1]);
            // 判断 当前时间是否属于这个秒杀场次
            if (time >= startTime && time <= endTime) {
                // 当前时间属于这个秒杀场次 -> 获取 这个秒杀场次关联的所有商品信息
                // 从 Redis 中获取当前场次关联的所有 "sessionId_skuId" 数据
                List<String> sessionIdAndSkuIdList = redisTemplate.opsForList().range(key, -100, 100);
                // 获取 BoundHashOperations对象，用于操作存储在Redis中的秒杀相关数据
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                // 根据 "sessionId_skuId" 数据集合，获取 当前秒杀场次关联的所有商品信息 (获取的数据格式是 JSON字符串)
                List<String> skuList = hashOps.multiGet(sessionIdAndSkuIdList);
                if (skuList != null) {
                    List<SecKillSkuRedisTo> collect = skuList.stream().map(item -> {
                        // 将 String字符串 转换为 SecKillSkuRedisTo对象
                        SecKillSkuRedisTo redisTo = JSON.parseObject((String) item, SecKillSkuRedisTo.class);
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }
        return null;
    }

    // 将 最近三天的商品秒杀活动信息 缓存到Redis中
    @Override
    public void uploadSecKillSkuLatest3Days() {
        // 获取最近三天的商品秒杀活动
        R secKillSessionWithSkuR = couponFeignService.getLatest3DaysSession();
        if (secKillSessionWithSkuR.getCode() == 0) {
            // 从 secKillSessionWithSkuR 中获取 secKillSessionWithSku
            List<SecKillSessionWithSku> secKillSessionWithSku = secKillSessionWithSkuR.getData(new TypeReference<List<SecKillSessionWithSku>>() {
            });
            // 缓存数据到 Redis 中
            // 缓存 秒杀活动信息
            saveSessionInfos(secKillSessionWithSku);
            // 缓存 秒杀活动关联的商品信息
            saveSessionSkuInfos(secKillSessionWithSku);
        }
    }

    // 缓存 秒杀活动信息
    private void saveSessionInfos(List<SecKillSessionWithSku> secKillSessionWithSku) {
        secKillSessionWithSku.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;

            // 判断Redis中是否已经存在 key - 保证 幂等性
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item ->
                        item.getPromotionSessionId() + "_" + item.getSkuId().toString()
                ).collect(Collectors.toList());
                // 缓存 秒杀活动信息
                //   key   = seckill:sessions:1595347200000_1595376000000
                //   value = [1_1, 1_2, 1_3, 1_11, 1_12]
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    // 缓存 秒杀活动关联的商品信息
    private void saveSessionSkuInfos(List<SecKillSessionWithSku> secKillSessionWithSku) {
        secKillSessionWithSku.stream().forEach(session -> {
            // 获取 BoundHashOperations对象，用于操作存储在Redis中的秒杀相关数据
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

            session.getRelationSkus().stream().forEach(secKillSkuVo -> {
                // 判断Redis中是否已经存在 key - 保证 幂等性
                if (!ops.hasKey(secKillSkuVo.getPromotionSessionId().toString() + "_" + secKillSkuVo.getSkuId().toString())) {
                    // 封装对象 - Redis中保存的 秒杀商品对象
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    // 根据 skuId 查询 sku信息
                    R skuInfoR = productFeignService.getSkuInfo(secKillSkuVo.getSkuId());
                    if (skuInfoR.getCode() == 0) {
                        // 从 skuInfoR 中获取 skuInfo
                        SkuInfoVo skuInfo = skuInfoR.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfo);
                    }

                    // sku的秒杀信息 (属性复制)
                    BeanUtils.copyProperties(secKillSkuVo, redisTo);

                    // 设置 当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    /*
                     * 商品秒杀随机码 (保证秒杀的公平性)
                     *   随机码只有在商品秒杀活动开始的那一刻才会被暴露出来
                     *   这样可以防止恶意攻击 -> 只知道商品id，不知道随机码是无法秒杀商品的
                     *   想秒杀商品，请求地址中就必须包含随机码
                     *   在秒杀活动开始前，没有人知道秒杀商品的随机码，也就没有人能够提前知道秒杀商品的请求地址了
                     *   所以，商品秒杀随机码可以保证秒杀的公平性
                     */

                    // 设置 商品秒杀随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);

                    // Redis中缓存 秒杀活动关联的商品信息
                    //   key   = sessionId_skuId
                    //   value = SecKillSkuRedisTo对象
                    String secKillSkuRedisToJsonString = JSON.toJSONString(redisTo);
                    ops.put(secKillSkuVo.getPromotionSessionId().toString() + "_" + secKillSkuVo.getSkuId().toString(), secKillSkuRedisToJsonString);

                    /*
                     * 分布式锁 - 信号量 (Semaphore)
                     *
                     * 信号量 可以用作 分布式限流 : 信号量为100，那就只放行100个请求，每放行一个请求，信号量减一，直至信号量减到0，不再放行请求
                     *
                     * 使用 商品可以秒杀的总件数(秒杀库存) 作为 分布式的信号量
                     *   秒杀商品不应该实时去数据库中扣减库存，而应该把库存信息保存到Redis缓存中
                     *   秒杀请求进来，应该首先去Redis中获取信号量，如果能获取到，就放行请求，同时Redis中的信号量减一
                     *   直至Redis中的信号量减到0，不再放行请求
                     *   被放行的请求才有机会请求数据库，创建订单和订单项等数据
                     *   使用这样的方式，可以保证没有抢到信号量的请求能够被快速释放掉
                     *   只有做到每个请求都可以很快得被处理完，才能提升整个系统的并发量
                     */

                    // 分布式锁 - 信号量
                    // 使用 商品可以秒杀的总件数(秒杀库存) 作为 分布式的信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 设置 信号量 - 商品可以秒杀的总件数(秒杀库存)
                    semaphore.trySetPermits(secKillSkuVo.getSeckillCount());
                }
            });
        });
    }
}
