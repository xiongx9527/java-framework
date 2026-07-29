---
change: abstract-ai-model-and-embedding
design-doc: docs/superpowers/specs/2026-07-27-abstract-ai-model-and-embedding-design.md
base-ref: 18be86e44a394e943c7177aa1cb7a1a27d8bcfb4
archived-with: 2026-07-29-abstract-ai-model-and-embedding
---

# AI 模型与向量存储抽象实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将模型和向量存储公共能力迁出 LangChain4j，保留 LangChain4j 默认兼容实现，并交付 Java 17 / Boot 3.x 后续实施蓝图与迁移文档。

**Architecture:** Java 8 Core 新增中立模型和向量存储模块，LangChain4j 模块只保留原生对象适配。AI Boot 和 Magic-API 依赖中立 registry；Spring AI、Spring AI Alibaba、AgentScope 2 Java 的 Java 17 / Spring Boot 3.x reactor、parent、starter、自动配置与真实 adapter 均为后续 change 的蓝图，本次不创建。

**Tech Stack:** 本次为 Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0、Maven Wrapper、JUnit；后续蓝图目标为 Java 17、Spring Boot 3+、Spring AI/Spring AI Alibaba/AgentScope 2 Java。

## Global Constraints

- Java 8 Core 公共 API 不得导入 `dev.langchain4j.*`、`org.springframework.ai.*` 或 AgentScope 类型。
- Java 17 目标框架只在后续 `fw-plus-ai-j17` 的 Java 17 / Spring Boot 3.x 高版本线处理，不得回迁或传递污染 Java 8 / Boot 2.7 路径；本次不创建该 reactor。
- LangChain4j 是首版唯一完整 model/store adapter；本次不提供 Java 17 target artifact、starter、自动配置或真实原生 adapter。
- 旧 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 保留兼容，中立配置同 key 优先。
- Magic-API `model[...]`、`vector[...]`、动态资源 key 和默认访问方式保持兼容，错误和日志必须脱敏。
- 每个 Java 修改任务先增加失败测试，再实现最小变更并运行目标模块测试；每个完成任务单独提交。

---

### Task 1: 建立依赖与构建基线

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Create: `docs/ai-framework-migration/compatibility-matrix.md`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml` Maven reactor

**Interfaces:**
- Consumes: 当前 `${revision}`、Java 8 Core parent 与 AI Boot BOM 属性。
- Produces: 中立 artifact 的 BOM 坐标，以及 Java 17+ 适配线的版本和基线记录。

- [x] **Step 1: 记录当前有效 POM 和依赖树**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml help:effective-pom -Doutput=/tmp/fw-plus-core-effective-pom.xml
./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml help:effective-pom -Doutput=/tmp/fw-plus-ai-effective-pom.xml
```
Expected: 两个命令退出码为 `0`，输出文件可读取。

- [x] **Step 2: 写入 Java 17 对接矩阵的失败校验目标**

Create `docs/ai-framework-migration/compatibility-matrix.md`，写明当前 Java 8 Core 不能传递引入 Boot 3 或目标 SDK，并记录未来 Java 17 artifact 的独立构建门槛。后续 Java 17 reactor 创建前，矩阵中的 Java 17 命令不得执行或视为本次验证。

- [x] **Step 3: 在 BOM 中注册中立 artifact**

Add `fw-plus-ai-model` 和 `fw-plus-ai-store-embedding` 到 Core dependencyManagement，版本使用 `${project.version}`；不要在 Java 8 BOM 中加入 Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 依赖。目标 SDK 的版本管理仅能在后续 Java 17 parent 中实现，本次不创建该 parent。

- [x] **Step 4: 验证 BOM 变更**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
```
Expected: `BUILD SUCCESS`。

- [x] **Step 5: 提交 Task 1 基线**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml docs/ai-framework-migration/compatibility-matrix.md
git commit -m "fw-plus-ai: add abstraction dependency baseline"
```

