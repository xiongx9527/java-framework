# 泛型意图识别 + 条件 agentic 编排 + 按意图 RAG 路由设计

- 日期：2026-10-11
- 状态：待评审

## 1. 背景与目标

fw-plus-ai 需要在助手侧提供「意图识别」能力，并让它天然接驳项目已有的 langchain4j-agentic 条件编排（`AgenticWorkflows.conditional(...)` 的 `subAgents(Predicate<AgenticScope>, agent)` 谓词路由）与 RAG（`ContentRetriever`）。目标：

- 提供通用的、与业务零耦合的意图识别组件：意图标识由使用方定义（`I extends Enum<I>`），框架不内置任何业务意图。
- 识别结果能注入 `AgenticScope`，直接支持 `AgenticWorkflows.conditional()` 的条件路由；同时支持「不同意图调用不同 RAG 检索器」，检索层（`ContentRetriever` 包装）与编排层（条件分支各自带检索器）两种形态都提供。
- 识别质量采用「规则 → Embedding → LLM」三级级联：规则层零成本兜高确定性输入；Embedding 层高置信短路；LLM 层兜底精判；低置信统一降级（兜底意图由使用方决定，如 `HUMAN`）。

## 2. 范围

**包含**

- 新增包 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent`，含：
  - 领域模型：`IntentResult<I>`、`IntentContext`、`IntentClassifier<I>`。
  - 三实现 + 编排器：`RuleIntentClassifier<I>`、`EmbeddingIntentClassifier<I>`、`LlmIntentClassifier<I>`、`CascadingIntentClassifier<I>`。
  - 门面 `IntentClassifiers`（静态工厂风格，对齐 `AgenticWorkflows`）。
- agentic 桥接：
  - `AgenticIntents`：scope 写入（`write(AgenticScope, IntentResult<?>)`）与条件谓词工厂（`is(I intent)`），供 `AgenticWorkflows.conditional().subAgents(...)` 使用。
  - `IntentRecognitionStep<I>`：可选 code-agent 步骤，将识别结果以 `intent` 键写入 `AgenticScope`（作为 sequence 的第一步）。
- 按意图 RAG 路由：
  - `IntentAwareContentRetriever<I> implements ContentRetriever`（内部按意图转发到对应检索器；意图来源以 `IntentSupplier` 抽象，支持自分类或复用已判定意图）。
  - 门面 `IntentRag`（`byIntent(classifier)` → `route(I, ContentRetriever)` → `fallback(...)` → `build()`）。
- Spring 装配与配置：
  - `IntentClassifierAutoConfiguration` + `IntentClassifierFactory`（注入 `ChatModel` / `EmbeddingModel` / `IntentProperties`）。
  - `IntentProperties`（`fw.plus.ai.intent.*`）。
- 测试：各层单测、级联短路、agentic 桥接联动、RAG 意图路由、装配冒烟。

**明确不做**

- 不修改 langchain4j-agentic 原生 API、现有 `AgenticWorkflows` 门面、`DifyContentRetriever`、`LangChain4jSupportAutoConfiguration` 等既有组件；`support/intent` 为全新包，不触碰这些既有类。
- 不预置「固定意图枚举」的自动配置 Bean：`I` 由使用方定义，框架只提供工厂与构建器，由使用方以 `@Bean IntentClassifier<MyIntent>` 自建。
- 不实现多意图并行返回（`List<IntentResult<I>>`）；第一版为单意图主识别。
- 不内置任何业务规则与语义示例；规则、示例全部由使用方通过配置或代码提供。
- 不提供官版 `QueryRouter` 适配（`DefaultRetrievalAugmentor` 扩展点）与 `DecisionModelQueryRouter` 包装；保留为后续扩展，第一版以 `ContentRetriever` 包装完成意图化路由。
- 不修改既有 SSE / 助手接口签名。

## 3. 架构与数据流

### 3.1 三级级联

```text
userInput + 会话上下文（IntentContext）
        ↓
RuleIntentClassifier       命中即返回（可顺带抽槽位，置信度=1.0）
        ↓ 未命中
EmbeddingIntentClassifier  embed 预计算示例 → max-pooling + threshold/margin → 高置信短路
        ↓ 低置信
LlmIntentClassifier        AiServices 结构化输出（意图名 + 槽位 + 置信度）
        ↓
