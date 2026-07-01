package com.xx9527.fw.plus.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.*;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Order(Integer.MIN_VALUE)
public class SpringUtils implements ApplicationContextAware, SmartLifecycle {

    /**
     * 应用程序上下文实例，用于管理Spring容器中的bean对象
     */
    private static ApplicationContext context;

    /**
     * 应用程序运行状态标志，使用原子布尔类型确保多线程环境下的线程安全性
     * true表示应用程序正在运行，false表示应用程序已停止
     */
    private static final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 应用程序启动事件发布状态标志，使用原子布尔类型确保多线程环境下的线程安全性
     * true表示启动事件已经发布，false表示启动事件尚未发布
     */
    private static final AtomicBoolean startupEventPublished = new AtomicBoolean(false);

    /**
     * 获取当前应用的名称
     * 
     * @return 应用名称，如果未配置则返回默认值
     */
    public static String getApplicationName() {
        return getAppName();
    }

    /**
     * 获取服务器端口号
     * 
     * @return 服务器端口号，从配置中获取 server.port 的值
     */
    public static String getServerPort() {
        return getAppConfig("server.port");
    }

    // ========== 应用生命周期事件 ==========

    /**
     * 应用启动完成事件 - 所有Bean初始化完成后触发
     */
    public static class ApplicationStartupCompleteEvent extends ApplicationEvent {
        private final long startupTime;
        private final Map<String, Object> startupInfo;

        public ApplicationStartupCompleteEvent(Object source) {
            super(source);
            this.startupTime = System.currentTimeMillis();
            this.startupInfo = new HashMap<>();
            this.startupInfo.put("startupTime", startupTime);
            this.startupInfo.put("appName", getAppName());
            this.startupInfo.put("springBootVersion", version());
        }

        public long getStartupTime() { return startupTime; }
        public Map<String, Object> getStartupInfo() { return startupInfo; }
    }

    /**
     * 应用准备关闭事件 - 在应用开始关闭前触发
     */
    public static class ApplicationPreShutdownEvent extends ApplicationEvent {
        private final String reason;

        public ApplicationPreShutdownEvent(Object source, String reason) {
            super(source);
            this.reason = reason;
        }

        public String getReason() { return reason; }
    }

    /**
     * 应用关闭完成事件 - 应用完全关闭后触发
     */
    public static class ApplicationShutdownCompleteEvent extends ApplicationEvent {
        private final long shutdownTime;
        private final long uptime;

        public ApplicationShutdownCompleteEvent(Object source, long startupTime) {
            super(source);
            this.shutdownTime = System.currentTimeMillis();
            this.uptime = shutdownTime - startupTime;
        }

        public long getShutdownTime() { return shutdownTime; }
        public long getUptime() { return uptime; }
    }

    // ========== 业务相关事件 ==========

    /**
     * 数据库连接就绪事件
     */
    public static class DatabaseReadyEvent extends ApplicationEvent {
        private final Map<String, String> dataSourceInfo;

        public DatabaseReadyEvent(Object source, Map<String, String> dataSourceInfo) {
            super(source);
            this.dataSourceInfo = dataSourceInfo;
        }

        public Map<String, String> getDataSourceInfo() { return dataSourceInfo; }
    }

    /**
     * 缓存就绪事件
     */
    public static class CacheReadyEvent extends ApplicationEvent {
        private final String cacheType; // redis, caffeine, etc.
        private final boolean clusterMode;

        public CacheReadyEvent(Object source, String cacheType, boolean clusterMode) {
            super(source);
            this.cacheType = cacheType;
            this.clusterMode = clusterMode;
        }

        public String getCacheType() { return cacheType; }
        public boolean isClusterMode() { return clusterMode; }
    }

    /**
     * 消息队列就绪事件
     */
    public static class MessageQueueReadyEvent extends ApplicationEvent {
        private final String queueType; // kafka, rabbitmq, rocketmq
        private final String servers;

