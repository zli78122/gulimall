package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    // 自定义的线程池对象
    @Autowired
    private ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    // 获取当前用户选中的所有购物项
    @Override
    public List<CartItem> getUserCartItems() {
        // 从 ThreadLocal 中获取 userInfoTo
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        if (userInfoTo.getUserId() == null) {
            // 用户未登录
            return null;
        } else {
            // 用户已登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 获取购物车的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            // 获取购物车中所有被选中的购物项
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item -> {
                        // 获取商品的最新价格
                        R skuPrice = productFeignService.getSkuPrice(item.getSkuId());
                        String price = (String) skuPrice.get("data");
                        item.setPrice(new BigDecimal(price));
                        return item;
                    })
                    .collect(Collectors.toList());
            return collect;
        }
    }

    // 删除购物项
    @Override
    public void deleteItem(Long skuId) {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 更新Redis (删除购物项)
        cartOps.delete(skuId.toString());
    }

    // 修改购物项的数量
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 根据 skuId 查询 购物项
        CartItem cartItem = getCartItem(skuId);
        // 修改购物项的数量
        cartItem.setCount(num);
        // 更新Redis
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    // 改变购物项的选中状态
    @Override
    public void checkItem(Long skuId, Integer check) {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 改变购物项的选中状态
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);
        // 更新Redis
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    // 获取购物车信息
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();

        // 从 ThreadLocal 中获取 userInfoTo
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        // userId != null : 用户已登录
        // userId == null : 用户未登录
        if (userInfoTo.getUserId() != null) {
            // 用户已登录 -> 合并临时购物车和在线购物车 (将临时购物车的数据全部合并到在线购物车)
            // 在线购物车Key
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 临时购物车Key
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            // 获取临时购物车数据
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null) {
                // 遍历临时购物车中的购物项
                for (CartItem item : tempCartItems) {
                    // 将临时购物车中的购物项添加到在线购物车
                    addToCart(item.getSkuId(), item.getCount());
                }
                // 清除临时购物车数据
                clearCart(tempCartKey);
            }
            // 获取在线购物车的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {
            // 用户未登录 -> 获取临时购物车的所有购物项
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }

        return cart;
    }

    // 获取购物车的所有购物项
    private List<CartItem> getCartItems(String cartKey) {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            // 遍历 values，获取购物车的所有购物项
            List<CartItem> collect = values.stream().map(obj -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    // 清空购物车
    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    // 根据 skuId 查询 购物项
    @Override
    public CartItem getCartItem(Long skuId) {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 根据 skuId 获取对应的购物项 (获取的是购物项的Json字符串)
        String res = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(res, CartItem.class);
        return cartItem;
    }

    // 将商品添加到购物车
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 根据 skuId 获取对应的购物项 (获取的是购物项的Json字符串)
        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            // 购物项为空，说明购物车无此商品 -> 将商品添加到购物车
            CartItem cartItem = new CartItem();

            /*
             * 开启异步任务 (两个异步任务)
             * 为什么要使用异步任务？
             * 如果第一个任务执行需要2秒，第二个任务执行需要3秒
             * 异步任务 : 总共需要3秒
             * 同步任务 : 总共需要5秒
             */

            // 异步任务1 : 调用远程服务 获取sku信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                // 获取sku信息
                R skuInfoR = productFeignService.getSkuInfo(skuId);
                if (skuInfoR.getCode() == 0) {
                    // 远程调用成功
                    // 从 skuInfoR 中获取 skuInfo
                    SkuInfoVo skuInfo = skuInfoR.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                    });
                    cartItem.setCheck(true);
                    cartItem.setCount(num);
                    cartItem.setImage(skuInfo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfo.getSkuTitle());
                    cartItem.setSkuId(skuId);
                    cartItem.setPrice(skuInfo.getPrice());
                }
            }, executor);

            // 异步任务2 : 调用远程服务 获取商品的所有销售属性的属性值
            CompletableFuture<Void> getSkuSaleAttrTask = CompletableFuture.runAsync(() -> {
                // 根据 skuId 获取 商品的所有销售属性的属性值
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, executor);

            // 等待所有异步任务完成
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrTask).get();

            // 更新Redis
            String cartItemJsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), cartItemJsonString);

            return cartItem;
        } else {
            // 购物项不为空，说明购物车有此商品 -> 修改商品数量
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            // 更新Redis
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
    private BoundHashOperations<String, Object, Object> getCartOps() {
        // 从 ThreadLocal 中获取 userInfoTo
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        // userId != null : 用户已登录
        // userId == null : 用户未登录
        if (userInfoTo.getUserId() != null) {
            // 用户已登录
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            // 用户未登录
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        // 获取 BoundHashOperations对象，用于操作存储在Redis中的购物车数据
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }
}
