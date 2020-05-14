package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.util.List;

public interface CartService {
    /**
     * 添加购物车用户id 商品id 商品数量
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(Long skuId,String userId ,Integer skuNum);

    /**
     * 通过id查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId , String userTempId);

    /**
     * 根据用户id 查询购物车列表，被选中的商品
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 更新选中状态
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 删除购物车中想要买的商品
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据用户id加载购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
