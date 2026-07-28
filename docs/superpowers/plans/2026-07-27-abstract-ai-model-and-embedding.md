---
change: abstract-ai-model-and-embedding
design-doc: docs/superpowers/specs/2026-07-27-abstract-ai-model-and-embedding-design.md
base-ref: 18be86e44a394e943c7177aa1cb7a1a27d8bcfb4
---

# AI 模型与向量存储抽象实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将模型和向量存储公共能力迁出 LangChain4j，保留 LangChain4j 默认兼容实现，并交付 Java 17+ 目标框架 starter 骨架与迁移文档。

**Architecture:** Java 8 Core 新增中立模型和向量存储模块，LangChain4j 模块只保留原生对象适配。AI Boot 和 Magic-API 依赖中立 registry；同仓 `fw-plus-ai-j17` 是 Java 17 / Spring Boot 3.x 高版本独立聚合，只提供 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的 starter 骨架和明确诊断。

**Tech Stack:** Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0、Maven Wrapper、JUnit；Java 17、Spring Boot 3+、Spring AI/Spring AI Alibaba/AgentScope 2 Java starter 骨架。

## Global Constraints

- Java 8 Core 公共 API 不得导入 `dev.langchain4j.*`、`org.springframework.ai.*` 或 AgentScope 类型。
- Java 17 目标框架只在 `fw-plus-ai-j17` 的 Java 17 / Spring Boot 3.x 高版本线处理，不得回迁或传递污染 Java 8 / Boot 2.7 路径。
- LangChain4j 是首版唯一完整 model/store adapter；Java 17 target artifact 不得宣称已提供真实原生 adapter。
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

Create `docs/ai-framework-migration/compatibility-matrix.md`，先写明测试前的期望：Java 8 Core 不能传递引入 Boot 3 或目标 SDK；Java 17 artifact 必须单独构建。测试将在 Task 7 使该矩阵中的命令可执行。

- [x] **Step 3: 在 BOM 中注册中立 artifact**

Add `fw-plus-ai-model` 和 `fw-plus-ai-store-embedding` 到 Core dependencyManagement，版本使用 `${project.version}`；不要在 Java 8 BOM 中加入 Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 依赖。Java 17 target SDK 的版本管理仅位于 `fw-plus-ai-j17` parent。

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

- [ ] **Step 1: 写入 registry 的失败测试**

Create a fake `AiModelAdapter` in `DefaultAiModelRegistryTest` and assert: duplicate key throws an exception containing the key; missing default throws an exception containing the requested default key; supported definition resolves to the fake model.

- [ ] **Step 2: 运行失败测试**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model test
```
Expected: 先因模块或类型不存在失败。

- [ ] **Step 3: 实现中立模型类型与 registry**

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

- [ ] **Step 4: 运行模块测试**

Run the command from Step 2. Expected: `BUILD SUCCESS` and all registry tests pass.

- [ ] **Step 5: 安装中立模块供 Java 17 独立 reactor 解析**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -am install -DskipTests
```
Expected: `fw-plus-ai-model` 及其 reactor 依赖安装到本地 Maven 仓库。Task 6 依赖该产物，但不得将 Java 17 聚合加入 Java 8 Core reactor。

- [ ] **Step 6: Commit**

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

- [ ] **Step 1: 写入默认 store 和重复 key 的失败测试**

Use a fake store adapter. Assert default lookup returns the selected key, two enabled definitions with the same key fail, and a disabled/default mismatch fails without exposing properties.

- [ ] **Step 2: 运行失败测试**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```
Expected: 先因模块或中立类型不存在失败。

- [ ] **Step 3: 实现中立 store API**

Expose `AiEmbeddingDocument`、`AiEmbeddingQuery`、`AiEmbeddingMatch` and an `AiEmbeddingStore` interface. Keep `EmbeddingStore<TextSegment>` and Spring AI `Document` out of every public signature.

- [ ] **Step 4: 运行模块测试并检查 API 依赖**

Run the command from Step 2, then:
```bash
rg -n 'dev\.langchain4j|org\.springframework\.ai|agentscope' fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main
```
Expected: Maven success and no search output.

- [ ] **Step 5: 安装中立模块供 Java 17 独立 reactor 解析**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am install -DskipTests
```
Expected: `fw-plus-ai-store-embedding` 及其 reactor 依赖安装到本地 Maven 仓库。

