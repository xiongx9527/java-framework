---
change: neutral-ai-adapter-autodiscovery
design-doc: docs/superpowers/specs/2026-07-29-neutral-ai-adapter-autodiscovery-design.md
base-ref: 90bab6c72486c4de08478fe38e68cf6884011fe7
---

# 中立 AI Adapter 自动发现实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除四个旧 LangChain4j model/store artifact，以统一可选 adapter 实现中立模型和向量 store 的安全自动发现。

**Architecture:** 中立 Core factory 根据 definition 与已发现 SPI 选择唯一 adapter；`fw-plus-ai-langchain4j-adapter` 承载全部 LangChain4j 类型和 provider 构造；其 Spring Boot starter 只提供一种条件化 Boot 集成。Magic-API 和中立 starter 只消费中立 registry。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Maven 3.8.8、LangChain4j 0.35.0、JUnit 5、Spring `ApplicationContextRunner`、Magic-API。

## Global Constraints

- 所有 Core 公开 API 必须保持 Java 8；不得使用 Java 9+ 集合或语言 API。
- Maven 验证必须使用 `JAVA_HOME=$(/usr/libexec/java_home -v 1.8)`；JDK 24 不支持项目当前 Lombok 基线。
- `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、中立 starter、公共自动配置和 Magic-API 不得依赖、导入、公开暴露或配置任何具体 AI framework。
- 删除旧 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding` 及两个旧 starter；不得保留 POM 兼容别名或 `fw-plus.ai.langchain4j.*` 配置绑定。
- LangChain4j starter 是 adapter 的 Spring Boot 集成实现，不是 adapter 的唯一装配形式。
- Spring AI、Spring AI Alibaba、AgentScope 2.0 仅写 Java 17 / Boot 3 后续蓝图，本次不得加入 Java 8 reactor。
- AskData 模块不删除；在其他 adapter、Boot 与 Magic-API 工作完成并获得用户再次明确确认后，最后迁移其对旧 artifact 的依赖并执行回归测试。

---

### Task 1: 中立 adapter 选择契约与模型 Red-Green 测试

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiModelAdapter.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/DefaultAiModelFactory.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/test/java/com/xiongx9527/fw/plus/ai/model/DefaultAiModelFactoryTest.java`

**Interfaces:**
- Produces `String getFramework()` on `AiModelAdapter` and deterministic `DefaultAiModelFactory#create(AiModelDefinition, List<AiModelAdapter>)` selection.
- Failure message includes `key`, `framework`, `provider`, `type` and candidate framework names.

- [x] **Step 1: 写入模型选择器失败测试**

```java
@Test
void shouldSelectOnlyCandidateWhenFrameworkIsBlank() { }
@Test
void shouldRejectMultipleCandidatesWhenFrameworkIsBlank() { }
@Test
void shouldRejectMissingExplicitFramework() { }
```

- [x] **Step 2: 运行失败测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -am -Dtest=DefaultAiModelFactoryTest test`
Expected: 失败，原因是 `getFramework()` 和唯一候选规则尚不存在。

- [x] **Step 3: 实现最小选择契约和模型选择器**

```java
public interface AiModelAdapter {
    String getFramework();
    boolean supports(AiModelDefinition definition);
    AiModel create(AiModelDefinition definition);
}
```

显式 framework 仅匹配同名 adapter；缺省 framework 仅接受一个 `supports` 候选；不使用 Bean 顺序或 LangChain4j 回退。

- [x] **Step 4: 运行模型选择器测试**

Run: 使用 Step 2 命令。
Expected: `DefaultAiModelFactoryTest` 全部通过。

- [x] **Step 5: 提交模型选择器任务**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model
git commit -m "feat(ai): select neutral model adapter deterministically"
```

