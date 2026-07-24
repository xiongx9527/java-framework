---
change: add-askdata-spring-boot-starter
design-doc: docs/superpowers/specs/2026-07-24-askdata-spring-boot-starter-design.md
base-ref: bf45ed49f309cca5d18c623a03dab70c72cbfa1e
---

# AskData Spring Boot Starter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `fw-plus-ai` 体系内新增 Java 8 兼容的 `fw-plus-ai-askdata-spring-boot-starter`，提供自然语言问数、SQL 生成、可选执行、步骤日志、多模型选择、向量召回、多 Agent 流程、上下文压缩和崩溃恢复能力。

**Architecture:** 新 starter 独立承载 AskData API、默认实现和自动配置，包名统一为 `com.xiongx9527.fw.plus.ai.askdata`，配置前缀为 `fw.plus.ai.askdata`。模型调用优先适配现有 `LangChain4jModelInfo` 注入的模型 Map，向量召回优先适配 Spring 容器中现有 LangChain4j `ContentRetriever` 或 `EmbeddingStore` Bean；工作流采用轻量 Agent 接口，默认同步串行执行，外部图编排仅可选评估 LangGraph4j JDK8 兼容构件。

**Tech Stack:** Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0、Spring JDBC、JUnit、Spring Boot Test、Maven Wrapper。

## Global Constraints

- 必须保持 Java 8 兼容，不使用 Java 9+ API，例如 `List.of()`、`Map.copyOf()`、无参 `Optional.orElseThrow()`。
- 必须兼容 Spring Boot 2.7.18，同时注册 `META-INF/spring.factories` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 配置前缀固定为 `fw.plus.ai.askdata`，不得使用 SQLBot 品牌、Logo、路径或配置前缀。
- 新模块固定为 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter`。
- POM 继承 `fw-plus-ai-boot-starters`，版本使用 `${revision}`，依赖版本通过 `fw-plus-ai-boot-dependencies` 管理。
- 第一版不实现 Magic-API `import askdata`，不实现 UI、用户体系、安装器、MCP Server 或 xpack 能力。
- 默认 SQL 执行策略保守：默认可关闭执行，只允许只读单语句查询，拒绝危险关键字，并强制默认行数限制。
- 多模型必须支持默认聊天模型、默认 embedding 模型和请求级 `modelKey` 覆盖；缺失时返回结构化错误。
- 模型技术顺序固定为 LangChain4j、Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其它实现；当前 Java 8 Starter 不得引入 Spring AI 运行时依赖。
- Agent 图编排可参考 LangGraph4j；当前 Java 8 Starter 只能评估 `langgraph4j-core-jdk8` 等 JDK8 兼容构件作为 optional 适配，不得引入 Java 17+ 的 LangGraph4j 1.8.x 主线模块或 Spring AI 相关 LangGraph4j 模块。
- 后续 Java 17 / Spring Boot 3 新能力必须通过独立 adapter starter 或条件化 adapter 接入，并复用当前 AskData 请求、结果、模型注册、Agent 流程和检查点契约。
- 向量召回只作为 Prompt 上下文片段输入，不直接扩大 SQL 可访问范围。
- 检查点结构化字段不得被上下文摘要替代；恢复时无法确认上一步安全状态必须返回 `RESUME_FAILED`。
- 当前计划创建阶段不得修改 OpenSpec、Design Doc 或源码；后续执行本计划时按任务修改代码和测试。

---

## File Structure

- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`，新增 starter 子模块。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`，纳入新 starter 的 dependencyManagement。
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml`，声明 starter 依赖。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/AskDataService.java`，主服务接口。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/AskDataRequest.java`，请求模型。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/AskDataResult.java`，结果模型。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/AskDataProperties.java`，配置属性。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/AskDataErrorCode.java`，错误码常量。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/model/*`，模型注册与 LangChain4j 适配。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/vector/*`，向量召回 Facade 和默认实现。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/metadata/*`，数据源元数据模型与提供器。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/prompt/*`，Prompt 模板与方言上下文构造。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/*`，SQL 校验、limit 策略和 JDBC 执行。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/flow/*`，Agent、子 Agent、dispatcher 和默认流程。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/conversation/*`，上下文压缩、检查点存储和恢复。
- Create: `.../src/main/java/com/xiongx9527/fw/plus/ai/askdata/autoconfigure/AskDataAutoConfiguration.java`，条件化 Bean 注册。
- Create: `.../src/main/resources/META-INF/spring.factories`，Spring Boot 2.7 兼容注册。
- Create: `.../src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，新式自动配置注册。
- Create: `.../src/test/java/com/xiongx9527/fw/plus/ai/askdata/**/*Test.java`，核心行为测试。

### Task 1: Maven 模块与自动配置资源骨架

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/askdata/autoconfigure/AskDataAutoConfigurationRegistrationTest.java`

