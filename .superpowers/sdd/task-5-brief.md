### Task 5: 旧向量插件兼容策略与迁移说明

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/milvus/starter/MagicAPIMilvusConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/qdrant/starter/MagicAPIQdrantConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/redisstack/starter/MagicAPIRedisStackConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/weaviate/starter/MagicAPIWeaviateConfiguration.java`
- Modify: existing `milvus`、`qdrant`、`redisstack`、`weaviate` module classes under `fw-plus-ai-boot-autoconfigure/src/main/java/.../plugin/*/module`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/legacy/LegacyVectorModuleSupport.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/legacy/LegacyVectorModuleSupportTest.java`
- Create: `docs/vector-magicapi-plugin-migration.md`

**Interfaces:**
- Consumes: `EmbeddingStoreRegistry`.
- Produces: old entry behavior that either delegates to registry by key or fails with a migration message.
- Produces: migration documentation from old imports to `import vector`.

- [ ] **Step 1: Write failing compatibility tests**

Create `LegacyVectorModuleSupportTest` and assert:

```java
@Test
public void shouldResolveLegacyProviderKeyThroughUnifiedRegistry() {
    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
    EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
    LegacyVectorModuleSupport support = new LegacyVectorModuleSupport(registry, "milvus");

    VectorStoreOperations operations = support.get("knowledge");

    assertSame(store, operations.getEmbeddingStore());
}

@Test
public void shouldTellUserToMigrateWhenLegacyKeyIsUnknown() {
    LegacyVectorModuleSupport support = new LegacyVectorModuleSupport(emptyRegistry(), "qdrant");

    try {
        support.get("missing");
        fail("Expected MagicAPIVectorException");
    } catch (MagicAPIVectorException ex) {
        assertTrue(ex.getMessage().contains("import vector"));
        assertTrue(ex.getMessage().contains("missing"));
        assertTrue(ex.getMessage().contains("qdrant"));
    }
}
```

- [ ] **Step 2: Run compatibility tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -Dtest=LegacyVectorModuleSupportTest test`

Expected: FAIL because compatibility support does not exist.

- [ ] **Step 3: Implement legacy support**

Implement `LegacyVectorModuleSupport` as a small adapter around `EmbeddingStoreRegistry`. It must not call provider builders, datasource clients, or `RedisStackModule.buildEmbeddingStore(...)`. It returns `VectorStoreOperations` and wraps registry errors into messages like:

```text
Legacy Magic-API vector provider '<provider>' is deprecated; migrate script to import vector and access vector['<key>']. Missing key: <key>
```

- [ ] **Step 4: Deprecate old configurations and modules**

Add `@Deprecated` to old provider Magic-API configuration/module classes. For old module access methods, delegate to `LegacyVectorModuleSupport` when `EmbeddingStoreRegistry` is present. If a legacy module still needs old datasource browsing controllers during the transition, keep controllers isolated from embedding-store construction and document that they are datasource-management only.

- [ ] **Step 5: Prevent duplicate provider construction**

Remove or stop using old Magic-API-side methods that build LangChain4j stores directly, including `RedisStackModule.buildEmbeddingStore(...)`, unless they delegate to Core registry. Add tests or static assertions that old module source does not call provider builders such as:

```bash
cd fw-plus-ai
rg -n "RedisEmbeddingStore\\.builder|MilvusEmbeddingStore\\.builder|QdrantEmbeddingStore\\.builder|WeaviateEmbeddingStore\\.builder" fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin
```

Expected: no output outside Core provider adapter packages.

- [ ] **Step 6: Add migration document**

Create `docs/vector-magicapi-plugin-migration.md` with:

- new configuration example using `fw-plus.ai.langchain4j.embedding-store.enabled/default-store/stores`;
- `import vector` script example;
- old `import milvus`/`import qdrant`/`import redisstack`/`import weaviate` mapping to `vector['key']`;
- note that old entries are deprecated compatibility wrappers.

- [ ] **Step 7: Run compatibility tests**

Run the command from Step 2.

Expected: PASS.

- [ ] **Step 8: Commit**

Commit message: `feat(vector): add legacy magicapi vector compatibility`.

