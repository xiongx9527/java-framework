package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module.DtpModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.web.MagicDtpController;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "magic-api.plugin.dtp", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.dromara.dynamictp.core.executor.DtpExecutor"})
@EnableConfigurationProperties(MagicAPIDtpConfig.class)
@Configuration
public class MagicAPIDtpConfiguration implements MagicPluginConfiguration, BaseService {

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicDtpDataSource magicDynamicDtpDataSource() {
        return new MagicDynamicDtpDataSource();
    }

    @Bean
    @ConditionalOnMissingBean(DtpExecutor.class)
    public DtpExecutor dtpExecutor() {
        return new DtpExecutor(getCorePoolSize(),
                getMaximumPoolSize(),
                getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(getQueueCapacity()));
    }

    @Bean
    @DependsOn(value = {"magicDynamicDtpDataSource"})
    public DtpModule dtpModule(MagicDynamicDtpDataSource magicDynamicDtpDataSource, DtpExecutor dtpExecutor) {
        return new DtpModule(magicDynamicDtpDataSource, dtpExecutor);
    }

    @Bean
    public DtpDataSourceInfoMagicResourceStorage dtpDataSourceInfoMagicResourceStorage() {
        return new DtpDataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public DtpDataSourceMagicDynamicRegistry dtpDataSourceMagicDynamicRegistry(DtpDataSourceInfoMagicResourceStorage dtpDataSourceInfoMagicResourceStorage,
                                                                               MagicDynamicDtpDataSource magicDynamicDtpDataSource) {
        return new DtpDataSourceMagicDynamicRegistry(dtpDataSourceInfoMagicResourceStorage, magicDynamicDtpDataSource);
    }

    @Override
    public Plugin plugin() {
        return new Plugin("dtp", "MagicDtp", "magic-dtp.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicDtpController(configuration, magicDynamicDtpDataSource()));
    }

}
