package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeadLetterMqConfig {
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    /**
     * 其他队列可以在rabbitListener上面做绑定
     * 哪错？删除的那个是什么？ 都说了，是交换机已经队列呀。
     * 不应该是这个里面的吗？ 对呀，这个里面有交换机。还有队列。
     * 如果你第一次启动成功，那么交换机与对应应该有绑定关系。可以从交换机中找队列，
     * 但是，如果没有启动成功，或者绑定不上，是找不到的。要从代码找。
     * 我给你们的初始化虚拟机都是能找到的。
     *
     * 从代码找：exchange.dead 绑定队列1 queue.dead.1 然后到延迟时间以后，则会
     * 由队列2处理 queue.dead.2 。
     * 总体来讲 exchange.dead 它应该绑定了两个队列。so 将其他全部删除，启动。
     * 你这交换机中队列绑定关系很多都是空？
     * 你删除了？我也不知道为啥，我只是删除了交换机，没删除里面的队列、、、
     * 行了，现在知道错误原因就行了。
     * 可以再演示一遍删除吗
     * 看懂么？我没有点。ok
     * @return
     */
    @Bean
    public DirectExchange exchange(){
        return new DirectExchange(exchange_dead,true,false,null);
    }
    @Bean
    public Queue queue1(){
        //设置参数
        Map<String, Object> map = new HashMap<>();
        //设置一个死信交换机
        map.put("x-dead-letter-exchange",exchange_dead);
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //方式二，统一延迟时间
        map.put("x-message-ttl", 10 * 1000);

        return new Queue(queue_dead_1, true, false, false, map);
    }
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }
    @Bean
    public Queue queue2(){
        return new Queue(queue_dead_2, true, false, false, null);
    }
    @Bean
    public Binding deadBinding(){
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }

}
