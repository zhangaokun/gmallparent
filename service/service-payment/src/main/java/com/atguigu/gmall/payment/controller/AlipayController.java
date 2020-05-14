package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {
    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;

    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId) {
        String from = "";
        try {
            from = alipayService.createaliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return from;
    }

    /**
     * 同步回调
     *
     * @return
     */
    @RequestMapping("callback/return")
    public String callBask() {
        // 给支付成功页面。
        // return_order_url=http://payment.gmall.com/pay/success.html
        //同步回调给用户展示信息
        return "redirect:" + AlipayConfig.return_order_url;
    }
    // 异步回调： 它需要做内网穿透，异步回调需要支付宝与电商平台做数据校验。
    // 校验过程： https://opendocs.alipay.com/open/270/105902
    // notify_payment_url=http://t5msem.natappfree.cc/api/payment/alipay/callback/notify
    @RequestMapping("callback/notify")
    @ResponseBody
    public String aliPayNotify(@RequestParam Map<String, String> paramMap) {
        //Map<String,String> paramsMap = ...
        boolean signVerified = false;
        //因为将支付宝异步通知的参数封装到paramMap集合中
        String trade_status = paramMap.get("trade_status");
        //获取out_trade_no 查询当前的paymentInfo数据
        String out_trade_no = paramMap.get("out_trade_no");
        try {
            //验证签名成功
            //调用SDK验证签名
           // signVerified = AlipaySignature.rsaCertCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (signVerified) {
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
                // out_trade_no,金额，等数据。
                // 验证金额，outTradeNo,app_id 都要获取全部通过才能返回success.

//                int amount =Integer.parseInt(total_amount);
//                BigDecimal totalAmount = new BigDecimal(amount);
//                if (paymentInfo.getTotalAmount().compareTo(totalAmount)==0 && paymentInfo.getOutTradeNo().equals(out_trade_no)){
//                    // 处理PAID,CLOSED 之外，那么就应该更新交易记录。
//                    paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramMap);
//                    // 返回支付成功
//                    return "success";
//                }
                // 查询支付交易记录状态， 如果是 payment_status=PAID,CLOSED 那么应该返回failure。
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
                    return "failure";
                }
                String total_amount = paramMap.get("total_amount");
                // 处理PAID,CLOSED 之外，那么就应该更新交易记录。
                paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramMap);
                // 返回支付成功
                return "success";
            }
        } else {
            return "failure";
        }
        return "failure";
    }
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
        //调用退款接口
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }
    //检查是否有交易记录的控制器
    @ResponseBody//将返回来的数据直接返回到页面
    @RequestMapping("checkPayment/{orderId}")
    public Boolean checkPayment(@PathVariable Long orderId){
        Boolean aBoolean = alipayService.checkPayment(orderId);
        return aBoolean;
    }
    //根据订单id关闭订单
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

}
