package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiProviderBinding;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiAdapterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringAiAdapterAutoConfiguration.class));

    @Test
    void shouldRegisterBothFrameworkAdaptersAndResolveExplicitBeanNames() {
        DashScopeChatModel dashScopeModel = mock(DashScopeChatModel.class);
        when(dashScopeModel.call(any(Prompt.class))).thenReturn(response("dashscope"));
        this.contextRunner
                .withBean("openAiChatModel", ChatModel.class, () -> chatModel("openai"))
                .withBean("dashScopeChatModel", DashScopeChatModel.class, () -> dashScopeModel)
                .withBean("openAiBinding", SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai", "openai", AiModelType.CHAT, "openAiChatModel"))
                .withBean("dashScopeBinding", SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai-alibaba", "dashscope", AiModelType.CHAT, "dashScopeChatModel"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    Map<String, AiModelAdapter> adapters = context.getBeansOfType(AiModelAdapter.class);
                    assertEquals(2, adapters.size());
                    assertEquals("spring-ai", adapters.get("springAiModelAdapter").getFramework());
                    assertEquals("spring-ai-alibaba", adapters.get("springAiAlibabaModelAdapter").getFramework());

                    AiModelDefinition definition = definition("spring-ai-alibaba", "dashscope", "dashScopeChatModel");
                    AiChatModel model = (AiChatModel) adapters.get("springAiAlibabaModelAdapter").create(definition);
                    assertEquals("dashscope", model.chat(new AiChatRequest(
                            "hello", Collections.emptyMap(), 0L)).getText());
                });
    }

    @Test
    void shouldResolveRegistryFromExistingNeutralConfiguration() {
        this.contextRunner
                .withConfiguration(AutoConfigurations.of(
                        com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration.class))
                .withBean("openAiChatModel", ChatModel.class, () -> chatModel("answer"))
                .withPropertyValues(
                        "fw-plus.ai.model.default-model=chat",
                        "fw-plus.ai.model.models.chat.framework=spring-ai",
                        "fw-plus.ai.model.models.chat.provider=openai",
                        "fw-plus.ai.model.models.chat.type=chat",
                        "fw-plus.ai.model.models.chat.properties.bean-name=openAiChatModel")
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    AiModelRegistry registry = context.getBean(AiModelRegistry.class);
                    assertEquals("answer", ((AiChatModel) registry.getDefault()).chat(new AiChatRequest(
                            "hello", Collections.emptyMap(), 0L)).getText());
                });
    }

    @Test
    void shouldBackOffIndependentlyWhenAdapterOrAlibabaSdkIsMissing() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.spring.adapter"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertTrue(context.getBeansOfType(AiModelAdapter.class).isEmpty());
                });

        this.contextRunner
                .withClassLoader(new FilteredClassLoader("com.alibaba.cloud.ai.dashscope"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertTrue(context.containsBean("springAiModelAdapter"));
                    assertFalse(context.containsBean("springAiAlibabaModelAdapter"));
                });
    }

    @Test
    void shouldBackOffWhenAllSpringAiModelApisAreMissing() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.ai"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertTrue(context.getBeansOfType(AiModelAdapter.class).isEmpty());
                });
    }

    @Test
    void shouldKeepChatAdapterWhenEmbeddingApiIsMissing() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.ai.embedding"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertTrue(context.containsBean("springAiModelAdapter"));
                    assertTrue(context.containsBean("springAiChatModelProviderAdapter"));
                    assertFalse(context.containsBean("springAiEmbeddingModelProviderAdapter"));
                });
    }

    @Test
    void shouldKeepEmbeddingAdapterWhenChatApiIsMissing() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.ai.chat"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertTrue(context.containsBean("springAiModelAdapter"));
                    assertFalse(context.containsBean("springAiChatModelProviderAdapter"));
                    assertTrue(context.containsBean("springAiEmbeddingModelProviderAdapter"));
                });
    }

    @Test
    void shouldNotRegisterAlibabaAdapterWhenModelCapabilitiesAreMissing() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(
                        "com.alibaba.cloud.ai.dashscope.chat",
                        "com.alibaba.cloud.ai.dashscope.embedding"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertFalse(context.containsBean("springAiAlibabaModelAdapter"));
                });
    }

    @Test
    void shouldRejectCrossFrameworkExplicitBeanName() {
        DashScopeChatModel dashScopeModel = mock(DashScopeChatModel.class);
        this.contextRunner
                .withBean("dashScopeChatModel", DashScopeChatModel.class, () -> dashScopeModel)
                .run(context -> {
                    AiModelDefinition genericDefinition = definition("spring-ai", "openai",
                            "dashScopeChatModel");
                    assertFalse(context.getBean("springAiModelAdapter", AiModelAdapter.class)
                            .supports(genericDefinition));
                    AiModelDefinition alibabaDefinition = definition("spring-ai-alibaba", "dashscope",
                            "dashScopeChatModel");
                    assertTrue(context.getBean("springAiAlibabaModelAdapter", AiModelAdapter.class)
                            .supports(alibabaDefinition));
                });
    }

    @Test
    void shouldBackOffByBeanNameWithoutBeingBlockedByOtherAdapters() {
        AiModelAdapter customSpring = new StubAdapter("custom-spring");
        AiModelAdapter unrelated = new StubAdapter("langchain4j");
        this.contextRunner
                .withBean("springAiModelAdapter", AiModelAdapter.class, () -> customSpring)
                .withBean("langChain4jAiModelAdapter", AiModelAdapter.class, () -> unrelated)
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertEquals(customSpring, context.getBean("springAiModelAdapter"));
                    assertTrue(context.containsBean("springAiAlibabaModelAdapter"));
                    assertEquals(3, context.getBeansOfType(AiModelAdapter.class).size());
                });
    }

    @Test
    void shouldNotInstantiateFactoryBeanDuringCatalogOrSupports() {
        CountingChatModelFactoryBean.reset();
        this.contextRunner
                .withInitializer(context -> {
                    GenericBeanDefinition definition = new GenericBeanDefinition();
                    definition.setBeanClass(CountingChatModelFactoryBean.class);
                    definition.setLazyInit(true);
                    definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ChatModel.class);
                    ((GenericApplicationContext) context).registerBeanDefinition("factoryChatModel", definition);
                })
                .withBean(SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai", "openai", AiModelType.CHAT, "factoryChatModel"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertEquals(0, CountingChatModelFactoryBean.constructors.get());
                    assertEquals(0, CountingChatModelFactoryBean.products.get());
                    AiModelDefinition model = definition("spring-ai", "openai", null);
                    AiModelAdapter adapter = context.getBean("springAiModelAdapter", AiModelAdapter.class);
                    assertTrue(adapter.supports(model));
                    assertEquals(0, CountingChatModelFactoryBean.constructors.get());
                    assertEquals(0, CountingChatModelFactoryBean.products.get());
                    adapter.create(model);
                    assertEquals(1, CountingChatModelFactoryBean.constructors.get());
                    assertEquals(1, CountingChatModelFactoryBean.products.get());
                });
    }

    @Test
    void shouldReportSafeExplicitBeanDiagnostics() {
        this.contextRunner.run(context -> {
            AiModelDefinition model = definition("spring-ai", "openai", "secret-client-bean");
            AiModelAdapter adapter = context.getBean("springAiModelAdapter", AiModelAdapter.class);
            RuntimeException failure = assertThrows(RuntimeException.class, () -> adapter.create(model));
            String messages = exceptionMessages(failure);
            assertTrue(messages.contains("key=chat"));
            assertTrue(messages.contains("framework=spring-ai"));
            assertTrue(messages.contains("provider=openai"));
            assertTrue(messages.contains("type=CHAT"));
            assertTrue(messages.contains("candidates=0"));
            assertTrue(messages.matches("(?s).*beanRef=[0-9a-f]{12}.*"));
            assertFalse(messages.contains("secret-client-bean"));
        });
    }

    @Test
    void shouldReportSafeMissingBindingDiagnostics() {
        this.contextRunner
                .withBean(SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai", "openai", AiModelType.CHAT, "secret-missing-binding"))
                .run(context -> {
                    AiModelAdapter adapter = context.getBean("springAiModelAdapter", AiModelAdapter.class);
                    RuntimeException failure = assertThrows(RuntimeException.class,
                            () -> adapter.create(definition("spring-ai", "openai", null)));
                    String messages = exceptionMessages(failure);
                    assertTrue(messages.contains("candidates=0"));
                    assertTrue(messages.matches("(?s).*beanRefs=\\[[0-9a-f]{12}\\].*"));
                    assertFalse(messages.contains("secret-missing-binding"));
                });
    }

    @Test
    void shouldReportSafeMultipleBindingDiagnostics() {
        this.contextRunner
                .withBean("firstSecretModel", ChatModel.class, () -> chatModel("first"))
                .withBean("secondSecretModel", ChatModel.class, () -> chatModel("second"))
                .withBean("firstBinding", SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai", "openai", AiModelType.CHAT, "firstSecretModel"))
                .withBean("secondBinding", SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai", "openai", AiModelType.CHAT, "secondSecretModel"))
                .run(context -> {
                    AiModelAdapter adapter = context.getBean("springAiModelAdapter", AiModelAdapter.class);
                    RuntimeException failure = assertThrows(RuntimeException.class,
                            () -> adapter.create(definition("spring-ai", "openai", null)));
                    String messages = exceptionMessages(failure);
                    assertTrue(messages.contains("candidates=2"));
                    assertTrue(messages.matches("(?s).*beanRefs=\\[[0-9a-f]{12}, [0-9a-f]{12}\\].*"));
                    assertFalse(messages.contains("firstSecretModel"));
                    assertFalse(messages.contains("secondSecretModel"));
                });
    }

    @Test
    void shouldNotInstantiateModelBeansDuringSupports() {
        AtomicInteger creations = new AtomicInteger();
        this.contextRunner
                .withBean("lazyChatModel", ChatModel.class, () -> {
                    creations.incrementAndGet();
                    return chatModel("lazy");
                })
                .withBean(SpringAiProviderBinding.class, () -> new SpringAiProviderBinding(
                        "spring-ai", "openai", AiModelType.CHAT, "lazyChatModel"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    creations.set(0);
                    AiModelDefinition definition = definition("spring-ai", "openai", null);
                    assertTrue(context.getBean("springAiModelAdapter", AiModelAdapter.class).supports(definition));
                    assertEquals(0, creations.get());
                });
    }

    private static String exceptionMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage()).append('\n');
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    public static final class CountingChatModelFactoryBean implements FactoryBean<ChatModel> {
        static final AtomicInteger constructors = new AtomicInteger();
        static final AtomicInteger products = new AtomicInteger();

        public CountingChatModelFactoryBean() {
            constructors.incrementAndGet();
        }

        static void reset() {
            constructors.set(0);
            products.set(0);
        }

        @Override
        public ChatModel getObject() {
            products.incrementAndGet();
            return chatModel("factory");
        }

        @Override
        public Class<?> getObjectType() {
            return ChatModel.class;
        }
    }

    private static ChatModel chatModel(String answer) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return response(answer);
            }
        };
    }

    private static ChatResponse response(String answer) {
        return new ChatResponse(Collections.singletonList(new Generation(new AssistantMessage(answer))));
    }

    private static AiModelDefinition definition(String framework, String provider, String beanName) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey("chat");
        definition.setFramework(framework);
        definition.setProvider(provider);
        definition.setType(AiModelType.CHAT);
        definition.setEnabled(true);
        if (beanName != null) {
            definition.getProperties().put("beanName", beanName);
        }
        return definition;
    }

    private static final class StubAdapter implements AiModelAdapter {

        private final String framework;

        private StubAdapter(String framework) {
            this.framework = framework;
        }

        @Override
        public String getFramework() {
            return framework;
        }

        @Override
        public boolean supports(AiModelDefinition definition) {
            return false;
        }

        @Override
        public com.xiongx9527.fw.plus.ai.model.AiModel create(AiModelDefinition definition) {
            throw new UnsupportedOperationException();
        }
    }
}
