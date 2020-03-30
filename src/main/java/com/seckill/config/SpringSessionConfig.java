package com.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;


/**
 * @Title SpringSessionConfig
 * @Description 设置cookie，使其能够在多个站点共享
 * @Author Zijun_Wei
 * @Date 2020/3/21
 */
@Configuration
public class SpringSessionConfig {

    public SpringSessionConfig() {
    }

    @Bean
    public CookieSerializer httpSessionIdResolver() {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        /** 取消仅限同一站点设置*/
        cookieSerializer.setSameSite(null);
        return cookieSerializer;
    }
}