### Task 2: 向量 store 选择契约与 Red-Green 测试

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingStoreAdapter.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreFactoryTest.java`

**Interfaces:**
- Produces `String getFramework()` on `AiEmbeddingStoreAdapter` and the same candidate rules as Task 1.

- [x] **Step 1: 写入 store 的唯一、零和多候选失败测试**

```java
@Test
void shouldUseOnlyStoreAdapterWhenFrameworkIsBlank() { }
@Test
void shouldRejectAmbiguousStoreAdapters() { }
@Test
void shouldIncludeStoreDefinitionContextInFailure() { }
```

- [x] **Step 2: 运行失败测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am -Dtest=DefaultAiEmbeddingStoreFactoryTest test`
Expected: 失败，直到 store adapter 声明 framework 并实施选择器。

- [x] **Step 3: 实现 store 选择器并脱敏异常**

```java
public interface AiEmbeddingStoreAdapter {
    String getFramework();
    boolean supports(AiEmbeddingStoreDefinition definition);
    AiEmbeddingStore create(AiEmbeddingStoreDefinition definition);
}
```

- [x] **Step 4: 运行 store 模块测试**

Run: 使用 Step 2 命令。
Expected: 新增 factory 测试及既有 registry 测试全部通过。

- [x] **Step 5: 提交 store 选择器任务**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding
git commit -m "feat(ai): select neutral embedding adapter deterministically"
```

### Task 3: 统一 LangChain4j Core adapter 并迁移旧实现

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/`
- Move: `fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/`
- Move: `fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

**Interfaces:**
- Consumes Task 1/2 SPI.
- Produces model/store adapter Beans with `getFramework()` returning `langchain4j` and no framework types in Core modules.

- [x] **Step 1: 为迁移后的 LangChain4j adapter 写失败测试**

迁移现有 `DefaultLangChain4jModelFactoryTest`、`DefaultEmbeddingStoreFactoryTest`，新增断言 adapter framework 为 `langchain4j`。

- [x] **Step 2: 验证旧路径仍被测试引用且新模块不存在**

Run: `rg -n 'fw-plus-ai-langchain4j-(model|store-embeding)' fw-plus-core/fw-plus-project/fw-plus-ai`
Expected: 旧目录和引用仍存在，作为迁移前证据。

- [x] **Step 3: 创建统一 adapter 并迁移实现**

将 LangChain4j 包名迁为 `com.xiongx9527.fw.plus.ai.langchain4j.adapter`；保留 provider/type、多模型别名、多 store、默认 key 和异常 cause 行为。adapter POM 仅依赖中立 Core 与必要 LangChain4j API，不传递具体 provider SDK。

- [x] **Step 4: 运行 adapter 与 Core reactor 测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter -am test`
Expected: BUILD SUCCESS。

- [x] **Step 5: 提交统一 adapter 任务**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml
git commit -m "feat(ai): unify langchain4j model and store adapter"
```

### Task 4: 拆分中立与 LangChain4j Boot 自动配置

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Move/Modify: `fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfiguration.java`
- Move/Modify: `fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/main/java/`
- Modify: `fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfigurationTest.java`
- Modify: `fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfigurationTest.java`

**Interfaces:**
- Public neutral configuration exposes only `fw-plus.ai.model` and `fw-plus.ai.embedding-store`.
- Adapter configuration is conditional on adapter + LangChain4j classpath and contributes adapter Beans only.

- [ ] **Step 1: 写入中立 classpath 与 adapter 条件装配失败测试**

使用 `ApplicationContextRunner` 和 `FilteredClassLoader` 覆盖无 adapter、唯一 adapter、两个 fake adapter 和显式 `framework=langchain4j`。

- [ ] **Step 2: 运行失败测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest test`
Expected: 失败，直到条件配置与 selector 接通。

- [ ] **Step 3: 迁移配置类与 properties 绑定**

中立 factory/registry/properties 留在公共自动配置；所有 LangChain4j provider Bean、反射构造、原生类型和旧 properties 迁至 adapter 自动配置。删除旧 `fw-plus.ai.langchain4j.*` binding 与 legacy adapter。

- [ ] **Step 4: 运行自动配置测试**

Run: 使用 Step 2 命令。
Expected: 目标测试通过且无旧前缀绑定。