- [ ] **Step 6: Commit**

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

- [ ] **Step 1: 扩展现有测试以证明兼容行为**

Add assertions that existing provider/model aliases and embedding store default keys still resolve through the new neutral registry, while the returned framework-native object remains internal to the LangChain4j adapter.

- [ ] **Step 2: 运行现有测试确认初始状态**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model,fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding test
```
Expected: 新断言在适配层尚未接入中立 SPI 时失败。

- [ ] **Step 3: 接入中立模块**

Update both POMs to depend on the relevant neutral module. Convert factory/registry construction to delegate to the neutral definition and registry while keeping existing artifact coordinates and LangChain4j provider behavior.

- [ ] **Step 4: 运行回归测试**

Run the command from Step 2. Expected: `BUILD SUCCESS` and all existing plus new compatibility tests pass.

- [ ] **Step 5: Commit**

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

- [ ] **Step 1: 写入配置优先级和 Magic-API 兼容失败测试**

Add test cases with (a) old LangChain4j properties only, (b) neutral properties only, and (c) both for the same key. Assert the neutral definition wins in case (c), a warning is available without the API key, and `model[...]`/`vector[...]` still resolve by key/default.

- [ ] **Step 2: 运行对应 AI Boot 测试确认失败**

Run:
```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure test -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest
```
Expected: 新优先级和中立 facade 断言失败。

- [ ] **Step 3: 实现中立 starter 与兼容映射**

Register neutral factory/registry Beans first. Map legacy prefixes only when no same-key neutral definition exists. Keep `LangChain4jModelProperties` and `EmbeddingStoreProperties` as compatibility inputs; do not log property maps containing credentials.

- [ ] **Step 4: 改造 Magic-API 配置类**

Change `MagicAPIModelConfiguration` and `MagicAPIVectorConfiguration` constructor/Bean signatures to use neutral registry/factory. Preserve resource names and script keys; return a controlled neutral facade and include key/framework/provider only in errors.

- [ ] **Step 5: 运行 AI Boot 回归测试**

Run the command from Step 2. Expected: `BUILD SUCCESS` and all compatibility/integration tests pass.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters
git commit -m "fw-plus-ai: use neutral registries in boot and magicapi"
```

### Task 6: 创建 Java 17+ 独立 artifact 与 starter 骨架

**Files:**
- Create: `fw-plus-ai-j17/pom.xml`
- Create: `fw-plus-ai-j17/fw-plus-ai-j17-parent/pom.xml`
- Create: `fw-plus-ai-j17/fw-plus-ai-spring-ai-j17-starter/pom.xml`
- Create: `fw-plus-ai-j17/fw-plus-ai-spring-ai-alibaba-j17-starter/pom.xml`
- Create: `fw-plus-ai-j17/fw-plus-ai-agentscope-j17-starter/pom.xml`
- Create: `fw-plus-ai-j17/*/src/main/java/com/xiongx9527/fw/plus/ai/j17/*UnavailableAutoConfiguration.java`
- Test: `fw-plus-ai-j17/*/src/test/java/com/xiongx9527/fw/plus/ai/j17/*UnavailableAutoConfigurationTest.java`

**Interfaces:**
- Consumes: published Java 8 neutral model/store artifacts.
- Produces: Java 17 / Boot 3 starter coordinates and a deterministic diagnostic for every unimplemented native adapter.

- [ ] **Step 1: 写入 Java 17 骨架的失败测试**

For each starter, create a Boot 3 test that supplies `framework=spring-ai`, `spring-ai-alibaba`, or `agentscope` and asserts no partial adapter Bean exists; the failure message must name the framework and `docs/ai-framework-migration/`.

- [ ] **Step 2: 运行 Java 17 测试确认初始失败**

