package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {
    /**
     * 获取sku详细信息
     * @param skuId
     * @return
     */
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable Long skuId);
}
