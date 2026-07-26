package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LangChain4jModelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
            .withUserConfiguration(FakeAdapterConfiguration.class);

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
                .run(context -> assertEquals(40, context.getBeansOfType(LangChain4jModelProviderAdapter.class).size()));
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
