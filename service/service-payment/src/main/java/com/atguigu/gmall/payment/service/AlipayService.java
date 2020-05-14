package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AlipayService {
    /**
     * 支付接口，根据订单id完成支付
     * @param orderId
     * @return
     * @throws AlipayApiException
     */
    String createaliPay(Long orderId) throws AlipayApiException;

    /**
     * 根据orderId退款
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /**
     * 关闭支付记录
     * @param orderId
     * @return
     */
    boolean closePay(Long orderId);

    /**
     * 是否在支付宝中有没有交易记录
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
