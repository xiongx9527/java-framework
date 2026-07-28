package com.xiongx9527.fw.plus.boot.autoconfigure.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Nacos 配置变更适配器，将 Nacos 的 Listener 桥接到通用的 {@link ConfigChangeEvent}。
 */
@Slf4j
public class NacosConfigChangeAdapter implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private final Environment environment;

    private final ConfigService nacosConfigService;

    private final ApplicationEventPublisher eventPublisher;

    public NacosConfigChangeAdapter(Environment environment,
                                     ConfigService nacosConfigService,
                                     ApplicationEventPublisher eventPublisher) {
        this.environment = environment;
        this.nacosConfigService = nacosConfigService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String dataId = resolveDataId();
        String group = resolveGroup();
        try {
            nacosConfigService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    Map<String, Object> properties = parseConfig(configInfo);
                    if (!properties.isEmpty()) {
                        log.debug("Nacos 配置变更: dataId={}, group={}", dataId, group);
                        eventPublisher.publishEvent(new ConfigChangeEvent(this, properties));
                    }
                }

                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
            log.info("Nacos 配置监听已注册: dataId={}, group={}", dataId, group);
        } catch (Exception e) {
            log.error("注册 Nacos 配置监听失败: dataId={}, group={}, error={}", dataId, group, e.getMessage());
        }
    }

    private String resolveDataId() {
        String appName = environment.getProperty("spring.application.name", "application");
        String fileExt = environment.getProperty("spring.cloud.nacos.config.file-extension", "properties");
        return appName + "." + fileExt;
    }

    private String resolveGroup() {
        return environment.getProperty("spring.cloud.nacos.config.group", "DEFAULT_GROUP");
    }

    private Map<String, Object> parseConfig(String configInfo) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(configInfo)) {
            return result;
        }
        try {
            Properties props = new Properties();
            props.load(new StringReader(configInfo));
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
        } catch (Exception e) {
            log.warn("解析 Nacos 配置内容失败, 将作为原始内容传递: {}", e.getMessage());
            result.put("nacos.config.content", configInfo);
        }
        return result;
    }

    @Override
    public void destroy() {
        // Nacos ConfigService 由框架管理生命周期，此处不关闭
    }
}
