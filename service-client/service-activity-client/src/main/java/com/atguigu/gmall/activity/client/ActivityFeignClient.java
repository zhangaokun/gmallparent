package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 活动Api接口
 */
@FeignClient(value = "service-activity",fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {
    /**
     * 获取全部列表
     * @return
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();

    /**
     * 获取实体
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable("skuId") Long skuId);

    /**
     * 秒杀确认订单
     * @return
     */
    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String, Object>> trade();

}
