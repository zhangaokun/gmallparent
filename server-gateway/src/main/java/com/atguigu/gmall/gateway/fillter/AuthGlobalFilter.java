package com.atguigu.gmall.gateway.fillter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {
    @Autowired
    private RedisTemplate redisTemplate;
    //匹配路径工具类
    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Value("${authUrls.url}")
    private String authUrls;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取用户请求
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        //内部接口,如果是内部接口，则网关拦截不允许外部访问！
        if (antPathMatcher.match("/**/inner/**", path)) {
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }
        //获取用户id
        String userId = getUserId(request);
        //获取临时用户id
        String userTempId = getUserTempId(request);
        //用户登陆认证
        //api 接口 ，异步请求，效验用户必须登陆
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            if (StringUtils.isEmpty(userId)) {
                ServerHttpResponse response = exchange.getResponse();
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }
        //验证url
        //未登录的情况下不允许访问配置文件中配置的url
        if (null != authUrls) {
            for (String authUrl : authUrls.split(",")) {
                //当前的url包含登陆的控制器域名，但是用户id为空
                if (path.indexOf(authUrl) != -1 && StringUtils.isEmpty(userId)) {
                    //303状态码表示由于请求对应的资源存在着另一个URI，应使用重定向获取请求的资源
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());
                    //从定向登陆
                    return response.setComplete();
                }
            }
        }

        // 将userId 传递给后端
        if (!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)) {

            if (!StringUtils.isEmpty(userId)){
                request.mutate().header("userId", userId).build();
            }
            if (!StringUtils.isEmpty(userTempId)){
                request.mutate().header("userTempId", userTempId).build();
            }
            // 将现在的request 变成 exchange对象
            return chain.filter(exchange.mutate().request(request).build());
        }
        return chain.filter(exchange);
    }

    //提示信息
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //提示信息告诉用户，提示信息被封装到resultCodeEnum 对象
        //将提示的信息封装到result中
        Result<Object> result = Result.build(null, resultCodeEnum);
        //将result 转化为字符串
        String resultStr = JSONObject.toJSONString(result);
        //将resultStr 转换成一个字节数组
        byte[] bytes = resultStr.getBytes(StandardCharsets.UTF_8);
        //声明一个dataBuilder
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //设置信息输入格式
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        // 将信息输入到页面
        return response.writeWith(Mono.just(wrap));
    }

    //获取userId
    private String getUserId(ServerHttpRequest request) {
        //用户id存储在缓存
        //key= user：login：token value = userId
        //必须先获取token token可能存在header可能存在cookie
        String token = "";
        List<String> tokenList = request.getHeaders().get("token");
        if (null != tokenList && tokenList.size() > 0) {
            //这个集合中只有一个key 这个key token，值对应的也是一个
            token = tokenList.get(0);
        } else {
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            HttpCookie cookie = cookies.getFirst("token");
            if (null != cookie) {
                token = URLDecoder.decode(cookie.getValue());
            }
        }
        if (!StringUtils.isEmpty(token)) {
            //才能从缓存中获取数据
            String userKey = "user:login:" + token;
            String userId = (String) redisTemplate.opsForValue().get(userKey);
            return userId;
        }
        return "";
    }

    /**
     * 获取当前用户临时用户id
     *
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        List<String> userTempIdList = request.getHeaders().get("userTempId");
        if (null != userTempIdList) {
            userTempId = userTempIdList.get(0);
        } else {
            //从cookie中获取
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if (null != cookie) {
                userTempId = cookie.getValue();

            }
        }
        return userTempId;
    }

}
