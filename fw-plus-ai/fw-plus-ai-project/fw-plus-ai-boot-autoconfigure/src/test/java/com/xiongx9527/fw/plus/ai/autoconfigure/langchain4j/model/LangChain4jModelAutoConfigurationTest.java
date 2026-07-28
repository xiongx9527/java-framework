package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
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
                    assertEquals("chat:gpt-4o-mini", registry.getDefault().getModel());
                });
    }

    @Test
    public void shouldMapLegacyPropertiesToNeutralRegistry() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.default-model=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat.model-type=chat")
                .run(context -> {
                    AiModelRegistry registry = context.getBean(AiModelRegistry.class);
                    assertTrue(registry.contains("chat"));
                    assertEquals("chat", registry.getDefaultKey());
                    assertEquals("langchain4j", registry.getDefault().getFramework());
                });
    }

    @Test
    public void shouldCreateNeutralRegistryFromNeutralPropertiesOnly() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.model.default-model=chat",
                        "fw-plus.ai.model.models.chat.framework=langchain4j",
                        "fw-plus.ai.model.models.chat.provider=openai",
                        "fw-plus.ai.model.models.chat.type=chat")
                .run(context -> {
                    AiModelRegistry registry = context.getBean(AiModelRegistry.class);
                    assertTrue(registry.contains("chat"));
                    assertEquals(AiModelType.CHAT, registry.getDefault().getType());
                });
    }

    @Test
    public void shouldCreateNeutralRegistryWhenLegacyConfigurationIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=false",
                        "fw-plus.ai.model.default-model=chat",
                        "fw-plus.ai.model.models.chat.framework=langchain4j",
                        "fw-plus.ai.model.models.chat.provider=openai",
                        "fw-plus.ai.model.models.chat.type=chat")
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertFalse(context.containsBean("langChain4jModelRegistry"));
                    assertTrue(context.getBean(AiModelRegistry.class).contains("chat"));
                });
    }

    @Test
    public void shouldFailForDuplicateEnabledNeutralModelKeyWithoutLeakingSecrets(CapturedOutput output) {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.model.models.primary.key=shared",
                        "fw-plus.ai.model.models.primary.framework=langchain4j",
                        "fw-plus.ai.model.models.primary.provider=openai",
                        "fw-plus.ai.model.models.primary.type=chat",
                        "fw-plus.ai.model.models.primary.properties.apiKey=primary-secret",
                        "fw-plus.ai.model.models.secondary.key=shared",
                        "fw-plus.ai.model.models.secondary.framework=langchain4j",
                        "fw-plus.ai.model.models.secondary.provider=openai",
                        "fw-plus.ai.model.models.secondary.type=chat",
                        "fw-plus.ai.model.models.secondary.properties.apiKey=secondary-secret")
                .run(context -> {
                    assertTrue(hasCause(context.getStartupFailure(), AiModelException.class,
                            "Duplicate enabled model key: shared"));
                    assertFalse(output.getAll().contains("primary-secret"));
                    assertFalse(output.getAll().contains("secondary-secret"));
                });
    }

    @Test
    public void shouldBridgeInjectedLegacyFactoryForRegistryAndRuntimeFactory() {
        RecordingLegacyFactory legacyFactory = new RecordingLegacyFactory();
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.model.models.chat.framework=langchain4j",
                        "fw-plus.ai.model.models.chat.provider=custom",
                        "fw-plus.ai.model.models.chat.type=chat")
                .withBean(LangChain4jModelFactory.class, () -> legacyFactory)
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertTrue(context.getBean(AiModelRegistry.class).contains("chat"));
                    AiModelDefinition runtime = new AiModelDefinition();
                    runtime.setKey("runtime-chat");
                    runtime.setFramework("langchain4j");
                    runtime.setProvider("custom");
                    runtime.setType(AiModelType.CHAT);
                    context.getBean(AiModelFactory.class).create(runtime,
                            Arrays.asList(context.getBeansOfType(AiModelAdapter.class).values()
                                    .toArray(new AiModelAdapter[0])));
                    assertEquals(2, legacyFactory.calls.get());
                });
    }

    @Test
    public void shouldPreferNeutralDefinitionForSameKeyWithoutLeakingApiKey(CapturedOutput output) {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.default-model=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.api-key=legacy-secret",
                        "fw-plus.ai.model.default-model=chat",
                        "fw-plus.ai.model.models.chat.framework=langchain4j",
                        "fw-plus.ai.model.models.chat.provider=openai",
                        "fw-plus.ai.model.models.chat.type=embedding")
                .run(context -> {
                    AiModelRegistry registry = context.getBean(AiModelRegistry.class);
                    assertEquals(AiModelType.EMBEDDING, registry.get("chat").getType());
                    assertTrue(output.getAll().contains("chat"));
                    assertFalse(output.getAll().contains("legacy-secret"));
                });
    }

    @Test
    public void shouldNotRestoreDisabledNeutralDefinitionFromLegacyProperties() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.models.chat.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat.model-type=chat",
                        "fw-plus.ai.model.models.chat.enabled=false",
                        "fw-plus.ai.model.models.chat.framework=langchain4j",
                        "fw-plus.ai.model.models.chat.provider=openai",
                        "fw-plus.ai.model.models.chat.type=chat")
                .run(context -> assertFalse(context.getBean(AiModelRegistry.class).contains("chat")));
    }

    @Test
    public void shouldUseInjectedNeutralFactoryWhenCreatingRegistry() {
        AiModelProperties neutral = new AiModelProperties();
        AiModelProperties.Model definition = new AiModelProperties.Model();
        definition.setKey("chat");
        definition.setFramework("custom");
        definition.setProvider("custom");
        definition.setType(AiModelType.CHAT);
        neutral.getModels().put("chat", definition);
        LangChain4jModelProperties legacy = new LangChain4jModelProperties();
        final AiModel expected = new TestAiModel("chat");
        AiModelFactory factory = new AiModelFactory() {
            @Override
            public AiModel create(AiModelDefinition ignored, List<AiModelAdapter> adapters) {
                return expected;
            }
        };

        AiModelRegistry registry = new LangChain4jModelAutoConfiguration().aiModelRegistry(
                neutral, legacy, factory, Collections.<AiModelAdapter>emptyList(),
                testFactory(),
                Collections.<LangChain4jModelProviderAdapter>emptyList());

        assertSame(expected, registry.get("chat"));
    }

    @Test
    public void shouldCreateModelsFromMultipleModelNamesAndDefaultAlias() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=true",
                        "fw-plus.ai.langchain4j.model.default-model=chat-default",
                        "fw-plus.ai.langchain4j.model.models.chat-default.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat-default.base-url=http://localhost:11434/v1",
                        "fw-plus.ai.langchain4j.model.models.chat-default.api-key=demo",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[0]=gpt-4o-mini",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[1]=qwen-plus",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[2]=llama3.1"
                )
                .run(context -> {
                    LangChain4jModelRegistry registry = context.getBean(LangChain4jModelRegistry.class);
                    assertTrue(registry.contains("chat-default"));
                    assertTrue(registry.contains("chat-default.gpt-4o-mini"));
                    assertTrue(registry.contains("chat-default.qwen-plus"));
                    assertTrue(registry.contains("chat-default.llama3.1"));
                    assertSame(registry.get("chat-default.gpt-4o-mini"), registry.get("chat-default"));
                    assertSame(registry.get("chat-default.gpt-4o-mini"), registry.getDefault());
                    assertEquals("chat-default.qwen-plus:qwen-plus", registry.get("chat-default.qwen-plus").getModel());
                    assertEquals("chat-default", registry.keys().iterator().next());
                });
    }

    @Test
    public void shouldAllowDefaultModelToReferenceModelNameKey() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=true",
                        "fw-plus.ai.langchain4j.model.default-model=chat-default.qwen-plus",
                        "fw-plus.ai.langchain4j.model.models.chat-default.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[0]=gpt-4o-mini",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[1]=qwen-plus"
                )
                .run(context -> {
                    LangChain4jModelRegistry registry = context.getBean(LangChain4jModelRegistry.class);
                    assertSame(registry.get("chat-default.qwen-plus"), registry.getDefault());
                    assertSame(registry.get("chat-default.gpt-4o-mini"), registry.get("chat-default"));
                });
    }

    @Test
    public void shouldUseConfiguredDefaultModelName() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=true",
                        "fw-plus.ai.langchain4j.model.default-model=chat-default",
                        "fw-plus.ai.langchain4j.model.models.chat-default.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat-default.default-model-name=qwen-plus",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[0]=gpt-4o-mini",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[1]=qwen-plus"
                )
                .run(context -> {
                    LangChain4jModelRegistry registry = context.getBean(LangChain4jModelRegistry.class);
                    assertTrue(registry.contains("chat-default"));
                    assertTrue(registry.contains("chat-default.gpt-4o-mini"));
                    assertTrue(registry.contains("chat-default.qwen-plus"));
                    assertSame(registry.get("chat-default.qwen-plus"), registry.get("chat-default"));
                    assertSame(registry.get("chat-default.qwen-plus"), registry.getDefault());
                });
    }

    @Test
    public void shouldMergePerModelNamePropertiesOverSharedProperties() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=true",
                        "fw-plus.ai.langchain4j.model.default-model=chat-default",
                        "fw-plus.ai.langchain4j.model.models.chat-default.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[0]=gpt-4o-mini",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-name[1]=qwen-plus",
                        "fw-plus.ai.langchain4j.model.models.chat-default.properties.temperature=0.2",
                        "fw-plus.ai.langchain4j.model.models.chat-default.properties.maxTokens=1024",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-configs.qwen-plus.properties.temperature=0.7",
                        "fw-plus.ai.langchain4j.model.models.chat-default.model-configs.qwen-plus.properties.maxTokens=2048"
                )
                .run(context -> {
                    LangChain4jModelRegistry registry = context.getBean(LangChain4jModelRegistry.class);
                    assertEquals("chat-default.gpt-4o-mini:gpt-4o-mini:0.2:1024",
                            registry.get("chat-default.gpt-4o-mini").getModel());
                    assertEquals("chat-default.qwen-plus:qwen-plus:0.7:2048",
                            registry.get("chat-default.qwen-plus").getModel());
                });
    }

    @Test
    public void shouldFailWhenGeneratedModelNameKeyDuplicatesFlatModelKey() {
        LangChain4jModelProperties properties = new LangChain4jModelProperties();
        properties.getModels().put("chat-default.gpt-4o-mini", model("openai", LangChain4jModelType.CHAT, "gpt-4o-mini"));
        properties.getModels().put("chat-default", model("openai", LangChain4jModelType.CHAT,
                "gpt-4o-mini", "qwen-plus"));

        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> new LangChain4jModelAutoConfiguration().langChain4jModelRegistry(properties, testFactory(),
                        Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertTrue(exception.getMessage().contains("Model key is duplicated: chat-default.gpt-4o-mini"));
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

    private LangChain4jModelProperties.Model model(String provider, LangChain4jModelType modelType, String... modelNames) {
        LangChain4jModelProperties.Model model = new LangChain4jModelProperties.Model();
        model.setProvider(provider);
        model.setModelType(modelType);
        model.setModelName(Arrays.asList(modelNames));
        return model;
    }

    private LangChain4jModelFactory testFactory() {
        return new LangChain4jModelFactory() {
            @Override
            public LangChain4jModel create(LangChain4jModelDefinition definition,
                                           List<LangChain4jModelProviderAdapter> adapters) {
                return new LangChain4jModel(definition.getProvider(), definition.getModelType(),
                        definition.getKey() + ":" + definition.getModelName());
            }
        };
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type, String message) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current) && current.getMessage() != null && current.getMessage().contains(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static class RecordingLegacyFactory implements LangChain4jModelFactory {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public LangChain4jModel create(LangChain4jModelDefinition definition,
                                       List<LangChain4jModelProviderAdapter> adapters) {
            calls.incrementAndGet();
            return new LangChain4jModel(definition.getProvider(), definition.getModelType(), definition.getKey());
        }
    }

    private static class TestAiModel implements AiModel {

        private final String key;

        private TestAiModel(String key) {
            this.key = key;
        }

        @Override public String getKey() { return key; }
        @Override public String getFramework() { return "custom"; }
        @Override public String getProvider() { return "custom"; }
        @Override public AiModelType getType() { return AiModelType.CHAT; }
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
                    String value = definition.getKey() + ":" + definition.getModelName();
                    if (definition.getProperties().containsKey("temperature")) {
                        value = value + ":" + definition.getProperties().get("temperature")
                                + ":" + definition.getProperties().get("maxTokens");
                    }
                    return new LangChain4jModel(definition.getProvider(), definition.getModelType(), value);
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
