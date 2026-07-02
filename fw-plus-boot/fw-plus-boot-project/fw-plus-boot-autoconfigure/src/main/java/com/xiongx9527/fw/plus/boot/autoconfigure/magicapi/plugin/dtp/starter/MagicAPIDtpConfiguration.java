package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module.DtpModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.web.MagicDtpController;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.dtp", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.dromara.dynamictp.core.executor.DtpExecutor"})
@EnableConfigurationProperties(MagicAPIDtpConfig.class)
@Configuration
public class MagicAPIDtpConfiguration implements MagicPluginConfiguration , BaseService {

    /** MagicAPI配置属性 */
    private final MagicAPIProperties properties;

    /**
     * 构造函数
     * 
     * @param properties MagicAPI配置属性
     */
    public MagicAPIDtpConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建并配置MagicDynamicDtpDataSource数据源Bean
     *
     * <p>该方法用于创建MagicDynamicDtpDataSource实例，并将其注册为Spring容器中的Bean。
     * 使用@ConditionalOnMissingBean注解确保只有在容器中不存在同类型Bean时才会创建此Bean，
     * 避免重复创建和配置冲突。
     *
     * @return MagicDynamicDtpDataSource 返回新创建的动态数据源实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicDtpDataSource magicDynamicDtpDataSource() {
        return new MagicDynamicDtpDataSource();
    }

    /**
     * 创建并配置DtpExecutor线程池实例
     *
     * @return 配置好的DtpExecutor线程池实例
     *
     * 该方法会在容器中不存在DtpExecutor类型的Bean时创建一个新的线程池实例。
     * 线程池的核心参数包括核心线程数、最大线程数、线程空闲时间以及任务队列容量，
     * 这些参数都通过对应的getter方法获取，确保了配置的灵活性和可定制性。
     */
    @Bean
    @ConditionalOnMissingBean(DtpExecutor.class)
    public DtpExecutor dtpExecutor() {
        // 使用配置参数创建DtpExecutor实例，任务队列使用LinkedBlockingQueue实现
        return new DtpExecutor(getCorePoolSize(),
                getMaximumPoolSize(),
                getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(getQueueCapacity()));
    }

    /**
     * 创建DtpModule实例的Bean
     *
     * @param magicDynamicDtpDataSource 动态数据源实例，用于DTP模块的数据源管理
     * @param dtpExecutor DTP执行器，可选参数，用于处理DTP相关的执行任务
     * @return 返回配置好的DtpModule实例
     */
    @Bean
    @DependsOn(value={"magicDynamicDtpDataSource"})
    public DtpModule dtpModule(MagicDynamicDtpDataSource magicDynamicDtpDataSource,DtpExecutor dtpExecutor) {
        return new DtpModule(magicDynamicDtpDataSource, dtpExecutor);
    }

    /**
     * 创建并配置DtpDataSourceInfoMagicResourceStorage实例
     *
     * @return DtpDataSourceInfoMagicResourceStorage 数据源信息魔法资源存储器实例
     */
    @Bean
    public DtpDataSourceInfoMagicResourceStorage dtpDataSourceInfoMagicResourceStorage() {
        return new DtpDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建DtpDataSourceMagicDynamicRegistry实例的工厂方法
     * <p>
     * 该方法用于创建数据源动态注册器实例，当容器中不存在相同类型的bean时才会创建
     *
     * @param dtpDataSourceInfoMagicResourceStorage 数据源信息存储资源管理器
     * @param magicDynamicDtpDataSource 动态数据源处理器
     * @return 返回配置好的DtpDataSourceMagicDynamicRegistry实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DtpDataSourceMagicDynamicRegistry dtpDataSourceMagicDynamicRegistry(DtpDataSourceInfoMagicResourceStorage dtpDataSourceInfoMagicResourceStorage,
                                                                               MagicDynamicDtpDataSource magicDynamicDtpDataSource) {
        return new DtpDataSourceMagicDynamicRegistry(dtpDataSourceInfoMagicResourceStorage,magicDynamicDtpDataSource);
    }

    /**
     * 获取插件信息
     * 
     * @return 插件信息实例
     */
    @Override
    public Plugin plugin() {
        return new Plugin("dtp", "MagicDtp", "magic-dtp.1.0.0.iife.js");
    }


    /**
     * 获取魔法控制器注册器
     *
     * @return 控制器注册器，用于注册MagicDtpController控制器实例
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        // 创建并返回一个控制器注册器，该注册器负责将MagicDtpController注册到映射配置中
        return (mapping, configuration) -> mapping.registerController(new MagicDtpController(configuration));
    }

}
