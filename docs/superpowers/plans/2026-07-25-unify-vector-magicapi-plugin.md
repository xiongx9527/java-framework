---
change: unify-vector-magicapi-plugin
design-doc: docs/superpowers/specs/2026-07-25-unify-vector-magicapi-plugin-design.md
base-ref: c695308855f1512ebbcf705f965aaa4d2c352765
archived-with: 2026-07-26-unify-vector-magicapi-plugin
---

# 统一向量 Store Starter 与 Magic-API Vector 插件 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 Design Doc `docs/superpowers/specs/2026-07-25-unify-vector-magicapi-plugin-design.md`，在 Core 层提供多向量 store starter/registry/factory，并在 AI Magic-API 侧收敛为单一 `vector` 插件。

**Architecture:** Core 基础模块 `fw-plus-ai-langchain4j-store-embeding` 承载 Java 8 兼容的配置模型、registry、factory、provider adapter 抽象和统一异常；新增 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 负责 Spring Boot 2.7 配置绑定、启动期校验和默认 bean 暴露。AI 层新增 `fw-plus-ai-magicapi-plugin-vector`，脚本侧只通过 `import vector` 访问 Core registry，旧 Milvus/Qdrant/RedisStack/Weaviate 入口保留为 deprecated thin wrapper 或明确迁移提示，不能继续复制 provider 构建逻辑。

**Tech Stack:** Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0、Magic-API 2.2.2、MagicScript 1.9.0、JUnit、Spring Boot Test、Maven Wrapper、OpenSpec。

## Global Constraints

- 必须保持 Java 8 兼容，不使用 `List.of()`、`Map.copyOf()`、无参 `Optional.orElseThrow()` 等 Java 9+ API。
- 必须保持 Spring Boot 2.7.x 路线，不引入 Java 17、Spring Boot 3 或 Spring AI starter 运行时依赖路径。
- 必须保持 LangChain4j 0.35.0 路线，provider 版本由 Core/AI BOM 或 LangChain4j BOM 管理，不在新增模块中硬编码 provider 版本。
- 必须保持现有模块拼写 `embeding`，不在本 change 中改名为 `embedding`。
- Core starter 必须支持 `enabled`、`default-store`、`stores` 多实例配置语义。
- Core registry 必须支持多 provider、多 key、同 provider 多实例、默认 store，并在重复 key、未知 provider、无效默认 store、provider adapter 缺失时给出清晰错误。
- Magic-API 统一插件必须只暴露推荐的一等 `vector` 入口，并通过 Core registry/factory 获取 `EmbeddingStore<TextSegment>`。
- 旧 Milvus、Qdrant、RedisStack、Weaviate Magic-API 入口若保留，只能作为兼容薄壳委托统一 registry 或 `vector` 模块。
- 配置示例和脚本示例只推荐 `import vector` 与按 key 访问方式。
- 使用各顶层目录内 Maven Wrapper 验证，不使用系统 `mvn`。
- 执行本计划时不得修改 OpenSpec tasks 勾选状态；OpenSpec tasks 只作为验收映射。

---

## File Structure

- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/BaseEmbeddingStore.java`
  - 保留现有包装语义，必要时补充 key/provider 只读元数据，不承担 registry 职责。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreProperties.java`
  - 从空壳扩展为多向量配置根对象，保留 `enabled`、`defaultStore`、`stores`。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreDefinition.java`
  - 描述单个 store 的 key、provider、enabled、dimension、metadataKeys 和 provider 专属属性。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreRegistry.java`
  - 对外按 key/default 获取 `EmbeddingStore<TextSegment>`。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreRegistry.java`
  - 不可变 registry 实现，负责 key 列表、默认 key 和错误信息。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreFactory.java`
  - 根据 definition 与 adapter 列表构建 store。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreFactory.java`
  - 默认 factory 实现，选择支持目标 provider 的 adapter。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreProviderAdapter.java`
  - provider adapter SPI。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreException.java`
  - 统一异常。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml`
  - 新 Core starter 模块。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfiguration.java`
  - Spring Boot 2.7 自动配置，绑定 properties、注册 adapter/factory/registry。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/resources/META-INF/spring.factories`
  - Boot 2.7 自动配置注册；不创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
  - 新增 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` module。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
  - 新 starter 进入 dependencyManagement，版本使用 `${project.version}`。
- Create or Modify: Core provider adapter classes under the new starter package `...store.embeding.autoconfigure.provider.*`
  - 注册 RedisStack/Redis、Milvus、Elasticsearch、Qdrant、Weaviate provider adapter。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector/pom.xml`
  - AI Magic-API vector 插件资源模块。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector/src/main/resources/magic-editor/plugins/magic-vector.1.0.0.iife.js`
  - Magic-API 控制台 vector 插件资源。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
  - 新增 vector 子模块，保留旧子模块作为兼容入口。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
  - 新增 `fw-plus-ai-magicapi-plugin-vector` dependencyManagement。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/starter/MagicAPIVectorConfiguration.java`
  - 注册 `Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js")` 和 `VectorModule`。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/module/VectorModule.java`
  - MagicScript `DynamicModule`/`DynamicAttribute` 入口，按 key/default 委托 registry。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/module/VectorStoreOperations.java`
  - 脚本侧暴露的 store 操作包装对象。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/exception/MagicAPIVectorException.java`
  - 将 Core registry/factory 异常转换为清晰 Magic-API 运行时错误。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
  - 导入 `MagicAPIVectorConfiguration`，旧 provider 配置按兼容策略保留或降级。
- Modify: existing `milvus`、`qdrant`、`redisstack`、`weaviate` Magic-API module/configuration classes
  - 标记 deprecated，并移除或隔离重复 provider 构建逻辑，委托 `EmbeddingStoreRegistry`。
- Create: Focused tests under Core and AI autoconfigure modules
  - 覆盖 registry/factory、auto configuration、provider adapter、Magic-API vector 访问和旧入口兼容。

## OpenSpec Task Mapping

- `1.1` -> Task 3
- `1.2` -> Task 3
- `1.3` -> Task 1
- `1.4` -> Task 3
- `1.5` -> Task 1, Task 2, Task 3
- `2.1` -> Task 1
- `2.2` -> Task 2
- `2.3` -> Task 2
- `2.4` -> Task 2, Task 6
- `3.1` -> Task 4
- `3.2` -> Task 4
- `3.3` -> Task 4
- `3.4` -> Task 5
- `3.5` -> Task 4, Task 5
- `4.1` -> Task 5
- `4.2` -> Task 5
- `4.3` -> Task 6
- `4.4` -> Task 6
- `4.5` -> Task 6

### Task 1: Core 多向量 registry/factory 基础模型

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreProperties.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/BaseEmbeddingStore.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreDefinition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreProviderAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreException.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreRegistryTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreFactoryTest.java`

**Interfaces:**
- Produces: `EmbeddingStore<TextSegment> EmbeddingStoreRegistry.get(String key)`
- Produces: `EmbeddingStore<TextSegment> EmbeddingStoreRegistry.getDefault()`
- Produces: `boolean EmbeddingStoreRegistry.contains(String key)`
- Produces: `Set<String> EmbeddingStoreRegistry.keys()`
- Produces: `EmbeddingStore<TextSegment> EmbeddingStoreFactory.create(EmbeddingStoreDefinition definition, List<EmbeddingStoreProviderAdapter> adapters)`
- Produces: `boolean EmbeddingStoreProviderAdapter.supports(String provider)`
- Produces: `EmbeddingStore<TextSegment> EmbeddingStoreProviderAdapter.create(EmbeddingStoreDefinition definition)`

- [x] **Step 1: Write failing registry tests**

Create `DefaultEmbeddingStoreRegistryTest` with fake `EmbeddingStore<TextSegment>` instances and assert:

```java
@Test
public void shouldReturnStoreByKeyAndDefaultStore() {
    EmbeddingStore<TextSegment> knowledge = new InMemoryEmbeddingStore<TextSegment>();
    EmbeddingStore<TextSegment> local = new InMemoryEmbeddingStore<TextSegment>();
    Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
    stores.put("knowledge", knowledge);
    stores.put("local-cache", local);

    EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(stores, "knowledge");

    assertSame(knowledge, registry.get("knowledge"));
    assertSame(local, registry.get("local-cache"));
    assertSame(knowledge, registry.getDefault());
    assertTrue(registry.contains("knowledge"));
    assertTrue(registry.keys().contains("local-cache"));
}
```

Add separate tests for unknown key, missing default key, and null/blank default access.

- [x] **Step 2: Run registry tests and verify failure**

