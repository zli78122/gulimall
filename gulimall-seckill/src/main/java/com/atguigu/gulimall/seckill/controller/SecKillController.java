package com.atguigu.gulimall.seckill.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.service.SecKillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SecKillController {

    @Autowired
    private SecKillService secKillService;

    /**
     * 秒杀
     *
     * @param killId 秒杀Id = sessionId_skuId
     * @param key    秒杀随机码
     * @param num    购买商品数量
     */
    @GetMapping("/secKill")
    public String secKill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model) {

        String orderSn = secKillService.secKill(killId, key, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }

    /**
     * 根据 skuId 获取 商品秒杀信息
     */
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSecKillInfo(@PathVariable("skuId") Long skuId) {
        // 根据 skuId 获取 商品秒杀信息
        SecKillSkuRedisTo to = secKillService.getSkuSecKillInfo(skuId);
        return R.ok().setData(to);
    }

    /**
     * 获取 当前时间正在参与秒杀的所有商品
     */
    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSecKillSku() {
        // 获取 当前时间正在参与秒杀的所有商品
        List<SecKillSkuRedisTo> vos = secKillService.getCurrentSecKillSku();
        return R.ok().setData(vos);
    }
}
