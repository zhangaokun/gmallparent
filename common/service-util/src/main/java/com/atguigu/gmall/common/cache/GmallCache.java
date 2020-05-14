package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})//表示注解使用在方法上
@Retention(RetentionPolicy.RUNTIME)//这个的级别最高
public @interface GmallCache {
    /**
     * 缓存key的前缀
     * @return
     */
    String prefix()default "cache";
}
