package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnconfiguredVectorStorePortTest {

    private final UnconfiguredVectorStorePort port = new UnconfiguredVectorStorePort();
    private static final String MSG =
            "vector store is not configured; set fw.plus.ai.vector-store.base-url to enable it";

    @Test
    void metadataReportsUnconfiguredPlaceholder() {
        assertFalse(port.configured());
        assertEquals("unconfigured", port.name());
        assertEquals(StoreType.VECTOR, port.type());
        assertEquals("default", port.defaultCollection());
    }

    @Test
    void allReadWriteOperationsThrowIllegalStateException() {
        assertThrows(IllegalStateException.class, port::health);
        assertThrows(IllegalStateException.class, () -> port.ensureCollection("c", 4));
        assertThrows(IllegalStateException.class, () -> port.dropCollection("c"));
        assertThrows(IllegalStateException.class, () -> port.upsert("c", "id", new double[]{1.0}, null));
        assertThrows(IllegalStateException.class, () -> port.delete("c", "id"));
        assertThrows(IllegalStateException.class,
                () -> port.search(VectorSearchRequest.builder().collection("c").vector(new double[]{1.0}).build()));
    }

    @Test
    void exceptionMessageGuidesConfiguration() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, port::health);
        assertTrue(ex.getMessage().contains(MSG), ex.getMessage());
    }
}
