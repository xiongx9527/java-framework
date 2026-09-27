package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.absent;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.present;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReflectiveLangChain4jEmbeddingStoreProviderAdapterTest {

    @Test
    public void shouldBuildAStoreUsingFallbacksAndTypeConversion() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("test", BuilderStore.class.getName(), "test-alias")
                .require("host", "port")
                .builderPlan()
                .property("host", "host")
                .property("port", "port")
                .property("secure", "secure", "tls")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        BuilderStore store = (BuilderStore) adapter.create(definition("test-alias", values(
                "host", "localhost", "port", "6334", "tls", "true")));

        assertEquals("localhost", store.host);
        assertEquals(6334, store.port);
        assertTrue(store.secure);
    }

    @Test
    public void shouldResolveSpringBeanArguments() {
        StaticApplicationContext context = new StaticApplicationContext();
        TestClient client = new TestClient();
        context.getBeanFactory().registerSingleton("testClient", client);
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("bean-store", BeanStore.class.getName())
                .builderPlan()
                .passthrough()
                .beanProperty("client", "clientBean")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, context);

        BeanStore store = (BeanStore) adapter.create(definition("bean-store",
                Collections.<String, Object>singletonMap("clientBean", "testClient")));

        assertSame(client, store.client);
    }

    @Test
    public void shouldSelectTheOnlyMatchingConditionalConstructorPlan() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("constructor", ConstructorStore.class.getName())
                .constructorPlan(absent("name"), property("dimension"))
                .constructorPlan(present("name"), property("name"), property("dimension"))
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        ConstructorStore unnamed = (ConstructorStore) adapter.create(definition("constructor",
                Collections.<String, Object>singletonMap("dimension", "4")));
        ConstructorStore named = (ConstructorStore) adapter.create(definition("constructor",
                values("name", "docs", "dimension", 8)));

        assertEquals(null, unnamed.name);
        assertEquals(4, unnamed.dimension);
        assertEquals("docs", named.name);
        assertEquals(8, named.dimension);
    }

    @Test
    public void shouldInvokeAMultiArgumentBuilderMethod() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("multi", MultiArgumentStore.class.getName())
                .builderPlan()
                .invoke("table", present("table"), property("table"), property("mode"))
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        MultiArgumentStore store = (MultiArgumentStore) adapter.create(definition("multi",
                values("table", "embeddings", "mode", "create-if-missing")));

        assertEquals("embeddings", store.table);
        assertEquals(Mode.CREATE_IF_MISSING, store.mode);
    }

    @Test
    public void shouldTreatAnyNonNullPropertyAsDefinedForConditionalRequirements() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("defined", DriverStore.class.getName())
                .requireWhen(LangChain4jEmbeddingStoreProviderSpec.undefined("driver"), "uri")
                .builderPlan()
                .property("driver", "driver")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);
        Object driver = new Object();

        DriverStore store = (DriverStore) adapter.create(definition("defined",
                Collections.<String, Object>singletonMap("driver", driver)));

        assertSame(driver, store.driver);
    }

    @Test
    public void shouldRejectMissingRequiredProperties() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("required", BuilderStore.class.getName())
                .require("host")
                .auto()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> adapter.create(definition("required", Collections.<String, Object>emptyMap())));

        assertTrue(exception.getMessage().contains("host"));
        assertTrue(exception.getMessage().contains("required"));
        assertFalse(exception.getMessage().contains("secret-value"));
    }

    @Test
    public void shouldPreferTheMoreSpecificOverload() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("specific", SpecificOverloadStore.class.getName())
                .builderPlan()
                .property("value", "value")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        SpecificOverloadStore store = (SpecificOverloadStore) adapter.create(definition("specific",
                Collections.<String, Object>singletonMap("value", "text")));

        assertEquals("string", store.overload);
    }

    @Test
    public void shouldPreferASubtypeInterfaceOverObject() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("interface-specific", InterfaceOverloadStore.class.getName())
                .builderPlan()
                .property("value", "value")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        InterfaceOverloadStore store = (InterfaceOverloadStore) adapter.create(
                definition("interface-specific", Collections.<String, Object>singletonMap("value", "text")));

        assertEquals("char-sequence", store.overload);
    }

    @Test
    public void shouldPreserveImmutableBuilderResultsDuringPassthrough() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("immutable", ImmutableBuilderStore.class.getName())
                .builderPlan()
                .passthrough()
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        ImmutableBuilderStore store = (ImmutableBuilderStore) adapter.create(definition("immutable",
                Collections.<String, Object>singletonMap("host", "localhost")));

        assertEquals("localhost", store.host);
    }

    @Test
    public void shouldRejectAmbiguousConstructorMatches() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("ambiguous", AmbiguousStore.class.getName())
                .constructorPlan(property("value"))
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> adapter.create(definition("ambiguous",
                        Collections.<String, Object>singletonMap("value", "1"))));

        assertTrue(exception.getMessage().contains("Ambiguous"));
    }

    @Test
    public void shouldPreferAnExplicitBlankValueOverAFallback() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("fallback", FallbackStore.class.getName())
                .builderPlan()
                .property("token", "apiKey", "token")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        FallbackStore store = (FallbackStore) adapter.create(definition("fallback",
                values("apiKey", "", "token", "legacy-token")));

        assertEquals("", store.token);
    }

    @Test
    public void shouldApplyAnExplicitBlankOptionalValueWithoutAFallback() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("blank", FallbackStore.class.getName())
                .builderPlan()
                .property("token", "apiKey")
                .end()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        FallbackStore store = (FallbackStore) adapter.create(definition("blank",
                Collections.<String, Object>singletonMap("apiKey", "")));

        assertEquals("", store.token);
    }

    @Test
    public void shouldPreferBuilderOverConstructorArgsInAutoMode() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("hybrid", HybridStore.class.getName())
                .auto()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        HybridStore store = (HybridStore) adapter.create(definition("hybrid",
                values("constructorArgs", Collections.singletonList("constructor"))));

        assertEquals("builder", store.creationPath);
    }

    @Test
    public void shouldUseExplicitConstructorArgumentsWhenTheRawPlanMatches() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("raw", RawStore.class.getName())
                .rawConstructorArgsPlan(present("constructorArgs"))
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        RawStore store = (RawStore) adapter.create(definition("raw",
                values("constructorArgs", Arrays.asList("raw", "7"))));

        assertEquals("raw", store.name);
        assertEquals(7, store.dimension);
    }

    @Test
    public void shouldPreserveTheCauseWithoutExposingItsSensitiveMessage() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("failing", FailingStore.class.getName())
                .auto()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> adapter.create(definition("failing", Collections.<String, Object>emptyMap())));

        assertTrue(exception.getCause() != null);
        assertFalse(exception.getMessage().contains("super-sensitive-token"));
        assertFalse(exception.getMessage().contains("token="));
    }

    @Test
    public void shouldNotSupportMissingProviderClasses() {
        LangChain4jEmbeddingStoreProviderSpec spec = LangChain4jEmbeddingStoreProviderSpec
                .builder("missing", "example.missing.NoSuchEmbeddingStore")
                .auto()
                .build();
        ReflectiveLangChain4jEmbeddingStoreProviderAdapter adapter =
                new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, null);

        assertFalse(adapter.supports(definition("missing", Collections.<String, Object>emptyMap())));
    }

    private AiEmbeddingStoreDefinition definition(String provider, Map<String, Object> properties) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey("knowledge");
        definition.setFramework("langchain4j");
        definition.setProvider(provider);
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(true);
        definition.setProperties(properties);
        return definition;
    }

    private Map<String, Object> values(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    public enum Mode {
        CREATE_IF_MISSING
    }

    public static final class TestClient {
    }

    public static final class BuilderStore extends BaseStore {
        private final String host;
        private final int port;
        private final boolean secure;

        private BuilderStore(Builder builder) {
            this.host = builder.host;
            this.port = builder.port;
            this.secure = builder.secure;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String host;
            private int port;
            private boolean secure;
            public Builder host(String host) { this.host = host; return this; }
            public Builder port(int port) { this.port = port; return this; }
            public Builder secure(boolean secure) { this.secure = secure; return this; }
            public BuilderStore build() { return new BuilderStore(this); }
        }
    }

    public static final class BeanStore extends BaseStore {
        private final TestClient client;
        private BeanStore(Builder builder) { this.client = builder.client; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private TestClient client;
            public Builder client(TestClient client) { this.client = client; return this; }
            public BeanStore build() { return new BeanStore(this); }
        }
    }

    public static final class ConstructorStore extends BaseStore {
        private final String name;
        private final int dimension;
        public ConstructorStore(int dimension) { this(null, dimension); }
        public ConstructorStore(String name, int dimension) { this.name = name; this.dimension = dimension; }
    }

    public static final class MultiArgumentStore extends BaseStore {
        private final String table;
        private final Mode mode;
        private MultiArgumentStore(Builder builder) { this.table = builder.table; this.mode = builder.mode; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private String table;
            private Mode mode;
            public Builder table(String table, Mode mode) { this.table = table; this.mode = mode; return this; }
            public MultiArgumentStore build() { return new MultiArgumentStore(this); }
        }
    }

    public static final class SpecificOverloadStore extends BaseStore {
        private final String overload;
        private SpecificOverloadStore(Builder builder) { this.overload = builder.overload; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private String overload;
            public Builder value(Object value) { this.overload = "object"; return this; }
            public Builder value(String value) { this.overload = "string"; return this; }
            public SpecificOverloadStore build() { return new SpecificOverloadStore(this); }
        }
    }

    public static final class InterfaceOverloadStore extends BaseStore {
        private final String overload;
        private InterfaceOverloadStore(Builder builder) { this.overload = builder.overload; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private String overload;
            public Builder value(Object value) { this.overload = "object"; return this; }
            public Builder value(CharSequence value) { this.overload = "char-sequence"; return this; }
            public InterfaceOverloadStore build() { return new InterfaceOverloadStore(this); }
        }
    }

    public static final class ImmutableBuilderStore extends BaseStore {
        private final String host;
        private ImmutableBuilderStore(String host) { this.host = host; }
        public static Builder builder() { return new Builder(null); }
        public static final class Builder {
            private final String host;
            private Builder(String host) { this.host = host; }
            public Builder host(String host) { return new Builder(host); }
            public ImmutableBuilderStore build() { return new ImmutableBuilderStore(host); }
        }
    }

    public static final class AmbiguousStore extends BaseStore {
        public AmbiguousStore(Integer value) { }
        public AmbiguousStore(Long value) { }
    }

    public static final class DriverStore extends BaseStore {
        private final Object driver;
        private DriverStore(Builder builder) { this.driver = builder.driver; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private Object driver;
            public Builder driver(Object driver) { this.driver = driver; return this; }
            public DriverStore build() { return new DriverStore(this); }
        }
    }

    public static final class FallbackStore extends BaseStore {
        private final String token;
        private FallbackStore(Builder builder) { this.token = builder.token; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private String token;
            public Builder token(String token) { this.token = token; return this; }
            public FallbackStore build() { return new FallbackStore(this); }
        }
    }

    public static final class HybridStore extends BaseStore {
        private final String creationPath;
        public HybridStore(String creationPath) { this.creationPath = creationPath; }
        private HybridStore() { this.creationPath = "builder"; }
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            public HybridStore build() { return new HybridStore(); }
        }
    }

    public static final class FailingStore extends BaseStore {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            public FailingStore build() {
                throw new IllegalStateException("token=super-sensitive-token");
            }
        }
    }

    public static final class RawStore extends BaseStore {
        private final String name;
        private final int dimension;
        public RawStore(String name, int dimension) { this.name = name; this.dimension = dimension; }
    }

    public abstract static class BaseStore implements EmbeddingStore<TextSegment> {
        @Override public String add(Embedding embedding) { return null; }
        @Override public void add(String id, Embedding embedding) { }
        @Override public String add(Embedding embedding, TextSegment embedded) { return null; }
        @Override public List<String> addAll(List<Embedding> embeddings) { return Collections.emptyList(); }
        @Override public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) { return Collections.emptyList(); }
        @Override public void remove(String id) { }
        @Override public void removeAll(java.util.Collection<String> ids) { }
        @Override public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            return new EmbeddingSearchResult<TextSegment>(Collections.emptyList());
        }
    }
}
