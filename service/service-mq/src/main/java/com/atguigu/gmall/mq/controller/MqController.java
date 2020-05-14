package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
@Slf4j
public class MqController {
    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("sendConfirm")
    public Result sendConfirm() {
        String message = "hello RabbitMq!";
        rabbitService.sendMessage("exchange.confirm", "routing.confirm", message);
        return Result.ok();
    }
    //http://cart.gmall.com:8282/mq/sendDeadLettle
//    @GetMapping("sendDeadLettle")
//    public Result sendDeadLettle() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        //方式一：指定每个消息延迟时间，存在问题：后进入队列会阻塞先进入队列超时消息，原因：先进先出，即使超时也出不来
//        this.rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "11", message -> {
//            // 如果配置了 params.put("x-message-ttl", 5 * 1000); 那么这一句也可以省略,具体根据业务需要是声明 Queue 的时候就指定好延迟时间还是在发送自己控制时间
//            message.getMessageProperties().setExpiration(1 * 1000 * 10 + "");
//            System.out.println(sdf.format(new Date()) + " Delay sent.");
//            return message;
//        });
//
//        return Result.ok();
//    }

    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "11");
        System.out.println(sdf.format(new Date()) + " Delay sent.");
        return Result.ok();
    }

    @GetMapping("sendDealy")
    public Result sendDealy() {
        // 发送数据
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay,
                simpleDateFormat.format(new Date()), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 设置延迟时间
                        message.getMessageProperties().setDelay(10 * 1000);
                        System.out.println(simpleDateFormat.format(new Date()) + " Delay send....");
                        return message;
                    }
                });
        return Result.ok();
    }


}
