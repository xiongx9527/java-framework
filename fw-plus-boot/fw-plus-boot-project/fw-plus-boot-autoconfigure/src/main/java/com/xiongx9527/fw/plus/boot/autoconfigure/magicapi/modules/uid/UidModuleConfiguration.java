package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.uid;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UidModuleConfiguration类用于配置UidModule相关的Bean
 * 该配置类通过Spring的@Configuration注解标识，用于创建和管理UidModule实例
 */
@Configuration
public class UidModuleConfiguration {

    /**
     * 创建并配置UidModule Bean
     * 该方法通过@Bean注解将UidModule实例注册到Spring容器中
     *
     * @return 返回新创建的UidModule实例
     */
    @Bean
    public UidModule uidModule() {
        return new UidModule();
    }

}
