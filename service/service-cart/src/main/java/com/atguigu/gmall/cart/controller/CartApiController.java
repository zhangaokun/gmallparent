package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {
    @Autowired
    private CartService cartService;

    /**
     * 添加购物车，判断购物车是否有该商品
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){
        //如何获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            //获取临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId,userId,skuNum);
        return Result.ok();
    }

    /**
     * 查询购物车
     * @param request
     * @return
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //获取用户临时id
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList  = cartService.getCartList(userId, userTempId);
        return Result.ok(cartInfoList);
    }

    /**
     * 更新选中状态
     * @param skuId
     * @param isChecked
     * @param request
     * @return
     */
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //update cartInfo set isChecked=? where userId =? and skuId=?
        if (StringUtils.isEmpty(userId)){
            //未登录
            userId = AuthContextHolder.getUserTempId(request);
        }
        //调用更新方法
        cartService.checkCart(userId,isChecked,skuId);
        return Result.ok();
    }

    /**
     * 删除购物车中想要买的商品
     * @param skuId
     * @return
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        //如何获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            //获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId,userId);
        return Result.ok();

    }

    /**
     * 从购物车中获取购物清单
     * @param userId
     * @return
     */
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }
}