Run: `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -Dtest=DefaultEmbeddingStoreRegistryTest test`

Expected: FAIL because registry classes do not exist.

- [x] **Step 3: Implement registry and exception**

Implement `EmbeddingStoreException` as an unchecked exception carrying a concise message. Implement `DefaultEmbeddingStoreRegistry` with defensive `LinkedHashMap` copy, Java 8 compatible `Collections.unmodifiableMap`, and these exact error messages:

```text
Embedding store key is not registered: <key>
Default embedding store is not configured
Default embedding store key is invalid: <key>
```

- [x] **Step 4: Run registry tests and verify pass**

Run the same command from Step 2.

Expected: PASS.

- [x] **Step 5: Write failing factory tests**

Create `DefaultEmbeddingStoreFactoryTest` with an inner fake adapter. Assert adapter selection by provider, unknown provider failure, and adapter exception message includes provider/key:

```java
@Test
public void shouldCreateStoreWithMatchingProviderAdapter() {
    EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
    definition.setKey("knowledge");
    definition.setProvider("milvus");

    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
    EmbeddingStoreProviderAdapter adapter = new FakeAdapter("milvus", store);

    EmbeddingStoreFactory factory = new DefaultEmbeddingStoreFactory();

    assertSame(store, factory.create(definition, Collections.singletonList(adapter)));
}
```

- [x] **Step 6: Implement definition, properties, adapter SPI, and factory**

`EmbeddingStoreDefinition` must include `key`、`provider`、`enabled`、`dimension`、`metadataKeys`、`properties` fields. `EmbeddingStoreProperties` must include `enabled`、`defaultStore`、`stores` fields and initialize `stores` to an empty `LinkedHashMap`.

- [x] **Step 7: Run factory tests and full base-module tests**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding test
```

Expected: PASS.

- [x] **Step 8: Commit**

Commit message: `feat(vector): add embedding store registry foundation`.

### Task 2: Provider adapter 接入与 provider 配置映射

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/RedisStackEmbeddingStoreProviderAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/MilvusEmbeddingStoreProviderAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/ElasticsearchEmbeddingStoreProviderAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/QdrantEmbeddingStoreProviderAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/WeaviateEmbeddingStoreProviderAdapter.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/provider/EmbeddingStoreProviderAdapterTest.java`

**Interfaces:**
- Consumes: `EmbeddingStoreDefinition.getProvider()` and `EmbeddingStoreDefinition.getProperties()`.
- Produces: provider names `redisstack`、`redis`、`milvus`、`elasticsearch`、`qdrant`、`weaviate`.
- Produces: adapter-specific mapping from generic `properties` keys to LangChain4j builder methods.

- [x] **Step 1: Create starter module skeleton first if Task 3 has not created it**

If `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` does not exist yet, create only the directory and `pom.xml` needed for tests. The final Maven registration is still completed in Task 3.

- [x] **Step 2: Write failing adapter support tests**

Create `EmbeddingStoreProviderAdapterTest` and assert exact provider aliases:

```java
@Test
public void shouldMatchProviderNamesCaseInsensitively() {
    assertTrue(new RedisStackEmbeddingStoreProviderAdapter().supports("redisstack"));
    assertTrue(new RedisStackEmbeddingStoreProviderAdapter().supports("redis"));
    assertTrue(new MilvusEmbeddingStoreProviderAdapter().supports("MILVUS"));
    assertTrue(new ElasticsearchEmbeddingStoreProviderAdapter().supports("elasticsearch"));
    assertTrue(new QdrantEmbeddingStoreProviderAdapter().supports("qdrant"));
    assertTrue(new WeaviateEmbeddingStoreProviderAdapter().supports("weaviate"));
}
```

- [x] **Step 3: Run adapter tests and verify failure**

Run: `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -Dtest=EmbeddingStoreProviderAdapterTest test`

Expected: FAIL because adapter classes do not exist.

- [x] **Step 4: Add provider dependencies without hardcoded versions**