### Task 2: 创建 Java 8 中立模型模块与契约测试

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiModelDefinition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiModel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiModelAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/DefaultAiModelRegistry.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/test/java/com/xiongx9527/fw/plus/ai/model/DefaultAiModelRegistryTest.java`

**Interfaces:**
- Consumes: Java 8 collection API、稳定模型 key、framework、provider 和能力类型。
- Produces: `AiModelRegistry#get(String)`、`AiModelRegistry#getDefault()`、`AiModelAdapter#supports(AiModelDefinition)`、`AiModelAdapter#create(AiModelDefinition)`。

- [x] **Step 1: 写入 registry 的失败测试**

Create a fake `AiModelAdapter` in `DefaultAiModelRegistryTest` and assert: duplicate key throws an exception containing the key; missing default throws an exception containing the requested default key; supported definition resolves to the fake model.

- [x] **Step 2: 运行失败测试**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model test
```
Expected: 先因模块或类型不存在失败。

- [x] **Step 3: 实现中立模型类型与 registry**

Define the minimum Java 8 API:
```java
public interface AiModel {
    String getKey();
    String getFramework();
    AiModelType getType();
}

public interface AiModelAdapter {
    boolean supports(AiModelDefinition definition);
    AiModel create(AiModelDefinition definition);
}
```
`DefaultAiModelRegistry` 使用 `LinkedHashMap`，只在构造完成后暴露不可变查询，不接收框架原生对象。

- [x] **Step 4: 运行模块测试**

Run the command from Step 2. Expected: `BUILD SUCCESS` and all registry tests pass.

- [x] **Step 5: 安装中立模块供 Java 17 独立 reactor 解析**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -am install -DskipTests
```
Expected: `fw-plus-ai-model` 及其 reactor 依赖安装到本地 Maven 仓库。Task 6 依赖该产物，但不得将 Java 17 聚合加入 Java 8 Core reactor。

- [x] **Step 6: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model
git commit -m "fw-plus-ai: add neutral model core"
```

### Task 3: 创建 Java 8 中立向量存储模块与契约测试

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingStore.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingStoreAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreRegistry.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreRegistryTest.java`

**Interfaces:**
- Consumes: 中立文本、metadata、向量、query/match DTO。
- Produces: `AiEmbeddingStoreRegistry#get(String)`、`getDefault()`、`AiEmbeddingStoreAdapter#supports(AiEmbeddingStoreDefinition)`。

- [x] **Step 1: 写入默认 store 和重复 key 的失败测试**

Use a fake store adapter. Assert default lookup returns the selected key, two enabled definitions with the same key fail, and a disabled/default mismatch fails without exposing properties.

- [x] **Step 2: 运行失败测试**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```
Expected: 先因模块或中立类型不存在失败。

- [x] **Step 3: 实现中立 store API**

Expose `AiEmbeddingDocument`、`AiEmbeddingQuery`、`AiEmbeddingMatch` and an `AiEmbeddingStore` interface. Keep `EmbeddingStore<TextSegment>` and Spring AI `Document` out of every public signature.

- [x] **Step 4: 运行模块测试并检查 API 依赖**

Run the command from Step 2, then:
```bash
rg -n 'dev\.langchain4j|org\.springframework\.ai|agentscope' fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main
```
Expected: Maven success and no search output.

- [x] **Step 5: 安装中立模块供 Java 17 独立 reactor 解析**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am install -DskipTests
```
Expected: `fw-plus-ai-store-embedding` 及其 reactor 依赖安装到本地 Maven 仓库。

- [x] **Step 6: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding
git commit -m "fw-plus-ai: add neutral embedding store core"
```

### Task 4: 将 LangChain4j 模块迁为默认适配器

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelFactory.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/DefaultEmbeddingStoreFactory.java`
- Test: existing `DefaultLangChain4jModelRegistryTest.java`, `DefaultLangChain4jModelFactoryTest.java`, `DefaultEmbeddingStoreRegistryTest.java`, `DefaultEmbeddingStoreFactoryTest.java`

