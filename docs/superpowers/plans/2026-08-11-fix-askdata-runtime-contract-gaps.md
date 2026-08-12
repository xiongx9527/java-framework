# AskData Runtime Contract Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一 AskData 与中立模型/向量契约，交付 Java 8 LangChain4j Adapter，并补齐默认执行链中的模型路由、多问题、SQL 插件、Agent、观测和压缩缺口。

**Architecture:** `fw-plus-ai-model` 负责厂商无关的模型定义、创建、注册和类型化执行；`fw-plus-ai-store-embedding` 负责中立向量存储；`fw-plus-ai-askdata` 负责带项目/操作人边界的业务 SPI。LangChain4j Adapter 只做 SDK 转换和异常分类，Spring Boot 自动配置只做能力发现、冲突校验和 Core SPI 接线。

**Tech Stack:** Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0、JUnit 4/5（沿用模块现状）、Maven 3.9.x、OpenSpec。

## Global Constraints

- 所有 Core 模块必须以 Java 8 编译，class major version 必须为 52。
- Core 公共签名不得引用 Spring、LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 类型。
- AskData Starter 不传递具体 AI 框架 Adapter 或 Provider；Adapter 由业务显式选择。
- 当前分支只实现 LangChain4j Adapter；其他三类 Adapter 只交付 Java 17 / Spring Boot 3 分支契约文档。
- 业务 Core SPI Bean 优先于默认实现；重复 key、未知 key 和版本不兼容必须明确失败。
- 治理授权、SQL 最终校验和监控脱敏边界不得因 Adapter 或兼容桥接而弱化。
- 不修改 `magicapi-dynamic-entity-plugin`、TinyEngine 或其他非 AskData 模块。

---

### Task 1: 中立类型化模型契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiChatModel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiChatRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiChatResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiEmbeddingModel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiEmbeddingRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiEmbeddingResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main/java/com/xiongx9527/fw/plus/ai/model/AiModelUsage.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/test/java/com/xiongx9527/fw/plus/ai/model/TypedAiModelContractTest.java`

**Interfaces:**
- Produces: `AiChatModel extends AiModel`, `AiEmbeddingModel extends AiModel` and immutable request/result values consumed by Task 3 and Task 4.

- [ ] **Step 1: Write failing immutable-contract tests**

```java
@Test
public void shouldExposeTypedChatAndBatchEmbeddingContracts() {
    AiChatRequest chat = new AiChatRequest("prompt", singletonMap("temperature", 0.2D), 3000L);
    AiEmbeddingRequest embedding = new AiEmbeddingRequest(Arrays.asList("a", "b"), emptyMap(), 3000L);

    assertEquals("prompt", chat.getInput());
    assertEquals(2, embedding.getInputs().size());
    assertThrows(UnsupportedOperationException.class,
            () -> embedding.getInputs().add("c"));
}
```

- [ ] **Step 2: Run the model test and verify it fails because the typed contracts do not exist**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-model -am \
  -Dtest=TypedAiModelContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Implement the minimal typed interfaces and immutable values**

```java
public interface AiChatModel extends AiModel {
    AiChatResult chat(AiChatRequest request);
}

public interface AiEmbeddingModel extends AiModel {
    AiEmbeddingResult embed(AiEmbeddingRequest request);
}

public final class AiChatResult {
    private final String text;
    private final String actualModelKey;
    private final String finishReason;
    private final AiModelUsage usage;
}
```

`AiEmbeddingRequest` 必须支持批量文本并防御性复制；`AiEmbeddingResult` 必须防御性复制每个 `float[]`；参数 Map 不得返回可修改引用；`timeoutMillis` 小于零时拒绝。

- [ ] **Step 4: Run all `fw-plus-ai-model` tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-model -am test
```

- [ ] **Step 5: Commit the typed model contract**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model
git commit -m "fw-plus-ai-model: 增加类型化模型执行契约"
```

