package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LockModuleConfiguration.LockTemplateConfiguration.class)
public class LockModuleConfiguration {

    /**
     * 创建本地锁模块实例
     * 当Spring容器中不存在com.baomidou.lock.LockTemplate类型的Bean时，创建并注册LocalLockModule Bean
     *
     * @return LocalLockModule 本地锁模块实例
     */
    @Bean
    @ConditionalOnMissingBean(type = {"com.baomidou.lock.LockTemplate"})
    public LocalLockModule localLockModule() {
        return new LocalLockModule();
    }

    @ConditionalOnBean(type = {"com.baomidou.lock.LockTemplate"})
    @Configuration
    static class LockTemplateConfiguration {
        /**
         * 创建锁模块实例
         * 当Spring容器中存在com.baomidou.lock.LockTemplate类型的Bean时，创建并注册LockModule Bean
         *
         * @param lockTemplate 锁模板实例，用于初始化锁模块
         * @return LockModule 锁模块实例
         */
        @Bean
        public LockModule lockModule(com.baomidou.lock.LockTemplate lockTemplate) {
            return new LockModule(lockTemplate);
        }

    }

}
