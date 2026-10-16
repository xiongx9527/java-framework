package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.Langchain4jModuleConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.Langchain4jDatasourceConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * magic-api 插件聚合：各插件自身的生效条件（classpath、资源服务、开关）由插件配置自己声明，
 * 本类只负责把它们接入自动配置链。
 */
@Configuration
@Import({MagicAPIMcpServerConfiguration.class, Langchain4jDatasourceConfiguration.class,
        Langchain4jModuleConfiguration.class})
public class MagicApiPluginAutoConfiguration {
}
