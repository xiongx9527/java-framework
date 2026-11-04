package com.xiongx9527.fw.plus.ai.base.store;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import com.xiongx9527.fw.plus.ai.base.store.graph.InMemoryGraphStore;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StoreRegistryTest {

    @Test
    public void getByNameReturnsRegistered() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore();
        registry.register("g", store);
        assertSame(store, registry.get("g"));
    }

    @Test
    public void getByTypeCastsSafely() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore();
        registry.register("g", store);
        assertSame(store, registry.get("g", GraphStorePort.class));
    }

    @Test
    public void getByTypeMismatchThrows() {
        StoreRegistry registry = new StoreRegistry();
        registry.register("g", new InMemoryGraphStore());
        try {
            registry.get("g", VectorStorePort.class);
            fail("expected IllegalStateException for type mismatch");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("not of type"));
        }
    }

    @Test
    public void firstByTypeFindsAcrossNames() {
        StoreRegistry registry = new StoreRegistry();
        InMemoryGraphStore store = new InMemoryGraphStore();
        registry.register("other", store);
        assertSame(store, registry.first(GraphStorePort.class));
    }

    @Test
    public void missingNameThrows() {
        StoreRegistry registry = new StoreRegistry();
        try {
            registry.get("nope");
            fail("expected IllegalStateException for missing name");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("nope"));
        }
    }

    @Test
    public void nullNameOrStoreRejected() {
        StoreRegistry registry = new StoreRegistry();
        try {
            registry.register(null, new InMemoryGraphStore());
            fail("expected IllegalArgumentException for null name");
        } catch (IllegalArgumentException expected) {
            // ok
        }
        try {
            registry.register("x", null);
            fail("expected IllegalArgumentException for null store");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void sizeAndContains() {
        StoreRegistry registry = new StoreRegistry();
        registry.register("g", new InMemoryGraphStore());
        assertEquals(1, registry.size());
        assertTrue(registry.contains("g"));
    }
}
