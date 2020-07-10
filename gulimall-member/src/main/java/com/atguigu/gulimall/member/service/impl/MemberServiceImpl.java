package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UserNameExistException;
import com.atguigu.gulimall.member.service.MemberLevelService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegisterVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;

@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelService memberLevelService;

    // 社交登录
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        // 当前社交账户第一次登录 -> 注册
        // 当前社交账户之前登录过 -> 登录

        // 判断当前社交账户之前是否登录过 - 判断数据库中是否有当前账户的信息
        String uid = socialUser.getUid();
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("social_uid", uid);
        MemberEntity memberEntity = baseMapper.selectOne(wrapper);
        if (memberEntity != null) {
            // 当前社交账户之前登录过 -> 登录

            // 更新数据
            MemberEntity updateMemberEntity = new MemberEntity();
            updateMemberEntity.setId(memberEntity.getId());
            updateMemberEntity.setAccessToken(socialUser.getAccess_token());
            updateMemberEntity.setExpiresIn(socialUser.getExpires_in());
            baseMapper.updateById(updateMemberEntity);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());

            return memberEntity;
        } else {
            // 当前社交账户第一次登录 -> 注册

            MemberEntity registerMemberEntity = new MemberEntity();
            try {
                // 查询当前社交账户的基本信息 (昵称、性别等)
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUser.getAccess_token());
                query.put("uid", socialUser.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String userName = jsonObject.getString("idstr");
                    String nikeName = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");
                    registerMemberEntity.setUsername(userName);
                    registerMemberEntity.setNickname(nikeName);
                    registerMemberEntity.setGender("m".equals(gender) ? 1 : 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            registerMemberEntity.setSocialUid(socialUser.getUid());
            registerMemberEntity.setAccessToken(socialUser.getAccess_token());
            registerMemberEntity.setExpiresIn(socialUser.getExpires_in());

            // 保存账户信息
            baseMapper.insert(registerMemberEntity);

            return registerMemberEntity;
        }
    }

    // 登录
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginacct).or().eq("mobile", loginacct);

        MemberEntity memberEntity = baseMapper.selectOne(wrapper);
        if (memberEntity == null) {
            return null;
        } else {
            // 验证密码
            String dbPassWord = memberEntity.getPassword();
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean matches = encoder.matches(password, dbPassWord);
            if (matches) {
                return memberEntity;
            } else {
                return null;
            }
        }
    }

    // 注册
    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        // 设置 会员的等级 为 默认等级
        MemberLevelEntity memberLevelEntity = memberLevelService.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        // 检查用户名、手机号是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());

        // 盐值加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encode = encoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        baseMapper.insert(memberEntity);
    }

    // 检查手机号是否唯一
    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile", phone);

        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

    // 检查用户名是否唯一
    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", userName);

        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }
}
