package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LockModuleConfiguration.LockTemplateConfiguration.class)
public class LockModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(type = {"com.baomidou.lock.LockTemplate"})
    public LocalLockModule localLockModule() {
        return new LocalLockModule();
    }

    @ConditionalOnBean(type = {"com.baomidou.lock.LockTemplate"})
    @Configuration
    static class LockTemplateConfiguration {

        @Bean
        public LockModule lockModule(com.baomidou.lock.LockTemplate lockTemplate) {
            return new LockModule(lockTemplate);
        }
    }

}