In the starter POM, depend on `fw-plus-ai-langchain4j-store-embeding` and provider artifacts. Use Core dependencyManagement/LangChain4j BOM for versions:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-store-embeding</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-redis</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-milvus</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-elasticsearch</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-qdrant</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-weaviate</artifactId>
</dependency>
```

If `langchain4j-weaviate` is unavailable in LangChain4j `0.35.0`, replace it with the actual `0.35.0` artifact discovered from the local/remote BOM during implementation, and keep the artifact version inherited.

- [x] **Step 5: Implement minimal supports methods**

Implement each adapter with Java 8 compatible string checks and no Spring AI imports. `create(...)` should map only explicit, documented keys such as `host`、`port`、`uri`、`collectionName`、`indexName`、`dimension`、`metadataKeys`、`apiKey`、`grpcHost`、`grpcPort`、`className`、`scheme`、`username`、`password`.

- [x] **Step 6: Write provider builder smoke tests**

Use lightweight configuration definitions to verify each adapter either creates an `EmbeddingStore<TextSegment>` without opening a network connection or fails with a clear `EmbeddingStoreException`. Assertions should check message text, not remote connectivity:

```java
@Test
public void shouldWrapInvalidMilvusConfigurationWithProviderAndKey() {
    EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
    definition.getProperties().put("host", "");

    try {
        new MilvusEmbeddingStoreProviderAdapter().create(definition);
        fail("Expected EmbeddingStoreException");
    } catch (EmbeddingStoreException ex) {
        assertTrue(ex.getMessage().contains("provider=milvus"));
        assertTrue(ex.getMessage().contains("key=knowledge"));
    }
}
```

- [x] **Step 7: Run adapter tests**

Run the command from Step 3.

Expected: PASS.

- [x] **Step 8: Scan provider adapter source for Java 9+ APIs**

Run:

```bash
cd fw-plus-core
rg -n "List\\.of|Map\\.of|Set\\.of|copyOf|orElseThrow\\(\\)" fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java
```

Expected: no output.

- [x] **Step 9: Commit**

Commit message: `feat(vector): add embedding store provider adapters`.


### Task 3: Core Spring Boot starter 与 Maven/BOM 接入

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Create/Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfiguration.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationMetadataTest.java`

**Interfaces:**
- Consumes: `EmbeddingStoreProperties` with prefix `fw-plus.ai.langchain4j.embedding-store`.
- Produces: `EmbeddingStoreFactory` bean when missing.
- Produces: `EmbeddingStoreRegistry` bean when `fw-plus.ai.langchain4j.embedding-store.enabled=true` or missing.
- Produces: Spring Boot 2.7 `spring.factories` registration only.

- [x] **Step 1: Write failing metadata test**

Create `EmbeddingStoreAutoConfigurationMetadataTest`:

```java
@Test
public void shouldRegisterAutoConfigurationInSpringFactoriesOnly() throws Exception {
    String factories = read("META-INF/spring.factories");
    assertTrue(factories.contains("com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.EmbeddingStoreAutoConfiguration"));
    assertFalse(new ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports").exists());
    assertFalse(new ClassPathResource("META-INF/spring/org.springframework.boot.autoconfig.AutoConfiguration.imports").exists());
}
```

- [x] **Step 2: Write failing auto-configuration tests**

Use `ApplicationContextRunner` to cover enabled=false, multiple stores, default store valid, default store invalid, unknown provider:

```java
@Test
public void shouldCreateRegistryWithMultipleStoresAndDefaultKey() {
    this.contextRunner
            .withPropertyValues(
                    "fw-plus.ai.langchain4j.embedding-store.enabled=true",
                    "fw-plus.ai.langchain4j.embedding-store.default-store=knowledge",
                    "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                    "fw-plus.ai.langchain4j.embedding-store.stores.local-cache.provider=fake")
            .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
            .run(context -> {
                assertTrue(context.containsBean("embeddingStoreRegistry"));
                EmbeddingStoreRegistry registry = context.getBean(EmbeddingStoreRegistry.class);
                assertTrue(registry.contains("knowledge"));
                assertTrue(registry.contains("local-cache"));
                assertSame(registry.get("knowledge"), registry.getDefault());
            });
}
```

- [x] **Step 3: Run focused tests and verify failure**

Run: `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -Dtest=EmbeddingStoreAutoConfigurationMetadataTest,EmbeddingStoreAutoConfigurationTest test`

Expected: FAIL because starter module and auto-configuration are incomplete.

- [x] **Step 4: Register Maven module and dependencyManagement**

Add module after `fw-plus-ai-langchain4j-store-embeding` in `fw-plus-ai-langchain4j/pom.xml`:

```xml
<module>fw-plus-ai-langchain4j-store-embeding-spring-boot-starter</module>
```

