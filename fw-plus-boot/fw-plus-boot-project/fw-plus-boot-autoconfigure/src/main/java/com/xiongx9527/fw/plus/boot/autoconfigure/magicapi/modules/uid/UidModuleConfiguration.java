package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.uid;

import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
     * @param magicDynamicDataSource Magic-API 动态数据源（可选，未配置时不支持多数据源模式）
     * @param epochStr               UID 纪元时间配置（与 fw.plus.uid.epochStr 一致）
     * @return 返回新创建的UidModule实例
     */
    @Bean
    public UidModule uidModule(@Autowired(required = false) MagicDynamicDataSource magicDynamicDataSource,
                               @Value("${fw.plus.uid.epochStr:}") String epochStr) {
        return new UidModule(magicDynamicDataSource, epochStr);
    }

}
