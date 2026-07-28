package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service.ConfigMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter.server.MagicAPIEnvironmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * MagicAPI配置服务器自动配置类
 *
 * 该类用于自动配置魔法API的配置服务器功能，基于Spring Cloud Config Server
 * 只有在满足特定条件时才会被加载和启用
 *
 * 条件说明：
 * 1. 需要配置属性 fw.plus.config.server.enabled 为 true（默认为true）
 * 2. 类路径下必须存在 org.springframework.cloud.config.server.config.ConfigServerConfiguration
 * 3. 类路径下必须存在 org.springframework.cloud.config.server.environment.AbstractScmEnvironmentRepository
 */
@ConditionalOnProperty(prefix = "fw.plus.config.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.springframework.cloud.config.server.config.ConfigServerConfiguration",
        "org.springframework.cloud.config.server.environment.AbstractScmEnvironmentRepository"})
public class MagicAPIConfServerAutoConfiguration {

    /**
     * 创建并配置MagicAPI环境仓库实例
     *
     * <p>该方法会创建一个MagicAPIEnvironmentRepository实例，用于管理MagicAPI的环境配置。</p>
     *
     * @param storage 配置资源存储对象
     * @return 初始化完成的MagicAPI环境仓库实例，用于与配置服务器交互
     */
    @Bean
    public MagicAPIEnvironmentRepository magicAPIEnvironmentRepository(ConfigMagicResourceStorage storage) {
        return new MagicAPIEnvironmentRepository(storage);
    }

}