**Interfaces:**
- Consumes: existing AI parent `com.xiongx9527.fw.plus.ai:fw-plus-ai-boot-starters:${revision}`.
- Produces: artifact `com.xiongx9527.fw.plus.ai:fw-plus-ai-askdata-spring-boot-starter:${revision}` and class `AskDataAutoConfiguration`.

- [ ] **Step 1: Write failing registration test**

Create `AskDataAutoConfigurationRegistrationTest`:

```java
package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

public class AskDataAutoConfigurationRegistrationTest {

    @Test
    public void shouldRegisterAskDataAutoConfigurationInBothSpringBootMetadataFiles() throws Exception {
        String factories = read("META-INF/spring.factories");
        String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        String autoConfiguration = "com.xiongx9527.fw.plus.ai.askdata.autoconfigure.AskDataAutoConfiguration";
        assertTrue(factories.contains(autoConfiguration));
        assertTrue(imports.contains(autoConfiguration));
    }

    private String read(String path) throws Exception {
        Scanner scanner = new Scanner(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8.name());
        try {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        } finally {
            scanner.close();
        }
    }
}
```

- [ ] **Step 2: Run focused test and verify it fails**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataAutoConfigurationRegistrationTest test`

Expected: FAIL because the module and metadata files do not exist.

- [ ] **Step 3: Add Maven module**

Add `<module>fw-plus-ai-askdata-spring-boot-starter</module>` to `fw-plus-ai-boot-starters/pom.xml`, and add dependencyManagement entry:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-askdata-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

Create new starter `pom.xml` with parent `fw-plus-ai-boot-starters` and dependencies on `fw-plus-ai-boot-autoconfigure`, `fw-plus-ai-langchain4j-spring-boot-starter`, `spring-boot-autoconfigure`, `spring-jdbc`, `langchain4j-core`, and optional `spring-boot-configuration-processor`.

- [ ] **Step 4: Add minimal auto-configuration class and metadata**

Create:

```java
package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
public class AskDataAutoConfiguration {
}
```

Create `spring.factories` containing:

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.xiongx9527.fw.plus.ai.askdata.autoconfigure.AskDataAutoConfiguration
```

Create `AutoConfiguration.imports` containing:

```text
com.xiongx9527.fw.plus.ai.askdata.autoconfigure.AskDataAutoConfiguration
```

- [ ] **Step 5: Run registration test**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataAutoConfigurationRegistrationTest test`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `feat(askdata): add starter module skeleton`.

### Task 2: 核心 API、结果模型与配置属性

**Files:**
- Create: `.../askdata/AskDataService.java`
- Create: `.../askdata/AskDataRequest.java`
- Create: `.../askdata/AskDataResult.java`
- Create: `.../askdata/AskDataStepLog.java`
- Create: `.../askdata/AskDataErrorCode.java`
- Create: `.../askdata/AskDataProperties.java`
- Test: `.../askdata/AskDataApiModelTest.java`
- Test: `.../askdata/autoconfigure/AskDataPropertiesBindingTest.java`

**Interfaces:**
- Produces: `AskDataResult ask(AskDataRequest request)`.
- Produces: `AskDataProperties.Model`, `AskDataProperties.Sql`, `AskDataProperties.Flow`, `AskDataProperties.Conversation`, `AskDataProperties.Vector`.

- [ ] **Step 1: Write failing API model tests**

Create `AskDataApiModelTest`:

```java
package com.xiongx9527.fw.plus.ai.askdata;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AskDataApiModelTest {

    @Test
    public void shouldKeepRequestLevelModelKeyAndOptions() {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("统计本月订单数");
        request.setDatasourceKey("order");
        request.setDialect("mysql");
        request.setModelKey("gpt4o-mini");
        request.setConversationId("conv-1");
        request.setResumeToken("resume-1");
        request.setExecute(Boolean.TRUE);
        request.setMaxRows(50);
        request.setOptions(Collections.singletonMap("tenantId", "9527"));

        assertEquals("gpt4o-mini", request.getModelKey());
        assertEquals("resume-1", request.getResumeToken());
        assertEquals("9527", request.getOptions().get("tenantId"));
    }

    @Test
    public void shouldCreateFailureResultWithStepLog() {
        AskDataResult result = AskDataResult.failure(AskDataErrorCode.SQL_REJECTED, "危险 SQL 被拒绝");
        result.addStep("validate-sql", "SQL 校验", false, "DROP 被拒绝");

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(1, result.getSteps().size());
        assertEquals("validate-sql", result.getSteps().get(0).getStepKey());
    }
}
```

- [ ] **Step 2: Write failing properties binding test**

Create `AskDataPropertiesBindingTest` with `ApplicationContextRunner`:

```java
new ApplicationContextRunner()
    .withUserConfiguration(AskDataAutoConfiguration.class)
    .withPropertyValues(
        "fw.plus.ai.askdata.enabled=true",
        "fw.plus.ai.askdata.model.default-chat-model=chat-default",
        "fw.plus.ai.askdata.model.default-embedding-model=embedding-default",
        "fw.plus.ai.askdata.sql.execute-enabled=false",
        "fw.plus.ai.askdata.sql.default-max-rows=200",
        "fw.plus.ai.askdata.conversation.compression-threshold-chars=8000")
    .run(context -> {
        AskDataProperties properties = context.getBean(AskDataProperties.class);
        assertEquals("chat-default", properties.getModel().getDefaultChatModel());
        assertEquals("embedding-default", properties.getModel().getDefaultEmbeddingModel());
        assertEquals(Integer.valueOf(200), properties.getSql().getDefaultMaxRows());
        assertEquals(Integer.valueOf(8000), properties.getConversation().getCompressionThresholdChars());
    });
