package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.LangChain4jModelAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.module.ModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.starter.MagicAPIModelConfiguration;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicAPIModelAutoConfigurationIntegrationTest {

    @Test
    void importsModelConfiguration() {
        Import imported = MagicApiPluginAutoConfiguration.class.getAnnotation(Import.class);

        assertTrue(Arrays.asList(imported.value()).contains(MagicAPIModelConfiguration.class));
    }

    @Test
    void createsModelPluginBeansThroughAutoConfiguration() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class, LangChain4jModelAutoConfiguration.class))
                .withPropertyValues(
                        "fw.plus.magicapi.enabled=true",
                        "magic-api.plugin.aia.enabled=false",
                        "fw-plus.ai.langchain4j.model.default-model=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.provider=fake",
                        "fw-plus.ai.langchain4j.model.models.chat.model-type=chat")
                .withBean(MagicAPIProperties.class, MagicAPIProperties::new)
                .withBean(LangChain4jModelProviderAdapter.class, FakeModelProviderAdapter::new)
                .run(context -> {
                    AiModelRegistry registry = context.getBean(AiModelRegistry.class);
                    assertTrue(registry.contains("chat"));
                    assertNotNull(context.getBean(MagicDynamicModelDataSource.class));
                    assertNotNull(context.getBean(MagicDynamicModelDataSource.class).getModel("chat"));
                    assertNotNull(context.getBean(ModelDataSourceInfoMagicResourceStorage.class));
                    assertNotNull(context.getBean(ModelDataSourceMagicDynamicRegistry.class));
                    assertNotNull(context.getBean(ModelModule.class));
                    assertNotNull(context.getBean(MagicAPIModelConfiguration.class));
                });
    }

    private static class FakeModelProviderAdapter implements LangChain4jModelProviderAdapter {
        @Override
        public boolean supports(LangChain4jModelDefinition definition) {
            return "fake".equals(definition.getProvider());
        }

        @Override
        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            return new LangChain4jModel("fake", LangChain4jModelType.CHAT, "native");
        }
    }
}
