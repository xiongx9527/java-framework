package com.xiongx9527.fw.plus.ai.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAiModelFactoryTest {

    private final DefaultAiModelFactory factory = new DefaultAiModelFactory();

    @Test
    public void shouldSelectOnlyCandidateWhenFrameworkIsBlank() {
        AiModelAdapter unsupported = new StubAdapter("unsupported", false);
        AiModelAdapter supported = new StubAdapter("only", true);
        AiModelDefinition definition = definition();
        definition.setFramework(" ");

        AiModel model = factory.create(definition, Arrays.asList(unsupported, supported));

        assertSame(((StubAdapter) supported).model, model);
    }

    @Test
    public void shouldRejectMultipleCandidatesWhenFrameworkIsBlank() {
        AiModelDefinition definition = definition();
        definition.setFramework(null);

        AiModelException exception = assertThrows(AiModelException.class,
                () -> factory.create(definition, Arrays.<AiModelAdapter>asList(
                        new StubAdapter("second", true), new StubAdapter("first", true))));

        assertContext(exception, definition);
        assertTrue(exception.getMessage().contains("candidates=[first, second]"));
    }

    @Test
    public void shouldRejectMissingExplicitFramework() {
        AiModelDefinition definition = definition();
        definition.setFramework("missing");

        AiModelException exception = assertThrows(AiModelException.class,
                () -> factory.create(definition, Arrays.<AiModelAdapter>asList(
                        new StubAdapter("second", true), new StubAdapter("first", true))));

        assertContext(exception, definition);
        assertTrue(exception.getMessage().contains("candidates=[first, second]"));
    }

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
        assertContext(exception, definition());
    }

    private void assertContext(AiModelException exception, AiModelDefinition definition) {
        assertTrue(exception.getMessage().contains("key=" + definition.getKey()));
        assertTrue(exception.getMessage().contains("framework=" + definition.getFramework()));
        assertTrue(exception.getMessage().contains("provider=" + definition.getProvider()));
        assertTrue(exception.getMessage().contains("type=" + definition.getType()));
    }

    private static class StubAdapter implements AiModelAdapter {

        private final String framework;

        private final boolean supported;

        private final AiModel model = new AiModel() {
            @Override
            public String getKey() {
                return "chat";
            }

            @Override
            public String getFramework() {
                return framework;
            }

            @Override
            public String getProvider() {
                return "fake-provider";
            }

            @Override
            public AiModelType getType() {
                return AiModelType.CHAT;
            }
        };

        private StubAdapter() {
            this("fake", false);
        }

        private StubAdapter(String framework, boolean supported) {
            this.framework = framework;
            this.supported = supported;
        }

        @Override
        public String getFramework() {
            return framework;
        }

        @Override
        public boolean supports(AiModelDefinition definition) {
            return supported;
        }

        @Override
        public AiModel create(AiModelDefinition definition) {
            return supported ? model : null;
        }
    }
}
