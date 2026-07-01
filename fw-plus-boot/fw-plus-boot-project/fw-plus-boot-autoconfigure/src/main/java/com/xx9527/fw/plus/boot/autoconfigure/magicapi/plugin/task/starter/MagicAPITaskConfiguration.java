package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.service.ClusterTaskMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.service.StandaloneTaskMagicDynamicRegistry;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.ssssssss.magicapi.task.service.TaskInfoMagicResourceStorage;
import org.ssssssss.magicapi.task.web.MagicTaskController;
@ConditionalOnProperty(prefix = "magic-api.plugin.task", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.ssssssss.magicapi.task.starter.MagicTaskConfig"})
@Configuration
@EnableConfigurationProperties(org.ssssssss.magicapi.task.starter.MagicTaskConfig.class)
public class MagicAPITaskConfiguration implements MagicPluginConfiguration {
    /**
     * 定时任务配置信息
     * 用于存储和管理定时任务的相关配置参数
     */
    private final org.ssssssss.magicapi.task.starter.MagicTaskConfig config;

    /**
     * 构造函数，初始化MagicAPITaskConfiguration实例
     *
     * @param config 定时任务配置信息
     */
    public MagicAPITaskConfiguration(org.ssssssss.magicapi.task.starter.MagicTaskConfig config) {
        this.config = config;
    }

    /**
     * 创建TaskInfoMagicResourceStorage Bean实例
     * 用于存储和管理定时任务信息资源
     *
     * @return TaskInfoMagicResourceStorage实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskInfoMagicResourceStorage taskInfoMagicResourceStorage() {
        return new TaskInfoMagicResourceStorage();
    }

    /**
     * 创建ClusterTaskMagicDynamicRegistry Bean实例
     * 用于在集群环境中管理定时任务，基于Redis实现分布式锁机制
     * 仅在存在StringRedisTemplate时创建此Bean
     *
     * @param taskInfoMagicResourceStorage 任务信息资源存储器
     * @return ClusterTaskMagicDynamicRegistry实例
     */
    @Bean
    @ConditionalOnBean(type = {"com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
    public ClusterTaskMagicDynamicRegistry clusterTaskMagicDynamicRegistry(TaskInfoMagicResourceStorage taskInfoMagicResourceStorage) {
        org.ssssssss.magicapi.task.starter.MagicTaskConfig.Shutdown shutdown = config.getShutdown();
        ThreadPoolTaskScheduler poolTaskScheduler = null;
        if(config.isEnable()){
            poolTaskScheduler = new ThreadPoolTaskScheduler();
            poolTaskScheduler.setPoolSize(config.getPool().getSize());
            poolTaskScheduler.setWaitForTasksToCompleteOnShutdown(shutdown.isAwaitTermination());
            if(shutdown.getAwaitTerminationPeriod() != null){
                poolTaskScheduler.setAwaitTerminationSeconds((int) shutdown.getAwaitTerminationPeriod().getSeconds());
            }
            poolTaskScheduler.setThreadNamePrefix(config.getThreadNamePrefix());
            poolTaskScheduler.initialize();
        }
        return new ClusterTaskMagicDynamicRegistry(taskInfoMagicResourceStorage, poolTaskScheduler, config.isLog());
    }

    /**
     * 创建TaskMagicDynamicRegistry Bean实例
     * 用于在单机环境中管理定时任务
     * 仅在不存在StringRedisTemplate时创建此Bean
     *
     * @param taskInfoMagicResourceStorage 任务信息资源存储器
     * @return TaskMagicDynamicRegistry实例
     */
    @Bean
    @ConditionalOnMissingBean(type = {"com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
    public StandaloneTaskMagicDynamicRegistry taskMagicDynamicRegistry(TaskInfoMagicResourceStorage taskInfoMagicResourceStorage) {
        org.ssssssss.magicapi.task.starter.MagicTaskConfig.Shutdown shutdown = config.getShutdown();
        ThreadPoolTaskScheduler poolTaskScheduler = null;
        if(config.isEnable()){
            poolTaskScheduler = new ThreadPoolTaskScheduler();
            poolTaskScheduler.setPoolSize(config.getPool().getSize());
            poolTaskScheduler.setWaitForTasksToCompleteOnShutdown(shutdown.isAwaitTermination());
            if(shutdown.getAwaitTerminationPeriod() != null){
                poolTaskScheduler.setAwaitTerminationSeconds((int) shutdown.getAwaitTerminationPeriod().getSeconds());
            }
            poolTaskScheduler.setThreadNamePrefix(config.getThreadNamePrefix());
            poolTaskScheduler.initialize();
        }
        return new StandaloneTaskMagicDynamicRegistry(taskInfoMagicResourceStorage, poolTaskScheduler, config.isLog());
    }

    /**
     * 获取插件信息
     *
     * @return Plugin对象，包含插件名称、标识和资源文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("task", "MagicTask", "magic-task.1.0.0.iife.js");
    }

    /**
     * 获取控制器注册器
     *
     * @return MagicControllerRegister对象，用于注册MagicTaskController控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicTaskController(configuration));
    }

}