        public MessageQueueReadyEvent(Object source, String queueType, String servers) {
            super(source);
            this.queueType = queueType;
            this.servers = servers;
        }

        public String getQueueType() { return queueType; }
        public String getServers() { return servers; }
    }

    /**
     * 外部服务就绪事件
     */
    public static class ExternalServiceReadyEvent extends ApplicationEvent {
        private final String serviceName;
        private final String endpoint;
        private final boolean healthCheckPassed;

        public ExternalServiceReadyEvent(Object source, String serviceName, String endpoint, boolean healthCheckPassed) {
            super(source);
            this.serviceName = serviceName;
            this.endpoint = endpoint;
            this.healthCheckPassed = healthCheckPassed;
        }

        public String getServiceName() { return serviceName; }
        public String getEndpoint() { return endpoint; }
        public boolean isHealthCheckPassed() { return healthCheckPassed; }
    }

    /**
     * 配置加载完成事件
     */
    public static class ConfigurationLoadedEvent extends ApplicationEvent {
        private final Map<String, Object> configSummary;

        public ConfigurationLoadedEvent(Object source, Map<String, Object> configSummary) {
            super(source);
            this.configSummary = configSummary;
        }

        public Map<String, Object> getConfigSummary() { return configSummary; }
    }

    /**
     * 定时任务就绪事件
     */
    public static class SchedulerReadyEvent extends ApplicationEvent {
        private final int scheduledTaskCount;

        public SchedulerReadyEvent(Object source, int scheduledTaskCount) {
            super(source);
            this.scheduledTaskCount = scheduledTaskCount;
        }

        public int getScheduledTaskCount() { return scheduledTaskCount; }
    }

    /**
     * 业务模块就绪事件
     */
    public static class BusinessModuleReadyEvent extends ApplicationEvent {
        private final String moduleName;
        private final String version;
        private final boolean initialized;

        public BusinessModuleReadyEvent(Object source, String moduleName, String version, boolean initialized) {
            super(source);
            this.moduleName = moduleName;
            this.version = version;
            this.initialized = initialized;
        }

        public String getModuleName() { return moduleName; }
        public String getVersion() { return version; }
        public boolean isInitialized() { return initialized; }
    }

    // ========== 系统监控事件 ==========

    /**
     * 系统资源就绪事件
     */
    public static class SystemResourceReadyEvent extends ApplicationEvent {
        private final Runtime runtime;
        private final long maxMemory;
        private final long totalMemory;
        private final long freeMemory;

        public SystemResourceReadyEvent(Object source) {
            super(source);
            this.runtime = Runtime.getRuntime();
            this.maxMemory = runtime.maxMemory();
            this.totalMemory = runtime.totalMemory();
            this.freeMemory = runtime.freeMemory();
        }

        public Runtime getRuntime() { return runtime; }
        public long getMaxMemory() { return maxMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
    }

    /**
     * 健康检查通过事件
     */
    public static class HealthCheckPassedEvent extends ApplicationEvent {
        private final Map<String, Object> healthDetails;

        public HealthCheckPassedEvent(Object source, Map<String, Object> healthDetails) {
            super(source);
            this.healthDetails = healthDetails;
        }

        public Map<String, Object> getHealthDetails() { return healthDetails; }
    }

    // ========== 事件发布方法 ==========

    /**
     * 发布数据库就绪事件
     */
    public static void publishDatabaseReadyEvent(Map<String, String> dataSourceInfo) {
        publishEvent(new DatabaseReadyEvent(SpringUtils.class, dataSourceInfo));
    }

    /**
     * 发布缓存就绪事件
     */
    public static void publishCacheReadyEvent(String cacheType, boolean clusterMode) {
        publishEvent(new CacheReadyEvent(SpringUtils.class, cacheType, clusterMode));
    }

    /**
     * 发布消息队列就绪事件
     */
    public static void publishMessageQueueReadyEvent(String queueType, String servers) {
        publishEvent(new MessageQueueReadyEvent(SpringUtils.class, queueType, servers));
    }