```

- [ ] **Step 3: Run tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataApiModelTest,AskDataPropertiesBindingTest test`

Expected: FAIL because API and properties classes do not exist.

- [ ] **Step 4: Implement API and configuration model**

Create Java 8 POJOs with getters/setters. Required signatures:

```java
public interface AskDataService {
    AskDataResult ask(AskDataRequest request);
}

public final class AskDataErrorCode {
    public static final String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";
    public static final String DEFAULT_MODEL_NOT_CONFIGURED = "DEFAULT_MODEL_NOT_CONFIGURED";
    public static final String METADATA_NOT_FOUND = "METADATA_NOT_FOUND";
    public static final String MODEL_RESPONSE_INVALID = "MODEL_RESPONSE_INVALID";
    public static final String SQL_REJECTED = "SQL_REJECTED";
    public static final String SQL_EXECUTION_FAILED = "SQL_EXECUTION_FAILED";
    public static final String RESUME_FAILED = "RESUME_FAILED";
    public static final String CHECKPOINT_NOT_FOUND = "CHECKPOINT_NOT_FOUND";
    public static final String CONTEXT_COMPRESSION_FAILED = "CONTEXT_COMPRESSION_FAILED";
}
```

`AskDataRequest` fields: `question`, `datasourceKey`, `dialect`, `modelKey`, `conversationId`, `resumeToken`, `Boolean execute`, `Integer maxRows`, `Map<String, Object> options`.

`AskDataResult` fields: `success`, `errorCode`, `errorMessage`, `sql`, `List<String> tables`, `chartType`, `brief`, `List<AskDataColumn> columns`, `List<Map<String, Object>> rows`, `Integer rowCount`, `List<AskDataStepLog> steps`, `conversationId`, `resumeToken`, `checkpointId`, `rawModelResponse`.

`AskDataProperties` default values: `enabled=true`, `model.defaultChatModel="default"`, `model.defaultEmbeddingModel="embedding-default"`, `sql.executeEnabled=false`, `sql.defaultMaxRows=100`, `sql.enforceLimit=true`, `conversation.compressionThresholdChars=12000`, `conversation.checkpointVersion="1"`, `flow.defaultSubAgentTimeoutMillis=30000`, `vector.defaultTopK=5`.

- [ ] **Step 5: Enable properties in auto-configuration**

Add `@EnableConfigurationProperties(AskDataProperties.class)` to `AskDataAutoConfiguration`.

- [ ] **Step 6: Run tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `feat(askdata): define core api and properties`.

### Task 3: 多模型注册表与 LangChain4j 适配

**Files:**
- Create: `.../askdata/model/AskDataChatModel.java`
- Create: `.../askdata/model/AskDataEmbeddingModel.java`
- Create: `.../askdata/model/AskDataModelRegistry.java`
- Create: `.../askdata/model/AskDataModelResponse.java`
- Create: `.../askdata/model/AskDataModelException.java`
- Create: `.../askdata/model/LangChain4jAskDataModelRegistry.java`
- Test: `.../askdata/model/LangChain4jAskDataModelRegistryTest.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`

**Interfaces:**
- Consumes: `AskDataProperties.getModel().getDefaultChatModel()`, `AskDataProperties.getModel().getDefaultEmbeddingModel()`, optional existing `LangChain4jModelInfo`.
- Produces: `AskDataModelRegistry.getChatModel(String modelKey)` and `AskDataModelRegistry.getEmbeddingModel(String modelKey)`.

- [ ] **Step 1: Write failing registry tests**

Create tests covering default model, request model, missing request model, and missing default model:

```java
@Test
public void shouldSelectRequestLevelChatModelWhenModelKeyExists() {
    AskDataProperties properties = new AskDataProperties();
    properties.getModel().setDefaultChatModel("default");
    LangChain4jModelInfo modelInfo = new LangChain4jModelInfo();
    Map<String, ChatLanguageModel> models = new HashMap<String, ChatLanguageModel>();
    models.put("default", prompt -> new AiMessage("default-sql"));
    models.put("fast", prompt -> new AiMessage("fast-sql"));
    modelInfo.setChatLanguageModelMap(models);

    AskDataModelRegistry registry = new LangChain4jAskDataModelRegistry(properties, modelInfo);
    assertEquals("fast-sql", registry.getChatModel("fast").generate("question").getText());
}
```

