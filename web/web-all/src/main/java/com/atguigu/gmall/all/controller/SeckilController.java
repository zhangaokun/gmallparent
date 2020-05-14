package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 秒杀
 */
@Controller
public class SeckilController {
    @Autowired
    private ActivityFeignClient activityFeignClient;

    /**
     * 秒杀列表
     *
     * @param model
     * @return
     */
    @GetMapping("seckill.html")
    public String index(Model model) {
        Result result = activityFeignClient.findAll();
        model.addAttribute("list", result.getData());
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        //通过skuId查询skuInfo
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item", result.getData());
        return "seckill/item";
    }

    /**
     * 秒杀排队
     *
     * @param skuId
     * @param skuIdStr
     * @param request
     * @return
     */
    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name = "skuId") Long skuId,
                        @RequestParam(name = "skuIdStr") String skuIdStr,
                        HttpServletRequest request) {
        // 存储skuIdStr，skuId
        request.setAttribute("skuId", skuId);
        request.setAttribute("skuIdStr", skuIdStr);
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model) {
        //获取到下单数据
        Result<Map<String, Object>> result = activityFeignClient.trade();
        if (result.isOk()) {
            //数据返回true将数据保存，给页面提供渲染
            model.addAllAttributes(result.getData());
            //返回订单页面
            return "seckill/trade";
        } else {
            //存储失败信息
            model.addAttribute("message", result.getMessage());
            //返回订单页面
            return "seckill/fail";
        }
    }
}