### Task 2: Embedding Store 授权过滤契约

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingQuery.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingStore.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetriever.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreRegistryTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetrieverTest.java`

**Interfaces:**
- Consumes: Task 1 `AiEmbeddingModel` through the AskData bridge added in Task 4.
- Produces: `AiEmbeddingQuery(..., Map<String,Object> filters)` and `AiEmbeddingStore.supportsFilters()`.

- [ ] **Step 1: Write failing filter preservation and fail-closed tests**

```java
@Test
public void shouldRejectAuthorizedFiltersWhenStoreCannotApplyThem() {
    AiEmbeddingQuery query = new AiEmbeddingQuery(vector, 5, 0.0D,
            singletonMap("projectId", "p1"));
    assertFalse(store.supportsFilters());
    assertThrows(AiEmbeddingStoreException.class, () -> guardedSearch(store, query));
}
```

- [ ] **Step 2: Run the two focused test classes and verify failure**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-store-embedding -am \
  -Dtest=DefaultAiEmbeddingStoreRegistryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Add immutable filters and a backward-compatible capability method**

```java
public interface AiEmbeddingStore {
    default boolean supportsFilters() {
        return false;
    }

    AiEmbeddingSearchResult search(AiEmbeddingQuery query);
}
```

保留原三参数 `AiEmbeddingQuery` 构造方法并委托空过滤条件；新构造方法复制 Map。Retriever 只有在过滤为空或 Store 明确支持过滤时调用 Store，否则抛出受控异常。

- [ ] **Step 4: Run Store and Retriever tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-store-embedding -am test
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=AiEmbeddingStoreAskDataVectorRetrieverTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 5: Commit the Store filter contract**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector
git commit -m "fw-plus-ai-store: 保留向量检索授权过滤"
```

### Task 3: LangChain4j 类型化模型 Adapter

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/DefaultLangChain4jModelFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/LangChain4jAiChatModel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/LangChain4jAiEmbeddingModel.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/adapter/LangChain4jModelAdapterAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/LangChain4jModelAskDataAutoConfiguration.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/adapter/DefaultLangChain4jModelFactoryTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/LangChain4jAskDataModelRegistryTest.java`

**Interfaces:**
- Consumes: Task 1 `AiChatModel` and `AiEmbeddingModel`.
- Produces: LangChain4j-backed typed model instances registered in `AiModelRegistry`.

- [ ] **Step 1: Write failing Adapter contract tests**

```java
@Test
public void shouldCreateTypedChatModelAndReportActualKey() {
    AiModel model = factory.create(chatDefinition("sales-chat"), adapters);
    assertTrue(model instanceof AiChatModel);
    AiChatResult result = ((AiChatModel) model).chat(new AiChatRequest("hello", emptyMap(), 0L));
    assertEquals("sales-chat", result.getActualModelKey());
}
```

Embedding 测试必须传入两个文本并断言返回向量顺序和数量一致；异常测试必须把超时/网络错误映射为中立 Adapter 异常，不泄漏 SDK 类型到公共签名。

- [ ] **Step 2: Run LangChain4j focused tests and verify failure**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter -am \
  -Dtest=DefaultLangChain4jModelFactoryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Implement Chat and Embedding wrappers**

```java
final class LangChain4jAiChatModel implements AiChatModel {
    private final AiModelDefinition definition;
    private final ChatLanguageModel delegate;

    @Override
    public AiChatResult chat(AiChatRequest request) {
        String text = delegate.generate(request.getInput());
        return new AiChatResult(text, getKey(), null, AiModelUsage.unknown());
    }
}
```

Embedding wrapper 对批量输入逐项调用 0.35.0 API，并返回防御性复制后的 `float[]`。不得在 Adapter 中实现 AskData 权限、SQL 或重试策略。

- [ ] **Step 4: Update auto-configuration and run Adapter tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jAskDataModelRegistryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 5: Commit the LangChain4j Adapter**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "fw-plus-ai: 实现 LangChain4j 类型化模型适配"
```

