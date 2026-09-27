package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LangChain4jEmbeddingStoreProviderSpecTest {

    @Test
    public void shouldRejectDuplicateProviderAliases() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LangChain4jEmbeddingStoreProviderSpec.builder(
                        "test", TestStore.class.getName(), "TEST"));

        assertTrue(exception.getMessage().contains("Duplicate provider alias"));
    }

    @Test
    public void shouldRequireACreationPlan() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LangChain4jEmbeddingStoreProviderSpec.builder(
                        "test", TestStore.class.getName()).build());

        assertTrue(exception.getMessage().contains("creation plan"));
    }

    @Test
    public void shouldRejectOverlappingUnconditionalCreationPlans() {
        LangChain4jEmbeddingStoreProviderSpec.Builder builder =
                LangChain4jEmbeddingStoreProviderSpec.builder(
                        "test", TestStore.class.getName())
                        .auto()
                        .constructorPlan();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        assertTrue(exception.getMessage().contains("creation plan"));
    }

    @Test
    public void shouldRejectAnUnconditionalPlanCombinedWithAConditionalPlan() {
        LangChain4jEmbeddingStoreProviderSpec.Builder builder =
                LangChain4jEmbeddingStoreProviderSpec.builder(
                        "test", TestStore.class.getName())
                        .auto()
                        .constructorPlan(LangChain4jEmbeddingStoreProviderSpec.present("name"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        assertTrue(exception.getMessage().contains("creation plan"));
    }

    @Test
    public void shouldRejectOverlappingConditionalPlans() {
        assertOverlappingPlansRejected(
                LangChain4jEmbeddingStoreProviderSpec.present("name"),
                LangChain4jEmbeddingStoreProviderSpec.present("name"));
        assertOverlappingPlansRejected(
                LangChain4jEmbeddingStoreProviderSpec.present("name"),
                LangChain4jEmbeddingStoreProviderSpec.defined("name"));
        assertOverlappingPlansRejected(
                LangChain4jEmbeddingStoreProviderSpec.absent("name"),
                LangChain4jEmbeddingStoreProviderSpec.undefined("name"));
    }

    @Test
    public void shouldAllowMutuallyExclusiveConditionalPlans() {
        LangChain4jEmbeddingStoreProviderSpec spec =
                LangChain4jEmbeddingStoreProviderSpec.builder("test", TestStore.class.getName())
                        .constructorPlan(LangChain4jEmbeddingStoreProviderSpec.present("name"))
                        .constructorPlan(LangChain4jEmbeddingStoreProviderSpec.absent("name"))
                        .build();

        assertEquals(2, spec.getCreationPlans().size());
    }

    @Test
    public void shouldNormalizeProviderAliasesAndExposeImmutableValues() {
        LangChain4jEmbeddingStoreProviderSpec spec =
                LangChain4jEmbeddingStoreProviderSpec.builder(
                        "Test", TestStore.class.getName(), "TEST-ALIAS")
                        .require("host")
                        .auto()
                        .build();

        assertEquals(new java.util.LinkedHashSet<String>(
                java.util.Arrays.asList("test", "test-alias")), spec.getProviders());
        assertThrows(UnsupportedOperationException.class,
                () -> spec.getProviders().add("other"));
        assertThrows(UnsupportedOperationException.class,
                () -> spec.getCreationPlans().add(null));
        assertEquals(Collections.singletonList("host"),
                spec.getRequirements().get(0).getProperties());
    }

    private void assertOverlappingPlansRejected(
            LangChain4jEmbeddingStoreProviderSpec.Condition first,
            LangChain4jEmbeddingStoreProviderSpec.Condition second) {
        LangChain4jEmbeddingStoreProviderSpec.Builder builder =
                LangChain4jEmbeddingStoreProviderSpec.builder("test", TestStore.class.getName())
                        .constructorPlan(first)
                        .constructorPlan(second);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        assertTrue(exception.getMessage().contains("creation plan"));
    }

    public static final class TestStore {
    }
}
