package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("gpt-4o-mini", properties.getModels().get("chat-default").getModelName().get(0));
        assertEquals("0.2", String.valueOf(properties.getModels().get("chat-default").getProperties().get("temperature")));
    }

    @Test
    public void shouldBindMultimodalExtensionModelTypes() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("fw-plus.ai.langchain4j.model.models.audio.provider", "custom")
                .withProperty("fw-plus.ai.langchain4j.model.models.audio.model-type", "audio")
                .withProperty("fw-plus.ai.langchain4j.model.models.video.provider", "custom")
                .withProperty("fw-plus.ai.langchain4j.model.models.video.model-type", "video")
                .withProperty("fw-plus.ai.langchain4j.model.models.pdf.provider", "custom")
                .withProperty("fw-plus.ai.langchain4j.model.models.pdf.model-type", "pdf");

        LangChain4jModelProperties properties = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()))
                .bind("fw-plus.ai.langchain4j.model", LangChain4jModelProperties.class)
                .get();

        assertEquals(LangChain4jModelType.AUDIO, properties.getModels().get("audio").getModelType());
        assertEquals(LangChain4jModelType.VIDEO, properties.getModels().get("video").getModelType());
        assertEquals(LangChain4jModelType.PDF, properties.getModels().get("pdf").getModelType());
    }

    @Test
    public void shouldBindMultipleModelNames() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("fw-plus.ai.langchain4j.model.default-model", "chat-default")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.provider", "openai")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-type", "chat")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.default-model-name", "qwen-plus")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-name[0]", "gpt-4o-mini")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-name[1]", "qwen-plus");

        LangChain4jModelProperties properties = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()))
                .bind("fw-plus.ai.langchain4j.model", LangChain4jModelProperties.class)
                .get();

        LangChain4jModelProperties.Model model = properties.getModels().get("chat-default");
        assertEquals("openai", model.getProvider());
        assertEquals(LangChain4jModelType.CHAT, model.getModelType());
        assertEquals("qwen-plus", model.getDefaultModelName());
        assertEquals("gpt-4o-mini", model.getModelName().get(0));
        assertEquals("qwen-plus", model.getModelName().get(1));
    }

    @Test
    public void shouldBindPerModelNameConfigs() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.provider", "openai")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-type", "chat")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-name[0]", "gpt-4o-mini")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-name[1]", "qwen-plus")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.properties.temperature", "0.2")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-configs.qwen-plus.properties.temperature", "0.7")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-configs.qwen-plus.properties.maxTokens", "2048");

        LangChain4jModelProperties properties = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()))
                .bind("fw-plus.ai.langchain4j.model", LangChain4jModelProperties.class)
                .get();

        LangChain4jModelProperties.Model model = properties.getModels().get("chat-default");
        LangChain4jModelProperties.ModelConfig qwenConfig = model.getModelConfigs().get("qwen-plus");
        assertEquals("0.7", String.valueOf(qwenConfig.getProperties().get("temperature")));
        assertEquals("2048", String.valueOf(qwenConfig.getProperties().get("maxTokens")));
    }
}
