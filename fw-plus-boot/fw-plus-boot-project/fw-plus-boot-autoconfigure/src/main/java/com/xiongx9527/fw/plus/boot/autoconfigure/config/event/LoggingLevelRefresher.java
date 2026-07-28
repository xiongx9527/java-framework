package com.xiongx9527.fw.plus.boot.autoconfigure.config.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * 日志级别动态刷新器，监听 {@link ConfigChangeEvent} 中 {@code logging.level.*} 前缀的配置变更，
 * 将最新日志级别实时应用到当前日志框架，无需重启。
 *
 * <p>通过 Spring Boot 的 {@link LoggingSystem} 抽象层操作底层日志框架，同时兼容 Logback 和 Log4j2。
 * <p>通过 {@link OnConfigChange @OnConfigChange} 注解被 {@link ConfigChangeEventListener} 自动发现并注册，
 * 兼容 Apollo、Nacos、Spring Cloud Config 等所有已适配的配置中心。
 */
@Slf4j
public class LoggingLevelRefresher {

    private static final String PREFIX = "logging.level";

    @OnConfigChange(prefix = PREFIX)
    public void onLoggingLevelChange(Map<String, Object> changed) {
        LoggingSystem loggingSystem = LoggingSystem.get(ClassUtils.getDefaultClassLoader());
        for (Map.Entry<String, Object> entry : changed.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // "logging.level.com.xxx.Xxx" -> "com.xxx.Xxx"
            String loggerName = key.substring(PREFIX.length());
            if (loggerName.startsWith(".")) {
                loggerName = loggerName.substring(1);
            }
            if (loggerName.isEmpty()) {
                log.debug("忽略空的 logger name: key={}", key);
                continue;
            }

            if (value == null || value.toString().trim().isEmpty()) {
                // 值为空时重置为默认级别
                loggingSystem.setLogLevel(loggerName, null);
                log.info("日志级别已重置(继承父级): {}", loggerName);
            } else {
                LogLevel newLevel = LogLevel.valueOf(value.toString().trim().toUpperCase());
                loggingSystem.setLogLevel(loggerName, newLevel);
                log.info("日志级别已动态刷新: {} => {}", loggerName, newLevel);
            }
        }
    }
}
