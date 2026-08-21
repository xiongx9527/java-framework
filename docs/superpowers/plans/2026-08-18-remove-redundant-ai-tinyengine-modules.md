# 移除冗余 AI/TinyEngine 模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`、`fw-plus-ai-tiny-engine-plugin-langchain4j` 和 `fw-plus-ai-tiny-engine-spring-boot-starter`，保留 `fw-plus-ai-langchain4j-adapter` 作为 LangChain4j 核心适配实现，并继续让 Tiny Engine 识别 LangChain4j Provider。

**Architecture:** `fw-plus-ai-tiny-engine-spring-boot-starter` 和 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 是依赖聚合器，移除后由业务显式引入已有的 `fw-plus-ai-boot-autoconfigure`、基础 Tiny Engine Starter、模型/向量模块和 `fw-plus-ai-langchain4j-adapter`。由于 `TinyEngineAiProvider` 当前位于 AI Boot 自动配置层，LangChain4j 到 Tiny Engine 的 Provider 桥接放入现有 `fw-plus-ai-boot-autoconfigure`，由 LangChain4j Adapter 自动配置按 classpath 和配置条件加载；不让 Core 适配器反向依赖 Boot 模块。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Maven, LangChain4j 0.35.0, Spring Boot auto-configuration, JUnit 5。

**Spec:** 用户确认的模块职责：Tiny Engine AI 标准层不绑定具体 AI 框架；LangChain4j 由 `fw-plus-ai-langchain4j-adapter` 接入；不保留三个冗余 Starter/Plugin 模块。

## Global Constraints

- 所有新增 Java 代码兼容 Java 8，不使用 Java 9+ API。
- 不让 `fw-plus-ai-langchain4j-adapter` 依赖 `fw-plus-ai-boot-autoconfigure`，保持 Core → Boot 的分层方向。
- Tiny Engine AI 标准接口和 DTO 继续保持框架无关；LangChain4j 类型只能出现在条件适配边界。
- LangChain4j 版本保持项目现有的 0.35.0，不修改 Magic-API 2.2.2 或内部仓库配置。
- 删除模块后，源码、聚合 POM、BOM、样例 POM 和当前文档不得继续引用三个 artifactId；历史归档证据文件不重写。
- 每个删除或迁移动作都必须有对应 Maven/单元测试验证，不能只依赖编译通过。

---

### Task 1: 建立删除前依赖基线和 Provider 迁移测试

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/...`（新增迁移后的 Provider 测试目录）
- Reference: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j/src/test/java/.../LangChain4jTinyEngineAiProviderTest.java`
- Reference: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j/src/test/java/.../LangChain4jTinyEnginePluginAutoConfigurationTest.java`

**Interfaces:**
- Consumes: 现有 `LangChain4jTinyEngineAiProvider` 行为和 `LangChain4jTinyEnginePluginAutoConfiguration` 条件装配测试。
- Produces: 在 Boot AutoConfigure 模块中验证 `TinyEngineAiProvider` 注册和 LangChain4j Provider 行为的测试基线。

- [ ] **Step 1: 将现有 Provider 测试复制到 Boot AutoConfigure 的新包路径**

目标包使用：

```text
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyengine
```

测试类命名为：

```text
LangChain4jTinyEngineAiProviderTest
LangChain4jTinyEngineAutoConfigurationTest
```

先只迁移测试并更新 package/import，不移动生产类。

- [ ] **Step 2: 运行迁移测试确认缺少生产类时失败**

运行：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) \
/usr/local/bin/mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest='LangChain4jTinyEngineAiProviderTest,LangChain4jTinyEngineAutoConfigurationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

预期：测试因新包中的 Provider/AutoConfiguration 尚不存在而失败，证明测试能够覆盖迁移目标。

---

### Task 2: 将 Tiny Engine LangChain4j Provider 迁移到现有 AI Boot 自动配置

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/tinyengine/LangChain4jTinyEngineAiProvider.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/tinyengine/LangChain4jTinyEngineAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/adapter/LangChain4jAdapterAutoConfiguration.java`
- Modify: migrated tests from Task 1
- Delete later: old Provider and AutoConfiguration under `fw-plus-ai-tiny-engine-plugin-langchain4j`

**Interfaces:**
- Consumes: Core `fw-plus-ai-langchain4j-adapter` 的 `LangChain4jAiModel`、`AiModelRegistry`；Boot 层 `TinyEngineAiProvider`、`TinyEngineAiProperties`。
- Produces: 当 LangChain4j Adapter 存在时，注册 key 为 `langchain4j` 的 `TinyEngineAiProvider`；当适配器不存在时自动跳过。

- [ ] **Step 1: 移动 Provider 实现并修改包名**

