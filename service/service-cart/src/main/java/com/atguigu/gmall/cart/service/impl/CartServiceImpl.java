package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        //获取购物车的key
        String cartKey = getCartKey(userId);
        if (!redisTemplate.hasKey(cartKey)) {
            loadCartCache(userId);
        }
        //获取数据库对象
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id", skuId).eq("user_id", userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        //说明缓存中有数据
        if (cartInfoExist != null) {
            //数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            //查询最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            //更新数据
            cartInfoExist.setSkuPrice(skuPrice);
            // 更新数据库
            cartInfoMapper.updateById(cartInfoExist);
            // 添加到缓存：添加完成之后，如果查询购物车列表的时候，直接走缓存了。如果缓存过期了，才走数据库。
            // redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        } else {
            CartInfo cartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            //添加数据库
            cartInfoMapper.insert(cartInfo);
            cartInfoExist = cartInfo;
        }
        //更新缓存
        //hset（key，field，value）
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfoExist);
        //设置过期时间
        setCartKeyExpire(cartKey);
    }

    /**
     * 过id查询购物车列表
     *
     * @param userId
     * @param userTempId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        //创建一个返回集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        //未登录，临时用户id 获取未登录的购物车数据
        if (StringUtils.isEmpty(userId)) {
            cartInfoList = getCartList(userTempId);
            return cartInfoList;
        }
        /*
         1. 准备合并购物车
         2. 获取未登录的购物车数据
         3. 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同 则数量相加，合并完成之后，删除未登录的数据！
         4. 如果未登录购物车没有数据，则直接显示已登录的数据
          */

        //已经登录
        if (!StringUtils.isEmpty(userId)) {
            List<CartInfo> cartInfoArrayList = getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartInfoArrayList)) {
                //如果登陆购物车中有数据，则进行合并 合并的条件：skuId 相同
                cartInfoList = mergeToCartList(cartInfoArrayList, userId);
                //删除未登录购物车的数据
                deleteCartList(userTempId);
            }
            if (StringUtils.isEmpty(userTempId) || CollectionUtils.isEmpty(cartInfoArrayList)) {
                //根据什么查询？userId
                cartInfoList = getCartList(userId);
            }

            return cartInfoList;
        }
        return cartInfoList;
    }

    /**
     * 根据用户id查询购物车列表，被选中的状态，缓存里一定存在
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //定义key user :userId:cart
        String cartKey = getCartKey(userId);
        //获取缓存中的数据
        List<CartInfo> cartCachInfoList = redisTemplate.opsForHash().values(cartKey);
        //循环购物车中的数据
        if (null != cartCachInfoList && cartCachInfoList.size() > 0) {
            for (CartInfo cartInfo : cartCachInfoList) {
                //获取选中的商品
                if (cartInfo.getIsChecked().intValue() == 1) {
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    /**
     * 更新选中状态
     * @param userId
     * @param isChecked
     * @param skuId
     */
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //update cartInfo set isChecked = ? where skuId =? and userId=?
        //修改数据库
        //第一个参数表示修改的数据，第二个参数表示条件
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        queryWrapper.eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,queryWrapper);
        //修改缓存
        //定义key user:userId:cart
        String cartKey = getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> hashOperations  = redisTemplate.boundHashOps(cartKey);
        //获取用户选择的商品
        if (hashOperations.hasKey(skuId.toString())){
            CartInfo cartInfoUpd  = hashOperations.get(skuId.toString());
            //cartInfoUpd 写会缓存
            cartInfoUpd.setIsChecked(isChecked);
            //更新缓存
            hashOperations.put(skuId.toString(),cartInfoUpd);
            //设置过去时间
            setCartKeyExpire(cartKey);
        }
    }

    /**
     * 删除购物车中想要买的商品
     * @param skuId
     * @param userId
     */
    @Override
    public void deleteCart(Long skuId, String userId) {
        String cartKey = getCartKey(userId);
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userId).eq("sku_id",skuId));
        //获取缓存对象
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        if (hashOperations.hasKey(skuId.toString())){
            hashOperations.delete(skuId.toString());
        }
    }

    /**
     * 删除购物车
     *
     * @param userTempId
     */
    private void deleteCartList(String userTempId) {
        //删除数据库，删除缓存
        //delete from userInfo where userId =？ userTempId
        QueryWrapper queryWrapper = new QueryWrapper<CartInfo>();
        queryWrapper.eq("user_id", userTempId);
        cartInfoMapper.delete(queryWrapper);
        //删除缓存的
        String cartKey = getCartKey(userTempId);
        Boolean flag = redisTemplate.hasKey(cartKey);
        if (flag) {
            redisTemplate.delete(cartKey);
        }
    }

    /**
     * 合并
     *
     * @param cartInfoArrayList
     * @param userId
     * @return
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoArrayList, String userId) {
/*
    demo1:
        登录：
            37 1
            38 1
        未登录：
            37 1
            38 1
            39 1
        合并之后的数据
            37 2
            38 2
            39 1
     demo2:
         未登录：
            37 1
            38 1
            39 1
            40  1
          合并之后的数据
            37 1
            38 1
            39 1
            40  1
     */
        List<CartInfo> cartInfoListLogin = getCartList(userId);
        Map<Long, CartInfo> cartInfoMapLogin = cartInfoListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));

        for (CartInfo cartInfoNoLogin : cartInfoArrayList) {
            Long skuId = cartInfoNoLogin.getSkuId();
            //有更新数量
            if (cartInfoMapLogin.containsKey(skuId)) {
                CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
                //数量相加
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum());
                //合并数据：勾选(细节操作)
                //为登陆状态选中的商品
                if (cartInfoNoLogin.getIsChecked().intValue() == 1) {//如果状态等于1代表选中
                    cartInfoLogin.setIsChecked(1);
                }
                //更新数据库
                cartInfoMapper.updateById(cartInfoLogin);
            } else {
                //cartInfoNoLogin.setId(null);  这句话干嘛 主键置空 走完尚明走
                //将输入的直接插入到数据库
                cartInfoNoLogin.setUserId(userId);
                cartInfoMapper.insert(cartInfoNoLogin);
            }
        }
        //汇总数据 37 38 39
        //数据库中的数据
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    private List<CartInfo> getCartList(String userId) {
        //声明一个返回的集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) return cartInfoList;
        //定义key user：userId：cart
        String cartKey = getCartKey(userId);
        //获取数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            //购物车列表显示有顺序：按照商品的更新时间降序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {

                    return o1.getId().toString().compareTo((o2.getId().toString()));
                }
            });
            return cartInfoList;
        } else {
            //缓存中没有数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    /**
     * 通过userId查询购物车并放入缓存
     *
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {
        QueryWrapper queryWrapper = new QueryWrapper<CartInfo>();
        queryWrapper.eq("user_id", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        //将数据库中的数据查询并放入缓存
        HashMap<String, CartInfo> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {

            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        //定义key user：userId ：cart
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().putAll(cartKey, map);
        //设置过期时间
        setCartKeyExpire(cartKey);
        return cartInfoList;
    }


    /**
     * 设置过期时间
     *
     * @param cartKey
     */
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取购物车的key
     *
     * @param userId
     * @return
     */
    private String getCartKey(String userId) {
        //定义key user：userId：cart
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
