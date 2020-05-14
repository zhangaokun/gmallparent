package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 登陆方法
     * @param userInfo
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        UserInfo info  = userService.login(userInfo);
        //用户在数据库中存在
        if (info !=null){
            //声明map 集合记录相关数据
            HashMap<String, Object> hashMap = new HashMap<>();
            //根据sso的分析，用户登陆之后应该放入缓存，这样才能保证每个模块都可以访问到用户
            //声明一个token
            String token = UUID.randomUUID().toString().replace("-","");
            //记录token
            hashMap.put("token",token);
            //用户的称你记录到map中
            hashMap.put("nickName",info.getNickName());
            //定义key-user：login value=userId
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            redisTemplate.opsForValue().set(userKey,info.getId().toString() ,RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
            return Result.ok(hashMap);
        }else {
            return Result.fail().message("用户名或密码错误");
        }
    }

    /**
     * 登出方法
     * @param request
     * @return
     */
    @GetMapping("logout")
    private Result logout(HttpServletRequest request){
        //因为缓存中存储用户数据的时候需要token，所以删除的时候，需要token组成key
        //当登陆成功之后，token放入了coolie，header中
        //从header中获取token
//        String token = request.getHeader("token");
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX+request.getHeader("token"));
        //最好的方式，清空cookie中的数据
        return Result.ok();
    }
}
