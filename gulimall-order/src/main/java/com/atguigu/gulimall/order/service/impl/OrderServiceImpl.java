package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVO;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import com.atguigu.gulimall.order.vo.SkuStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WareFeignService wareFeignService;

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
