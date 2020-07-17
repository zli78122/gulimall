package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVO;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    // ThreadLocal : 同一个线程共享数据
    // 将 OrderSubmitVo对象 存入 ThreadLocal 中
    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new InheritableThreadLocal<>();

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderItemService orderItemService;

    // 提交订单 (下单)
    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        // 将 orderSubmitVo 存入 ThreadLocal 中
        submitVoThreadLocal.set(orderSubmitVo);

        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        // 初始化 code = 0
        response.setCode(0);
        // 从 ThreadLocal 中获取 memberResponseVO (memberResponseVO 中封装了 用户登录信息)
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        /*
         * 验证 防重令牌 (防止 重复提交订单，解决 接口幂等性 问题)
         *
         * 验证过程:
         *   第一步 : 根据 用户id 从Redis中获取 防重令牌
         *   第二步 : 比较 从Redis中获取的防重令牌 和 客户端传过来的防重令牌
         *           如果二者相同 => 删除Redis中的防重令牌，并返回1
         *           如果二者不同 => 直接返回0
         *
         * NOTICE : 整个验证过程必须保证原子性
         *
         * 使用 Lua 脚本，可以实现 整个验证过程 是原子性的
         *   String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
         */

        // 验证 防重令牌
        // 使用 Lua 脚本，可以实现 整个验证过程 是原子性的
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        // 获取 客户端传过来的防重令牌
        String orderToken = orderSubmitVo.getOrderToken();
        // 原子性 验证令牌 & 删除令牌
        //   第一个参数 : Lua 脚本
        //   第二个参数 : Lua 脚本 中的 KEYS 指代的参数值
        //   第三个参数 : Lua 脚本 中的 ARGV 指代的参数值
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVO.getId()), orderToken);
        if (execute == 0L) {
            // 防重令牌验证失败
            response.setCode(1);
            return response;
        } else {
            // 防重令牌验证成功

            // 1.创建订单
            OrderCreateTo order = createOrder();

            // 2.验价
            // 准确的价格 : 计算所得的订单价格 (应付总额 = 订单总额 + 运费)
            BigDecimal payAmount = order.getOrder().getPayAmount();
            // 从客户端传过来的价格
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 验价成功

                // 3.保存 订单 & 订单项
                saveOrder(order);

                // 4.锁定库存 (所有订单项都锁定成功才算锁定成功，只要有一个订单项锁定失败那就是锁定失败)
                // 锁定商品库存VO
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);

                // 调用远程微服务 锁定库存 (所有订单项都锁定成功才算锁定成功，只要有一个订单项锁定失败那就是锁定失败)
                R r = wareFeignService.orderLockStock(wareSkuLockVo);

                if (r.getCode() == 0) {
                    // 库存锁定 成功
                    response.setOrder(order.getOrder());
                    return response;
                } else {
                    // 库存锁定 失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                // 验价失败
                response.setCode(2);
                return response;
            }
        }
    }

    // 创建订单
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        // 生成订单号
        String orderSn = IdWorker.getTimeId();
        // 构建订单信息
        OrderEntity orderEntity = buildOrder(orderSn);
        // 构建所有订单项信息
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        // 计算订单价格 (应付总额 = 订单总额 + 运费)
        computePrice(orderEntity, itemEntities);

        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setItems(itemEntities);

        return orderCreateTo;
    }

    // 构建订单信息
    private OrderEntity buildOrder(String orderSn) {
        // 从 ThreadLocal 中获取 memberResponseVO (memberResponseVO 中封装了 用户登录信息)
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberResponseVO.getId());

        // 从 ThreadLocal 中获取 orderSubmitVo
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        // 根据 收货地址 计算 运费
        R fareR = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (fareR.getCode() == 0) {
            // 远程调用成功
            // 从 fareR 中获取 fareResp
            FareVo fareResp = fareR.getData(new TypeReference<FareVo>() {
            });
            // 设置 运费信息
            orderEntity.setFreightAmount(fareResp.getFare());
            // 设置 收货人信息
            orderEntity.setReceiverCity(fareResp.getAddress().getCity());
            orderEntity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
            orderEntity.setReceiverName(fareResp.getAddress().getName());
            orderEntity.setReceiverPhone(fareResp.getAddress().getPhone());
            orderEntity.setReceiverPostCode(fareResp.getAddress().getPostCode());
            orderEntity.setReceiverProvince(fareResp.getAddress().getProvince());
            orderEntity.setReceiverRegion(fareResp.getAddress().getRegion());
        }
        // 设置 订单状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 设置 自动确认收货时间
        orderEntity.setAutoConfirmDay(7);
        // 设置 删除状态为 未删除
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }

    // 构建所有订单项信息
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 获取当前用户选中的所有购物项
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                // 构建当前订单项信息 (构建每一个订单项信息)
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    // 构建每一个订单项信息
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();

        // 1.商品spu信息
        Long skuId = cartItem.getSkuId();
        // 根据 skuId 查询 spu信息
        R spuInfoR = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = spuInfoR.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfoVo.getId());
        itemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        itemEntity.setSpuName(spuInfoVo.getSpuName());
        itemEntity.setCategoryId(spuInfoVo.getCatalogId());

        // 2.商品sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        // 将 List集合 转换为 String字符串
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());

        // 3.商品优惠信息
        // ...

        // 4.商品积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // 5.当前订单项的价格信息 (付款前，最后一次确定每个订单项的价格)
        // 商品促销分解金额
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        // 优惠券优惠分解金额
        itemEntity.setCouponAmount(new BigDecimal("0"));
        // 积分优惠分解金额
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 该商品经过优惠后的分解金额 (当前订单项的实际金额)
        // 总额 = 单价 * 数量
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        // 支付金额 = 总额 - 促销金额 - 优惠卷金额 - 积分优惠
        BigDecimal orderItemFinalPrice = origin.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(orderItemFinalPrice);

        return itemEntity;
    }

    // 计算订单价格 (应付总额 = 订单总额 + 运费)
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        // 订单总额 = 叠加所有订单项金额
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity entity : itemEntities) {
            // 商品促销分解金额
            promotion = promotion.add(entity.getPromotionAmount());
            // 优惠券优惠分解金额
            coupon = coupon.add(entity.getCouponAmount());
            // 积分优惠分解金额
            integration = integration.add(entity.getIntegrationAmount());
            // 该商品经过优惠后的分解金额
            total = total.add(entity.getRealAmount());
            // 赠送积分
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            // 赠送成长值
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        // 订单总额
        orderEntity.setTotalAmount(total);
        // 应付总额 = 订单总额 + 运费
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        // 促销抵扣金额
        orderEntity.setPromotionAmount(promotion);
        // 优惠券抵扣金额
        orderEntity.setCouponAmount(coupon);
        // 积分抵扣金额
        orderEntity.setIntegrationAmount(integration);
        // 积分
        orderEntity.setIntegration(gift.intValue());
        // 成长值
        orderEntity.setGrowth(growth.intValue());
    }

    // 保存 订单 & 订单项
    private void saveOrder(OrderCreateTo order) {
        // 保存订单
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        // 保存订单项
        List<OrderItemEntity> orderItems = order.getItems();
        for (OrderItemEntity orderItem : orderItems) {
            orderItemService.save(orderItem);
        }
    }

    // 封装 OrderConfirmVo对象
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 从 ThreadLocal 中获取 loginMember
        MemberResponseVO loginMember = LoginUserInterceptor.loginUser.get();

        /*
         * 异步请求时，会出现 Feign远程调用丢失请求上下文 的问题
         *   出现这个问题的原因 :
         *     Feign远程调用请求 是由 分线程(异步任务) 发出的
         *     只有主线程可以得到 原始请求的上下文信息，分线程无法获得主线程中的数据，所以分线程无法得到 原始请求的上下文信息
         *     正因为 分线程(异步任务) 没有 原始请求的上下文信息
         *     所以，在分线程在发出 Feign远程调用请求 时，Feign远程调用 会丢失 原始请求的请求上下文信息
         *
         *   这个问题引发的结果 :
         *     Feign远程调用其他微服务，其他微服务无法从Feign请求中获取 原始请求的相关数据 (比如 : Cookie 请求头)
         *     导致远程微服务无法获取 原始请求的Cookie数据，即不能判断当前用户是否已登录
         *     所以会默认用户未登录，导致逻辑判断出错，进而引发了一系列错误
         *
         *   解决方案 :
         *     第一步 : 从主线程中获取 原始请求的上下文信息
         *     第二步 : 将 原始请求的上下文信息 保存到 异步任务所在的分线程 中 (分线程共享主线程的请求上下文信息)
         */

        // 从主线程中获取 原始请求的上下文信息
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 开启异步任务 : 获取 会员收货地址
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            // 将 原始请求的上下文信息 保存到 异步任务所在的分线程 中 (分线程共享主线程的请求上下文信息)
            RequestContextHolder.setRequestAttributes(requestAttributes);

            // 获取 会员收货地址
            List<MemberAddressVo> address = memberFeignService.getAddressByMemberId(loginMember.getId());
            confirmVo.setAddress(address);
        }, executor);

        // 开启异步任务 : 获取当前用户选中的所有购物项 & 每个购物项对应的商品是否有货
        CompletableFuture<Void> cartItemsFuture = CompletableFuture.runAsync(() -> {
            // 将 原始请求的上下文信息 保存到 异步任务所在的分线程 中 (分线程共享主线程的请求上下文信息)
            RequestContextHolder.setRequestAttributes(requestAttributes);

            // 获取 当前用户选中的所有购物项
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            // 上一个异步任务执行完毕之后，才会执行当前异步任务 - 异步任务的线程串行化

            // 用户选中的所有购物项
            List<OrderItemVo> items = confirmVo.getItems();
            // 所有购物项 对应的 skuId集合
            List<Long> skuIdList = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            // 批量查询商品是否有货
            R skuStockVoR = wareFeignService.getSkuHasStock(skuIdList);
            // 从 skuStockVoR 中获取 SkuStockVo集合
            // 每一个 SkuStockVo 包含了 商品id & 商品是否有货
            List<SkuStockVo> skuStockVoList = skuStockVoR.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (skuStockVoList != null) {
                // Map<skuId, hasStock>
                Map<Long, Boolean> map = skuStockVoList.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);

        // 设置 优惠劵信息 (会员积分)
        Integer integration = loginMember.getIntegration();
        confirmVo.setIntegration(integration);

        // 创建 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        // 在 Redis 中保存一份 防重令牌
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginMember.getId(), token, 30, TimeUnit.MINUTES);
        // 在 OrderConfirmVo对象 中保存一份 防重令牌
        // 服务器会将 OrderConfirmVo对象 传到HTML页面 -> 页面中可以获取 防重令牌
        confirmVo.setOrderToken(token);

        // 等待所有异步任务完成
        CompletableFuture.allOf(addressFuture, cartItemsFuture).get();

        return confirmVo;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }
}
