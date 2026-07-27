package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LangChain4jModelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
            .withUserConfiguration(FakeAdapterConfiguration.class);

    @Test
    public void shouldBackOffWhenOptionalModelCoreIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.langchain4j.model"))
                .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertFalse(context.containsBean("langChain4jModelRegistry"));
                });
    }

    @Test
    public void shouldCreateRegistryFromEnabledDefinitions() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=true",
                        "fw-plus.ai.langchain4j.model.default-model=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.model-name=gpt-4o-mini",
                        "fw-plus.ai.langchain4j.model.models.disabled.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.disabled.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.disabled.enabled=false"
                )
                .run(context -> {
                    LangChain4jModelRegistry registry = context.getBean(LangChain4jModelRegistry.class);
                    assertTrue(registry.contains("chat"));
                    assertFalse(registry.contains("disabled"));
                    assertEquals("fake-model", registry.getDefault().getModel());
                });
    }

    @Test
    public void shouldBackOffWhenUserProvidesRegistry() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
                .withUserConfiguration(CustomRegistryConfiguration.class)
                .run(context -> assertSame(context.getBean("customRegistry"), context.getBean(LangChain4jModelRegistry.class)));
    }

    @Test
    public void shouldRegisterProviderAdapters() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
                .run(context -> {
                    Map<String, LangChain4jModelProviderAdapter> adapters = context.getBeansOfType(LangChain4jModelProviderAdapter.class);
                    for (Map.Entry<String, Set<LangChain4jModelType>> entry : expectedProviderTypes().entrySet()) {
                        for (LangChain4jModelType type : LangChain4jModelType.values()) {
                            int supportCount = supportCount(adapters, definition(entry.getKey(), type));
                            assertEquals(entry.getValue().contains(type) ? 1 : 0, supportCount,
                                    "Unexpected support for provider=" + entry.getKey() + ", modelType=" + type);
                        }
                    }
                });
    }

    @Test
    public void shouldLeaveMultimodalExtensionTypesForCustomAdapters() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
                .run(context -> {
                    Map<String, LangChain4jModelProviderAdapter> adapters = context.getBeansOfType(LangChain4jModelProviderAdapter.class);
                    assertEquals(0, supportCount(adapters, definition("openai", LangChain4jModelType.AUDIO)));
                    assertEquals(0, supportCount(adapters, definition("openai", LangChain4jModelType.VIDEO)));
                    assertEquals(0, supportCount(adapters, definition("openai", LangChain4jModelType.PDF)));
                    assertEquals(0, supportCount(adapters, definition("gemini", LangChain4jModelType.AUDIO)));
                    assertEquals(0, supportCount(adapters, definition("gemini", LangChain4jModelType.VIDEO)));
                    assertEquals(0, supportCount(adapters, definition("gemini", LangChain4jModelType.PDF)));
                });
    }

    @Test
    public void shouldBuildAzureModelUsingBaseUrlAsEndpoint() {
        LangChain4jModelDefinition definition = definition("azure-openai", LangChain4jModelType.CHAT);
        definition.setKey("azure-chat");
        definition.setBaseUrl("https://example.openai.azure.com");
        definition.setApiKey("test-api-key");
        definition.setModelName("deployment-name");

        LangChain4jModel model = new LangChain4jModelAutoConfiguration()
                .azureOpenAiChatModelProviderAdapter().create(definition);

        assertEquals("dev.langchain4j.model.azure.AzureOpenAiChatModel", model.getModel().getClass().getName());
    }

    private Map<String, Set<LangChain4jModelType>> expectedProviderTypes() {
        Map<String, Set<LangChain4jModelType>> expected = new LinkedHashMap<String, Set<LangChain4jModelType>>();
        expected.put("openai", types(LangChain4jModelType.CHAT, LangChain4jModelType.STREAMING_CHAT,
                LangChain4jModelType.EMBEDDING, LangChain4jModelType.IMAGE, LangChain4jModelType.LANGUAGE,
                LangChain4jModelType.STREAMING_LANGUAGE));
        expected.put("azure-openai", types(LangChain4jModelType.CHAT, LangChain4jModelType.STREAMING_CHAT,
                LangChain4jModelType.EMBEDDING, LangChain4jModelType.IMAGE, LangChain4jModelType.LANGUAGE,
                LangChain4jModelType.STREAMING_LANGUAGE));
        expected.put("qwen", types(LangChain4jModelType.CHAT, LangChain4jModelType.STREAMING_CHAT,
                LangChain4jModelType.EMBEDDING, LangChain4jModelType.IMAGE, LangChain4jModelType.LANGUAGE,
                LangChain4jModelType.STREAMING_LANGUAGE));
        expected.put("ollama", types(LangChain4jModelType.CHAT, LangChain4jModelType.EMBEDDING, LangChain4jModelType.LANGUAGE));
        expected.put("anthropic", types(LangChain4jModelType.CHAT, LangChain4jModelType.STREAMING_CHAT));
        expected.put("qianfan", types(LangChain4jModelType.CHAT, LangChain4jModelType.STREAMING_CHAT,
                LangChain4jModelType.EMBEDDING, LangChain4jModelType.LANGUAGE));
        expected.put("zhipu", types(LangChain4jModelType.CHAT, LangChain4jModelType.STREAMING_CHAT,
                LangChain4jModelType.EMBEDDING, LangChain4jModelType.IMAGE));
        expected.put("hugging-face", types(LangChain4jModelType.CHAT, LangChain4jModelType.EMBEDDING, LangChain4jModelType.LANGUAGE));
        expected.put("local-ai", types(LangChain4jModelType.CHAT, LangChain4jModelType.EMBEDDING, LangChain4jModelType.LANGUAGE));
        expected.put("gemini", types(LangChain4jModelType.CHAT, LangChain4jModelType.EMBEDDING));
        expected.put("voyage-ai", types(LangChain4jModelType.EMBEDDING, LangChain4jModelType.SCORING));
        expected.put("all-mini-lm-l6-v2-quantized", types(LangChain4jModelType.EMBEDDING));
        return expected;
    }

    private Set<LangChain4jModelType> types(LangChain4jModelType... types) {
        Set<LangChain4jModelType> result = new LinkedHashSet<LangChain4jModelType>();
        for (LangChain4jModelType type : types) {
            result.add(type);
        }
        return result;
    }

    private int supportCount(Map<String, LangChain4jModelProviderAdapter> adapters, LangChain4jModelDefinition definition) {
        int count = 0;
        for (LangChain4jModelProviderAdapter adapter : adapters.values()) {
            if (adapter.supports(definition)) {
                count++;
            }
        }
        return count;
    }

    private LangChain4jModelDefinition definition(String provider, LangChain4jModelType type) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setProvider(provider);
        definition.setModelType(type);
        return definition;
    }

    @Configuration
    static class FakeAdapterConfiguration {

        @Bean
        LangChain4jModelProviderAdapter fakeAdapter() {
            return new LangChain4jModelProviderAdapter() {
                @Override
                public boolean supports(LangChain4jModelDefinition definition) {
                    return "openai".equals(definition.getProvider());
                }

                @Override
                public LangChain4jModel create(LangChain4jModelDefinition definition) {
                    return new LangChain4jModel(definition.getProvider(), definition.getModelType(), "fake-model");
                }
            };
        }
    }

    @Configuration
    static class CustomRegistryConfiguration {

        @Bean("customRegistry")
        LangChain4jModelRegistry customRegistry() {
            return new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), null);
        }
    }
}