保留现有 `chat`、`stream`、请求映射、错误脱敏和超时行为，只将 package 改为：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyengine;
```

- [ ] **Step 2: 创建条件自动配置**

使用以下条件和属性：

```java
@Configuration
@ConditionalOnClass({
    TinyEngineAiProvider.class,
    LangChain4jAiModel.class
})
@ConditionalOnProperty(
    prefix = "fw.plus.ai.langchain4j.adapter",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(TinyEngineAiProperties.class)
public class LangChain4jTinyEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "langChain4jTinyEngineAiProvider")
    public TinyEngineAiProvider langChain4jTinyEngineAiProvider(
            ObjectProvider<AiModelRegistry> modelRegistryProvider,
            TinyEngineAiProperties properties) {
        return new LangChain4jTinyEngineAiProvider(
                modelRegistryProvider.getIfAvailable(), properties);
    }
}
```

- [ ] **Step 3: 从 LangChain4j Adapter 自动配置导入 Tiny Engine 桥接**

在 `LangChain4jAdapterAutoConfiguration` 增加：

```java
@Import({
    LangChain4jModelAdapterAutoConfiguration.class,
    LangChain4jEmbeddingStoreAdapterAutoConfiguration.class,
    LangChain4jTinyEngineAutoConfiguration.class
})
```

这样 Tiny Engine Provider 只在 LangChain4j Adapter 自动配置满足条件时出现，不污染通用 Tiny Engine AI 层。

- [ ] **Step 4: 运行迁移测试确认通过**

运行 Task 1 的 Maven 命令，预期 Provider 测试和自动配置测试全部通过。

---

### Task 3: 删除三个模块并重接 Maven 依赖

**Files:**
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j/`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml`
- Modify: `README.md`

**Interfaces:**
- Consumes: Task 2 的 Boot 自动配置和 Core LangChain4j Adapter。
- Produces: 聚合构建不再包含三个 artifactId；AI Tiny Engine sample 使用显式基础依赖完成启动。

- [ ] **Step 1: 从 AI Boot Starters 聚合 POM 删除三个 module**

从 `fw-plus-ai-boot-starters/pom.xml` 删除：

```xml
<module>fw-plus-ai-langchain4j-adapter-spring-boot-starter</module>
<module>fw-plus-ai-tiny-engine-spring-boot-starter</module>
<module>fw-plus-ai-tiny-engine-plugin-langchain4j</module>
```

- [ ] **Step 2: 从 AI BOM 删除三个 dependency management 条目**

从 `fw-plus-ai-boot-dependencies/pom.xml` 删除三个对应 artifactId 的依赖管理项，保留：

```text
fw-plus-ai-langchain4j-adapter
fw-plus-ai-langchain4j-core
fw-plus-ai-langchain4j-openai
```

- [ ] **Step 3: 更新 Tiny Engine AI sample 依赖**

移除：

```xml
<artifactId>fw-plus-ai-tiny-engine-plugin-langchain4j</artifactId>
```

改为显式依赖：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-boot-autoconfigure</artifactId>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-tiny-engine-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-model-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-store-embedding-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
</dependency>
```

具体 LangChain4j Provider SDK 继续由业务按需显式引入。

- [ ] **Step 4: 更新 README 和当前文档**

删除三个模块的产品表格、依赖示例和说明；改为说明：

```text
Tiny Engine AI 标准由 fw-plus-ai-boot-autoconfigure 提供；
LangChain4j 适配由 fw-plus-ai-langchain4j-adapter 提供；
具体 Provider SDK 由业务显式选择。
```

历史 `docs/comet/archive/` 和历史验证快照不重写。

- [ ] **Step 5: 确认当前源代码没有残留引用**

运行：

```bash
rg -n --hidden \
  --glob '!**/target/**' \
  --glob '!**/node_modules/**' \
  'fw-plus-ai-langchain4j-adapter-spring-boot-starter|fw-plus-ai-tiny-engine-plugin-langchain4j|fw-plus-ai-tiny-engine-spring-boot-starter' \
  README.md fw-plus-ai fw-plus-sample docs/tiny-engine
```

预期：只允许历史归档文档中出现，不允许当前 POM、源码、样例或产品文档出现。

---

### Task 4: 聚合构建和依赖边界验证

**Files:**
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter`
- Test: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample`

**Interfaces:**
- Consumes: 三个模块删除后的聚合 POM 和 Task 2 自动配置。
- Produces: 无冗余 artifact、LangChain4j Provider 可注册、Tiny Engine sample 可启动的验证证据。

- [ ] **Step 1: 运行 Core LangChain4j Adapter 测试**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) \
/usr/local/bin/mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter \
  -am test
```

- [ ] **Step 2: 运行 AI Boot AutoConfigure 相关测试**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) \
/usr/local/bin/mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -am -Dtest='*TinyEngine*Test,*LangChain4j*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: 运行 Tiny Engine AI sample 验证**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) \
/usr/local/bin/mvn \
  -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml \
  test
```

- [ ] **Step 4: 验证依赖树和旧 artifact 不再解析**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) \
/usr/local/bin/mvn \
  -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml \
  dependency:tree
```

确认依赖树不包含三个已删除 artifact，并包含 `fw-plus-ai-langchain4j-adapter` 和 `fw-plus-ai-boot-autoconfigure`。

- [ ] **Step 5: 运行格式和工作区检查**

```bash
git diff --check
git status --short
```

---

## 验收覆盖检查

- 三个冗余模块从当前聚合 POM、BOM、sample 和产品文档移除：Task 3。
- LangChain4j Core Adapter 保持 Java 8、无 Boot 反向依赖：Task 2/4。
- Tiny Engine Provider 只在 LangChain4j Adapter 存在时注册：Task 2 的条件自动配置测试。
- 通用 Tiny Engine AI 层不直接引入 LangChain4j：Task 2/3 的条件和依赖树检查。
- 现有 LangChain4j 模型、Embedding Store 和 Tiny Engine AI 测试回归：Task 4。
