package com.xiongx9527.fw.plus.ai.langchain4j.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultLangChain4jModelRegistryTest {

    @Test
    public void shouldResolveModelsAndDefaultModel() {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        LangChain4jModel chat = new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object());
        models.put("chat", chat);

        DefaultLangChain4jModelRegistry registry = new DefaultLangChain4jModelRegistry(models, "chat");

        assertSame(chat, registry.get("chat"));
        assertSame(chat, registry.getDefault());
        assertTrue(registry.contains("chat"));
        assertTrue(registry.keys().contains("chat"));
    }

    @Test
    public void shouldFailWhenDefaultModelKeyIsMissing() {
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), "missing"));

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    public void shouldFailWhenModelKeyIsMissing() {
        DefaultLangChain4jModelRegistry registry =
                new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), null);

        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> registry.get("missing"));

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    public void shouldStoreAnUnmodifiableCopyOfModels() {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put("chat", new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object()));

        DefaultLangChain4jModelRegistry registry = new DefaultLangChain4jModelRegistry(models, "chat");
        models.clear();

        assertTrue(registry.contains("chat"));
        assertThrows(UnsupportedOperationException.class, () -> registry.keys().remove("chat"));
    }
}
