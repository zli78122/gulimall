package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberResponseVO;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 自定义拦截器 - CartInterceptor
 */
public class CartInterceptor implements HandlerInterceptor {

    // ThreadLocal : 同一个线程共享数据
    // 将 UserInfoTo对象 存入 ThreadLocal 中
    public static ThreadLocal<UserInfoTo> threadLocal = new InheritableThreadLocal<>();

    /**
     * 在目标方法执行之前拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从Session中获取用户信息
        //   如果能获取到，说明用户已登录
        //   如果获取不到，说明用户未登录
        HttpSession session = request.getSession();
        MemberResponseVO member = (MemberResponseVO) session.getAttribute(AuthServerConstant.LOGIN_USER);

        UserInfoTo userInfoTo = new UserInfoTo();

        if (member != null) {
            // 用户已登录
            userInfoTo.setUserId(member.getId());
        }

        // 判断浏览器中是否有 Name="user-key" 的Cookie
        //   如果有，说明浏览器近期访问过 购物车微服务
        //   如果没有，给 UserInfoTo对象 分配一个新的 userKey
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                // 浏览器中有 Name="user-key" 的Cookie
                if (cookieName.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setHasUserKeyCookie(true);
                }
            }
        }
        // 浏览器中没有 Name="user-key" 的Cookie -> 给 UserInfoTo对象 分配一个新的 userKey
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        // 将 userInfoTo 存入 ThreadLocal 中
        threadLocal.set(userInfoTo);

        // 放行
        return true;
    }

    /**
     * 在目标方法执行之后拦截
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 从 ThreadLocal 中获取 userInfoTo
        UserInfoTo userInfoTo = threadLocal.get();

        // 浏览器中没有 Name="user-key" 的Cookie -> 给浏览器分配一个新的 Name="user-key" 的Cookie
        if (!userInfoTo.getHasUserKeyCookie()) {
            // 给浏览器分配一个新的 Name="user-key" 的Cookie
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            // 放大Cookie的作用域 - 设置Cookie的作用域为 父域(gulimall.com)
            cookie.setDomain("gulimall.com");
            // Cookie的有效期为一个月
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