CascadingIntentClassifier  统一兜底：Source.FALLBACK，intent 可空（使用方决定映射）
```

### 3.2 两种消费路径

- 独立调用：`IntentClassifier<I>.classify(IntentContext)` → `IntentResult<I>`（意图、置信度、槽位、来源）。
- agentic 编排：`AgenticIntents.write(scope, result)` 把意图枚举名写入 `scope` 的 `intent` 键、槽位平铺写入 `slots.*` → `AgenticWorkflows.conditional().subAgents(AgenticIntents.is(Intent.X), branchAgent)`。
- RAG 路由（检索层）：`IntentAwareContentRetriever` 内部 `意图来源 → byIntent.getOrDefault(intent, fallback).retrieve(query)`，可挂在任意 `.contentRetriever(...)`（含 `AgenticUntypedAgentBuilder.contentRetriever(...)`）。意图来源可自 classify，也可复用编排层已判定的意图以避免重复识别（见 §4.6）。

## 4. 组件设计

### 4.1 领域模型

```java
public enum Source { RULE, EMBEDDING, LLM, FALLBACK }

public record IntentResult<I extends Enum<I>>(
        I intent, double confidence, Map<String, Object> slots, Source source) {
    public boolean confident(double min) { return intent != null && confidence >= min; }
    public static <I extends Enum<I>> IntentResult<I> none(Source s) {
        return new IntentResult<>(null, 0, Map.of(), s);
    }
}

public record IntentContext(String text, List<ChatMessage> history, Map<String, Object> attributes) {
    public static IntentContext of(String text) { return new IntentContext(text, List.of(), Map.of()); }
}

public interface IntentClassifier<I extends Enum<I>> {
    IntentResult<I> classify(IntentContext ctx);
}
```

注：`confident(min)` 仅按「有意图且置信度 ≥ min」判定，组件不硬编码任何意图名（含 `OTHER`）。各层产出置信度，由级联用 `min-confidence` 统一判定；使用方若要表达「未识别」，让其 `OTHER` 结果携带低于门槛的置信度即可。

### 4.2 层实现

- `RuleIntentClassifier<I>`：持有 `List<Rule<I>>`，`Rule(I intent, Pattern pattern, List<String> slotGroups)`；`Matcher.find()` 命中即返回 `(intent, 1.0, 分组槽位, RULE)`。规则由使用方提供，仅放「100% 不会错」的输入。
- `EmbeddingIntentClassifier<I>`：构造时对 `Map<I, List<String>> examples` 逐意图 `embedAll` 预计算；运行时仅 embed 用户输入一次；对每个意图取与示例最大余弦分（max-pooling），与第二名分差（margin）共同作为短路判据：`bestScore ≥ threshold && (bestScore - secondScore) ≥ margin`。
- `LlmIntentClassifier<I>`：以 `AiServices` 构建固定 DTO `LlmIntentOutput(String intent, double confidence, Map<String, String> slots)`；意图清单（枚举名 + 可选描述）经 `systemMessageProvider` 动态注入；返回后用 `I.valueOf(name)` 回填，解析失败按兜底处理。模型声明 `RESPONSE_FORMAT_JSON_SCHEMA` 能力时解析更稳（由使用方模型能力决定，组件不强求）。无法识别（返回 `OTHER` 或解析失败）时置信度记 0，级联据此降级。
- `CascadingIntentClassifier<I>`：按序执行各层，首个 `confident(min)` 即短路；兜底返回 `Source.FALLBACK`，`intent` 可空（由使用方决定把空意图映射为 `HUMAN` 等）。

### 4.3 门面 `IntentClassifiers`

```java
IntentClassifier<MyIntent> c = IntentClassifiers.cascade(MyIntent.class)
        .rule(List.of(rule1, rule2))                     // L0，可选
        .embedding(embeddingModel, examples, 0.72, 0.05)  // L1，可选
        .llm(chatModel)                                   // L2，可选
        .minConfidence(0.6)
        .build();
```

不配置某层则级联跳过；`embedding`/`llm` 均缺省时级联退化为纯规则 + 兜底。

### 4.4 agentic 桥接 `AgenticIntents`

```java
public final class AgenticIntents {
    public static final String SCOPE_KEY = "intent";
    public static final String SCOPE_SLOTS_PREFIX = "slots.";

    /** 写 scope：intent 键 = 枚举名；槽位平铺为 slots.<name>。 */
    public static void write(AgenticScope scope, IntentResult<?> result);

    /** 条件谓词工厂：直接喂给 AgenticWorkflows.conditional().subAgents(...)。 */
    public static <I extends Enum<I>> Predicate<AgenticScope> is(I intent);

    /** 指定 scope 键的谓词（多意图场景可自定义键）。 */
    public static <I extends Enum<I>> Predicate<AgenticScope> is(String scopeKey, I intent);
}
```

### 4.5 识别步骤 `IntentRecognitionStep<I>`

可选方便件：一个 code-agent（`@Agent` POJO），方法签名 `String recognize(@V("request") String request)`，内部 `classify` 后返回意图枚举名；通过 `outputKey(AgenticIntents.SCOPE_KEY)` 写入 scope。定位为 sequence 的第一步：

```java
AgenticWorkflows.sequence(MyAgent.class)
        .subAgents(IntentClassifiers.recognitionStep(classifier), conditional/后续步骤)
        .outputKey("reply").build();
