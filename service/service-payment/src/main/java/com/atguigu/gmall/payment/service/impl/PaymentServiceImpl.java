package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;
    /**
     * 保存交易记录
     *
     * @param orderInfo
     * @param paymentType 支付类型 （1，微信 2，支付宝）
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderInfo.getId());
        queryWrapper.eq("payment_type", paymentType);

        Integer count = paymentInfoMapper.selectCount(queryWrapper);
        if (count > 0) return;

        //保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfoMapper.insert(paymentInfo);
    }

    //根据outTradeNo付款方式查询交易记录
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        return paymentInfoMapper.selectOne(queryWrapper);
    }

    //根据outTradeNo付款方式更新交易记录
    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        // update payment_info set payment_status=PAID,callback_time=new Date() where out_trade_no=outTradeNo and payment_type=name;
        // 第一个参数paymentInfo ， 表示更新的内容放入paymentInfo 中。
        // 第二个参数更新条件，
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        // 更新payment_status=PAID
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        // 更新回调时间
        paymentInfoUpd.setCallbackTime(new Date());
        // 更新回调内容 callback_content 可以随意写。
        paymentInfoUpd.setCallbackContent(paramMap.toString());

        // 追加更新trade_no
        //trade_no支付宝交易号，在paramMap中可以获取
        String trade_no = paramMap.get("trade_no");
        paymentInfoUpd.setTradeNo(trade_no);
        //构建更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        paymentInfoMapper.update(paymentInfoUpd, paymentInfoQueryWrapper);
        //如果没有订单id ，那么就查询 getPaymentInfo()
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, name);
        // 支付成功之后，发送消息通知订单。 更改订单状态。
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    }

    //根据outTradeNo付款方式更新交易记录,多一个参数
    @Override
    public void paySuccess(String outTradeNo, String name) {
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        //更新payment_status=PAID
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        //更新回调时间
        paymentInfoUpd.setCallbackTime(new Date());
        //更新回调内容
        paymentInfoUpd.setCallbackContent("异步回调了");
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo).eq("payment_type", name);
        paymentInfoMapper.update(paymentInfoUpd, paymentInfoQueryWrapper);
    }

    //更新方法
    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        // update payment_info set payment_status=PAID where out_trade_no=outTradeNo and payment_type=name;
        paymentInfoMapper.update(paymentInfo, new QueryWrapper<PaymentInfo>().eq("out_trade_no", outTradeNo));
    }
    //关闭交易记录
    @Override
    public void closePayment(Long orderId) {

        // 更新paymentInfo payment_status=CLOSED
        // 第一个参数表示更新内容，第二个参数表示更新条件
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());

        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderId);
        // update payment_info set payment_status=CLOSED where order_id = ?
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        // 如果交易记录中没有当前数据，则返回，不执行关闭。
         /*
        交易记录表中的数据什么时候才会产生？
        当用户点击支付宝生成支付二维码的时候，paymentInfo 才会有记录。
        如果只是下单，不点生成二维码的时候，这个表是没有数据的。
        根据上述条件，先查询是否有交易记录，如果没有交易记录，则不关闭。
         */
        if (null == count || count.intValue()==0){
            return;
        }
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }
}
