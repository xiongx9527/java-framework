package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistry;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MagicApiMcpPluginAutoConfigurationTest {

    @Test
    void shouldExposeMcpConfigurationWhenImportedByPluginAutoConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MagicResourceService.class, () -> mock(MagicResourceService.class));
            context.register(MagicApiPluginAutoConfiguration.class);
            context.refresh();

            MagicAPIMcpConfiguration configuration = context.getBean(MagicAPIMcpConfiguration.class);
            assertNotNull(configuration);
            assertNotNull(context.getBean(MagicApiMcpProperties.class));
            assertNotNull(context.getBean(MagicApiMcpToolRegistry.class));
            assertNotNull(context.getBean(MagicApiMcpToolInvoker.class));
            assertNotNull(context.getBean(MagicApiMcpToolRegistrar.class));
            assertNotNull(context.getBean(
                    com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpResourceInfoMagicResourceStorage.class));
            assertNotNull(context.getBean(
                    com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpPromptInfoMagicResourceStorage.class));
            assertNotNull(context.getBean(
                    com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpResourceMagicDynamicRegistry.class));
            assertNotNull(context.getBean(
                    com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicMcpPromptMagicDynamicRegistry.class));
        }
    }
}
