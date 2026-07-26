### Task 4: AI Magic-API 统一 vector 插件

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector/src/main/resources/magic-editor/plugins/magic-vector.1.0.0.iife.js`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/starter/MagicAPIVectorConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/module/VectorModule.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/module/VectorStoreOperations.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/exception/MagicAPIVectorException.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/MagicAPIVectorConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/VectorModuleTest.java`

**Interfaces:**
- Consumes: `EmbeddingStoreRegistry`.
- Produces: Magic-API plugin metadata `Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js")`.
- Produces: MagicScript module `vector` supporting default access and `vector['knowledge']` access.
- Produces: `VectorStoreOperations` wrapper around `EmbeddingStore<TextSegment>`.

- [ ] **Step 1: Write failing plugin metadata test**

Create `MagicAPIVectorConfigurationTest`:

```java
@Test
public void shouldExposeSingleVectorPluginMetadata() {
    MagicAPIVectorConfiguration configuration = new MagicAPIVectorConfiguration();

    Plugin plugin = configuration.plugin();

    assertEquals("vector", plugin.getName());
    assertEquals("MagicVector", plugin.getGlobalName());
    assertEquals("magic-vector.1.0.0.iife.js", plugin.getJavascriptFilename());
}
```

- [ ] **Step 2: Write failing module access tests**

Create `VectorModuleTest` with fake registry:

```java
@Test
public void shouldReturnOperationsForExplicitKey() {
    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
    EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
    VectorModule module = new VectorModule(registry);

    VectorStoreOperations operations = module.getDynamicAttribute("knowledge");

    assertSame(store, operations.getEmbeddingStore());
}

@Test
public void shouldReturnDefaultOperationsWhenKeyIsMissing() {
    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
    EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
    VectorModule module = new VectorModule(registry);

    VectorStoreOperations operations = module.getDynamicModule(new MagicScriptContext());

    assertSame(store, operations.getEmbeddingStore());
}
```

Add tests for unknown key and missing default store, expecting `MagicAPIVectorException` messages containing the key or default-store hint.

- [ ] **Step 3: Run AI autoconfigure tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -Dtest=MagicAPIVectorConfigurationTest,VectorModuleTest test`

Expected: FAIL because vector classes do not exist.

- [ ] **Step 4: Add vector plugin starter module**

Add `<module>fw-plus-ai-magicapi-plugin-vector</module>` to `fw-plus-ai-magicapi-plugin/pom.xml`. Create module POM inheriting `fw-plus-ai-magicapi-plugin` and depending on `fw-plus-ai-boot-autoconfigure` plus Core `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` through dependencyManagement.

Add AI BOM entry:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-vector</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 5: Implement vector configuration and module**

`MagicAPIVectorConfiguration` should be conditional on `magic-api.plugin.vector.enabled=true` with `matchIfMissing=true`, and conditional on `EmbeddingStoreRegistry` class/bean. It registers:

- `VectorModule vectorModule(EmbeddingStoreRegistry registry)`;
- `Plugin plugin()` returning `new Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js")`;
- no provider-specific datasource registry.

`VectorModule` should implement `DynamicAttribute<VectorModule, VectorStoreOperations>` and `DynamicModule<VectorStoreOperations>`. Explicit key uses `registry.get(key)`, missing key uses `registry.getDefault()`.

- [ ] **Step 6: Import vector configuration**

Modify `MagicApiPluginAutoConfiguration` to import `MagicAPIVectorConfiguration`. Keep `MagicAPIAiaConfiguration` and old provider configurations until Task 5 handles compatibility.

- [ ] **Step 7: Add minimal Magic-API editor plugin resource**

Create `magic-vector.1.0.0.iife.js` following existing plugin resource naming style. It should expose the global name `MagicVector` and use the same plugin registration pattern as existing `magic-qdrant.1.0.0.iife.js`/`magic-weaviate.1.0.0.iife.js`.

- [ ] **Step 8: Run vector plugin tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 9: Commit**

Commit message: `feat(vector): add unified magicapi vector plugin`.

