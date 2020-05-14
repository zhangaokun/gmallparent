package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void testLock() {
        //定义一个锁：Lua脚本可以使用同一把锁，来实现删除
        String skuId = "26";//访问skuId为22的商品
        String locKey ="lock:"+skuId;//锁住的是每个商品的数剧

        //从redis中获取锁.setnx
        String uuid = UUID.randomUUID().toString();
        //使用setnx命令
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock",uuid,2, TimeUnit.SECONDS);

        //如果是true
        if (lock){
            //执行业务逻辑完成
            // 查询redis中的num值
            String value = (String) this.redisTemplate.opsForValue().get("num");
            //没有该值return
            if (StringUtils.isBlank(value)){
                return;
            }
            //有值就转成int
            int num = Integer.parseInt(value);
            //把redis中的值加一
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
//            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
//               //用完之后删除锁
//                this.redisTemplate.delete("lock");
//            }
            //定义lua脚本
            String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            //设置一下返回值类型为Long
            //因为删除判断的时候，返回的0，给其封装为数据类型
            //如果不封装默认返回的string类型。那么返回字符串与0会发生错误
            redisScript.setResultType(Long.class);
            //第一个要是script脚本，第二个需要判断的key
            redisTemplate.execute(redisScript, Arrays.asList(locKey));
        }else{
            //其他线程等待
            //睡眠
            try {
                Thread.sleep(1000);
                //睡醒之后再调用这个方法
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 读锁
     * @return
     */
    @Override
    public String readLock() {
        //初始化读写锁
        RReadWriteLock readwriteLock = redissonClient.getReadWriteLock("readwriteLock");
        //获取读写锁
        RLock rLock = readwriteLock.readLock();
        //加十秒锁
        rLock.lock(10,TimeUnit.SECONDS);
        String msg = this.redisTemplate.opsForValue().get("msg");

        return msg;
    }

    /**
     * 写锁
     * @return
     */
    @Override
    public String writeLock() {
        //获取读写锁对象
        RReadWriteLock readwriteLock = redissonClient.getReadWriteLock("readwriteLock");
        //获取写锁
        RLock rLock = readwriteLock.writeLock();
        //加十秒锁
        rLock.lock(10,TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());
        //rLock.unlock();
        return "数据写入成功";
    }
}
