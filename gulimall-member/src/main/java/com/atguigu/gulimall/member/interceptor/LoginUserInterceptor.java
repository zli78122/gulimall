package com.atguigu.gulimall.member.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberResponseVO;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录拦截器 - LoginUserInterceptor
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    // ThreadLocal : 同一个线程共享数据
    // 将 MemberResponseVO对象 存入 ThreadLocal 中
    public static ThreadLocal<MemberResponseVO> loginUser = new InheritableThreadLocal<>();

    /**
     * 在目标方法执行之前拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();

        // 放行请求 : "/member/**"
        boolean match = antPathMatcher.match("/member/**", uri);
        if (match) {
            // 放行
            return true;
        }

        MemberResponseVO loginMember = (MemberResponseVO) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (loginMember != null) {
            // 用户已登录 -> 放行
            // 将 loginMember 存入 ThreadLocal 中
            loginUser.set(loginMember);
            return true;
        } else {
            // 用户未登录 -> 跳转到登录页
            request.getSession().setAttribute("msg", "请先登录！");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
