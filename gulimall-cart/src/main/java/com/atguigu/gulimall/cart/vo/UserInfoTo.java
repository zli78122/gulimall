package com.atguigu.gulimall.cart.vo;

import lombok.Data;

@Data
public class UserInfoTo {
    // 用户id
    //   userId == null : 用户未登录
    //   userId != null : 用户已登录
    private Long userId;
    // 用户key
    private String userKey;
    // 浏览器中是否有 Name="user-key" 的Cookie
    private Boolean hasUserKeyCookie = false;
}
