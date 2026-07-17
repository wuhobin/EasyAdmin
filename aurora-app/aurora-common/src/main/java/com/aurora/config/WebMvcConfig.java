package com.aurora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * @author blue
 * @date 2022/3/10
 * @apiNote
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    /**
     * 注册跨域信息
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // 允许所有跨域地址
                .allowedHeaders("*")
                .allowedMethods("*")
                .maxAge(3600);
    }

}
