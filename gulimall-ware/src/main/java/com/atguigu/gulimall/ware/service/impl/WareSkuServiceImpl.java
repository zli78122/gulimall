package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private ProductFeignService productFeignService;

    /**
     * 锁定库存 (所有订单项都锁定成功才算锁定成功，只要有一个订单项锁定失败那就是锁定失败)
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo lockVo) {
        // 给 每一个订单项 都封装一个 SkuWareHasStock对象 (查询 每件商品在哪些仓库中有库存)
        List<OrderItemVo> locks = lockVo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 根据 skuId 查询 该商品在哪些仓库中有库存
            List<Long> wareIds = baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 锁定库存
        // 标识 是否所有订单项都可以锁定成功
        Boolean allLock = true;
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有当前商品
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                // 锁定库存 (根据 商品id、库存id、需要锁定的商品件数 锁定库存)
                //   锁定成功 : 返回1
                //   锁定失败 : 返回0
                Long count = baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    // 当前商品 锁定成功
                    skuStocked = true;
                    break;
                } else {
                    // 当前仓库 锁定 当前商品 失败
                    // 如果还有下一个仓库，尝试使用下一个仓库 锁定 当前商品
                }
            }
            if (!skuStocked) {
                // 所有仓库 锁定 当前商品 失败 -> 当前商品锁定失败 -> 全局锁定失败 -> 抛出 NoStockException异常
                throw new NoStockException(skuId);
            }
        }
        // 全部商品锁定成功
        return true;
    }

    // 查询sku是否有库存
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            // 查询 当前sku 的 总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    // 将 成功采购的商品 入库
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 如果仓库中还没有这件商品的记录 -> 新增
        // 如果仓库中已经有了这件商品的记录 -> 更新
        List<WareSkuEntity> entities = wareSkuDao.selectList(
                new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId)
        );
        if (entities == null || entities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);

            // 远程调用 商品微服务，查询 商品名称
            // 如果查询失败，事务不回滚！！！因为没必要因为一个不重要的字段没查出来，就把整个事务都回滚
            // 所以，如果 捕获到异常，不需要把异常抛出，直接自己把异常吞下即可 -> catch{} 中 除了打印异常信息外 其余什么都不做！
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 添加 商品库存记录
            wareSkuDao.insert(skuEntity);
        } else {
            // 更新 商品库存记录
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    // 分页条件查询
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }

    @Data
    class SkuWareHasStock {
        // 商品id
        private Long skuId;
        // 需要锁定的商品件数
        private Integer num;
        // 拥有该商品的仓库集合 (该商品在哪些仓库中有库存)
        private List<Long> wareId;
    }
}
