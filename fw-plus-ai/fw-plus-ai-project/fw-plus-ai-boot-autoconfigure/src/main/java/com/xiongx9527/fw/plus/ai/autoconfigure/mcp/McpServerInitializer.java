package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * MCP Server 启动器，在 Spring Boot 应用启动后扫描特性并启动 MCP Server。
 * <p>
 * 特性扫描放在启动阶段而非 Bean 创建阶段：此时应用上下文已完全就绪，可以可靠定位
 * {@code @SpringBootConfiguration} 启动类，也避免自动配置阶段做类路径扫描拖慢启动。
 * </p>
 */
public class McpServerInitializer implements ApplicationRunner {

    private final McpServer server;

    private final McpProperties properties;

    private final ApplicationContext context;

    private final FeatureMgr featureMgr;

    public McpServerInitializer(McpServer server, McpProperties properties, ApplicationContext context,
                                FeatureMgr featureMgr) {
        this.server = server;
        this.properties = properties;
        this.context = context;
        this.featureMgr = featureMgr;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String basePackage : resolveBasePackages(properties, context))
            featureMgr.init(basePackage);

        McpTransportSync transport = context.getBeanProvider(McpTransportSync.class).getIfAvailable();
        if (transport != null) {
            server.setTransport(transport);
        }
        server.start();
    }

    /**
     * 解析 {@code @McpService} 的扫描包路径。
     * <p>
     * {@code fw-plus.ai.mcp.base-packages} 支持逗号分隔的多个包，与 {@code @SpringBootConfiguration}
     * 启动类所在包取并集：配置包在前、默认包在后，重复包只扫描一次。
     * </p>
     *
     * @param properties MCP 配置属性
     * @param context    Spring 应用上下文，用于定位启动类
     * @return 待扫描的包路径，无可用包时返回空列表
     */
    static List<String> resolveBasePackages(McpProperties properties, ApplicationContext context) {
        // LinkedHashSet 保持配置包在前、默认包在后的顺序并去重。
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        String basePackages = properties.getBasePackages();

        if (basePackages != null) {
            for (String candidate : basePackages.split(",")) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty())
                    packages.add(trimmed);
            }
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
