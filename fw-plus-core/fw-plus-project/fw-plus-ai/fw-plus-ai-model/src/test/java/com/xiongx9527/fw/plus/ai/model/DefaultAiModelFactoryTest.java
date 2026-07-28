package com.xiongx9527.fw.plus.ai.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAiModelFactoryTest {

    private final DefaultAiModelFactory factory = new DefaultAiModelFactory();

    @Test
    public void shouldIncludeDefinitionContextWhenNoAdapterSupportsModel() {
        AiModelException exception = assertThrows(AiModelException.class,
                () -> factory.create(definition(), Collections.<AiModelAdapter>singletonList(new StubAdapter())));

        assertContext(exception);
        assertNull(exception.getCause());
    }

    @Test
    public void shouldWrapSupportsExceptionWithDefinitionContextAndCause() {
        RuntimeException cause = new IllegalStateException("supports failed");
        AiModelAdapter adapter = new StubAdapter() {
            @Override
            public boolean supports(AiModelDefinition definition) {
                throw cause;
            }
        };

        AiModelException exception = assertThrows(AiModelException.class,
                () -> factory.create(definition(), Collections.singletonList(adapter)));

        assertContext(exception);
        assertSame(cause, exception.getCause());
    }

    @Test
    public void shouldWrapCreateExceptionWithDefinitionContextAndCause() {
        RuntimeException cause = new IllegalStateException("create failed");
        AiModelAdapter adapter = new StubAdapter() {
            @Override
            public boolean supports(AiModelDefinition definition) {
                return true;
            }

            @Override
            public AiModel create(AiModelDefinition definition) {
                throw cause;
            }
        };

        AiModelException exception = assertThrows(AiModelException.class,
                () -> factory.create(definition(), Collections.singletonList(adapter)));

        assertContext(exception);
        assertSame(cause, exception.getCause());
    }

    @Test
    public void shouldRejectNullModelWithDefinitionContext() {
        AiModelAdapter adapter = new StubAdapter() {
            @Override
            public boolean supports(AiModelDefinition definition) {
                return true;
            }
        };

        AiModelException exception = assertThrows(AiModelException.class,
                () -> factory.create(definition(), Collections.singletonList(adapter)));

        assertContext(exception);
        assertNull(exception.getCause());
    }

    private AiModelDefinition definition() {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey("chat");
        definition.setFramework("fake");
        definition.setProvider("fake-provider");
        definition.setType(AiModelType.CHAT);
        definition.setEnabled(true);
        return definition;
    }

    private void assertContext(AiModelException exception) {
        assertTrue(exception.getMessage().contains("key=chat"));
        assertTrue(exception.getMessage().contains("framework=fake"));
        assertTrue(exception.getMessage().contains("provider=fake-provider"));
        assertTrue(exception.getMessage().contains("type=CHAT"));
    }

    private static class StubAdapter implements AiModelAdapter {

        @Override
        public boolean supports(AiModelDefinition definition) {
            return false;
        }

        @Override
        public AiModel create(AiModelDefinition definition) {
            return null;
        }
    }
}
