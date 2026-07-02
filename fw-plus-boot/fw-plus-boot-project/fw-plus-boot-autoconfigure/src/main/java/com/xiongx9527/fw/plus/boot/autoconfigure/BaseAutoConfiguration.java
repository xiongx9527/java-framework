package com.xiongx9527.fw.plus.boot.autoconfigure;

import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : jugui
 */
//@ConditionalOnWebApplication
@Configuration
public class BaseAutoConfiguration {

    /**
     * 创建并配置SpringUtils bean
     *
     * 该方法定义了一个Bean，当Spring应用上下文初始化时，会创建并管理该Bean的生命周期
     * SpringUtils实例可用于获取应用上下文中其他Bean的信息或直接获取Bean实例
     *
     * @return SpringUtils的实例，用于处理与Spring应用上下文相关的操作
     */
    @Bean
    public SpringUtils springUtils() {
        return new SpringUtils();
    }


}