Also assert `getChatModel("missing")` throws `AskDataModelException` with error code `MODEL_NOT_FOUND`, and `getChatModel(null)` throws `DEFAULT_MODEL_NOT_CONFIGURED` when no default model exists.

- [ ] **Step 2: Run focused model tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=LangChain4jAskDataModelRegistryTest test`

Expected: FAIL because model interfaces and registry do not exist.

- [ ] **Step 3: Implement model interfaces and exception**

Required signatures:

```java
public interface AskDataChatModel {
    AskDataModelResponse generate(String prompt);
}

public interface AskDataEmbeddingModel {
    List<Float> embed(String text);
}

public interface AskDataModelRegistry {
    AskDataChatModel getChatModel(String modelKey);
    AskDataEmbeddingModel getEmbeddingModel(String modelKey);
    String getDefaultChatModelKey();
    String getDefaultEmbeddingModelKey();
}
```

`AskDataModelResponse` contains `text`, `rawResponse`, `modelKey`. `AskDataModelException` contains `errorCode` and `message`.

- [ ] **Step 4: Implement LangChain4j adapter**

`LangChain4jAskDataModelRegistry` reads `LangChain4jModelInfo.getChatLanguageModelMap()` and `getEmbeddingModelMap()`. It resolves `null` or blank `modelKey` to configured defaults, validates the resolved key, and adapts:

```java
ChatLanguageModel chatLanguageModel = chatModels.get(resolvedKey);
return prompt -> new AskDataModelResponse(chatLanguageModel.generate(prompt).text(), null, resolvedKey);
```

For embeddings, call existing LangChain4j `EmbeddingModel.embed(text)` and convert vector values to `List<Float>`.

- [ ] **Step 5: Register default model registry bean**

In `AskDataAutoConfiguration`, add:

```java
@Bean
@ConditionalOnMissingBean
public AskDataModelRegistry askDataModelRegistry(AskDataProperties properties,
        ObjectProvider<LangChain4jModelInfo> modelInfoProvider) {
    return new LangChain4jAskDataModelRegistry(properties, modelInfoProvider.getIfAvailable());
}
```

- [ ] **Step 6: Run tests**

Run the command from Step 2.

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `feat(askdata): add multi model registry`.

### Task 4: 向量召回 Facade、元数据模型、Prompt 与方言

**Files:**
- Create: `.../askdata/vector/AskDataVectorRetriever.java`
- Create: `.../askdata/vector/AskDataVectorQuery.java`
- Create: `.../askdata/vector/AskDataRetrievedDocument.java`
- Create: `.../askdata/vector/CompositeAskDataVectorRetriever.java`
- Create: `.../askdata/vector/ContentRetrieverAskDataVectorRetriever.java`
- Create: `.../askdata/metadata/AskDataMetadataProvider.java`
- Create: `.../askdata/metadata/AskDataMetadata.java`
- Create: `.../askdata/metadata/AskDataTable.java`
- Create: `.../askdata/metadata/AskDataColumn.java`
- Create: `.../askdata/metadata/EmptyAskDataMetadataProvider.java`
- Create: `.../askdata/prompt/AskDataDialect.java`
- Create: `.../askdata/prompt/AskDataDialectRegistry.java`
- Create: `.../askdata/prompt/AskDataPromptTemplateProvider.java`
- Create: `.../askdata/prompt/DefaultAskDataPromptTemplateProvider.java`
- Test: `.../askdata/vector/AskDataVectorRetrieverTest.java`
- Test: `.../askdata/prompt/AskDataPromptTemplateProviderTest.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`

**Interfaces:**
- Consumes: optional `ContentRetriever` Bean and `AskDataProperties.Vector`.
- Produces: Prompt text including schema, terms, examples, vector documents, dialect rules, default read-only rule, and row limit rule.

- [ ] **Step 1: Write failing vector test**

Test default empty retriever and `ContentRetriever` adapter:

```java
@Test
public void shouldReturnEmptyListWhenNoRetrieverIsAvailable() {
    AskDataVectorRetriever retriever = new CompositeAskDataVectorRetriever(Collections.<AskDataVectorRetriever>emptyList());
    AskDataVectorQuery query = new AskDataVectorQuery("schema", "order", "vec", "订单数", 3, Collections.<String, Object>emptyMap());
    assertTrue(retriever.retrieve(query).isEmpty());
}
```

- [ ] **Step 2: Write failing Prompt and dialect tests**

Create metadata with one table `orders(id BIGINT, amount DECIMAL, created_at DATETIME)` and documents `术语: GMV = amount 求和`, `样例: 最近7天订单数`. Assert MySQL Prompt contains backtick identifier guidance, `LIMIT 100`, read-only single statement guidance, schema, terms, examples, and retrieved snippets. Assert PostgreSQL uses double quote guidance and ClickHouse uses `LIMIT` guidance.

- [ ] **Step 3: Run tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataVectorRetrieverTest,AskDataPromptTemplateProviderTest test`

