package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * 服务层接口
 */
public interface SeckillGoodsService {
    /**
     * 返回全部列表
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     *根据skuId获取实体
     * @param skuId
     * @return
     */
    SeckillGoods getSeckillGoodsById(Long skuId);

    /**
     * 秒杀预下单处理
     * @param skuId
     * @param userId
     */
    void seckillOrder(Long skuId, String userId);

    /**
     * 根据商品id和用户id查询订单状态
     * @param skuId
     * @param userId
     * @return
     */
    Result checkOrder(Long skuId, String userId);
}
