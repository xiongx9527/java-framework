package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name="com.xiongx9527.fw.plus.dify.DifyClientBuilder")
public class DifyModuleConfiguration {

    /**
     * 创建DifyModule的Bean实例
     * 注册Dify模块到Spring容器，用于在MagicAPI中调用Dify服务
     *
     * @return DifyModule实例，用于处理Dify相关操作
     */
    @Bean
    public DifyModule difyModule() {
        return new DifyModule();
    }

}