Expected: FAIL because vector, metadata, prompt and dialect classes do not exist.

- [ ] **Step 4: Implement vector Facade**

Required signatures:

```java
public interface AskDataVectorRetriever {
    List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query);
}

public class AskDataVectorQuery {
    public AskDataVectorQuery(String retrieveType, String datasourceKey, String vectorStoreKey,
            String question, int topK, Map<String, Object> filters) { ... }
}
```

`CompositeAskDataVectorRetriever` calls delegates in order and concatenates non-null results. `ContentRetrieverAskDataVectorRetriever` adapts LangChain4j `ContentRetriever.retrieve(Query.from(question))` to `AskDataRetrievedDocument`.

- [ ] **Step 5: Implement metadata model and empty provider**

Required signatures:

```java
public interface AskDataMetadataProvider {
    AskDataMetadata load(AskDataRequest request);
}
```

`EmptyAskDataMetadataProvider.load()` returns `AskDataMetadata` with dialect from request and empty tables, terms, examples and prompt fragments.

- [ ] **Step 6: Implement dialect registry and Prompt provider**

`AskDataDialectRegistry` must include keys `mysql`, `postgresql`, `clickhouse`; unknown dialect returns MySQL only when request dialect is blank, otherwise returns a dialect object whose name is the provided value and whose limit rule uses `LIMIT`.

`DefaultAskDataPromptTemplateProvider.buildPrompt(AskDataRequest request, AskDataMetadata metadata, List<AskDataRetrievedDocument> retrievedDocuments, int maxRows)` returns a single String that instructs the model to output JSON with keys `sql`, `tables`, `chartType`, `brief`.

- [ ] **Step 7: Register metadata, vector, dialect and prompt beans**

Register `EmptyAskDataMetadataProvider`, `CompositeAskDataVectorRetriever`, `AskDataDialectRegistry`, and `DefaultAskDataPromptTemplateProvider` with `@ConditionalOnMissingBean`.

- [ ] **Step 8: Run tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 9: Commit**

Commit message: `feat(askdata): add vector metadata prompt support`.

### Task 5: SQL 安全校验、limit 策略与 JDBC 执行

**Files:**
- Create: `.../askdata/sql/AskDataSqlValidator.java`
- Create: `.../askdata/sql/AskDataSqlValidationResult.java`
- Create: `.../askdata/sql/DefaultAskDataSqlValidator.java`
- Create: `.../askdata/sql/AskDataSqlExecutor.java`
- Create: `.../askdata/sql/AskDataSqlExecutionResult.java`
- Create: `.../askdata/sql/JdbcAskDataSqlExecutor.java`
- Test: `.../askdata/sql/DefaultAskDataSqlValidatorTest.java`
- Test: `.../askdata/sql/JdbcAskDataSqlExecutorTest.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`

**Interfaces:**
- Consumes: `AskDataProperties.Sql`, `AskDataDialect`, generated SQL.
- Produces: validated SQL and optional query result rows.

- [ ] **Step 1: Write failing SQL validator tests**

Cover empty SQL, semicolon multi-statement, comments hiding second statements, `INSERT`/`UPDATE`/`DELETE`/`DROP`/`ALTER`/`TRUNCATE`/`CREATE`/`MERGE`, read-only `SELECT`, existing `LIMIT`, and missing limit wrapping.

Expected assertions:

```java
assertFalse(validator.validate("drop table user", mysql, 100).isValid());
assertEquals(AskDataErrorCode.SQL_REJECTED, validator.validate("drop table user", mysql, 100).getErrorCode());
assertEquals("select * from orders limit 100", validator.validate("select * from orders", mysql, 100).getSql());
```

- [ ] **Step 2: Write failing JDBC executor test**

Use embedded H2 through Spring JDBC test dependency if already available through Spring Boot parent. Create table `orders(id bigint, amount decimal)` and assert executor returns column names, column types, two rows and `rowCount=2`.

- [ ] **Step 3: Run SQL tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest test`

Expected: FAIL because SQL classes do not exist.

- [ ] **Step 4: Implement SQL validator**

Required signatures:

```java
public interface AskDataSqlValidator {
    AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows);
}

