package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {

    // 将商品添加到购物车
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    // 根据 skuId 查询 购物项
    CartItem getCartItem(Long skuId);

    // 获取购物车信息
    Cart getCart() throws ExecutionException, InterruptedException;

    // 清空购物车
    void clearCart(String cartKey);

    // 改变购物项的选中状态
    void checkItem(Long skuId, Integer check);

    // 修改购物项的数量
    void changeItemCount(Long skuId, Integer num);

    // 删除购物项
    void deleteItem(Long skuId);

    // 获取当前用户选中的所有购物项
    List<CartItem> getUserCartItems();
}
