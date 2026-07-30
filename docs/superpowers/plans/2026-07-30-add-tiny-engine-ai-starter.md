---
change: add-tiny-engine-ai-starter
design-doc: docs/superpowers/specs/2026-07-30-add-tiny-engine-ai-starter-design.md
base-ref: 0c0a12fa996bbdb4f327b77faa28c55fe9aeda72
---

# TinyEngine AI Starter 与 AskData 中立 AI 依赖 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 TinyEngine AI starter，并把 AskData 默认模型/向量主路径对齐到 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 中立 API。

**Architecture:** TinyEngine AI 以独立 starter + autoconfigure + provider SPI 承载业务契约，默认 LangChain4j 只在 adapter/provider 层出现。AskData 保留 `AskDataModelRegistry` 与 `AskDataVectorRetriever` 业务边界，但默认实现改由 `AiModelRegistry` 与 `AiEmbeddingStoreRegistry` 提供能力。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Maven Wrapper, JUnit, `fw-plus-ai-model`, `fw-plus-ai-store-embedding`, `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。

## Global Constraints

- 产物和注释使用简体中文；Java 公开 API 保持 Java 8 兼容。
- 不使用 `List.of()`、`Map.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient`。
- 不升级 Spring Boot、LangChain4j、Spring AI、Magic-API 或主线 BOM 版本。
- TinyEngine AI starter 和 AskData starter 不得依赖旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding` 或旧 LangChain4j model/store starter 作为主路径。
- Spring AI、Spring AI Alibaba、AgentScope2 Java 版只能作为后续独立 Provider，不进入首版必需 classpath。
- AskData 本 change 只处理当前中立 AI 模型/向量依赖边界，不重做 SQLBot、治理、权限、观测、存储或 API/MCP 业务功能。

---

## File Structure