### Task 4: Core SPI 接线与模型路由

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/model/AskDataChatRequest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/model/AskDataChatResult.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/vector/AskDataEmbeddingModel.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/CoreAiModelAskDataAdapter.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java`

**Interfaces:**
- Consumes: Task 1 typed models, Task 3 LangChain4j implementations, existing `AskDataModelRouter` and capability registry.
- Produces: a default Flow that records the actual model key and uses business Core SPI beans first.

- [ ] **Step 1: Write failing Spring context replacement tests**

```java
@Test
public void shouldUseBusinessCoreMetadataAndConversationStore() {
    context.registerBean(com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider.class,
            () -> businessMetadataProvider);
    context.refresh();
    assertSame(businessMetadataProvider,
            context.getBean(com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider.class));
    assertSame(businessConversationStore,
            context.getBean(AskDataFlowExecutor.class).conversationStoreForTest());
}
```

增加能力注册测试：两个相同 key 的 Provider 必须启动失败；业务 Chat/Embedding、会话和元数据 Core Bean 必须优先于兼容桥。

- [ ] **Step 2: Run auto-configuration tests and verify the old wiring fails assertions**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Add one-way legacy bridges and unique capability registry wiring**

`AskDataAutoConfiguration` 必须收集 `ObjectProvider<AskDataCapabilityProvider>`，构建单一 `AskDataCapabilityRegistry`，并使用 `@ConditionalOnMissingBean` 让业务 Core SPI 优先。旧接口只允许包装为 Core，不允许 Core 再委托旧接口。

- [ ] **Step 4: Write and run failing route-attempt tests**

```java
@Test
public void shouldPersistActualFallbackModelKey() {
    AskDataResult result = executor.execute(requestUsingProjectRoute());
    assertEquals("fallback-chat", result.getModelKey());
    assertEquals("fallback-chat", checkpointStore.latest().getModelKey());
    assertEquals(Arrays.asList("primary-chat", "fallback-chat"), routeAudit.modelKeys());
}
```

- [ ] **Step 5: Route every model call through Core `AskDataModelRouter`**

Flow 构造 `AskDataModelRequest`，逐次解析模型并调用 Adapter。只有 `MODEL_INFRASTRUCTURE_FAILURE` 可切换；响应、检查点和事件使用 `AiChatResult.actualModelKey`，不得继续写入原请求 key。

- [ ] **Step 6: Run Core SPI, routing, resume and Starter context tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure,fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -am \
  -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest,AskDataResumeFlowTest,AskDataStarterContextTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: Commit Core SPI wiring and routing**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter
git commit -m "fw-plus-ai: 接通 AskData Core SPI 与模型路由"
```

### Task 5: 多问题 DAG 调度

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/agent/AskDataQuestionPlan.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/agent/AskDataMultiQuestionRequest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/agent/AskDataMultiQuestionResult.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/AskDataMultiQuestionScheduler.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/AskDataMultiQuestionFlowTest.java`

**Interfaces:**
- Consumes: existing Core `AskDataSubQuestion` dependency model and Task 4 single-question execution.
- Produces: `AskDataMultiQuestionScheduler.execute(...)` with stable-ID structured terminal states.

- [ ] **Step 1: Add failing DAG, concurrency, timeout and cancellation tests**

```java
@Test
public void shouldNotRunDependentQuestionBeforeDependencyCompletes() {
    AskDataMultiQuestionResult result = scheduler.execute(plan("q2", dependsOn("q1")));
    assertTrue(startedAt("q2") >= completedAt("q1"));
}

@Test
public void shouldNeverExceedConfiguredConcurrency() {
    scheduler.execute(independentPlan(6), 2);
    assertTrue(maxObservedConcurrency.get() <= 2);
}
```

- [ ] **Step 2: Run `AskDataMultiQuestionFlowTest` and verify failure**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataMultiQuestionFlowTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Implement bounded DAG scheduling**

使用 Java 8 `ExecutorService` 和 `CompletionService`；只提交依赖满足的节点；配置固定最大并发；节点终态为 `SUCCESS`、`FAILED`、`TIMED_OUT`、`CANCELLED`、`SKIPPED`。关闭调度器时必须取消尚未完成的 Future，不能创建无界线程池。

- [ ] **Step 4: Integrate scheduler and preserve single-question behavior**

删除默认 Flow 对 `List<String>` 的直接串行遍历；显式多问题和 Planner 输出统一转换为 Core 计划。每个子节点调用完整的单问题入口并产生独立 conversation/subQuestion 关联。

- [ ] **Step 5: Run multi-question, governance and recovery tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest='AskDataMultiQuestionFlowTest,DefaultAskDataFlowExecutorTest,AskDataResumeFlowTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 6: Commit the multi-question scheduler**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "fw-plus-ai: 实现 AskData 多问题依赖调度"
```