public class AskDataSqlValidationResult {
    public static AskDataSqlValidationResult accepted(String sql) { ... }
    public static AskDataSqlValidationResult rejected(String errorCode, String message) { ... }
}
```

Validation rules: trim non-empty SQL, reject more than one statement boundary, reject dangerous keywords outside string literals by using a character scanner, allow only SQL starting with `select` or `with`, and add limit through dialect when no limit is present.

- [ ] **Step 5: Implement JDBC executor**

Required signature:

```java
public interface AskDataSqlExecutor {
    AskDataSqlExecutionResult execute(AskDataRequest request, String sql);
}
```

`JdbcAskDataSqlExecutor` uses `JdbcTemplate.query(sql, ResultSetExtractor)` and maps `ResultSetMetaData.getColumnLabel(i)`, `getColumnTypeName(i)`, and row values into `List<Map<String, Object>>`.

- [ ] **Step 6: Register SQL beans**

Register `DefaultAskDataSqlValidator` and `JdbcAskDataSqlExecutor` with `@ConditionalOnMissingBean`. Use `ObjectProvider<JdbcTemplate>` so the starter can load without a `DataSource`; when execution is requested without `JdbcTemplate`, return `SQL_EXECUTION_FAILED`.

- [ ] **Step 7: Run tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 8: Commit**

Commit message: `feat(askdata): add safe sql validation and execution`.

### Task 6: 多 Agent、子 Agent dispatcher 与默认流程

**Files:**
- Create: `.../askdata/flow/AskDataAgent.java`
- Create: `.../askdata/flow/AskDataSubAgent.java`
- Create: `.../askdata/flow/AskDataAgentDispatcher.java`
- Create: `.../askdata/flow/AskDataFlowExecutor.java`
- Create: `.../askdata/flow/AskDataStepContext.java`
- Create: `.../askdata/flow/AskDataStepResult.java`
- Create: `.../askdata/flow/AskDataSubAgentRequest.java`
- Create: `.../askdata/flow/AskDataSubAgentResult.java`
- Create: `.../askdata/flow/AskDataDispatchRequest.java`
- Create: `.../askdata/flow/AskDataDispatchResult.java`
- Create: `.../askdata/flow/SequentialAskDataAgentDispatcher.java`
- Create: `.../askdata/flow/DefaultAskDataFlowExecutor.java`
- Test: `.../askdata/flow/SequentialAskDataAgentDispatcherTest.java`
- Test: `.../askdata/flow/DefaultAskDataFlowExecutorTest.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`

**Interfaces:**
- Consumes: model registry, metadata provider, vector retriever, prompt provider, SQL validator, SQL executor.
- Produces: default step order `metadata -> prompt -> generate-sql -> validate-sql -> execute-sql -> format-result`.

- [ ] **Step 1: Write failing dispatcher tests**

Test synchronous sub-agent execution order, result aggregation, timeout flag when elapsed time exceeds request timeout, and failure policies `CONTINUE`, `DEGRADE`, `TERMINATE`.

- [ ] **Step 2: Write failing flow tests**

Use stubs for metadata, prompt, model, validator and executor. Assert:

```java
assertEquals(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql", "execute-sql", "format-result"), stepKeys(result));
assertEquals("select count(*) from orders limit 100", result.getSql());
assertTrue(result.isSuccess());
```

Also test model missing returns `MODEL_NOT_FOUND`, invalid model response returns `MODEL_RESPONSE_INVALID`, SQL rejected short-circuits before executor, and `execute=false` returns SQL without rows.

- [ ] **Step 3: Run flow tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=SequentialAskDataAgentDispatcherTest,DefaultAskDataFlowExecutorTest test`

Expected: FAIL because flow classes do not exist.

- [ ] **Step 4: Implement Agent interfaces**

Required signatures:

```java
public interface AskDataAgent {
    String getName();
    AskDataStepResult execute(AskDataStepContext context);
}

public interface AskDataSubAgent extends AskDataAgent {
    AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request);
}

public interface AskDataAgentDispatcher {
    AskDataDispatchResult dispatch(AskDataDispatchRequest request);
}

public interface AskDataFlowExecutor {
    AskDataResult execute(AskDataRequest request);
}
```

- [ ] **Step 5: Implement sequential dispatcher**

`SequentialAskDataAgentDispatcher` loops over sub-agents, records start/end millis, timeout flag, success flag, output summary and error message. `TERMINATE` stops and marks dispatch failed; `CONTINUE` keeps original error; `DEGRADE` stores degraded output summary from request.

- [ ] **Step 6: Implement default flow executor**

`DefaultAskDataFlowExecutor` creates `AskDataStepContext`, loads metadata, retrieves vector documents for `schema`, `term`, `example`, builds Prompt, calls `AskDataModelRegistry.getChatModel(request.getModelKey())`, parses model JSON with Jackson already provided by Spring Boot, validates SQL, executes only when effective `execute` is true, and assembles `AskDataResult`.

- [ ] **Step 7: Register flow beans**

Register `SequentialAskDataAgentDispatcher` and `DefaultAskDataFlowExecutor` with `@ConditionalOnMissingBean`.

- [ ] **Step 8: Run tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 9: Commit**

Commit message: `feat(askdata): add agent flow executor`.

### Task 7: 上下文压缩、检查点存储与崩溃后恢复

**Files:**
- Create: `.../askdata/conversation/AskDataConversationStore.java`
- Create: `.../askdata/conversation/AskDataContextCompressor.java`
- Create: `.../askdata/conversation/AskDataCheckpoint.java`
- Create: `.../askdata/conversation/AskDataCompressedContext.java`
- Create: `.../askdata/conversation/InMemoryAskDataConversationStore.java`
- Create: `.../askdata/conversation/DefaultAskDataContextCompressor.java`
- Modify: `.../askdata/flow/DefaultAskDataFlowExecutor.java`
- Test: `.../askdata/conversation/AskDataConversationStoreTest.java`
- Test: `.../askdata/conversation/DefaultAskDataContextCompressorTest.java`
- Test: `.../askdata/flow/AskDataResumeFlowTest.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`

**Interfaces:**
- Consumes: `conversationId`, `resumeToken`, step context and flow state.
- Produces: stable checkpoints after each completed step and resume from latest safe checkpoint.

- [ ] **Step 1: Write failing conversation store tests**

Assert `save`, `loadLatest(conversationId)`, `loadByResumeToken(resumeToken)`, version preservation, and latest checkpoint ordering.

- [ ] **Step 2: Write failing compressor tests**

Build a context with long prompt and raw model response. Assert compressed text length is below threshold and structured fields remain unchanged: `conversationId`, `modelKey`, `datasourceKey`, `dialect`, `sql`, `sqlValidated`, `completedSteps`, `errorCode`.

- [ ] **Step 3: Write failing resume tests**

Cover:

```java
request.setConversationId("conv-1");
request.setResumeToken("resume-validate");
```

When checkpoint has completed `validate-sql` and `sqlValidated=true`, resume skips metadata, prompt, model and validator, then executes SQL or formats existing result. When checkpoint is missing, return `CHECKPOINT_NOT_FOUND`. When version mismatches, return `RESUME_FAILED`. When SQL was not validated, return `RESUME_FAILED` and do not execute.

- [ ] **Step 4: Run conversation and resume tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataConversationStoreTest,DefaultAskDataContextCompressorTest,AskDataResumeFlowTest test`

Expected: FAIL because conversation classes and resume flow do not exist.

- [ ] **Step 5: Implement checkpoint model and store**

Required signatures:

```java
public interface AskDataConversationStore {
    AskDataCheckpoint save(AskDataCheckpoint checkpoint);
    AskDataCheckpoint loadLatest(String conversationId);
    AskDataCheckpoint loadByResumeToken(String resumeToken);
}

public interface AskDataContextCompressor {
    AskDataCompressedContext compress(AskDataStepContext context);
}
```

`AskDataCheckpoint` fields: `conversationId`, `checkpointId`, `resumeToken`, `requestSummary`, `modelKey`, `datasourceKey`, `dialect`, `List<String> completedSteps`, `currentStep`, `sql`, `Boolean sqlValidated`, `Boolean sqlExecuted`, `Integer rowCount`, `compressedSummary`, `List<AskDataStepLog> steps`, `errorCode`, `version`, `createdAt`.

- [ ] **Step 6: Implement default compressor**

Compress only text fields: prompt, raw model response, retrieved snippets and step details. Preserve structured fields by copying them separately into checkpoint fields. If compression fails, return `AskDataResult.failure(CONTEXT_COMPRESSION_FAILED, message)` from the flow.

- [ ] **Step 7: Integrate checkpoints into flow**

After stable steps `metadata`, `generate-sql`, `validate-sql`, `execute-sql` and sub-agent merge, call `conversationStore.save(checkpoint)`. Generate `checkpointId` and `resumeToken` using `UUID.randomUUID().toString()`.

- [ ] **Step 8: Implement resume entry**

At flow start, if request has `resumeToken`, call `loadByResumeToken`; else if request has `conversationId`, call `loadLatest`. Validate version equals `AskDataProperties.Conversation.checkpointVersion`, validate completed step state, and continue from the first incomplete step. Do not repeat SQL execution when checkpoint has `sqlExecuted=true`; return stored result summary and proceed to `format-result`.

- [ ] **Step 9: Register conversation beans**

Register `InMemoryAskDataConversationStore` and `DefaultAskDataContextCompressor` with `@ConditionalOnMissingBean`.

- [ ] **Step 10: Run tests**

Run the command from Step 4.

Expected: PASS.

- [ ] **Step 11: Commit**

Commit message: `feat(askdata): add checkpoints and resume flow`.

### Task 8: AskDataService 默认实现与完整自动配置测试

**Files:**
- Create: `.../askdata/DefaultAskDataService.java`
- Modify: `.../askdata/autoconfigure/AskDataAutoConfiguration.java`
- Test: `.../askdata/DefaultAskDataServiceTest.java`
- Test: `.../askdata/autoconfigure/AskDataAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `AskDataFlowExecutor`.
- Produces: `AskDataService.ask(AskDataRequest request)` and all default replaceable Spring Beans.

- [ ] **Step 1: Write failing service tests**

Assert null request, blank question, delegated success, and delegated failure:

```java
AskDataResult result = service.ask(request);
assertTrue(result.isSuccess());
assertEquals("select count(*) from orders limit 100", result.getSql());
```

For blank question, assert `success=false`, `errorCode=MODEL_RESPONSE_INVALID`, and no flow delegation.

- [ ] **Step 2: Write failing auto-configuration tests**

Using `ApplicationContextRunner`, assert default beans exist: `AskDataProperties`, `AskDataModelRegistry`, `AskDataVectorRetriever`, `AskDataMetadataProvider`, `AskDataPromptTemplateProvider`, `AskDataSqlValidator`, `AskDataSqlExecutor`, `AskDataFlowExecutor`, `AskDataConversationStore`, `AskDataContextCompressor`, `AskDataService`.

Also register custom `AskDataSqlValidator` and custom `AskDataService` beans and assert `@ConditionalOnMissingBean` preserves user beans.

- [ ] **Step 3: Run tests and verify failure**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataServiceTest,AskDataAutoConfigurationTest test`

Expected: FAIL because service and complete auto-configuration are missing.

- [ ] **Step 4: Implement service**

Required class:

```java
public class DefaultAskDataService implements AskDataService {
    private final AskDataFlowExecutor flowExecutor;

    public DefaultAskDataService(AskDataFlowExecutor flowExecutor) {
        this.flowExecutor = flowExecutor;
    }

    @Override
    public AskDataResult ask(AskDataRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return AskDataResult.failure(AskDataErrorCode.MODEL_RESPONSE_INVALID, "question must not be blank");
        }
        return flowExecutor.execute(request);
    }
}
```

- [ ] **Step 5: Complete auto-configuration**

Add `@ConditionalOnProperty(prefix = "fw.plus.ai.askdata", name = "enabled", havingValue = "true", matchIfMissing = true)`. Register every default Bean with `@ConditionalOnMissingBean`. Use `ObjectProvider` for optional dependencies: `LangChain4jModelInfo`, `ContentRetriever`, `JdbcTemplate`, custom sub-agents and custom delegates.

- [ ] **Step 6: Run tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 7: Commit**

Commit message: `feat(askdata): wire service auto configuration`.

### Task 9: Full verification and Maven build gates

**Files:**
- Test-only changes if prior verification exposes compile or behavior gaps.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: verified starter module and aggregate AI build.

- [ ] **Step 1: Run focused starter test suite**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter test`