**Interfaces:**
- Consumes: Task 2 `AiModelAdapter` and Task 3 `AiEmbeddingStoreAdapter`.
- Produces: LangChain4j-only bridge implementations and compatibility adapters for existing artifact consumers.

- [x] **Step 1: 扩展现有测试以证明兼容行为**

Add assertions that existing provider/model aliases and embedding store default keys still resolve through the new neutral registry, while the returned framework-native object remains internal to the LangChain4j adapter.

- [x] **Step 2: 运行现有测试确认初始状态**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model,fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding test
```
Expected: 新断言在适配层尚未接入中立 SPI 时失败。

- [x] **Step 3: 接入中立模块**

Update both POMs to depend on the relevant neutral module. Convert factory/registry construction to delegate to the neutral definition and registry while keeping existing artifact coordinates and LangChain4j provider behavior.

- [x] **Step 4: 运行回归测试**

Run the command from Step 2. Expected: `BUILD SUCCESS` and all existing plus new compatibility tests pass.

- [x] **Step 5: Commit**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j
git commit -m "fw-plus-ai: adapt langchain4j modules to neutral core"
```

### Task 5: 改造 AI Boot starter、配置映射与 Magic-API

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/starter/MagicAPIModelConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/starter/MagicAPIVectorConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Create: neutral model/store starter POMs under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/`
- Test: existing `LangChain4jModelAutoConfigurationTest.java`, `EmbeddingStoreAutoConfigurationTest.java`, `MagicAPIModelAutoConfigurationIntegrationTest.java`, `MagicAPIVectorAutoConfigurationIntegrationTest.java`

**Interfaces:**
- Consumes: neutral registries and legacy LangChain4j property classes.
- Produces: neutral starter Beans, legacy-to-neutral mapping, Magic-API facade access without native store/model signatures.

- [x] **Step 1: 写入配置优先级和 Magic-API 兼容失败测试**

Add test cases with (a) old LangChain4j properties only, (b) neutral properties only, and (c) both for the same key. Assert the neutral definition wins in case (c), a warning is available without the API key, and `model[...]`/`vector[...]` still resolve by key/default.

- [x] **Step 2: 运行对应 AI Boot 测试确认失败**

Run:
```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure test -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest
```
Expected: 新优先级和中立 facade 断言失败。

- [x] **Step 3: 实现中立 starter 与兼容映射**

Register neutral factory/registry Beans first. Map legacy prefixes only when no same-key neutral definition exists. Keep `LangChain4jModelProperties` and `EmbeddingStoreProperties` as compatibility inputs; do not log property maps containing credentials.

- [x] **Step 4: 改造 Magic-API 配置类**

Change `MagicAPIModelConfiguration` and `MagicAPIVectorConfiguration` constructor/Bean signatures to use neutral registry/factory. Preserve resource names and script keys; return a controlled neutral facade and include key/framework/provider only in errors.

- [x] **Step 5: 运行 AI Boot 回归测试**

Run the command from Step 2. Expected: `BUILD SUCCESS` and all compatibility/integration tests pass.

- [x] **Step 6: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters
git commit -m "fw-plus-ai: use neutral registries in boot and magicapi"
```

### Task 6: 编写 Java 17 / Boot 3.x 后续实施蓝图（不实现）

**Files:**
- Create: Java 17 / Boot 3.x 后续实施说明，位于 `docs/ai-framework-migration/`
- Test: `docs/ai-framework-migration/verify-migration-docs.sh`

**Interfaces:**
- Consumes: published Java 8 neutral model/store artifacts.
- Produces: Java 17 / Boot 3.x 后续模块布局、依赖边界、实施步骤、验证与回滚说明；本任务不产生 artifact、starter 或自动配置代码。

- [x] **Step 1: 写入后续模块与依赖蓝图**

Describe the future `fw-plus-ai-j17` aggregator, Java 17 / Boot 3.x parent, three optional starters, target SDK dependency management and their isolation from Java 8 Core. Do not create the reactor or POM files.

- [x] **Step 2: 写明未来自动配置与 adapter 实施步骤**

