package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter.SpringAiAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.module.ModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.starter.MagicAPIModelConfiguration;
import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                .withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class, AiModelAutoConfiguration.class))
                .withPropertyValues(
                        "fw.plus.magicapi.enabled=true",
                        "magic-api.plugin.aia.enabled=false",
                        "fw-plus.ai.model.default-model=chat",
                        "fw-plus.ai.model.models.chat.provider=fake",
                        "fw-plus.ai.model.models.chat.type=chat")
                .withBean(MagicAPIProperties.class, MagicAPIProperties::new)
                .withBean(AiModelAdapter.class, FakeModelAdapter::new)
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

    @Test
    void exposesSpringAiChatThroughNeutralMagicModelDatasourceWithoutVectorAdapter() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class,
                        SpringAiAdapterAutoConfiguration.class, AiModelAutoConfiguration.class))
                .withPropertyValues(
                        "fw.plus.magicapi.enabled=true",
                        "magic-api.plugin.aia.enabled=false",
                        "fw-plus.ai.model.default-model=chat",
                        "fw-plus.ai.model.models.chat.framework=spring-ai",
                        "fw-plus.ai.model.models.chat.provider=openai",
                        "fw-plus.ai.model.models.chat.type=chat",
                        "fw-plus.ai.model.models.chat.properties.bean-name=openAiChatModel")
                .withBean(MagicAPIProperties.class, MagicAPIProperties::new)
                .withBean("openAiChatModel", ChatModel.class, () -> new ChatModel() {
                    @Override
                    public ChatResponse call(Prompt prompt) {
                        return new ChatResponse(Collections.singletonList(
                                new Generation(new AssistantMessage("spring-answer"))));
                    }
                })
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    MagicDynamicModelDataSource datasource = context.getBean(MagicDynamicModelDataSource.class);
                    assertTrue(datasource.contains("chat"));
                    assertEquals("spring-answer", ((AiChatModel) datasource.getModel("chat")).chat(
                            new AiChatRequest("hello", Collections.emptyMap(), 0L)).getText());
                    assertTrue(context.getBeansOfType(AiEmbeddingStoreAdapter.class).isEmpty());
                });
    }

    private static class FakeModelAdapter implements AiModelAdapter {
        @Override
        public String getFramework() {
            return "fake-framework";
        }

        @Override
        public boolean supports(AiModelDefinition definition) {
            return "fake".equals(definition.getProvider());
        }

        @Override
        public AiModel create(final AiModelDefinition definition) {
            return new AiModel() {
                @Override public String getKey() { return definition.getKey(); }
                @Override public String getFramework() { return "fake-framework"; }
                @Override public String getProvider() { return definition.getProvider(); }
                @Override public AiModelType getType() { return definition.getType(); }
            };
        }
    }
}