- [ ] **Step 5: 提交 Boot 拆分任务**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure
git commit -m "refactor(ai): isolate langchain4j boot autoconfiguration"
```

### Task 5: 发布 LangChain4j adapter Spring Boot starter 并更新依赖入口

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/NeutralStarterDependencyTest.java`

- [ ] **Step 1: 写入 starter 依赖边界失败测试**

断言中立 starter 不解析 `dev.langchain4j` 与统一 adapter；adapter starter 解析 adapter 自动配置但不传递全部 provider SDK。

- [ ] **Step 2: 实现 starter POM 与 BOM 注册**

adapter starter 依赖中立 model/store starter、统一 adapter 和 adapter 自动配置。删除两个旧 starter 的聚合/BOM 条目与目录。

- [ ] **Step 3: 运行 starter dependency 测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter -am -Dtest=NeutralStarterDependencyTest test`
Expected: 中立 starter 零 LangChain4j 传递依赖断言通过。

- [ ] **Step 4: 提交 starter 任务**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies
 git commit -m "feat(ai): add langchain4j adapter boot starter"
```

### Task 6: 改造 Magic-API 以只消费中立 registry

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/model/MagicDynamicModelDataSource.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/model/MagicDynamicVectorDataSource.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/starter/MagicAPIModelConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector/starter/MagicAPIVectorConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector/pom.xml`
- Test: `MagicDynamicModelDataSourceTest`, `MagicDynamicVectorDataSourceTest`, `MagicAPIModelAutoConfigurationIntegrationTest`, `MagicAPIVectorAutoConfigurationIntegrationTest`

- [ ] **Step 1: 写入 Magic-API 零原生类型和歧义失败测试**

以 fake `AiModelRegistry` / `AiEmbeddingStoreRegistry` 注入动态资源；断言多候选错误包含候选 framework，脚本结果不返回 LangChain4j 类型。

- [ ] **Step 2: 迁移 Magic-API POM 与运行时访问**

删除 model/vector plugin 对旧 starter 的依赖，按需要显式依赖中立 starter；所有创建和读取经中立 factory/registry，不再创建 legacy LangChain4j registry。

- [ ] **Step 3: 运行 Magic-API 目标测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=MagicDynamicModelDataSourceTest,MagicDynamicVectorDataSourceTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest test`
Expected: 目标集成测试通过。

- [ ] **Step 4: 提交 Magic-API 任务**

```bash
git add fw-plus-ai/fw-plus-ai-project
git commit -m "refactor(ai): use neutral registry from magicapi"
```

### Task 7: 经用户确认后最后迁移保留的 AskData starter

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml`
- Test: AskData starter 现有目标测试与依赖树检查。

- [ ] **Step 0: 等待用户明确确认**

未获得确认时，不得派发或执行本任务；可继续本计划中不修改 AskData 的任务。

- [ ] **Step 1: 写入 AskData 依赖迁移失败测试**

断言 AskData starter 不再解析两个已删除的 LangChain4j model/store starter，且仍通过统一 LangChain4j adapter 获得既有模型能力。

- [ ] **Step 2: 迁移 AskData starter 依赖**

仅替换已删除的旧 model/store starter 坐标为统一 adapter starter；不删除 AskData 模块，不重写其内部 Java 8 行为或业务逻辑。

- [ ] **Step 3: 运行 AskData 目标测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -am test`
Expected: BUILD SUCCESS。

- [ ] **Step 4: 提交 AskData 集成任务**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter
git commit -m "refactor(ai): migrate askdata to unified adapter starter"
```

### Task 8: 删除旧模块、清理全仓坐标并执行静态边界测试

**Files:**
- Delete: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-milvus/pom.xml`
- Modify: all POM/document/sample files returned by `rg -l 'fw-plus-ai-langchain4j-(model|store-embeding)'`
- Create: static boundary test under the existing AI Boot starter test package.

- [ ] **Step 1: 写入旧坐标和中立边界失败测试**

