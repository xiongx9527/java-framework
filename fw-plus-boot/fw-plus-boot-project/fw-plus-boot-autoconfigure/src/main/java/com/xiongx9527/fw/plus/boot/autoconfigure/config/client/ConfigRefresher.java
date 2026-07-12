package com.xiongx9527.fw.plus.boot.autoconfigure.config.client;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ConfigRefresher implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private final ConfigClientProperties clientProperties;

    private final ConfigServicePropertySourceLocator locator;

    private final ConfigurableEnvironment environment;

    private final ContextRefresher contextRefresher;

    private final ApplicationEventPublisher eventPublisher;

    private final AtomicReference<String> lastDigest = new AtomicReference<>();

    private static final HashFunction HASH = Hashing.farmHashFingerprint64();

    private ScheduledExecutorService scheduler;

    public ConfigRefresher(ConfigClientProperties clientProperties,
                           ConfigServicePropertySourceLocator locator,
                           ConfigurableEnvironment environment,
                           ContextRefresher contextRefresher,
                           ApplicationEventPublisher eventPublisher) {
        this.clientProperties = clientProperties;
        this.locator = locator;
        this.environment = environment;
        this.contextRefresher = contextRefresher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!clientProperties.isEnabled() || !clientProperties.isRefreshEnabled()) {
            return;
        }
        int interval = clientProperties.getRefreshInterval();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fw-plus-config-refresher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::refresh, interval, interval, TimeUnit.SECONDS);
        log.info("配置刷新定时器已启动, 间隔={}秒", interval);
    }

    private void refresh() {
        try {
            PropertySource<?> newSource = locator.locate(environment);
            if (newSource == null) {
                return;
            }
            Map<String, Object> newConfig = flattenSource(newSource);
            String digest = fingerprint(newConfig);
            if (digest.equals(lastDigest.get())) {
                return;
            }
            log.info("检测到配置变更, 开始刷新...");
            MutablePropertySources propertySources = environment.getPropertySources();
            if (propertySources.contains("configService")) {
                propertySources.remove("configService");
            }
            propertySources.addFirst(newSource);
            lastDigest.set(digest);
            contextRefresher.refresh();
            log.info("配置刷新完成, source={}", newSource.getName());
            eventPublisher.publishEvent(new ConfigChangeEvent(this, newConfig));
        } catch (Exception e) {
            log.error("刷新配置失败: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> flattenSource(PropertySource<?> source) {
        Map<String, Object> result = new HashMap<>();
        if (source instanceof CompositePropertySource) {
            for (PropertySource<?> ps : ((CompositePropertySource) source).getPropertySources()) {
                Object src = ps.getSource();
                if (src instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) src;
                    result.putAll(map);
                }
            }
        } else if (source.getSource() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) source.getSource();
            result.putAll(map);
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    private static String fingerprint(Map<String, Object> config) {
        if (config.isEmpty()) {
            return "";
        }
        TreeMap<String, Object> sorted = new TreeMap<>(config);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return HASH.hashString(sb.toString(), StandardCharsets.UTF_8).toString();
    }

    @Override
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
