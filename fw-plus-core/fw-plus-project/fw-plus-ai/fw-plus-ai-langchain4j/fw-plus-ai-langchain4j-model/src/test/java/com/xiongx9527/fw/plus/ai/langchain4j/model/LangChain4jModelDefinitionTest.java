package com.xiongx9527.fw.plus.ai.langchain4j.model;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LangChain4jModelDefinitionTest {

    @Test
    public void shouldExposeJavaBeanModelDefinitionFields() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("temperature", 0.3);

        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey("chat-default");
        definition.setProvider("openai");
        definition.setModelType(LangChain4jModelType.CHAT);
        definition.setEnabled(true);
        definition.setBaseUrl("http://localhost:11434/v1");
        definition.setApiKey("demo");
        definition.setModelName("gpt-4o-mini");
        definition.setProperties(properties);

        assertEquals("chat-default", definition.getKey());
        assertEquals("openai", definition.getProvider());
        assertEquals(LangChain4jModelType.CHAT, definition.getModelType());
        assertTrue(definition.isEnabled());
        assertEquals("http://localhost:11434/v1", definition.getBaseUrl());
        assertEquals("demo", definition.getApiKey());
        assertEquals("gpt-4o-mini", definition.getModelName());
        assertEquals(0.3, definition.getProperties().get("temperature"));
    }

    @Test
    public void shouldExposeAllSupportedModelTypes() {
        assertNotNull(LangChain4jModelType.valueOf("CHAT"));
        assertNotNull(LangChain4jModelType.valueOf("STREAMING_CHAT"));
        assertNotNull(LangChain4jModelType.valueOf("EMBEDDING"));
        assertNotNull(LangChain4jModelType.valueOf("IMAGE"));
        assertNotNull(LangChain4jModelType.valueOf("LANGUAGE"));
        assertNotNull(LangChain4jModelType.valueOf("STREAMING_LANGUAGE"));
    }
}
