package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddingStorePropertiesTest {

    @Test
    public void shouldInitializeStoresAsEmptyLinkedHashMap() {
        EmbeddingStoreProperties properties = new EmbeddingStoreProperties();

        assertNotNull(properties.getStores());
        assertTrue(properties.getStores().isEmpty());
        assertTrue(properties.getStores() instanceof LinkedHashMap);
    }
}
