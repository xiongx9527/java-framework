package com.xiongx9527.fw.plus.ai.base.store;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import com.xiongx9527.fw.plus.ai.base.store.graph.InMemoryGraphStore;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StoreRegistryTest {

    @Test
    public void getByNameReturnsRegistered() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        registry.register("g", store);
        assertSame(store, registry.get("g"));
    }

    @Test
    public void getByTypeCastsSafely() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        registry.register("g", store);
        assertSame(store, registry.get("g", GraphStorePort.class));
    }

    @Test
    public void getByTypeMismatchThrows() {
        StoreRegistry registry = new StoreRegistry();
        registry.register("g", new InMemoryGraphStore("g"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> registry.get("g", VectorStorePort.class));
        assertTrue(ex.getMessage().contains("not of type"));
    }

    @Test
    public void firstByTypeFindsAcrossNames() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore("other");
        registry.register("other", store);
        assertSame(store, registry.first(GraphStorePort.class));
    }

    @Test
    public void getByRecordStoreType() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore("g");
        registry.register("g", store);
        assertSame(store, registry.get("g", RecordStore.class));
    }

    @Test
    public void missingNameThrows() {
        StoreRegistry registry = new StoreRegistry();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registry.get("nope"));
        assertTrue(ex.getMessage().contains("nope"));
    }

    @Test
    public void nullNameOrStoreRejected() {
        StoreRegistry registry = new StoreRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(null, new InMemoryGraphStore("g")));
        assertThrows(IllegalArgumentException.class, () -> registry.register("x", null));
    }

    @Test
    public void sizeAndContains() {
        StoreRegistry registry = new StoreRegistry();
        registry.register("g", new InMemoryGraphStore("g"));
        assertEquals(1, registry.size());
        assertTrue(registry.contains("g"));
    }
}
