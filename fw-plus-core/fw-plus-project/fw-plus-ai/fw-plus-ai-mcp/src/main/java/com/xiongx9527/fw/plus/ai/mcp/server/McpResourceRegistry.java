package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源注册表
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
class McpResourceRegistry {
    // key: uri, value: resource info
    private final Map<String, McpResourceInfo> resources = new ConcurrentHashMap<>();
    // key: uri, value: provider
    private final Map<String, McpResourceProvider> resourceProviders = new ConcurrentHashMap<>();
    // key: uriTemplate, value: template info
    private final Map<String, McpResourceTemplate> resourceTemplates = new ConcurrentHashMap<>();
    // key: uriTemplate, value: provider
    private final Map<String, McpResourceProvider> templateProviders = new ConcurrentHashMap<>();

    void register(McpResourceInfo resource, McpResourceProvider provider) {
        if (resource == null || provider == null) {
            throw new IllegalArgumentException("Resource and provider must not be null");
        }
        resources.put(resource.uri(), resource);
        resourceProviders.put(resource.uri(), provider);
    }

    void registerTemplate(McpResourceTemplate template, McpResourceProvider provider) {
        if (template == null || provider == null) {
            throw new IllegalArgumentException("Template and provider must not be null");
        }
        resourceTemplates.put(template.uriTemplate(), template);
        templateProviders.put(template.uriTemplate(), provider);
    }

    McpResourceProvider getResourceProvider(String uri) {
        return resourceProviders.get(uri);
    }

    McpResourceProvider getTemplateProvider(String uriTemplate) {
        return templateProviders.get(uriTemplate);
    }

    Collection<McpResourceInfo> listResources(String sessionId) {
        return Collections.unmodifiableCollection(resources.values());
    }

    Collection<McpResourceTemplate> listResourceTemplates() {
        return Collections.unmodifiableCollection(resourceTemplates.values());
    }

    /**
     * 查找与指定 URI 匹配的资源模板 key（用于 resources/read 模板展开后的查找）。
     * 匹配规则：模板 URI 模式以 {param} 标记占位符，按 URI 前缀/字面量匹配。
     */
    String findTemplateMatch(String uri) {
        if (uri == null) {
            return null;
        }
        for (String templateKey : resourceTemplates.keySet()) {
            if (matchesTemplate(templateKey, uri)) {
                return templateKey;
            }
        }
        return null;
    }

    private boolean matchesTemplate(String template, String uri) {
        if (template == null || uri == null) {
            return false;
        }
        // 简单实现：将模板中的 {param} 替换为 .* 进行正则匹配
        String regex = template.replaceAll("\\{[^/]+\\}", "[^/]+");
       	if (regex.equals(template)) {
       		// 模板没有占位符，直接字面匹配
       		return template.equals(uri);
       	}
        try {
            return uri.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }

    Collection<String> listResourceUris() {
        return Collections.unmodifiableCollection(resources.keySet());
    }

    boolean isEmpty() {
        return resources.isEmpty() && resourceTemplates.isEmpty();
    }

    void clear() {
        resources.clear();
        resourceProviders.clear();
        resourceTemplates.clear();
        templateProviders.clear();
    }
}
