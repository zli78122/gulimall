package com.atguigu.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberReceiveAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * 会员收货地址
 *
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:12:01
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageUtils queryPage(Map<String, Object> params);

    // 根据 memberId 获取 会员收货地址
    List<MemberReceiveAddressEntity> getAddressByMemberId(Long memberId);
}