Add Core BOM entry near existing LangChain4j modules:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-store-embeding-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [x] **Step 5: Implement auto-configuration**

`EmbeddingStoreAutoConfiguration` should be `@Configuration`, `@EnableConfigurationProperties(EmbeddingStoreProperties.class)`, `@ConditionalOnProperty(prefix = "fw-plus.ai.langchain4j.embedding-store", name = "enabled", havingValue = "true", matchIfMissing = true)`. It should:

- register `DefaultEmbeddingStoreFactory` when missing;
- collect all `EmbeddingStoreProviderAdapter` beans;
- build enabled store definitions from `properties.getStores()`;
- copy map key into `EmbeddingStoreDefinition.key` when the object key is blank;
- validate duplicate key, blank provider, invalid default store, unknown provider;
- expose a single `EmbeddingStoreRegistry` bean.

- [x] **Step 6: Add `spring.factories`**

Create:

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.EmbeddingStoreAutoConfiguration
```

- [x] **Step 7: Run starter tests**

Run the command from Step 3.

Expected: PASS.

- [x] **Step 8: Run Core LangChain4j reactor build**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am test
```

Expected: BUILD SUCCESS.

- [x] **Step 9: Commit**

Commit message: `feat(vector): add embedding store spring boot starter`.

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

- [x] **Step 1: Write failing plugin metadata test**

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

- [x] **Step 2: Write failing module access tests**

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

- [x] **Step 3: Run AI autoconfigure tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -Dtest=MagicAPIVectorConfigurationTest,VectorModuleTest test`

Expected: FAIL because vector classes do not exist.

- [x] **Step 4: Add vector plugin starter module**

Add `<module>fw-plus-ai-magicapi-plugin-vector</module>` to `fw-plus-ai-magicapi-plugin/pom.xml`. Create module POM inheriting `fw-plus-ai-magicapi-plugin` and depending on `fw-plus-ai-boot-autoconfigure` plus Core `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` through dependencyManagement.

Add AI BOM entry:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-vector</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [x] **Step 5: Implement vector configuration and module**

`MagicAPIVectorConfiguration` should be conditional on `magic-api.plugin.vector.enabled=true` with `matchIfMissing=true`, and conditional on `EmbeddingStoreRegistry` class/bean. It registers:

- `VectorModule vectorModule(EmbeddingStoreRegistry registry)`;
- `Plugin plugin()` returning `new Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js")`;
- no provider-specific datasource registry.

`VectorModule` should implement `DynamicAttribute<VectorModule, VectorStoreOperations>` and `DynamicModule<VectorStoreOperations>`. Explicit key uses `registry.get(key)`, missing key uses `registry.getDefault()`.

- [x] **Step 6: Import vector configuration**

Modify `MagicApiPluginAutoConfiguration` to import `MagicAPIVectorConfiguration`. Keep `MagicAPIAiaConfiguration` and old provider configurations until Task 5 handles compatibility.

- [x] **Step 7: Add minimal Magic-API editor plugin resource**

Create `magic-vector.1.0.0.iife.js` following existing plugin resource naming style. It should expose the global name `MagicVector` and use the same plugin registration pattern as existing `magic-qdrant.1.0.0.iife.js`/`magic-weaviate.1.0.0.iife.js`.

- [x] **Step 8: Run vector plugin tests**

Run the command from Step 3.

Expected: PASS.

- [x] **Step 9: Commit**

Commit message: `feat(vector): add unified magicapi vector plugin`.

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

- [x] **Step 1: Write failing compatibility tests**

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

