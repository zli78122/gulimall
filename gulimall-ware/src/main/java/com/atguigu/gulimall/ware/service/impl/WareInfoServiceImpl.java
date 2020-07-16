package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.feign.MemberFeignService;
import com.atguigu.gulimall.ware.vo.FareVo;
import com.atguigu.gulimall.ware.vo.MemberAddressVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareInfoDao;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;
import com.atguigu.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Resource
    private MemberFeignService memberFeignService;

    // 根据 收货地址 计算 运费
    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        // 根据 收货地址id 获取 收货地址信息
        R addrInfoR = memberFeignService.addrInfo(addrId);
        // 从 addrInfoR 中获取 memberAddressVo
        MemberAddressVo memberAddressVo = addrInfoR.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
        });
        if (memberAddressVo != null) {
            // 调用第三方物流接口，计算运费
            // 此处模拟计算运费 : 用户手机号最后一位的数字为运费
            String phone = memberAddressVo.getPhone();
            String substring = phone.substring(phone.length() - 1, phone.length());
            BigDecimal bigDecimal = new BigDecimal(substring);
            // 设置 会员收货地址
            fareVo.setAddress(memberAddressVo);
            // 设置 运费
            fareVo.setFare(bigDecimal);
            return fareVo;
        }
        return null;
    }

    // 分页条件查询
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wareInfoEntityQueryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wareInfoEntityQueryWrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }

        IPage<WareInfoEntity> page = this.page(new Query<WareInfoEntity>().getPage(params), wareInfoEntityQueryWrapper);

        return new PageUtils(page);
    }
}
