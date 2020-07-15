package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 获取当前用户选中的所有购物项
     */
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems() {
        return cartService.getUserCartItems();
    }

    /**
     * 删除购物项
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 修改购物项的数量
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId, num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 改变购物项的选中状态
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {

        cartService.checkItem(skuId, check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 将商品添加到购物车
     * <p>
     * RedirectAttributes : 重定向 携带数据 - 实现请求重定向时的数据共享
     * addFlashAttribute() : 将数据放在Session中
     * addAttribute()      : 将数据作为请求参数放在URL后面
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {

        // 将商品添加到购物车
        cartService.addToCart(skuId, num);
        // 将 skuId 作为请求参数放在URL后面
        ra.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * 根据 skuId 查询 购物项，并跳转到 购物车添加成功页面 (success.html)
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        // 根据 skuId 查询 购物项
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItem);
        // 跳转到 购物车添加成功页面 (success.html)
        return "success";
    }

    /**
     * 获取购物车信息，并跳转到购物车页面 (cartList.html)
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        // 获取购物车信息
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        // 跳转到购物车页面 (cartList.html)
        return "cartList";
    }
}
