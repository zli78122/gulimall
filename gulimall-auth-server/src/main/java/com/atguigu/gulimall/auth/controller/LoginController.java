package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {

    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private MemberFeignService memberFeignService;

    /**
     * 登录
     *
     * @param vo                 登录VO
     * @param redirectAttributes 重定向 携带数据 - 实现请求重定向时的数据共享
     * @param session            Session
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session) {
        R loginR = memberFeignService.login(vo);
        if (loginR.getCode() == 0) {
            // 登录成功
            // 从 loginR 中获取 loginUser
//            MemberResponseVO loginUser = loginR.getData(new TypeReference<MemberResponseVO>() {
//            });
//            session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);
            return "redirect:http://gulimall.com";
        } else {
            // 登录失败
            Map<String, String> errors = new HashMap<>();
            // 从 loginR 中获取 错误消息
            errors.put("msg", loginR.getData("msg", new TypeReference<String>() {
            }));
            // 重定向 携带数据 - 实现请求重定向时的数据共享
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    /**
     * 注册
     *
     * @param registerVo         注册VO
     * @param result             JSR 303 数据校验结果
     * @param redirectAttributes 重定向 携带数据 - 实现请求重定向时的数据共享
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo registerVo, BindingResult result, RedirectAttributes redirectAttributes) {
        // JSR 303 数据校验
        if (result.hasErrors()) {
            // Map<String, String>
            //   key : fieldError.getField()
            //   value : fieldError.getDefaultMessage()
            Map<String, String> errors = new HashMap<>();
            for (FieldError fieldError : result.getFieldErrors()) {
                if (!errors.containsKey(fieldError.getField())) {
                    errors.put(fieldError.getField(), fieldError.getDefaultMessage());
                }
            }

            // 重定向 携带数据 - 实现请求重定向时的数据共享
            redirectAttributes.addFlashAttribute("errors", errors);

            return "redirect:http://auth.gulimall.com/reg.html";
        }

        // 校验验证码
        String code = registerVo.getCode();
        String phone = registerVo.getPhone();
        String redisStorageCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisStorageCode)) {
            if (code.equals(redisStorageCode.split("_")[0])) {
                // 删除验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
                // 调用远程微服务 : 注册
                R registerR = memberFeignService.register(registerVo);
                if (registerR.getCode() == 0) {
                    // 注册成功
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    // 注册失败
                    Map<String, String> errors = new HashMap<>();
                    // 从 registerR 中获取 错误消息
                    errors.put("msg", registerR.getData("msg", new TypeReference<String>() {
                    }));
                    // 重定向 携带数据 - 实现请求重定向时的数据共享
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                // 注册失败
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                // 重定向 携带数据 - 实现请求重定向时的数据共享
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            // 注册失败
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            // 重定向 携带数据 - 实现请求重定向时的数据共享
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    /**
     * 发送验证码
     */
    @GetMapping("/sms/sendcode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long saveTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - saveTime < 60 * 1000) {
                // 60秒之内 只能发送一次验证码
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        // Redis中保存验证码信息
        //   Key : sms:code:18931007018
        //   Value : 8386_1594337611831
        //   TTL : 600
        String code = (1000 + (int) (Math.random() * 9000)) + "";
        String redisStorageCode = code + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redisStorageCode, 10, TimeUnit.MINUTES);

        // 发送验证码
        thirdPartyFeignService.sendSMSCode(phone, code);

        return R.ok();
    }
}
