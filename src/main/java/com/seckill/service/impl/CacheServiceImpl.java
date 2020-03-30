package com.seckill.service.impl;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seckill.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @Title CacheServiceImpl
 * @Description 使用guava cache或Caffeine来存储
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String, Object> commonCache = null;


    @PostConstruct
    public void init(){
        /**commonCache设置的方法是一串方法的实现，最后.build()来创建commonCache*/
        commonCache = Caffeine.newBuilder()
                /**设置缓存容器的初始容量为10*/
                .initialCapacity(10)
                /**设置缓存中最大存储100个key，超过100个之后会按照lru的策略移除缓存项*/
                .maximumSize(100)
                /**设置写缓存之后多少秒过期*/
                .expireAfterWrite(60, TimeUnit.SECONDS).build();

    }
    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}











