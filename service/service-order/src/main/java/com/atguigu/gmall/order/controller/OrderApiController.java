package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/order")
public class OrderApiController {
    @Autowired
    // UserFeignClient
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 确认订单
     *
     * @param request
     * @return
     */
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        //获取到用户id
        String userId = AuthContextHolder.getUserId(request);
        //获取用户地址
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //渲染送货清单
        //先得到用户想要购买的商品
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId);
        //声明一个集合来存储订单明细
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        int totalNum = 0;
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            //记录数+skuNum即可
            totalNum += cartInfo.getSkuNum();
            //添加到集合
            detailArrayList.add(orderDetail);
        }
        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        //计算总金额
        orderInfo.sumTotalAmount();

        Map<String, Object> result = new HashMap<>();
        result.put("userAddressList", userAddressList);
        result.put("detailArrayList", detailArrayList);
        //保存总金额
        //result.put("totalNum", detailArrayList.size());
        result.put("totalNum", totalNum);
        result.put("totalAmount", orderInfo.getTotalAmount());

        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        result.put("tradeNo", tradeNo);

        return Result.ok(result);
    }

    //下订单的控制器，带有auth用户必须登陆
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,
                              HttpServletRequest request) {
        //userId 在控制器能获取到，暂时不用写
        String userId = AuthContextHolder.getUserId(request);
        //在保存之前将用户id复制给orderInfo
        orderInfo.setUserId(Long.parseLong(userId));
        // 获取前台页面的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 调用服务层的比较方法
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
        if (!flag) {
            // 比较失败！
            return Result.fail().message("不能重复提交订单！");
        }
        //  删除流水号
        orderService.deleteTradeNo(userId);
        //验证库存，用户购买的每个商品必须验证
        //循环订单明细中每个商品
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (null != orderDetailList && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                //判断循环result=true 表示有足够的库存，如果result=fasle 表示没有足够的库存。
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    return Result.fail().message(orderDetail.getSkuName() + "没有足够库存了");
                }
                //检查价格是否有变动，orderDetail.getOrderPrice
                //如果比较结果不一致：价格有变动，提示用户重新下单
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                    //判断只要不等于0，那么就说明价格有变动
                    //更新购物车中的价格，重新查询一遍
                    cartFeignClient.loadCartCache(userId);
                    return Result.fail().message(orderDetail.getSkuName() + "商品价格有变动，请重新下单！");
                }
            }
        }
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    /**
     * 内部调用获取订单
     *
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    // http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        //获取传递过来的参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //获取子订单集合，根据当前传递过来的参数进行获取
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId), wareSkuMap);
        List<Map> mapArrayList = new ArrayList<>();
        //获取子订单集合的字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            //将子订单中的部分数据变成map，再将map转化为字符串
            //一个map集合表示一个订单对象，因为查单可能有多个orderInfo，所有将map放入一个集合中统一存储
            Map map = orderService.initWareOrder(orderInfo);
            mapArrayList.add(map);
        }
        //返回子订单的集合字符串
        return JSON.toJSONString(mapArrayList);
    }
    //提交秒杀订单
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        //保存订单数据
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