- [x] **Step 2: Run compatibility tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -Dtest=LegacyVectorModuleSupportTest test`

Expected: FAIL because compatibility support does not exist.

- [x] **Step 3: Implement legacy support**

Implement `LegacyVectorModuleSupport` as a small adapter around `EmbeddingStoreRegistry`. It must not call provider builders, datasource clients, or `RedisStackModule.buildEmbeddingStore(...)`. It returns `VectorStoreOperations` and wraps registry errors into messages like:

```text
Legacy Magic-API vector provider '<provider>' is deprecated; migrate script to import vector and access vector['<key>']. Missing key: <key>
```

- [x] **Step 4: Deprecate old configurations and modules**

Add `@Deprecated` to old provider Magic-API configuration/module classes. For old module access methods, delegate to `LegacyVectorModuleSupport` when `EmbeddingStoreRegistry` is present. If a legacy module still needs old datasource browsing controllers during the transition, keep controllers isolated from embedding-store construction and document that they are datasource-management only.

- [x] **Step 5: Prevent duplicate provider construction**

Remove or stop using old Magic-API-side methods that build LangChain4j stores directly, including `RedisStackModule.buildEmbeddingStore(...)`, unless they delegate to Core registry. Add tests or static assertions that old module source does not call provider builders such as:

```bash
cd fw-plus-ai
rg -n "RedisEmbeddingStore\\.builder|MilvusEmbeddingStore\\.builder|QdrantEmbeddingStore\\.builder|WeaviateEmbeddingStore\\.builder" fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin
```

Expected: no output outside Core provider adapter packages.

- [x] **Step 6: Add migration document**

Create `docs/vector-magicapi-plugin-migration.md` with:

- new configuration example using `fw-plus.ai.langchain4j.embedding-store.enabled/default-store/stores`;
- `import vector` script example;
- old `import milvus`/`import qdrant`/`import redisstack`/`import weaviate` mapping to `vector['key']`;
- note that old entries are deprecated compatibility wrappers.

- [x] **Step 7: Run compatibility tests**

Run the command from Step 2.

Expected: PASS.

- [x] **Step 8: Commit**

Commit message: `feat(vector): add legacy magicapi vector compatibility`.

### Task 6: 端到端验证、兼容扫描与 OpenSpec 校验

**Files:**
- Modify: none required unless verification exposes defects in files changed by Tasks 1-5.
- Test evidence: Maven output and OpenSpec validation output.

**Interfaces:**
- Consumes: all tasks above.
- Produces: verified Core build, AI build, Java 8 compatibility scan, and strict OpenSpec validation.

- [x] **Step 1: Run Core focused reactor tests**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am test
```

Expected: BUILD SUCCESS.

- [x] **Step 2: Run Core LangChain4j package build**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j -am -DskipTests package
```

Expected: BUILD SUCCESS.

- [x] **Step 3: Run AI Magic-API focused tests**

Run:

```bash
cd fw-plus-ai
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure,fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector -am test
```

Expected: BUILD SUCCESS.

- [x] **Step 4: Run AI plugin package build**

Run:

```bash
cd fw-plus-ai
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin -am -DskipTests package
```

Expected: BUILD SUCCESS.

- [x] **Step 5: Scan for Java 9+ APIs in touched Java packages**

Run:

```bash
rg -n "List\\.of|Map\\.of|Set\\.of|copyOf|orElseThrow\\(\\)" \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector
```

Expected: no output.

- [x] **Step 6: Scan Magic-API plugin code for duplicated provider builders**

Run:

```bash
rg -n "RedisEmbeddingStore\\.builder|MilvusEmbeddingStore\\.builder|QdrantEmbeddingStore\\.builder|WeaviateEmbeddingStore\\.builder" \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin
```

Expected: no output outside compatibility comments or Core provider adapter references.

- [x] **Step 7: Validate OpenSpec**

Run:

```bash
openspec validate unify-vector-magicapi-plugin --strict
```

Expected: passes with no validation errors.

- [x] **Step 8: Review OpenSpec task coverage without modifying tasks.md**

Open `openspec/changes/unify-vector-magicapi-plugin/tasks.md` and verify every item maps to `OpenSpec Task Mapping` in this plan. Do not change checkbox state in `tasks.md`.

- [x] **Step 9: Commit**

Commit message: `test(vector): verify unified vector plugin change`.

## Self-Review

- Spec coverage: Design Doc goals are covered by Task 1-5; OpenSpec tasks `1.1` through `4.5` are mapped explicitly.
- Placeholder scan: No task uses deferred-work marker language; unresolved artifact names such as `langchain4j-weaviate` are handled by an implementation-time dependency discovery step with a concrete fallback rule.
- Type consistency: `EmbeddingStoreDefinition`、`EmbeddingStoreRegistry`、`EmbeddingStoreFactory`、`EmbeddingStoreProviderAdapter`、`VectorModule`、`VectorStoreOperations` names are introduced before downstream tasks consume them.
- Compatibility strategy: Plan chooses the lower-risk Design Doc path, keeping old Magic-API provider entries as deprecated thin wrappers unless implementation proves a wrapper causes Maven cycles.
