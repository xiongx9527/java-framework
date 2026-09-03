package dev.langchain4j.agentic.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.internal.PendingResponse;
import dev.langchain4j.agentic.internal.SuspendedResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgenticSuspensionRecoveryTest {

    @AfterEach
    void clearPersistenceProvider() {
        AgenticScopePersister.setStore(null);
    }

    @Test
    void suspendedResponseCanBeCompletedAfterScopeRoundTrip() {
        DefaultAgenticScope original = new DefaultAgenticScope("conversation", DefaultAgenticScope.Kind.PERSISTENT);
        SuspendedResponse<String> response = new SuspendedResponse<String>("approval");
        original.writeState("approval", response);
        original.writeState("alreadyDone", "before-suspension");

        String json = AgenticScopeSerializer.toJson(original);
        assertTrue(json.contains("approval"));
        assertFalse(json.contains("CompletableFuture"));
        DefaultAgenticScope restored = AgenticScopeSerializer.fromJson(json);
        assertEquals("conversation", restored.memoryId());
        assertEquals(Collections.singleton("approval"), restored.pendingResponseIds());
        assertTrue(restored.completePendingResponse("approval", "approved"));
        assertFalse(restored.completePendingResponse("approval", "again"));
        assertEquals("approved", restored.readState("approval"));
        assertEquals("before-suspension", restored.readState("alreadyDone"));
        assertTrue(restored.pendingResponseIds().isEmpty());
    }

    @Test
    void pendingResponseIdsRequireExplicitIdWhenMoreThanOneIsPresent() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        scope.writeState("first", new PendingResponse<String>("first-id"));
        scope.writeState("second", new PendingResponse<String>("second-id"));

        assertEquals(2, scope.pendingResponseIds().size());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> scope.completePendingResponse("value"));
        assertTrue(scope.completePendingResponse("second-id", "second"));
        assertEquals(Collections.singleton("first-id"), scope.pendingResponseIds());
    }

    @Test
    void registryLoadsPersistedScopeAfterInMemoryEviction() {
        MemoryStore store = new MemoryStore();
        AgenticScopePersister.setStore(store);
        AgenticScopeRegistry registry = new AgenticScopeRegistry("agent");
        DefaultAgenticScope created = registry.create("memory");
        created.writeState("checkpoint", "saved");
        registry.update(created);
        registry.clearInMemory();

        DefaultAgenticScope loaded = registry.get("memory");
        assertNotNull(loaded);
        assertEquals("saved", loaded.readState("checkpoint"));
        assertSame(loaded, registry.get("memory"));
        assertEquals(2, store.saveCount.get());
    }

    @Test
    void registryDeleteRemovesMemoryAndPersistentScope() {
        MemoryStore store = new MemoryStore();
        AgenticScopePersister.setStore(store);
        AgenticScopeRegistry registry = new AgenticScopeRegistry("agent");
        registry.create("memory");
        assertNotNull(registry.get("memory"));

        assertTrue(registry.evict("memory", null));
        assertNull(registry.get("memory"));
        assertTrue(store.deleted.contains(new AgenticScopeKey("agent", "memory")));
        assertFalse(registry.evict("memory", null));
    }

    private static final class MemoryStore implements AgenticScopeStore {
        private final Map<AgenticScopeKey, DefaultAgenticScope> values =
                new LinkedHashMap<AgenticScopeKey, DefaultAgenticScope>();
        private final Set<AgenticScopeKey> deleted =
                Collections.newSetFromMap(new LinkedHashMap<AgenticScopeKey, Boolean>());
        private final AtomicInteger saveCount = new AtomicInteger();

        @Override
        public boolean save(AgenticScopeKey key, DefaultAgenticScope scope) {
            values.put(key, scope);
            saveCount.incrementAndGet();
            return true;
        }

        @Override
        public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public boolean delete(AgenticScopeKey key) {
            deleted.add(key);
            return values.remove(key) != null;
        }

        @Override
        public Set<AgenticScopeKey> getAllKeys() {
            return values.keySet();
        }
    }
}
