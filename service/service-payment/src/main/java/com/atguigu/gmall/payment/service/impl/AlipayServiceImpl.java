package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.logging.Handler;

@Service
public class AlipayServiceImpl implements AlipayService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private AlipayClient alipayClient;


    @Override
    public String createaliPay(Long orderId) throws AlipayApiException {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        //生成二维码
        AlipayTradePagePayRequest alipayRequest  = new AlipayTradePagePayRequest();
        //同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        //在公共参数中设置回调和通知地址栏

        //参数
        //声明一个map集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",orderInfo.getTotalAmount());
        map.put("subject","test");

        alipayRequest.setBizContent(JSON.toJSONString(map));
        return alipayClient.pageExecute(alipayRequest).getBody();
        //调用sdk生成表单
    }
    //根据orderId退款
    @Override
    public boolean refund(Long orderId) {
        //获取订单数据
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        //声明一个map
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",orderInfo.getTotalAmount());
        map.put("refund_reason","不值得一买。。。");
//        map.put("out_request_no","HZ01RF001"); 如果部分退款则需要使用它。
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()){
            //关闭交易状态
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(),paymentInfo);
            System.out.println("调用成功");
            return true;
        }else {
            System.out.println("调用失败");
            return false;
        }
    }
    //关闭支付记录
    @Override
    public boolean closePay(Long orderId) {
        //        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        // out_trade_no 在orderInfo,paymentInfo 都存在，并且这两个表中的值应该是一样。
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("operator_id","YX01");
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Boolean checkPayment(Long orderId) {

        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());

        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