Run with a Java 17 `JAVA_HOME`:
```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml test
```
Expected: reactor/modules do not yet exist.

- [ ] **Step 3: 实现独立聚合、parent 和三个 starter**

Set `maven.compiler.release` to `17` only in `fw-plus-ai-j17-parent`. Each starter depends on the neutral artifact coordinates and uses an `*UnavailableAutoConfiguration` that fails only when its framework is explicitly enabled. Do not create a fake `AiModelAdapter` or `AiEmbeddingStoreAdapter` implementation.

- [ ] **Step 4: 验证 Java 17 构建和 Java 8 隔离**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml test
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai dependency:tree
```
Expected: Java 17 reactor succeeds; Core dependency tree contains no Spring Boot 3, Spring AI, Spring AI Alibaba or AgentScope dependency.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai-j17
git commit -m "fw-plus-ai: add java17 adapter starter skeletons"
```

### Task 7: 编写 LangChain4j 到 Java 17 的迁移文档

**Files:**
- Create: `docs/ai-framework-migration/overview.md`
- Create: `docs/ai-framework-migration/spring-ai-java17.md`
- Create: `docs/ai-framework-migration/spring-ai-alibaba-java17.md`
- Create: `docs/ai-framework-migration/agentscope-java17.md`
- Create: `docs/ai-framework-migration/verify-migration-docs.sh`
- Modify: `docs/ai-framework-migration/compatibility-matrix.md`

**Interfaces:**
- Consumes: Task 5 stable key/config behavior and Task 6 artifact coordinates.
- Produces: an operational migration and rollback guide that cannot overstate Java 17 skeleton support.

- [ ] **Step 1: 写入文档完整性校验脚本**

Create `docs/ai-framework-migration/verify-migration-docs.sh`. It SHALL loop over `spring-ai-java17.md`, `spring-ai-alibaba-java17.md` and `agentscope-java17.md`, then use `rg -q` to require these literal headings: `前置条件`, `Maven Parent 与 Starter`, `同 key 配置迁移`, `Java API 迁移`, `Magic-API 影响`, `验证`, `回滚`, `当前支持状态`.

- [ ] **Step 2: 写入四份迁移文档**

`overview.md` must give the Java 8 / Java 17 compatibility matrix. Each target document must use the same business key as the LangChain4j example and state explicitly: the first Java 17 release supplies a starter skeleton only, so real model/store execution is unavailable until the subsequent adapter change.

- [ ] **Step 3: 运行文档校验**

Run:
```bash
zsh docs/ai-framework-migration/verify-migration-docs.sh
```
Expected: exit code `0`; every target document contains the required headings and the first-release limitation.

- [ ] **Step 4: Commit**

```bash
git add docs/ai-framework-migration
git commit -m "docs: add java17 ai framework migration guides"
```

### Task 8: 全量回归、样例验证与交付检查

**Files:**
- Modify: `openspec/changes/abstract-ai-model-and-embedding/tasks.md`
- Modify: `docs/ai-framework-migration/compatibility-matrix.md`
- Test: Core, AI Boot, Java 17 reactor and relevant samples

**Interfaces:**
- Consumes: all previous tasks.
- Produces: reproducible build evidence and completed OpenSpec task checklist.

- [ ] **Step 1: 执行 Core 与 AI Boot 回归**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml clean test
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml clean test
```
Expected: both commands exit with `0`.

- [ ] **Step 2: 执行 Java 17 reactor 与样例聚合验证**

Run:
```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml clean test
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```
Expected: Java 17 skeleton tests and sample aggregation validate successfully.

- [ ] **Step 3: 更新兼容矩阵与勾选 OpenSpec 任务**

Record the exact JDK/Maven commands and outcomes in the compatibility matrix. Check each completed `tasks.md` item only after its targeted tests and build evidence have passed.

- [ ] **Step 4: Commit**

```bash
git add openspec/changes/abstract-ai-model-and-embedding/tasks.md docs/ai-framework-migration/compatibility-matrix.md
git commit -m "fw-plus-ai: verify framework abstraction migration"
```
