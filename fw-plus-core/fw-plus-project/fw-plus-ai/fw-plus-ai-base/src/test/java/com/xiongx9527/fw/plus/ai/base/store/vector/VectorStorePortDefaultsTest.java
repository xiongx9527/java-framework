package com.xiongx9527.fw.plus.ai.base.store.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.RecordStore;
import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 锁定 {@link VectorStorePort} 只实现最小 {@link Store} 契约，及其便利方法的既定语义。 */
class VectorStorePortDefaultsTest {

    private static class RecordingStore implements VectorStorePort {

        String dropped;

        final List<String> upserted = new ArrayList<String>();

        final List<String> deleted = new ArrayList<String>();

        @Override
        public String name() {
            return "fake";
        }

        @Override
        public StoreType type() {
            return StoreType.VECTOR;
        }

        @Override
        public boolean health() {
            return true;
        }

        @Override
        public String defaultCollection() {
            return "default";
        }

        @Override
        public void ensureCollection(String collection, int dimension) {
        }

        @Override
        public void dropCollection(String collection) {
            dropped = collection;
        }

        @Override
        public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
            upserted.add(id);
        }

        @Override
        public void delete(String collection, String id) {
            deleted.add(id);
        }

        @Override
        public VectorSearchResult search(VectorSearchRequest request) {
            return VectorSearchResult.empty();
        }
    }

    @Test
    void storeContractOnlyCoversIdentityAndLifecycle() {
        assertTrue(Store.class.isAssignableFrom(VectorStorePort.class));
        assertFalse(RecordStore.class.isAssignableFrom(VectorStorePort.class));
        assertTrue(RecordStore.class.isAssignableFrom(GraphStorePort.class));
        assertThrows(NoSuchMethodException.class, () -> Store.class.getMethod("get", String.class));
        assertThrows(NoSuchMethodException.class, () -> Store.class.getMethod("save", StoreRecord.class));
        assertThrows(NoSuchMethodException.class, () -> Store.class.getMethod("search", StoreQuery.class));
    }

    @Test
    void saveDelegatesToUpsertOnDefaultCollection() {
        RecordingStore store = new RecordingStore();
        store.save(StoreRecord.builder().id("a").vector(new double[]{1.0}).build());
        assertEquals(Collections.singletonList("a"), store.upserted);
    }

    @Test
    void deleteAlwaysReportsTrueButDelegates() {
        RecordingStore store = new RecordingStore();
        assertTrue(store.delete("a"));
        assertEquals(Collections.singletonList("a"), store.deleted);
        assertFalse(store.delete(null));
    }

    @Test
    void getByIdIsExplicitlyUnsupported() {
        RecordingStore store = new RecordingStore();
        assertThrows(UnsupportedOperationException.class, () -> store.get("a"));
    }

    @Test
    void searchRequiresVector() {
        RecordingStore store = new RecordingStore();
        assertThrows(UnsupportedOperationException.class,
                () -> store.search(StoreQuery.builder().text("q").build()));
    }

    @Test
    void clearDropsDefaultCollectionAndCountIsUnknown() {
        RecordingStore store = new RecordingStore();
        store.clear();
        assertEquals("default", store.dropped);
        assertEquals(-1L, store.count());
    }

    @Test
    void updateIsAnIdempotentSave() {
        RecordingStore store = new RecordingStore();
        store.update(StoreRecord.builder().id("a").vector(new double[]{1.0}).build());
        assertEquals(Collections.singletonList("a"), store.upserted);
    }
}