**新增 TinyEngine AI starter 与自动配置：**
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml`：TinyEngine AI starter 入口依赖。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/starter/TinyEngineAiStarterDependencyTest.java`：starter 依赖边界测试。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/**`：TinyEngine AI 属性、自动配置、服务、Provider SPI、错误模型。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/**`：自动配置、Provider、RAG/知识库服务测试。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`：注册新增 starter 模块。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`：管理新增 starter artifact。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：注册 TinyEngine AI 自动配置。

**AskData 中立边界：**
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistry.java`：新增中立模型 registry 默认实现。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetriever.java`：新增中立向量 store retriever。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`：默认 Bean 改为中立 registry，旧 LangChain4j 原生桥接加条件隔离。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistryTest.java`：AskData 模型中立化测试。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetrieverTest.java`：AskData 向量中立化测试。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/askdata/starter/AskDataStarterDependencyTest.java`：扩展依赖边界断言。

**文档与验证记录：**
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/README.md`：TinyEngine AI starter 配置示例。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md`：AskData 中立 AI 配置说明。
- `openspec/changes/add-tiny-engine-ai-starter/tasks.md`：每个实现任务完成后勾选。

---

### Task 1: 模块注册与依赖边界

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/starter/TinyEngineAiStarterDependencyTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/askdata/starter/AskDataStarterDependencyTest.java`

**Interfaces:**
- Consumes: Existing Maven reactor and AI Boot BOM.
- Produces: `fw-plus-ai-tiny-engine-spring-boot-starter` artifact and dependency boundary tests.

- [x] **Task 1 Step 1: Write failing dependency tests**

Add assertions that TinyEngine AI starter contains `fw-plus-ai-model`, `fw-plus-ai-store-embedding`, `fw-plus-ai-boot-autoconfigure`, `fw-plus-tiny-engine-core`, and does not contain legacy starter coordinates. Extend AskData dependency test to assert no old LangChain4j model/store starter and no Spring AI/Spring AI Alibaba/AgentScope2 required dependency.

- [x] **Task 1 Step 2: Run tests to verify failure**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml test -Dtest=AskDataStarterDependencyTest`

Expected: existing AskData test passes or new TinyEngine test cannot compile until module exists.

- [x] **Task 1 Step 3: Add starter module and BOM entries**

Create the TinyEngine AI starter POM with dependencies on AI autoconfigure, TinyEngine core, `fw-plus-ai-model-spring-boot-starter`, `fw-plus-ai-store-embedding-spring-boot-starter`, and `fw-plus-ai-langchain4j-adapter-spring-boot-starter`. Add module to starter aggregator and dependency management entry to AI Boot BOM.

- [x] **Task 1 Step 4: Run focused Maven validation**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test`

Expected: dependency tests pass.

- [x] **Task 1 Step 5: Commit**

Run: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml && git commit -m "feat(ai): add tiny engine ai starter module"`

### Task 2: TinyEngine AI 配置、自动配置入口与服务契约

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiProperties.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineAiChatService.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineRagService.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineKnowledgeBaseService.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Spring Boot conditional autoconfiguration.
- Produces: Property prefix `fw.plus.tiny-engine.ai`, disabled/only-chat/RAG condition behavior, service interfaces.

- [x] **Task 2 Step 1: Write failing autoconfiguration tests**

Use `ApplicationContextRunner` to assert: disabled total switch creates no TinyEngine AI beans; `chat.enabled=true` creates chat service without requiring vector store; `rag.enabled=true` without store/model fails fast with diagnostic message.

- [x] **Task 2 Step 2: Add properties and marker service interfaces**

Define nested properties: `enabled`, `provider`, `chat.enabled`, `rag.enabled`, `vector.enabled`, `model.chatKey`, `model.streamingChatKey`, `model.embeddingKey`, `vector.storeKey`, `missingDependencyPolicy`, and RAG chunk/search fields. Use Java 8 getters/setters.

- [x] **Task 2 Step 3: Implement minimal autoconfiguration**

Register properties and conditional service beans. Initial service implementations may be package-private default classes that throw clear unsupported/unavailable exceptions until Provider tasks fill behavior.

- [x] **Task 2 Step 4: Register autoconfiguration**

Add `TinyEngineAiAutoConfiguration` to `spring.factories` and `AutoConfiguration.imports` if Boot 2.7 import path is used by this module.

- [x] **Task 2 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "feat(ai): add tiny engine ai autoconfiguration"`

### Task 3: TinyEngine Provider SPI 与默认 LangChain4j Provider

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiProvider.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiCapability.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiProviderRegistry.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/LangChain4jTinyEngineAiProvider.java`
- Create: TinyEngine request/response DTOs under `.../tinyengine/chat` and `.../tinyengine/rag`.
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiProviderRegistryTest.java`

**Interfaces:**
- Consumes: `AiModelRegistry`, `AiModel`, `AiModelType`.
- Produces: Provider registry, capability checks, default provider selection, unsupported capability errors.

- [x] **Task 3 Step 1: Write provider registry tests**

Assert default provider key is `langchain4j`, custom provider can replace it, unsupported capability returns a deterministic exception containing provider and capability.

- [x] **Task 3 Step 2: Define SPI and DTOs**

Add Java 8 interfaces/classes for chat request, chat response, streaming chunk, provider capability enum, provider registry. Keep DTOs free of `dev.langchain4j.*` imports.

- [x] **Task 3 Step 3: Implement LangChain4j default provider through neutral registry**

Provider should resolve configured model key from `AiModelRegistry`, verify `AiModelType.CHAT` or `STREAMING_CHAT`, and only unwrap `LangChain4jAiModel` inside provider implementation. If unwrapping is impossible, return a clear unsupported framework/provider error.

- [x] **Task 3 Step 4: Connect chat service to provider registry**

`TinyEngineAiChatService` delegates to current provider and maps stream/non-stream requests.

- [x] **Task 3 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiProviderRegistryTest,TinyEngineAiAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "feat(ai): add tiny engine ai provider spi"`

### Task 4: AskData 模型注册中立化

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistry.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Keep or adapt: `LangChain4jModelAskDataAutoConfiguration.java`, `LangChain4jModelRegistryAskDataModelSource.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistryTest.java`
- Modify Test: `AskDataAutoConfigurationTest.java`, `LangChain4jAskDataModelRegistryTest.java` as needed.

**Interfaces:**
- Consumes: `AiModelRegistry`, `AiModelType.CHAT`, `AiModelType.EMBEDDING`.
- Produces: Default `AskDataModelRegistry` without direct `Map<String, ChatLanguageModel>` / `Map<String, EmbeddingModel>` input.

- [x] **Task 4 Step 1: Write failing AskData model tests**

Create test registry with neutral `AiModel` stubs for chat and embedding. Assert `getChatModel` and `getEmbeddingModel` resolve configured keys, missing key throws `AskDataModelException`, and custom `AskDataModelRegistry` bean prevents default registration.

- [x] **Task 4 Step 2: Implement `AiModelAskDataModelRegistry`**

Resolve candidate/default keys against `AiModelRegistry.keys()`. For LangChain4j default adapter, unwrap only inside this implementation when the model is `LangChain4jAiModel`; keep `AskDataModelRegistry` interface unchanged.

- [x] **Task 4 Step 3: Adjust `AskDataAutoConfiguration`**

Change the default `askDataModelRegistry` bean signature to consume `AiModelRegistry` and optional model source only if still needed. Remove main-path direct collection of `ChatLanguageModel` / `EmbeddingModel` maps.

- [x] **Task 4 Step 4: Preserve optional compatibility**

If keeping `LangChain4jAskDataModelRegistry`, isolate it behind `@ConditionalOnClass` and never make it the default when `AiModelRegistry` is present.

- [x] **Task 4 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest,AskDataAutoConfigurationTest,LangChain4jAskDataModelRegistryTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "refactor(ai): route askdata models through neutral registry"`

### Task 5: AskData 向量检索中立化

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetriever.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetrieverTest.java`
- Modify Test: `AskDataVectorRetrieverTest.java`, `EmbeddingStoreAskDataVectorRetrieverTest.java`.

**Interfaces:**
- Consumes: `AiEmbeddingStoreRegistry`, `AiEmbeddingStore`, `AiEmbeddingQuery`, `AiEmbeddingMatch`.
- Produces: Default AskData vector retriever through neutral store registry.

- [x] **Task 5 Step 1: Write failing vector tests**

Assert neutral retriever returns documents from `AiEmbeddingSearchResult`, handles blank query with empty list, and missing default store is diagnosed. Assert custom `AskDataVectorRetriever` bean overrides default.

- [x] **Task 5 Step 2: Implement neutral vector retriever**

Implement `AiEmbeddingStoreAskDataVectorRetriever` that resolves store key from AskData properties, builds `AiEmbeddingQuery`, maps `AiEmbeddingMatch.document().getText()` or equivalent content to `AskDataRetrievedDocument`, and never imports `dev.langchain4j.*`.

- [x] **Task 5 Step 3: Adjust default vector auto configuration**

Make the default retriever consume `AiEmbeddingStoreRegistry`. Keep `ContentRetrieverAskDataVectorRetriever` and `EmbeddingStoreAskDataVectorRetriever` only as optional compatibility when their classes/beans are present.

- [x] **Task 5 Step 4: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataVectorRetrieverTest,EmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "refactor(ai): route askdata vectors through neutral store"`

### Task 6: TinyEngine RAG 与知识库服务

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/DefaultTinyEngineRagService.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/DefaultTinyEngineKnowledgeBaseService.java`
- Create DTOs under `.../tinyengine/rag` for query, result, document, indexing result.
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/TinyEngineRagServiceTest.java`
- Test: `.../TinyEngineKnowledgeBaseServiceTest.java`

**Interfaces:**
- Consumes: `AiModelRegistry`, `AiEmbeddingStoreRegistry`, `AiEmbeddingDocument`, `AiEmbeddingQuery`.
- Produces: RAG search and knowledge indexing services using neutral store.

- [ ] **Task 6 Step 1: Write failing RAG tests**

Assert search maps score/content/metadata, collection filter is applied through metadata conventions, missing store key fails fast by default, and degrade returns unavailable reason.

- [ ] **Task 6 Step 2: Write failing knowledge indexing tests**

Assert unsupported format is counted as skipped, missing path is reported, supported text content is chunked with configured chunk size/overlap, and metadata includes `source`, `format`, `timestamp`, `collection`.

- [ ] **Task 6 Step 3: Implement RAG service**

Use neutral `AiEmbeddingStore.search()` and TinyEngine DTOs. Keep collection filtering in service layer if store does not support metadata filtering directly.

- [ ] **Task 6 Step 4: Implement knowledge service**

Start with plain text/resource content support that is Java 8 compatible. Return diagnostic counts for loaded/skipped/failed.

- [ ] **Task 6 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineRagServiceTest,TinyEngineKnowledgeBaseServiceTest,TinyEngineAiAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "feat(ai): add tiny engine rag services"`

### Task 7: 文档、README 与样例配置

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/README.md`
- Modify/Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md`
- Optional Modify: `fw-plus-ai/README.zh-CN.md`

**Interfaces:**
- Consumes: Completed starter and AskData property names.
- Produces: Chinese configuration examples for neutral model/store and LangChain4j adapter.

- [ ] **Task 7 Step 1: Document TinyEngine AI starter**

Include Maven dependency, `fw.plus.tiny-engine.ai.enabled`, model keys, vector store key, missing dependency policy, disable behavior, and note that LangChain4j is adapter/provider only.

- [ ] **Task 7 Step 2: Document AskData neutral AI configuration**

Explain `fw.plus.ai.askdata` model/vector keys now resolve against `AiModelRegistry` and `AiEmbeddingStoreRegistry`. Show default LangChain4j adapter dependency and business Bean override options.

- [ ] **Task 7 Step 3: Add boundary warning**

State that old LangChain4j model/store starter coordinates are not used by the current starter line.

- [ ] **Task 7 Step 4: Run docs grep checks and commit**

Run: `rg -n "fw-plus-ai-langchain4j-model-spring-boot-starter|fw-plus-ai-langchain4j-store-embeding-spring-boot-starter|LangChain4jModelRegistry|EmbeddingStoreRegistry" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter`

Expected: no usage that presents old artifacts/registries as current main path.

Commit: `git add fw-plus-ai fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters && git commit -m "docs(ai): document tiny engine and askdata neutral ai setup"`

### Task 8: 综合验证与 OpenSpec 任务收口

**Files:**
- Modify: `openspec/changes/add-tiny-engine-ai-starter/tasks.md`
- Verification only: Maven outputs and dependency tree checks.

**Interfaces:**
- Consumes: All prior task outputs.
- Produces: Checked tasks, build evidence, dependency boundary evidence.

- [ ] **Task 8 Step 1: Run strict OpenSpec validation**

Run: `openspec validate add-tiny-engine-ai-starter --strict`

Expected: `Change 'add-tiny-engine-ai-starter' is valid`.

- [ ] **Task 8 Step 2: Run focused AI autoconfigure tests**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngine*Test,AiModelAskDataModelRegistryTest,AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest`

Expected: tests pass.

- [ ] **Task 8 Step 3: Run starter tests**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test`

Expected: tests pass.

- [ ] **Task 8 Step 4: Run dependency boundary scans**

Run: `rg -n "fw-plus-ai-langchain4j-model|fw-plus-ai-langchain4j-store-embeding|fw-plus-ai-langchain4j-model-spring-boot-starter|fw-plus-ai-langchain4j-store-embeding-spring-boot-starter" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata`

Expected: only negative assertions or optional compatibility comments, no main-path dependency.

- [ ] **Task 8 Step 5: Mark OpenSpec tasks complete**

Update `openspec/changes/add-tiny-engine-ai-starter/tasks.md` by checking completed items with exact evidence from commands above.

- [ ] **Task 8 Step 6: Commit final task state**

Run: `git add openspec/changes/add-tiny-engine-ai-starter/tasks.md && git commit -m "chore(ai): record tiny engine ai build verification"`

## Self-Review

- Spec coverage: TinyEngine starter module, AI enable/disable behavior, LangChain4j default provider, Provider SPI, RAG/knowledge service, failure/degrade semantics, API compatibility, and AskData neutral AI boundary each map to at least one task.
- Placeholder scan: 未发现占位内容或未决任务文本。
- Type consistency: TinyEngine and AskData default paths consistently use `AiModelRegistry` and `AiEmbeddingStoreRegistry`; LangChain4j appears only as default adapter/provider or optional compatibility bridge.
