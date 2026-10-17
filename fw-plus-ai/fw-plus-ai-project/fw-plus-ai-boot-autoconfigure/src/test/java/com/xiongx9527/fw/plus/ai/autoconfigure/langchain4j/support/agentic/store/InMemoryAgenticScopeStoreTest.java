package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store;

import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InMemoryAgenticScopeStore}：进程内引用语义（不做序列化）、基础 CRUD 与快照行为。
 */
class InMemoryAgenticScopeStoreTest {

    private final InMemoryAgenticScopeStore store = new InMemoryAgenticScopeStore();

    private static DefaultAgenticScope scopeWith(String key, Object value) {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        scope.writeState(key, value);
        return scope;
    }

    @Test
    void saveLoadDeleteShouldRoundTrip() {
        AgenticScopeKey key = new AgenticScopeKey("agent-1", "memory-1");
        DefaultAgenticScope scope = scopeWith("answer", "hello");

        assertThat(store.save(key, scope)).isTrue();
        assertThat(store.load(key)).contains(scope);
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.getAllKeys()).containsExactly(key);

        assertThat(store.delete(key)).isTrue();
        assertThat(store.delete(key)).isFalse();
        assertThat(store.load(key)).isEmpty();
        assertThat(store.size()).isZero();
    }

    @Test
    void loadShouldReturnEmptyForUnknownKey() {
        assertThat(store.load(new AgenticScopeKey("x", "y"))).isEmpty();
    }

    @Test
    void saveShouldOverwriteExistingScope() {
        AgenticScopeKey key = new AgenticScopeKey("a", 1);
        store.save(key, scopeWith("v", 1));
        store.save(key, scopeWith("v", 2));

        assertThat(store.load(key).orElseThrow().readState("v")).isEqualTo(2);
    }

    @Test
    void getAllKeysShouldBeASnapshot() {
        AgenticScopeKey key = new AgenticScopeKey("a", "m");
        store.save(key, scopeWith("v", 1));

        Set<AgenticScopeKey> keys = store.getAllKeys();
        store.delete(key);

        assertThat(keys).containsExactly(key);
        assertThat(store.getAllKeys()).isEmpty();
    }

    @Test
    void clearShouldEmptyStore() {
        store.save(new AgenticScopeKey("a", "m"), scopeWith("v", 1));

        store.clear();

        assertThat(store.size()).isZero();
    }
}
