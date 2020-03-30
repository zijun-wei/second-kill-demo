package com.seckill.service;

/**
 * @Title CacheService
 * @Description 封装本地缓存操作类
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
public interface CacheService {
    /**存方法*/
    void setCommonCache(String key, Object value);

    /**取方法*/
    Object getCommonCache(String key);
}
