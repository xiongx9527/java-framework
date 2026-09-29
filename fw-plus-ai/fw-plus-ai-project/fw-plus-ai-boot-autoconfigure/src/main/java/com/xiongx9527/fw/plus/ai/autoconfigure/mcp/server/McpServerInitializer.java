package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.ajaxjs.mcp.transport.McpTransportSync;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.feature.McpFeatureScanner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * MCP Server 启动器，在 Spring Boot 应用启动后扫描特性并启动各 runtime。
 * <p>
 * 特性扫描放在启动阶段而非 Bean 创建阶段：此时应用上下文已完全就绪，可以可靠定位
 * {@code @SpringBootConfiguration} 启动类，也避免自动配置阶段做类路径扫描拖慢启动。
 * </p>
 * <p>
 * 一次扫描即可按 {@code @McpServer} 注解把能力路由到各 runtime 的 FeatureMgr（扫描源为全部
 * runtime 的 basePackages 并集）；随后逐个 runtime 绑定传输并 {@code start()}。
 * </p>
 */
public class McpServerInitializer implements ApplicationRunner {

    private final List<McpServerRuntime> runtimes;

    private final McpProperties properties;

    private final ApplicationContext context;

    public McpServerInitializer(List<McpServerRuntime> runtimes, McpProperties properties,
                                ApplicationContext context) {
        this.runtimes = runtimes;
        this.properties = properties;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, com.ajaxjs.mcp.server.feature.FeatureMgr> featureMgrs = new LinkedHashMap<>();
        for (McpServerRuntime runtime : runtimes) {
            featureMgrs.put(runtime.getKey(), runtime.getFeatureMgr());
        }

        McpFeatureScanner scanner = new McpFeatureScanner();
        for (String basePackage : resolveBasePackages(properties, context, runtimes)) {
            try {
                scanner.register(java.util.Collections.singletonList(basePackage), featureMgrs);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to scan MCP features in package '" + basePackage + "'", e);
            }
        }

        for (McpServerRuntime runtime : runtimes) {
            if (runtime.getRegistry() instanceof McpTransportSync) {
                runtime.getServer().setTransport((McpTransportSync) runtime.getRegistry());
            }
            runtime.getServer().start();
        }
    }

    /**
     * 解析待扫描的包路径集合。
     * <p>
     * {@code fw-plus.ai.mcp.base-packages} 支持逗号分隔的多个包，与 {@code @SpringBootConfiguration}
     * 启动类所在包取并集：配置包在前、默认包在后，重复包只扫描一次。
     * </p>
     *
     * @param properties MCP 配置属性
     * @param context    Spring 应用上下文，用于定位启动类
     * @param runtimes   全部 runtime，其 basePackages 一并纳入扫描源
     * @return 待扫描的包路径，无可用包时返回空列表
     */
    static List<String> resolveBasePackages(McpProperties properties, ApplicationContext context,
                                            List<McpServerRuntime> runtimes) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        String basePackages = properties.getBasePackages();

        if (basePackages != null) {
            for (String candidate : basePackages.split(",")) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty())
                    packages.add(trimmed);
            }
        }

        for (McpServerRuntime runtime : runtimes) {
            packages.addAll(runtime.getBasePackages());
        }

        String defaultPackage = defaultBasePackage(context);
        if (defaultPackage != null && !defaultPackage.isEmpty())
            packages.add(defaultPackage);

        return new ArrayList<>(packages);
    }

    private static String defaultBasePackage(ApplicationContext context) {
        for (String name : context.getBeanNamesForAnnotation(SpringBootConfiguration.class)) {
            Class<?> type = context.getType(name);
            if (type != null && type.getPackage() != null)
                return type.getPackage().getName();
        }

        return null;
    }
}