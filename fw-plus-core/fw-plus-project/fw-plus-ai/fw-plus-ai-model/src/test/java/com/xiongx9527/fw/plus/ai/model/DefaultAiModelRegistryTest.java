package com.xiongx9527.fw.plus.ai.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAiModelRegistryTest {

    @Test
    public void shouldRejectDuplicateEnabledModelKey() {
        AiModelDefinition first = definition("chat");
        AiModelDefinition duplicate = definition("chat");

        AiModelException exception = assertThrows(AiModelException.class,
                () -> new DefaultAiModelRegistry(Arrays.asList(first, duplicate), "chat",
                        Collections.<AiModelAdapter>singletonList(new FakeAiModelAdapter())));

        assertTrue(exception.getMessage().contains("chat"));
    }

    @Test
    public void shouldRejectMissingDefaultModelKey() {
        AiModelException exception = assertThrows(AiModelException.class,
                () -> new DefaultAiModelRegistry(Collections.singletonList(definition("chat")), "missing",
                        Collections.<AiModelAdapter>singletonList(new FakeAiModelAdapter())));

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    public void shouldResolveSupportedDefinitionThroughAdapter() {
        FakeAiModelAdapter adapter = new FakeAiModelAdapter();
        AiModelDefinition definition = definition("chat");
        DefaultAiModelRegistry registry = new DefaultAiModelRegistry(Collections.singletonList(definition), "chat",
                Collections.<AiModelAdapter>singletonList(adapter));

        assertSame(adapter.getModel(), registry.get("chat"));
        assertSame(adapter.getModel(), registry.getDefault());
    }

    private AiModelDefinition definition(String key) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(key);
        definition.setFramework("fake");
        definition.setProvider("fake-provider");
        definition.setType(AiModelType.CHAT);
        definition.setEnabled(true);
        return definition;
    }

    private static class FakeAiModelAdapter implements AiModelAdapter {

        private final AiModel model = new FakeAiModel();

        @Override
        public boolean supports(AiModelDefinition definition) {
            return "fake".equals(definition.getFramework())
                    && "fake-provider".equals(definition.getProvider())
                    && AiModelType.CHAT == definition.getType();
        }

        @Override
        public AiModel create(AiModelDefinition definition) {
            return model;
        }

        public AiModel getModel() {
            return model;
        }
    }

    private static class FakeAiModel implements AiModel {

        @Override
        public String getKey() {
            return "chat";
        }

        @Override
        public String getFramework() {
            return "fake";
        }

        @Override
        public AiModelType getType() {
            return AiModelType.CHAT;
        }
    }
}