Expected: PASS. All AskData unit and context tests pass.

- [ ] **Step 2: Run starter module install with upstream dependencies**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -am clean install -DskipTests`

Expected: PASS. Maven compiles upstream AI modules and installs the new starter.

- [ ] **Step 3: Run AI starter aggregate compile**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters -am clean install -DskipTests`

Expected: PASS. Existing AI starters continue to compile with the new module entry.

- [ ] **Step 4: Run dependency tree check for new starter**

Run: `cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter dependency:tree -Dincludes=com.xiongx9527.fw.plus.ai,dev.langchain4j,org.springframework`

Expected: output includes `fw-plus-ai-boot-autoconfigure`, `fw-plus-ai-langchain4j-spring-boot-starter`, Spring Boot 2.7.x managed dependencies, and LangChain4j 0.35.0 managed dependencies. Output does not include SQLBot runtime or UI dependencies.

- [ ] **Step 5: Inspect Java 8 compatibility**

Run: `cd fw-plus-ai && rg -n "List\\.of|Map\\.of|Map\\.copyOf|Set\\.of|Optional\\.orElseThrow\\(\\)" fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/java`

Expected: no matches.

- [ ] **Step 6: Inspect forbidden marker text and brand leakage**

Run: `cd fw-plus-ai && rg -n "SQLBot|sqlbot|待定|稍后实现|补充细节" fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter`

Expected: no matches except test method names or comments deliberately asserting absence of SQLBot text.

- [ ] **Step 7: Commit verification fixes**

If Steps 1-6 required test-only or compile-fix changes, commit message: `test(askdata): complete starter verification`.

Expected: `git status --short` shows only intentional committed changes after commit.

## Self-Review

**Spec coverage:** This plan maps Maven module接入 to Task 1, core API to Task 2, 多模型默认模型和请求级 `modelKey` to Task 3, 向量复用 to Task 4, Prompt/方言 to Task 4, SQL 安全和执行 to Task 5, 多 Agent/子 Agent to Task 6, 上下文压缩和崩溃后恢复 to Task 7, 自动配置 to Tasks 1 and 8, 测试与 Maven Wrapper 验证 to every task and Task 9.

**Placeholder scan:** Plan text avoids forbidden marker text and every task includes exact files, signatures, commands and expected results.

**Type consistency:** Core signatures use `AskDataRequest`, `AskDataResult`, `AskDataModelRegistry`, `AskDataVectorRetriever`, `AskDataFlowExecutor`, `AskDataConversationStore`, and `AskDataContextCompressor` consistently across producing and consuming tasks.