```

第一版 scope 中至少保证 `intent` 键（意图枚举名），槽位写入以如实测支持为准（`@Agent` 方法若能注入 agent 上下文则经 `AgenticIntents.write` 平铺；否则第一版仅写 intent，槽位由业务在分类结果中直接消费）。该取舍在实现验证后以测试固定。

### 4.6 按意图 RAG：`IntentAwareContentRetriever<I>` 与 `IntentRag`

```java
/** 意图来源：可自 classify，也可复用外部已判定的意图（避免重复识别）。 */
@FunctionalInterface
public interface IntentSupplier<I extends Enum<I>> {
    I intent(Query query);
}

public class IntentAwareContentRetriever<I extends Enum<I>> implements ContentRetriever {
    private final IntentSupplier<I> intentSource;   // 关键：意图来源可替换
    private final Map<I, ContentRetriever> byIntent;
    private final ContentRetriever fallback;        // 兜底：空检索（q -> List.of()）或默认库

    /** 意图来源 = 自 classify（形态 1 独用；分类宜为规则/embedding 或带缓存）。 */
    public static <I extends Enum<I>> IntentAwareContentRetriever<I> classifying(
            IntentClassifier<I> classifier, Map<I, ContentRetriever> byIntent, ContentRetriever fallback) {
        return new IntentAwareContentRetriever<>(
                q -> classifier.classify(IntentContext.of(q.text())).intent(), byIntent, fallback);
    }

    /** 意图来源 = 外部供给（复用已判定意图，不重复识别；与编排叠加时用）。 */
    public static <I extends Enum<I>> IntentAwareContentRetriever<I> supplied(
            IntentSupplier<I> intentSource, Map<I, ContentRetriever> byIntent, ContentRetriever fallback) {
        return new IntentAwareContentRetriever<>(intentSource, byIntent, fallback);
    }

    private IntentAwareContentRetriever(IntentSupplier<I> intentSource,
            Map<I, ContentRetriever> byIntent, ContentRetriever fallback) {
        this.intentSource = intentSource;
        this.byIntent = byIntent;
        this.fallback = fallback;
    }

    @Override
    public List<Content> retrieve(Query query) {
        I intent = intentSource.intent(query);
        return byIntent.getOrDefault(intent, fallback).retrieve(query);
    }
}
```

```java
ContentRetriever rag = IntentRag.byIntent(classifier)
        .route(Intent.LOGISTICS, logisticsRetriever)     // 任意 ContentRetriever（含 DifyContentRetriever）
        .route(Intent.REFUND,    refundRetriever)
        .fallback(q -> List.of())                        // 不匹配意图不检索
        .build();
```

挂载：`IntentAwareContentRetriever` 可挂在 `AiServices.builder(...).contentRetriever(...)`，也可挂在 agentic 的 `AgenticUntypedAgentBuilder.contentRetriever(...)`。

**意图来源与避免重复识别（关键）**：RAG 每次问答前由 `RetrievalAugmentor` 触发，若此处再 `classify` 一次，而编排层（`AgenticIntents.is(...)`）已判定过意图，则同一请求的意图识别执行两次、成本与延迟叠加。因此：

- **优先形态 2（编排层）**：意图判定一次并写入 scope，各分支 agent 静态绑定 `.contentRetriever(...)`，检索阶段不判意图，零重复。
- **形态 1 独用**（不经编排）：用 `classifying(...)`，此时分类宜为规则 / Embedding 层，或对同一请求的意图结果加缓存，避免每次检索都跑 LLM。
- **叠加使用**：用 `supplied(...)` 复用外部（编排步骤 / 上下文）已判定的意图，保证一次问答只识别一次。

`IntentSupplier` 返回 `null` 时视为未命中，走 `fallback`。

## 5. Spring 装配与配置

目标：不预置固定泛型 `IntentClassifier<?>` Bean，提供工厂 + 配置。

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IntentProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.service.AiServices")
public class IntentClassifierAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    IntentClassifierFactory intentClassifierFactory(
            ObjectProvider<ChatModel> chatModel,
            ObjectProvider<EmbeddingModel> embeddingModel,
            IntentProperties props) { ... }
}
```

使用方：

```java
@Bean
IntentClassifier<SupportIntent> supportIntent(IntentClassifierFactory f) {
    // 规则 / 语义示例 / 各阈值由 f 从 IntentProperties 读取；也可在链上显式覆盖
    return f.cascade(SupportIntent.class).build();
}
```

