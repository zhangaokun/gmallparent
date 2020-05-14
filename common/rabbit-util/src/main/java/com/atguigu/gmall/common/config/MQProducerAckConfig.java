package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping
    public void init(){
        //confirmCallback
        rabbitTemplate.setConfirmCallback(this);
        //ReturnCallback
        rabbitTemplate.setReturnCallback(this);
    }
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack){
            log.info("消息发送成功："+ JSON.toJSONString(correlationData));

        }else {
            log.info("消息发送失败："+cause+"数据："+JSON.toJSONString(correlationData));
        }
    }

    /**
     *
     * @param message 消息的内容
     * @param replyCode 消息码
     * @param replyText 消息对应的内容
     * @param exchange 绑定的交换机
     * @param routingKey 绑定的routingKey
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
    //反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

    }
}
