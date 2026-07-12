package com.xiongx9527.fw.plus.boot.autoconfigure.config.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Apollo 配置变更适配器，将 Apollo 的 ConfigChangeListener 桥接到通用的 {@link ConfigChangeEvent}。
 */
@Slf4j
public class ApolloConfigChangeAdapter implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private final Environment environment;

    private final ApplicationEventPublisher eventPublisher;

    public ApolloConfigChangeAdapter(Environment environment, ApplicationEventPublisher eventPublisher) {
        this.environment = environment;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String[] namespaces = resolveNamespaces();
        for (String namespace : namespaces) {
            Config config = ConfigService.getConfig(namespace.trim());
            config.addChangeListener(changeEvent -> {
                Set<String> changedKeys = changeEvent.changedKeys();
                Map<String, Object> properties = new HashMap<>();
                for (String key : changedKeys) {
                    ConfigChange change = changeEvent.getChange(key);
                    properties.put(key, change.getNewValue());
                }
                if (!properties.isEmpty()) {
                    log.debug("Apollo 配置变更: namespace={}, keys={}", namespace, changedKeys);
                    eventPublisher.publishEvent(new ConfigChangeEvent(this, properties));
                }
            });
            log.info("Apollo 配置监听已注册: namespace={}", namespace);
        }
    }

    private String[] resolveNamespaces() {
        String namespaces = environment.getProperty("apollo.bootstrap.namespaces", "application");
        return namespaces.split(",");
    }

    @Override
    public void destroy() {
        // Apollo 的 ConfigChangeListener 无法移除，此方法保留以备扩展
    }
}
