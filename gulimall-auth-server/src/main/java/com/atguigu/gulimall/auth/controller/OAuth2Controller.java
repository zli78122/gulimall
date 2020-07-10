package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2Controller {

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "3682553736");
        map.put("client_secret", "ba4e058b83491111b19d64f997b60fac");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0weibo/success");
        map.put("code", code);
        // 获取 Access Token
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<>(), map, new HashMap<>());

        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            // 将 json字符串 转换为 SocialUser对象
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            // 调用远程微服务 : 社交登录
            R loginR = memberFeignService.oauthLogin(socialUser);

            if (loginR.getCode() == 0) {
                // 登录成功

//                MemberResponseVO loginUser = loginR.getData(new TypeReference<MemberResponseVO>() {
//                });
//                // session 子域共享问题
//                session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);

                // 跳转到网站首页
                return "redirect:http://gulimall.com";
            } else {
                // 登录失败
                return "redirect:http://auth.gulimall.com/login.html ";
            }
        } else {
            // 登录失败
            return "redirect:http://auth.gulimall.com/login.html ";
        }
    }
}