    /**
     * 发布业务模块就绪事件
     */
    public static void publishBusinessModuleReadyEvent(String moduleName, String version, boolean initialized) {
        publishEvent(new BusinessModuleReadyEvent(SpringUtils.class, moduleName, version, initialized));
    }

    // ========== SmartLifecycle 实现 ==========

        /**
     * 应用启动时间戳，记录应用启动的毫秒数
     */
    private volatile long startupTime;

    /**
     * 设置Spring应用上下文
     *
     * @param context Spring应用上下文对象
     * @throws BeansException 当设置上下文过程中发生错误时抛出
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SpringUtils.context = context;
        log.debug("ApplicationContext set in SpringUtils");
    }

    /**
     * 启动生命周期回调方法
     * 当Spring容器启动完成后执行，用于发布应用启动完成事件
     */
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.startupTime = System.currentTimeMillis();
            log.info("🎉 SpringUtils SmartLifecycle started - Application is fully ready!");

            // 发布系统事件
            publishSystemEvents();

            // 确保应用启动完成事件只发布一次
            if (startupEventPublished.compareAndSet(false, true)) {
                try {
                    publishEvent(new ApplicationStartupCompleteEvent(this));
                    log.info("📢 ApplicationStartupCompleteEvent published");
                } catch (Exception e) {
                    log.error("Failed to publish ApplicationStartupCompleteEvent", e);
                }
            }
        }
    }

    /**
     * 停止生命周期回调方法
     * 当Spring容器关闭时执行，用于发布应用关闭相关事件
     */
    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("🛑 SpringUtils SmartLifecycle stopped - Application is shutting down");

            try {
                // 发布预关闭和关闭完成事件
                publishEvent(new ApplicationPreShutdownEvent(this, "Normal shutdown"));
                publishEvent(new ApplicationShutdownCompleteEvent(this, startupTime));
            } catch (Exception e) {
                log.error("Error occurred while publishing shutdown events", e);
            }
        }
    }

    /**
     * 带回调的停止生命周期回调方法
     * 在停止应用后执行指定的回调函数
     *
     * @param callback 停止完成后需要执行的回调函数
     */
    @Override
    public void stop(Runnable callback) {
        log.info("🛑 SpringUtils SmartLifecycle stopping with callback");
        stop();
        callback.run();
    }

    /**
     * 检查应用是否正在运行
     *
     * @return true表示应用正在运行，false表示已停止
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取生命周期阶段值
     *
     * @return 返回Integer.MAX_VALUE，确保在最后阶段执行
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * 检查是否自动启动
     *
     * @return true表示支持自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }


    /**
     * 发布系统就绪事件
     */
    private void publishSystemEvents() {
        // 发布系统资源事件
        publishEvent(new SystemResourceReadyEvent(this));

        // 发布配置加载事件
        Map<String, Object> configSummary = new HashMap<>();
        configSummary.put("appName", getAppName());
        configSummary.put("profiles", getActiveProfiles());
        configSummary.put("configSources", "application.yml, application.properties");
        publishEvent(new ConfigurationLoadedEvent(this, configSummary));

        // 发布健康检查事件
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("status", "UP");
        healthDetails.put("timestamp", System.currentTimeMillis());
        publishEvent(new HealthCheckPassedEvent(this, healthDetails));
    }

    /**
     * 获取激活的配置文件
     */
    public static String[] getActiveProfiles() {
        return context != null ? context.getEnvironment().getActiveProfiles() : new String[0];
    }

    // ========== 原有的静态方法保持不变 ==========

    public static String version() {
        try {
            return org.springframework.boot.SpringBootVersion.getVersion();
        } catch (Throwable e) {
            log.error("获取Spring Boot版本信息失败", e);
        }
        return "0";
    }

    /**
     * 安全的版本号解析和比较
     */
    private static boolean compareMajorVersion(String version, int targetVersion) {
        if (StringUtils.isEmpty(version)) {
            return false;
        }

        try {
            // 只取主版本号部分进行比较
            String majorVersionStr = version.split("\\.")[0];
            int majorVersion = Integer.parseInt(majorVersionStr);
            return majorVersion >= targetVersion;
        } catch (Exception e) {
            log.warn("版本号比较失败: {}, 期望版本: {}", version, targetVersion, e);
            return false;
        }
    }

    /**
     * 判断当前Spring Boot版本是否支持指定的目标版本
     * 通过比较当前版本与目标版本的主要版本号来确定兼容性
     *
     * @param targetVersion 目标Spring Boot版本号（主要版本号）
     * @return 如果当前版本支持目标版本则返回true，否则返回false
     */
    public static boolean isSupportSpringBoot(int targetVersion) {
        String version = version();
        return compareMajorVersion(version, targetVersion);
    }


    /**
     * 判断当前Spring Boot版本是否支持Spring Boot 3
     *
     * @return true表示支持Spring Boot 3，false表示不支持
     */
    public static boolean isSupportSpringBoot3() {
        return isSupportSpringBoot( 3);
    }

    /**
     * 获取应用程序名称
     *
     * @return 应用程序名称，如果上下文为空则返回null
     */
    public static String getAppName() {
        return context != null ? context.getEnvironment().getProperty("spring.application.name") : null;
    }

    /**
     * 获取 Spring 应用配置属性值
     *
     * <p>该方法用于从 Spring 应用上下文中获取指定配置键的属性值。支持根据当前激活的配置文件（profiles）
     * 动态拼接配置键名，实现不同环境下的配置隔离。</p>
     *
     * @param configKey 配置项的键名，用于定位需要获取的配置属性
     *                  不能为空或空白字符串，否则返回 null
     * @param includeProfilesPrefix 是否在配置键名中包含当前激活的配置文件前缀
     *                              - true: 自动获取当前激活的 profiles 并追加到 configKey 后面，格式为 "configKey.profileName"
     *                              - false: 直接使用原始的 configKey 进行查询
     * @return String 配置项对应的值
     *         如果找到则返回配置值字符串，如果配置不存在、上下文未初始化或 configKey 为空则返回 null
     */
    public static String getAppConfig(String configKey, boolean includeProfilesPrefix) {
        if (StringUtils.isBlank(configKey)) {
            return null;
        }
        if (includeProfilesPrefix){
            // 获取当前激活的配置文件并拼接到配置键名中
            String[] profiles = getActiveProfiles();
            if (profiles != null && profiles.length > 0) {
                configKey += "." + profiles[0];
            }
        }
        return context != null ? context.getEnvironment().getProperty(configKey) : null;
    }

    /**
     * 获取应用程序配置属性值（智能匹配模式）
     * 
     * <p>该方法用于从 Spring 应用上下文中获取指定配置键的属性值。采用智能匹配策略：
     * 首先尝试使用当前激活的配置文件前缀进行查询，如果未找到则回退到不使用前缀的通用配置。
     * 这种机制支持环境隔离的配置管理，同时保持向后兼容性。</p>
     *
     * @param configKey 配置项的键名，用于定位需要获取的配置属性
     *                  不能为空或空白字符串，否则返回 null
     * @return String 配置项对应的值
     *         优先返回带 profiles 前缀的配置值，如果不存在则返回不带前缀的配置值
     *         如果两种情况都未找到或上下文未初始化则返回 null
     */
    public static String getAppConfig(String configKey) {
        String value = getAppConfig(configKey, true);
        if (StringUtils.isBlank(value)) {
            value = getAppConfig(configKey, false);
        }
        return value;
    }

    /**
     * 获取应用程序配置属性值（支持默认值）
     *
     * <p>该方法用于从 Spring 应用上下文中获取指定配置键的属性值，当配置不存在时返回指定的默认值。
     * 支持根据是否包含配置文件前缀来灵活查询配置。</p>
     *
     * @param configKey 配置项的键名，用于定位需要获取的配置属性
     *                  不能为空或空白字符串，否则返回 defaultValue
     * @param includeProfilesPrefix 是否在配置键名中包含当前激活的配置文件前缀
     *                              - true: 自动获取当前激活的 profiles 并追加到 configKey 后面
     *                              - false: 直接使用原始的 configKey 进行查询
     * @param defaultValue 默认值，当配置项不存在或值为空时返回此值
     *                     如果 defaultValue 为 null 或空，则配置不存在时也返回 null
     * @return String 配置项对应的值
     *         如果配置存在且非空则返回实际配置值，否则返回 defaultValue
     */
    public static String getAppConfig(String configKey, boolean includeProfilesPrefix, String defaultValue) {
        String value = getAppConfig(configKey,includeProfilesPrefix);
        if (StringUtils.isBlank(value) && includeProfilesPrefix) {
            value = getAppConfig(configKey,false);
        }
        // 如果配置值存在或者默认值为 null，则返回配置值；否则返回默认值
        return (StringUtils.isNotBlank(value) || StringUtils.isBlank(defaultValue)) ? value : defaultValue;
    }


    /**
     * 获取应用程序配置属性值（智能匹配 + 默认值）
     *
     * <p>该方法用于从 Spring 应用上下文中获取指定配置键的属性值，支持智能匹配和默认值回退机制。
     * 优先使用带 profiles 前缀的配置，如果不存在则尝试不带前缀的配置，最后返回默认值。</p>
     *
     * @param configKey 配置项的键名，用于定位需要获取的配置属性
     *                  不能为空或空白字符串，否则返回 defaultValue
     * @param defaultValue 默认值，当所有配置查询都失败时返回此值
     *                     如果 defaultValue 为 null 或空，则配置不存在时也返回 null
     * @return String 配置项对应的值
     *         按优先级返回：1) 带 profiles 前缀的配置值 2) 不带前缀的配置值 3) defaultValue
     */
    public static String getAppConfig(String configKey, String defaultValue) {
        return getAppConfig(configKey, true, defaultValue);
    }

    /**
     * 根据类型获取Spring容器中的Bean实例
     *
     * @param <T> Bean的类型
     * @param clazz Bean的Class对象
     * @return Bean实例，如果clazz为null则返回null
     */
    public static <T> T getBean(Class<T> clazz) {
        return (T)(clazz == null ? null : context.getBean(clazz));
    }

    /**
     * 根据Bean名称和类型获取Spring容器中的Bean实例
     *
     * @param <T> Bean的类型
     * @param beanName Bean名称
     * @param clazz Bean的Class对象
     * @return Bean实例，如果beanName为空或clazz为null则返回null
     */
    public static <T> T getBean(String beanName, Class<T> clazz) {
        if (null != beanName && !"".equals(beanName.trim())) {
            return (T)(clazz == null ? null : context.getBean(beanName, clazz));
        } else {
            return null;
        }
    }

    /**
     * 根据类型获取Spring容器中所有该类型的Bean实例
     *
     * @param <T> Bean的类型
     * @param clazz Bean的Class对象
     * @return 包含所有匹配类型Bean的Map，key为Bean名称，value为Bean实例
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        try {
            return clazz == null ? null : context.getBeansOfType(clazz);
        } catch (Throwable e) {
            // 发生异常时返回空的HashMap
            return new HashMap<>();
        }
    }

    /**
     * 获取Spring应用上下文
     *
     * @return ApplicationContext实例，如果上下文为空则返回null
     */
    public static ApplicationContext getContext() {
        return context == null ? null : context;
    }

    /**
     * 发布应用事件
     *
     * @param event 要发布的应用事件
     */
    public static void publishEvent(ApplicationEvent event) {
        if (context != null) {
            context.publishEvent(event);
        }
    }


    /**
     * 检查应用是否完全启动
     */
    public static boolean isApplicationFullyStarted() {
        return running.get();
    }
}
