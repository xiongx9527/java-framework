package com.xiongx9527.fw.plus.boot.autoconfigure.config.client;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CachingConfigServicePropertySourceLocator extends ConfigServicePropertySourceLocator {

    private final com.xiongx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientProperties clientProperties;

    private final Environment environment;

    public CachingConfigServicePropertySourceLocator(
            ConfigClientProperties standardProperties,
            com.xiongx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientProperties clientProperties,
            Environment environment) {
        super(standardProperties);
        this.clientProperties = clientProperties;
        this.environment = environment;
    }

    @Override
    public PropertySource<?> locate(Environment env) {
        try {
            PropertySource<?> source = super.locate(env);
            if (source != null && clientProperties.isCacheEnabled()) {
                Map<String, Object> config = flattenSource(source);
                if (!config.isEmpty()) {
                    CompletableFuture.runAsync(() -> writeCache(config));
                }
            }
            return source;
        } catch (Exception e) {
            log.warn("从配置中心获取配置失败: {}, 尝试从本地缓存加载", e.getMessage());
            if (clientProperties.isCacheEnabled()) {
                Map<String, Object> cached = readCache();
                if (cached != null && !cached.isEmpty()) {
                    log.info("从本地缓存恢复配置成功, 配置项数量={}", cached.size());
                    return new MapPropertySource("configService", cached);
                }
            }
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.config.client.loadFailedNoCache"), e);
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
        return result;
    }

    private Path cacheFilePath() {
        String dir = clientProperties.getCacheDir();
        if (!StringUtils.hasText(dir)) {
            dir = System.getProperty("user.home") + "/.fw-plus/config";
        }
        String app = environment.getProperty("spring.application.name", "application");
        String profiles = environment.getProperty("spring.profiles.active", "default");
        return Paths.get(dir, app + "-" + profiles + ".properties");
    }

    private void writeCache(Map<String, Object> config) {
        try {
            Path path = cacheFilePath();
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.createDirectories(path.getParent());
            Properties props = new Properties();
            config.forEach((k, v) -> props.setProperty(k, v != null ? v.toString() : ""));
            try (OutputStream os = Files.newOutputStream(tmp)) {
                props.store(os, "fw-plus config cache");
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.warn("写入本地缓存失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> readCache() {
        try {
            Path path = cacheFilePath();
            if (!Files.exists(path)) {
                return null;
            }
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(path)) {
                props.load(is);
            }
            Map<String, Object> result = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.warn("读取本地缓存失败: {}", e.getMessage());
            return null;
        }
    }
}
