package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


public interface OrderService extends IService<OrderInfo> {
    //定义保存订单 由trade.html 可知需要返回一个订单Id
    //传入的参数：通过trade.html
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生产流水号
     * @param userId
     * @return
     */
    // 生成流水号，同时放入缓存。
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param tradeNo 页面的流水号
     * @param userId 获取缓存的流水号
     * @return
     */
    boolean checkTradeNo(String tradeNo,String userId);

    /**
     * 删除缓存的流水号
     * @param userId
     */
    void deleteTradeNo(String userId);

    /**
     * 根据skuId skuNum 来判断还有没有足够的库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 根据orderId 关闭过去订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据orderId修改订单状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);
    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 通过orderId发消息给库存，通知减库存
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo转为字符串
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(long orderId, String wareSkuMap);

    /**
     * 关闭订单，后来改造的方法
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);


}