### Task 6: SQL 插件阶段与 Agent 步骤替换

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlInterceptor.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlInterceptorChain.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlCommand.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlStage.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/AskDataAgent.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlPipelineTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/sql/AskDataSqlInterceptorAutoConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java`

**Interfaces:**
- Produces: deterministic stages `COMPOSE`, `REWRITE`, `PREPARE`, `RESULT_MAP`, `AUDIT`; Agent replacement key `getStepKey()`.

- [ ] **Step 1: Add failing named-parameter and stage tests**

```java
@Test
public void shouldPreserveNamedParametersAddedByPlugin() {
    AskDataSqlCommand output = chain.execute(input);
    assertEquals("tenant-1", output.getParameters().get("tenantId").getValue());
}

@Test
public void shouldRejectParameterConflict() {
    assertThrows(IllegalArgumentException.class, () -> chain.execute(commandWithDuplicateParameter()));
}
```

- [ ] **Step 2: Run SQL tests and verify the parameter-preservation assertion fails**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-askdata -am \
  -Dtest=AskDataSqlPipelineTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Implement stage-aware immutable command propagation**

```java
public interface AskDataSqlInterceptor {
    String getKey();
    AskDataSqlStage getStage();
    int getOrder();
    AskDataSqlCommand intercept(AskDataSqlCommand command);
}
```

合并插件新增参数并拒绝冲突；安全上下文取输入值；授权摘要只能保持或收窄；每个改写阶段后保留能力 key 和脱敏摘要；最终统一调用 Core SQL Validator。

- [ ] **Step 4: Add failing Agent replacement tests**

```java
@Test
public void shouldSkipDefaultModelGenerationWhenGenerateSqlAgentSucceeds() {
    executor.execute(request);
    assertEquals(0, defaultChatModel.invocationCount());
    assertEquals(1, sqlValidator.invocationCount());
}
```

- [ ] **Step 5: Add stable Agent step keys and unique replacement selection**

为旧 `AskDataAgent` 提供默认空 step key 兼容旁路 Agent；替换 Agent 必须声明已知 key。自动配置对每个 key 选择唯一实现，冲突时启动失败。类型化步骤结果必须能携带 SQL、参数、验证状态或结果映射，而不是只返回摘要字符串。

- [ ] **Step 6: Run SQL, Agent and governance regression tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest='AskDataSqlInterceptorAutoConfigurationTest,DefaultAskDataFlowExecutorTest,SequentialAskDataAgentDispatcherTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: Commit SQL and Agent changes**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "fw-plus-ai: 完善 SQL 插件与 Agent 步骤替换"
```

### Task 7: 完整观测与可降级上下文压缩

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/observability/AskDataEvent.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/observability/AskDataEventPublisher.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/conversation/DefaultAskDataContextCompressor.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/conversation/FallbackAskDataContextCompressor.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/observability/AskDataObservabilityContractTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogStoreTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/conversation/DefaultAskDataContextCompressorTest.java`

**Interfaces:**
- Consumes: stable sub-question IDs, model attempts, SQL stages and Agent steps from Tasks 4-6.
- Produces: complete event sequence, non-throwing publisher composition and deterministic compression fallback.

- [ ] **Step 1: Add failing full lifecycle event test**

```java
@Test
public void shouldPublishEveryStableLifecycleStage() {
    executor.execute(request);
    assertContainsInOrder(events.types(),
            CONVERSATION_STARTED, QUESTION_PLANNED, SUB_QUESTION_STARTED,
            MODEL_ATTEMPTED, AGENT_NODE_EXECUTED, SQL_COMPOSED,
            SQL_VALIDATED, SQL_EXECUTED, CHECKPOINT_SAVED,
            CONVERSATION_COMPLETED);
}
```

- [ ] **Step 2: Add failing monitoring-store isolation test**

```java
@Test
public void shouldContinueConversationWhenExternalStoreFails() {
    externalStore.failOnAppend();
    AskDataResult result = service.ask(request);
    assertTrue(result.isSuccess());
    assertEquals(1, localFallback.query(context, conversationId).size());
}
```

- [ ] **Step 3: Extend event types and implement non-throwing delivery**

为子问题开始/完成/取消、模型尝试、SQL 组装、压缩、长期记忆、工具和降级增加稳定类型。`toStore` 和组合发布器捕获单个存储/导出异常；先尝试配置存储，再写本地 fallback；所有失败不得向 Flow 抛出。

- [ ] **Step 4: Add failing compression fallback tests**

```java
@Test
public void shouldPreserveStructuredStateWhenSemanticCompressionFails() {
    semanticCompressor.failWithTimeout();
    AskDataCompressedContext compressed = fallback.compress(context);
    assertEquals("project-1", compressed.getProjectId());
    assertEquals("validated", compressed.getCurrentStep());
    assertNotNull(compressed.getCompressedSummary());
}
```

- [ ] **Step 5: Replace raw substring compression with deterministic structured compression**

默认压缩器按问题摘要、已完成步骤、错误和受控检索摘要分配字符预算，不截断结构化安全字段。`FallbackAskDataContextCompressor` 先调用业务/语义压缩器，异常或空结果时调用默认实现并发布 `CONTEXT_COMPRESSION_DEGRADED`。

- [ ] **Step 6: Run observability, compression and recovery tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest='AskDataMonitoringLogStoreTest,DefaultAskDataContextCompressorTest,AskDataResumeFlowTest,DefaultAskDataFlowExecutorTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: Commit observability and compression changes**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "fw-plus-ai: 补齐 AskData 观测与压缩降级"
```