For Spring AI, Spring AI Alibaba and AgentScope 2 Java, document how a later change will bind neutral definitions, reject unimplemented capabilities, preserve business keys and avoid half-initialized Beans. Do not claim current code offers these diagnostics.

- [x] **Step 3: 写明未来验证与回滚步骤**

Document the future Java 17 build, dependency-tree and Boot 3 condition tests, plus the current Java 8 isolation check and rollback to LangChain4j. Mark all Java 17 commands as future-only until a later change creates the reactor.

### Task 7: 编写 LangChain4j 到 Java 17 的迁移文档

**Files:**
- Create: `docs/ai-framework-migration/overview.md`
- Create: `docs/ai-framework-migration/spring-ai-java17.md`
- Create: `docs/ai-framework-migration/spring-ai-alibaba-java17.md`
- Create: `docs/ai-framework-migration/agentscope-java17.md`
- Create: `docs/ai-framework-migration/verify-migration-docs.sh`
- Modify: `docs/ai-framework-migration/compatibility-matrix.md`

**Interfaces:**
- Consumes: Task 5 stable key/config behavior and Task 6 future artifact-coordinate blueprint.
- Produces: an operational migration and rollback guide that cannot overstate Java 17 skeleton support.

- [x] **Step 1: 写入文档完整性校验脚本**

Create `docs/ai-framework-migration/verify-migration-docs.sh`. It SHALL loop over `spring-ai-java17.md`, `spring-ai-alibaba-java17.md` and `agentscope-java17.md`, then use `rg -q` to require these literal headings: `前置条件`, `Maven Parent 与 Starter`, `同 key 配置迁移`, `Java API 迁移`, `Magic-API 影响`, `验证`, `回滚`, `当前支持状态`.

- [x] **Step 2: 写入四份迁移文档**

`overview.md` must give the Java 8 / Java 17 compatibility matrix. Each target document must use the same business key as the LangChain4j example and state explicitly: this change supplies documentation only; no Java 17 artifact, starter, automatic configuration or real model/store execution exists yet.

- [x] **Step 3: 运行文档校验**

Run:
```bash
zsh docs/ai-framework-migration/verify-migration-docs.sh
```
Expected: exit code `0`; every target document contains the required headings and the first-release limitation.

- [x] **Step 4: Commit**

```bash
git add docs/ai-framework-migration
git commit -m "docs: add java17 ai framework migration guides"
```

### Task 8: Java 8 回归、样例验证与交付检查

**Files:**
- Modify: `openspec/changes/abstract-ai-model-and-embedding/tasks.md`
- Modify: `docs/ai-framework-migration/compatibility-matrix.md`
- Test: Java 8 Core、AI Boot、迁移文档和相关 samples

**Interfaces:**
- Consumes: all previous tasks.
- Produces: reproducible build evidence and completed OpenSpec task checklist.

- [x] **Step 1: 执行 Core 与 AI Boot 回归**

Run (按受影响模块执行，避免无关模块扩大回归时间):
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model,fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model,fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -am test
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -am test -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: both commands exit with `0`.

- [x] **Step 2: 执行样例与迁移文档验证**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
zsh -n docs/ai-framework-migration/verify-migration-docs.sh
zsh docs/ai-framework-migration/verify-migration-docs.sh
```
Expected: sample aggregation and documentation validation succeed. Java 17 reactor、SDK probe、Boot 3 条件装配与真实 adapter 测试均为后续 change 的门槛，当前不得作为本次构建要求。

- [x] **Step 3: 更新兼容矩阵与勾选 OpenSpec 任务**

Record the exact JDK/Maven commands and outcomes in the compatibility matrix. Check each completed `tasks.md` item only after its targeted tests and build evidence have passed.

- [x] **Step 4: Commit**

```bash
git add openspec/changes/abstract-ai-model-and-embedding/tasks.md docs/ai-framework-migration/compatibility-matrix.md
git commit -m "fw-plus-ai: verify framework abstraction migration"
```