断言 Core/AI BOM、聚合、starter 和源码中不含四个旧 artifact、旧前缀或具体 framework 导入；允许 LangChain4j adapter 专属路径出现 LangChain4j。

- [ ] **Step 2: 删除旧模块并替换全部引用**

先替换所有 provider 子模块、样例与 POM 依赖，再删除目录。不得保留 deprecated forwarding POM。

- [ ] **Step 3: 运行静态扫描与 Maven 依赖树检查**

Run: `rg -n 'fw-plus-ai-langchain4j-(model|store-embeding)(-spring-boot-starter)?|fw-plus\.ai\.langchain4j\.(model|embedding-store)' fw-plus-core fw-plus-ai fw-plus-sample docs`
Expected: 仅迁移说明中的历史坐标允许出现，生产 POM/Java/YAML 不得出现。

- [ ] **Step 4: 提交破坏性清理任务**

```bash
git add -A fw-plus-core/fw-plus-project/fw-plus-ai fw-plus-ai/fw-plus-ai-project fw-plus-sample
git commit -m "refactor(ai): remove legacy langchain4j model and store modules"
```

### Task 9: 编写迁移与 Java 17 / Boot 3 后续文档

**Files:**
- Modify: `fw-plus-ai/README.zh-CN.md`
- Modify: `fw-plus-ai/README.md`
- Modify: `docs/ai-framework-migration/overview.md`
- Modify: `docs/ai-framework-migration/spring-ai-java17.md`
- Modify: `docs/ai-framework-migration/agentscope-java17.md`
- Create: `docs/ai-framework-migration/spring-ai-alibaba-java17.md`

- [ ] **Step 1: 写入 LangChain4j 破坏性迁移章节**

逐项映射四个删除坐标到统一 adapter 与 starter；给出 `fw-plus.ai.model`、`fw-plus.ai.embedding-store` 和多 adapter `framework` 配置示例；说明 `model[...]` / `vector[...]` key 稳定但原生类型不可用。

- [ ] **Step 2: 写入失败诊断和回滚章节**

说明零候选、多候选、provider SDK 缺失、残留旧坐标的诊断命令；回滚到前一发布版本，禁止新旧 artifact 混装。

- [ ] **Step 3: 写入 Java 17 后续实现蓝图**

说明独立 parent/BOM/reactor、`jakarta.*`、Boot 3 注册、三类 adapter SPI/测试要求和 AgentScope 2.0 进程内 SDK 边界；明确本次不实现。

- [ ] **Step 4: 提交文档任务**

```bash
git add fw-plus-ai/README.md fw-plus-ai/README.zh-CN.md docs/ai-framework-migration
git commit -m "docs(ai): document adapter migration and java17 roadmap"
```

### Task 10: 全量验收、任务勾选与最终审查

**Files:**
- Modify: `openspec/changes/neutral-ai-adapter-autodiscovery/tasks.md`
- Create: `docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-verify.md`

- [ ] **Step 1: 用 JDK 8 运行 Core adapter reactor**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter -am test`
Expected: BUILD SUCCESS。

- [ ] **Step 2: 用 JDK 8 运行 AI Boot 自动配置、Magic-API 与 AskData 测试**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test`
Expected: BUILD SUCCESS，记录测试数量和失败数。

- [ ] **Step 3: 执行边界、依赖树和 OpenSpec 验证**

Run: `openspec validate neutral-ai-adapter-autodiscovery --type change --strict && git diff --check`
Expected: 验证通过且无空白错误。

- [ ] **Step 4: 逐项勾选完成任务并写验证报告**

报告必须记录 JDK 8 环境、通过命令、失败/重试记录、旧坐标扫描结果、遗留风险和 Java 17 非实现边界。

- [ ] **Step 5: 提交验收产物**

```bash
git add openspec/changes/neutral-ai-adapter-autodiscovery/tasks.md docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-verify.md
git commit -m "test(ai): verify neutral adapter autodiscovery"
```
