package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型 （1，微信 2，支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);

    /**
     * 根据outTradeNo付款方式查询交易记录
     * @param out_trade_no
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String out_trade_no, String name);

    /**
     * 根据outTradeNo付款方式更新交易记录
     * @param out_trade_no
     * @param name
     * @param paramMap
     */
    void paySuccess(String out_trade_no, String name, Map<String, String> paramMap);
//根据outTradeNo付款方式更新交易记录,多一个参数
    void paySuccess(String out_trade_no, String name);

    /**
     * 更新方法
     * @param outTradeNo
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    /**
     * 关闭交易记录
     * @param orderId
     */
    void closePayment(Long orderId);
}
