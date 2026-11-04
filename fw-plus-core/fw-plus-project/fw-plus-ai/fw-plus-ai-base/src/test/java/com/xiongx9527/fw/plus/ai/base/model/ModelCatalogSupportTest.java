package com.xiongx9527.fw.plus.ai.base.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCatalogSupportTest {

    private static ModelInfo info(String id, ModelType type) {
        return ModelInfo.builder().id(id).type(type).build();
    }

    @Test
    void ofTypeKeepsOnlyMatchingCapability() {
        List<ModelInfo> all = Arrays.asList(
                info("gpt-4o", ModelType.LLM),
                info("text-embedding-3-small", ModelType.EMBEDDING),
                info("bge-reranker", ModelType.RERANK));

        assertEquals(1, ModelCatalogSupport.ofType(all, ModelType.LLM).size());
        assertEquals("text-embedding-3-small",
                ModelCatalogSupport.ofType(all, ModelType.EMBEDDING).get(0).getId());
        assertTrue(ModelCatalogSupport.ofType(null, ModelType.LLM).isEmpty());
    }

    @Test
    void findPrefersExactMatchThenIgnoresCase() {
        List<ModelInfo> all = Arrays.asList(info("GPT-4o", ModelType.LLM), info("gpt-4o", ModelType.LLM));

        assertEquals("gpt-4o", ModelCatalogSupport.find(all, "gpt-4o").getId());
        assertEquals("GPT-4o", ModelCatalogSupport.find(all, "gpt-4O").getId());
        assertNull(ModelCatalogSupport.find(all, "missing"));
        assertNull(ModelCatalogSupport.find(null, "gpt-4o"));
    }

    @Test
    void validateRejectsBlankModelId() {
        ModelValidation result = ModelCatalogSupport.validate(ModelType.LLM, Collections.emptyList(), " ", 1L);

        assertFalse(result.isValid());
        assertEquals("model id is required", result.getMessage());
        assertEquals(1L, result.getLatencyMillis());
    }

    @Test
    void validateAcceptsCatalogHit() {
        List<ModelInfo> catalog = Collections.singletonList(info("gpt-4o", ModelType.LLM));

        ModelValidation result = ModelCatalogSupport.validate(ModelType.LLM, catalog, "gpt-4o", 7L);

        assertTrue(result.isValid());
        assertNull(result.getMessage());
        assertEquals("gpt-4o", result.getModel());
    }

    @Test
    void validateExplainsEmptyCatalogVersusMissingModel() {
        ModelValidation empty = ModelCatalogSupport.validate(ModelType.EMBEDDING, null, "bge-m3", 0L);
        assertFalse(empty.isValid());
        assertEquals("no embedding model available from provider", empty.getMessage());

        List<ModelInfo> catalog = Collections.singletonList(info("gpt-4o", ModelType.LLM));
        ModelValidation missing = ModelCatalogSupport.validate(ModelType.LLM, catalog, "gpt-3.5", 0L);
        assertFalse(missing.isValid());
        assertEquals("model not found in provider catalog", missing.getMessage());
    }
}
