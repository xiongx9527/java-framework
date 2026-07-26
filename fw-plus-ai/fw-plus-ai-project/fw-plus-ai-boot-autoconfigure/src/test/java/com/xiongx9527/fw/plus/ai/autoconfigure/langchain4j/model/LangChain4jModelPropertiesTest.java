package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LangChain4jModelPropertiesTest {

    @Test
    public void shouldBindModelProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("fw-plus.ai.langchain4j.model.enabled", "true")
                .withProperty("fw-plus.ai.langchain4j.model.default-model", "chat-default")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.provider", "openai")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-type", "chat")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.base-url", "http://localhost:11434/v1")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.api-key", "demo")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-name", "gpt-4o-mini")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.properties.temperature", "0.2");

        LangChain4jModelProperties properties = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()))
                .bind("fw-plus.ai.langchain4j.model", LangChain4jModelProperties.class)
                .get();

        assertTrue(properties.isEnabled());
        assertEquals("chat-default", properties.getDefaultModel());
        assertEquals("openai", properties.getModels().get("chat-default").getProvider());
        assertEquals(LangChain4jModelType.CHAT, properties.getModels().get("chat-default").getModelType());
        assertEquals("gpt-4o-mini", properties.getModels().get("chat-default").getModelName());
        assertEquals("0.2", String.valueOf(properties.getModels().get("chat-default").getProperties().get("temperature")));
    }
}
