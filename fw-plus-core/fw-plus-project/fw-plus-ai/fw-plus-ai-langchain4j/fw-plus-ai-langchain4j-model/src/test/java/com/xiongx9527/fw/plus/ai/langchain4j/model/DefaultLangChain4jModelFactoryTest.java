package com.xiongx9527.fw.plus.ai.langchain4j.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultLangChain4jModelFactoryTest {

    private final DefaultLangChain4jModelFactory factory = new DefaultLangChain4jModelFactory();

    @Test
    public void shouldCreateModelWithFirstSupportedAdapter() {
        LangChain4jModelDefinition definition = definition("chat");
        LangChain4jModel expected = new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object());

        LangChain4jModel model = factory.create(definition, Arrays.<LangChain4jModelProviderAdapter>asList(
                new FakeAdapter(false, null),
                new FakeAdapter(true, expected)
        ));

        assertSame(expected, model);
    }

    @Test
    public void shouldFailWhenDefinitionIsInvalid() {
        assertThrows(LangChain4jModelException.class,
                () -> factory.create(null, Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertInvalidDefinition("key", null, "openai", LangChain4jModelType.CHAT);
        assertInvalidDefinition("provider", "chat", " ", LangChain4jModelType.CHAT);
        assertInvalidDefinition("modelType", "chat", "openai", null);
    }

    @Test
    public void shouldFailWhenProviderIsUnsupported() {
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition("chat"), Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertContext(exception);
    }

    @Test
    public void shouldWrapAdapterExceptionWithContext() {
        ThrowingAdapter adapter = new ThrowingAdapter();
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition("chat"),
                        Collections.<LangChain4jModelProviderAdapter>singletonList(adapter)));

        assertContext(exception);
        assertSame(adapter.failure, exception.getCause());
    }

    @Test
    public void shouldFailWhenAdapterReturnsNull() {
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition("chat"),
                        Collections.<LangChain4jModelProviderAdapter>singletonList(new FakeAdapter(true, null))));

        assertTrue(exception.getMessage().contains("returned null"));
        assertContext(exception);
    }

    private void assertInvalidDefinition(String field, String key, String provider, LangChain4jModelType modelType) {
        LangChain4jModelDefinition definition = definition(key);
        definition.setProvider(provider);
        definition.setModelType(modelType);

        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition, Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertTrue(exception.getMessage().contains(field));
    }

    private void assertContext(LangChain4jModelException exception) {
        assertTrue(exception.getMessage().contains("chat"));
        assertTrue(exception.getMessage().contains("openai"));
        assertTrue(exception.getMessage().contains("CHAT"));
    }

    private LangChain4jModelDefinition definition(String key) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey(key);
        definition.setProvider("openai");
        definition.setModelType(LangChain4jModelType.CHAT);
        definition.setEnabled(true);
        return definition;
    }

    private static class FakeAdapter implements LangChain4jModelProviderAdapter {
        private final boolean supports;
        private final LangChain4jModel model;

        private FakeAdapter(boolean supports, LangChain4jModel model) {
            this.supports = supports;
            this.model = model;
        }

        public boolean supports(LangChain4jModelDefinition definition) {
            return supports;
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            return model;
        }
    }

    private static class ThrowingAdapter implements LangChain4jModelProviderAdapter {
        private final LangChain4jModelException failure = new LangChain4jModelException("boom");

        public boolean supports(LangChain4jModelDefinition definition) {
            return true;
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            throw failure;
        }
    }
}
