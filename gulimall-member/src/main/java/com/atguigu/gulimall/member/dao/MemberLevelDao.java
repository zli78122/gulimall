package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级
 * 
 * @author zhengyuli
 * @email zli78122@usc.edu
 * @date 2020-06-23 00:12:01
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {

    // 获取 会员的默认等级
    MemberLevelEntity getDefaultLevel();
}