`IntentClassifierFactory` 的构建方法从配置与注入模型组合各层；`ChatModel`/`EmbeddingModel` 缺省时对应层自动降级。

`IntentProperties`（`fw.plus.ai.intent.*`）：

```yaml
fw.plus.ai.intent:
  min-confidence: 0.6          # 级联短路统一门槛
  embedding-threshold: 0.72    # L1 绝对阈值
  embedding-margin: 0.05       # L1 与第二名分差
  rules:
    - intent: HUMAN_HANDOFF    # 枚举名
      regex: "转人工|人工客服"
      slotGroups: []
  examples:
    ORDER_QUERY: ["我的订单到哪了", "什么时候发货"]
```

默认 `embedding` 相关阈值仅为示例值；真实值须在使用方语料上标定（验收项见 §9）。

## 6. 错误处理与兼容性

- 模型缺失降级：`ChatModel` 或 `EmbeddingModel` 未装配时，对应层不参与级联，不抛错。
- Embedding 调用失败：捕获并回落下一层，不阻断级联。
- LLM 输出非法（`I.valueOf` 失败、JSON 解析失败、返回空）：按 `Source.FALLBACK` 处理，绝不抛给调用方。
- 槽位透传与校验：组件只透传槽位，不做业务校验；订单号等关键槽位的存在性与归属校验由使用方完成（安全边界）。
- 兜底语义：`CascadingIntentClassifier` 兜底 `intent=null`（`Source.FALLBACK`）；使用方自行映射为空意图动作（如转人工 / 默认话术）。
- 意图来源为空：`IntentAwareContentRetriever` 的意图来源返回 `null` 时走 `fallback`（空检索或默认库），不抛错。
- 兼容性：整套组件新增在 `support/intent` 包内，不改任何既有类签名；`AutoConfiguration.imports` 仅追加新配置类；不触碰工作区既有未提交改动。

## 7. 测试与验收

1. **领域模型**：`IntentResult.confident` 对 `null` 意图、阈值上/下边界的判定。
2. **RuleIntentClassifier**：正则命中、槽位分组、`find` 语义（部分匹配即可）与优先级（按列表序第一条命中）。
3. **EmbeddingIntentClassifier**：预计算时机（构造一次）、max-pooling、threshold/margin 双判据、近义意图（自定义两意图示例）的短路行为。
4. **LlmIntentClassifier**：以替身 `ChatModel` 断言意图清单注入、DTO → `I.valueOf` 回填、非法输出兜底。
5. **CascadingIntentClassifier**：短路顺序（rule → embedding → llm）、兜底 `FALLBACK`、未识别意图的贯通。
6. **AgenticIntents + 条件编排**：复用 `AgenticPatternsTest` 的 `TypedConditionalAgent` 模式，断言 `AgenticIntents.is(...)` 谓词在 `AgenticWorkflows.conditional()` 下的分支路由与 `write` 的 state 形状。
7. **IntentAwareContentRetriever**：按意图转发到对应检索器、未匹配意图（含意图来源返回 `null`）走 fallback（空检索）、`classifying(...)` 自分类与 `supplied(...)` 复用两种意图来源的行为。
8. **意图仅识别一次（叠加场景）**：条件编排 + `IntentAwareContentRetriever.supplied(...)` 组合下，用计数替身断言分类器每个问答只被调用一次。
9. **装配冒烟**：`@SpringBootTest` 下 `IntentClassifierFactory` 注入、模型缺省降级、`IntentProperties` 绑定。
10. **AI 层基线**：`SpringBoot3DependencyBaselineTest` 等既有基线用例保持通过。

验收标准：级联短路行为与兜底语义按测试 1-5 固定；`conditional + is()` 的 agentic 链路端到端可用；`IntentAwareContentRetriever` 能挂入 `.contentRetriever(...)` 并按意图选库；新配置类出现在 `AutoConfiguration.imports` 且不影响既有装配；工作区既有未提交改动不被卷入提交。

## 8. 实施边界

主要涉及：

- `fw-plus-ai-boot-autoconfigure` 的 `.../langchain4j/support/intent/`（领域模型、三层实现、门面、桥接、RAG、装配、配置），以及 `AutoConfiguration.imports`、`spring.factories` 的增量注册。
- `fw-plus-ai-boot-autoconfigure` 的 `src/test/java/.../support/intent/` 测试包。

实现不应修改：`AgenticWorkflows`、`AgenticUntypedAgentBuilder`、`DifyContentRetriever`、`LangChain4jSupportAutoConfiguration`、既有 assistant/SSE 接口；不得把当前工作区已有的未提交改动（含 staged 删除与新增）纳入本次提交。