### Task 8: 文档、规格和全量验证

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md`
- Create: `docs/askdata/java17-runtime-adapters.md`
- Modify: `docs/openspec/specs/askdata-core-contracts/spec.md` only through later archive, not during apply
- Modify: `docs/openspec/specs/askdata-query-engine/spec.md` only through later archive, not during apply
- Modify: `docs/openspec/specs/askdata-agent-observability-contracts/spec.md` only through later archive, not during apply
- Modify: `docs/openspec/changes/fix-askdata-runtime-contract-gaps/tasks.md`

**Interfaces:**
- Documents: LangChain4j Java 8 Adapter selection and Java 17 / Spring Boot 3 contracts for Spring AI, Spring AI Alibaba and AgentScope.

- [ ] **Step 1: Update Starter and Java 17 Adapter documentation**

文档必须分别列出三个 Java 17 Adapter 的模块边界、模型/Embedding/向量/Agent/工具映射、能力 key、异常分类、项目和操作人上下文、鉴权、流式事件、SQL 回流、观测、回滚和契约测试。每项状态明确标记“当前 Java 8 分支未实现”。

- [ ] **Step 2: Run Core Reactor tests with Java 8**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-askdata,fw-plus-ai/fw-plus-ai-model,fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter \
  -am clean test
```

- [ ] **Step 3: Install Core and run AI/Starter tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-askdata,fw-plus-ai/fw-plus-ai-model,fw-plus-ai/fw-plus-ai-store-embedding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter \
  -am install -DskipTests
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure,fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter \
  -am clean test
```

- [ ] **Step 4: Validate dependency and bytecode boundaries**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-ai/fw-plus-ai-askdata dependency:tree
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn \
  -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter dependency:tree
javap -verbose \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/target/classes/com/xiongx9527/fw/plus/ai/model/AiChatModel.class \
  | rg 'major version: 52'
```

Core 依赖树不得出现 Spring 或 LangChain4j；AskData Starter 依赖树不得出现具体 LangChain4j Provider、Spring AI、Spring AI Alibaba 或 AgentScope。

- [ ] **Step 5: Validate OpenSpec and repository hygiene**

```bash
cd docs/openspec
openspec validate fix-askdata-runtime-contract-gaps --type change --strict --no-interactive
cd ../..
git diff --check
rg -n 'List\.of\(|Map\.of\(|Map\.copyOf\(|List\.copyOf\(|Optional\.orElseThrow\(\)' \
  fw-plus-core/fw-plus-project/fw-plus-ai \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

- [ ] **Step 6: Mark only verified tasks complete and commit docs**

```bash
git add docs/openspec/changes/fix-askdata-runtime-contract-gaps \
        docs/askdata/java17-runtime-adapters.md \
        fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md
git commit -m "docs(ai): 记录 AskData Adapter 边界与验证结果"
```
