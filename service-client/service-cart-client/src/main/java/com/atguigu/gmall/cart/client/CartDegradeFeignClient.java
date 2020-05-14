package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartDegradeFeignClient implements CartFeignClient {
    @Override
    public Result addToCart(Long skuId, Integer SkuNum) {
        return null;
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        return null;
    }

    @Override
    public Result loadCartCache(String userId) {
        return null;
    }
}
