package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("pay.html")
    public String success(HttpServletRequest request){
        //获取订单id
        String orderId = request.getParameter("orderId");
        //远程调用订单
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        //保存作用域
        request.setAttribute("orderInfo",orderInfo);
        //model.setAttrbute("orderInfo", orderInfo);
        //返回支付页面
        return "payment/pay";
    }

    /**
     * 支付成功页
     * @param //request
     * @return
     */
    @GetMapping("pay/success.html")
    public String success() {
        return "payment/success";
    }

}
