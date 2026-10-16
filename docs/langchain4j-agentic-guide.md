# langchain4j-agentic 使用全过程

> 完整的 Agentic（智能体编排）使用指南：从状态作用域（scope store）到单 agent、
> 五种编排（sequence / loop / conditional / parallel / parallelMapper）、LLM 自主编排
> （supervisor / planner）、人工介入（human-in-the-loop）、远程 A2A、声明式装配
> （createAgenticSystem + declarative suppliers）、可观测性（observability）、
> agent-as-tool（ToolProvider 包装）与编排注册中心（AgenticPipelineRegistry）。

## 1. 概述

### 1.1 定位与版本

| 项 | 值 |
|---|---|
| 原生依赖 | `dev.langchain4j:langchain4j-agentic:1.22.0-beta32` |
| A2A 支撑 | `dev.langchain4j:langchain4j-agentic-a2a:1.22.0-beta32`（底层 a2a-java-sdk 1.3.1.Final） |
| graph 编排 | `org.bsc:langgraph4j`（1.9.x） |
| 框架门面包 | `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic` |
| 模块 | `fw-plus-ai-boot-autoconfigure` |

### 1.2 隔离层设计（为什么要用门面）

`langchain4j-agentic` 是实验性 beta API（`internal` / `workflow` / `planner` 等包随时可变）。
框架把所有对原生 agentic API 的引用收口在 **1 个门面类 + 13 个隔离层 Builder/句柄类** 中，
业务代码与 magic 脚本只接触框架自有类型与 `UntypedAgent` 句柄，不直接 import 任何
原生实验类型。该约束由 `AgenticIsolationTest` 反射守卫强制保证：

- 门面公开 API 的返回类型 / 参数类型 / 公开字段不得出现
  `dev.langchain4j.agentic.internal.`、`...workflow.`、`...planner.` 前缀及
  `...supervisor.SupervisorAgent`；
- 门面必须暴露 14 个静态工厂：`sequence` / `loop` / `conditional` / `parallel` /
  `parallelMapper` / `supervisor` / `planner` / `humanInTheLoop` / `agent` /
  `agentBuilder` / `createAgenticSystem` / `a2a` / `a2aBuilder` + `instance()`。

用户侧只需要一个入口：

```java
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticWorkflows;

// magic 脚本（Langchain4jModule）与 Java 业务侧共用同一无状态门面
AgenticWorkflows fw = AgenticWorkflows.instance();
```

### 1.3 模式速查（含使用场景）

> 先按下表定位「该用哪种模式」，再进入对应章节看构建参数与执行方式。
> 「何时需要模型」一栏中「视 agent / 分支」表示取决于该子 agent 是否为 AI agent——
> 用非 AI 的确定性 agent（带 `@Agent` 的普通方法）即可离线运行。

| 模式 | 使用场景 | 何时需要模型 |
|---|---|---|
| scope store | 需要跨请求 / 跨进程保留运行状态、断点续跑、跨服务传递、审计 | 否 |
| typed agent | agent 有稳定输入输出契约（接口 + `@Agent`），要类型安全且可复用 | 是 |
| untyped agent | 只要「一个提示词 + 一个输出键」，或运行期动态拼装 | 是 |
| sequence | 步骤与顺序都已知的固定流水线（抽取 → 回答） | 视 agent |
| loop | 反复迭代同一 agent 直到满足退出条件（而非固定步数） | 视 agent |
| conditional | 按运行期状态在分支间择一（意图路由、多策略选择） | 视分支 |
| parallel | 多个互不依赖的子任务并行以缩短总耗时 | 视 agent |
| parallelMapper | 对一批同类元素做同一种处理（列表逐项映射） | 视映射 agent |
| supervisor | 不确定调哪些 agent、什么顺序，交给监督者模型自主决策调度 | 是 |
| planner | 同 supervisor，但更强调显式规划，适合多步骤、有依赖的自主编排 | 是 |
| human-in-the-loop | 流程中需要人工审批 / 补充 / 确认后才能继续 | 否 |
| declarative | 整条编排可用注解静态声明，「接口即系统」、少样板 | 视子 agent |
| A2A | 子 agent 部署在远端，需要跨进程 / 跨语言调用 | 视远端 agent |
| observability | 需要观测 / 审计编排执行（输入输出、耗时、token、失败） | 否 |
| agent-as-tool | 把整条编排当作工具交给上层模型自主决定何时调用 | 是（上层 LLM） |
| graph | 流程是有向图（条件边 / 环 / 分支汇聚），需显式图结构或挂为工具 | 否 |
| registry | 多条编排需统一注册 / 按 key 查找复用 | 否 |

## 2. 状态作用域 scope store（一切编排的运行时数据总线）

> **使用场景**：编排需要在 agent 之间读写 / 共享状态（几乎所有编排都隐式依赖），
> 或需要把作用域持久化以支持长会话恢复、跨服务传递与审计时。

### 2.1 概念

agentic 编排不是函数流水线，而是「带状态的多 agent 协作」：每次调用创建一个
`AgenticScope`（默认 `DefaultAgenticScope`），agent 之间通过 `readState / writeState`
读写具名状态（按 `@V("变量名")` 与 `outputKey` 关联）。`UntypedAgent` 与 `SupervisorAgent`
都实现 `AgenticScopeAccess`，可拿到/驱逐某 memoryId 的作用域：

```java
AgenticScope scope = agent.getAgenticScope(memoryId);   // 取 role 作用域
agent.evictAgenticScope(memoryId);                       // 驱逐（释放）
```

`AgenticScope` 核心 API：

```java
scope.memoryId();
scope.writeState("key", value);
scope.writeState(TypedKey.class, value);        // 类型化键
scope.writeStateIfAbsent("key", value);
scope.writeStates(Map.of("a", 1, "b", 2));
scope.hasState("key");
scope.readState("key");                          // Object
scope.readState("key", defaultVal);              // 带默认值
scope.readState(TypedKey.class);                 // 类型化读取
```

### 2.2 持久化 store（AgenticScopeStore）

`AgenticScopeStore` 把 scope 落库/落缓存，用于长会话恢复、跨服务传递、审计：

```java
public interface AgenticScopeStore {
    boolean save(AgenticScopeKey key, DefaultAgenticScope scope);
    Optional<DefaultAgenticScope> load(AgenticScopeKey key);
    boolean delete(AgenticScopeKey key);
    Set<AgenticScopeKey> getAllKeys();
}
```

`AgenticScopeKey` 是 record `(agentId, memoryId)`：同一 agent 的同一会话对应唯一 key。

用 `AgenticScopePersister.INSTANCE.setStore(...)` 全局接入自定义实现（如 Redis / DB；
`AgenticScopeSerializer` 提供 JSON 序列化：`toJson / fromJson`，反序列化需
`allowDeserializationPackagePrefix / allowDeserializationType` 白名单）：

```java
import dev.langchain4j.agentic.scope.*;

// 示例：内存 store（生产可用 Redis 实现，见 fw-plus-redis 增强）
AgenticScopeStore store = new AgenticScopeStore() {
    private final Map<AgenticScopeKey, DefaultAgenticScope> map = new ConcurrentHashMap<>();
    public boolean save(AgenticScopeKey k, DefaultAgenticScope s) { map.put(k, s); return true; }
    public Optional<DefaultAgenticScope> load(AgenticScopeKey k) { return Optional.ofNullable(map.get(k)); }
    public boolean delete(AgenticScopeKey k) { return map.remove(k) != null; }
    public Set<AgenticScopeKey> getAllKeys() { return map.keySet(); }
};
AgenticScopePersister.INSTANCE.setStore(store);
```

### 2.3 结果与挂起（ResultWithAgenticScope / 人工审批）

`invokeWithAgenticScope(...)` 返回 `ResultWithAgenticScope<T>`，同时携带结果与作用域：

```java
ResultWithAgenticScope<String> r = supervisor.invokeWithAgenticScope("用户请求");
r.result();                 // 最终输出
r.agenticScope();           // 执行后的作用域（readState 可读各 outputKey）
r.suspended();              // 是否因人工介入而挂起
r.completePendingResponse("人工批复内容");   // 补交挂起响应后继续
```

编排挂起时可能抛 `AgenticSystemSuspendedException`（`e.scope()` 可取作用域）；
对 `suspended()` 的处理见第 8 章 human-in-the-loop。

## 3. 单 agent（typed / untyped）

### 3.1 typed agent（接口代理）

> **使用场景**：agent 有稳定、可复用的输入 / 输出契约（接口 + `@Agent`），需要类型安全、
> 既能独立调用又能作为 subAgent 复用时。

带 `@Agent` 注解的接口经 `agent(Class)` 构建成可执行代理，可独立执行、也可作为
subAgent 挂入任意编排：

```java
public interface EchoAgent {
    @Agent(name = "echo", description = "回显输入", outputKey = "out")
    @UserMessage("请回显：{{text}}")
    String echo(@V("text") String text);
}

EchoAgent echo = AgenticWorkflows.agent(EchoAgent.class)
        .chatModel(chatModel)          // 必填
        .chatMemoryProvider(provider)  // 可选：记忆
        .tools(toolObjects)            // 可选：绑定工具
        .outputKey("out")
        .build();

String out = echo.echo("hello");       // 独立执行
```

### 3.2 untyped agent（UntypedAgent 句柄）

> **使用场景**：只想要「一个提示词 + 一个输出键」的轻量 agent，或需要在运行期
> （脚本 / 配置）动态拼装、不便预先定义接口时。

不定义接口，直接声明消息模板与响应键，产物为 `UntypedAgent`，`invoke(Map)` 执行：

```java
UntypedAgent agent = AgenticWorkflows.agent()
        .chatModel(chatModel)
        .userMessage("回答：{{question}}")
        .outputKey("answer")
        .build();

Object answer = agent.invoke(Map.of("question", "问题"));
```

typing 细节：
- `agentBuilder()` 与 `agentBuilder(Class)` 是等价别名（供脚本统一命名）；
- typed agent `build()` 返回用户接口；untyped、各编排 `build()` 返回 `UntypedAgent`
  （`dev.langchain4j.agentic.UntypedAgent`，唯一允许暴露给业务的原生类型）；
- typed agent 可直接 `subAgents(echo)` 挂入编排（框架自动拆包）。

## 4. 四种基础编排

> 全部返回 `UntypedAgent`，可独立 `invoke`，也可嵌套为别的编排的 subAgent。

### 4.1 sequence 顺序编排

> **使用场景**：任务可拆成固定的先后步骤（如「抽取 → 回答」「分类 → 检索 → 生成」），
> 步骤与顺序都已知、无需模型自主决策时。

按声明顺序依次执行，前一 agent 的输出经 scope 流入下一 agent：

```java
UntypedAgent pipeline = AgenticWorkflows.sequence()
        .subAgents(extractAgent, matchAgent, generateAgent)   // 支持 typed/untyped/句柄混排
        .outputKey("sql")                                     // 最终输出写回 scope
        .errorHandler(errorCtx -> ErrorRecoveryResult.fallback(Map.of("sql", "")))
        .listener(myAgentListener)
        .build();

Object result = pipeline.invoke(Map.of("question", "统计本月销售额"));
```

### 4.2 loop 循环编排

> **使用场景**：需要对同一 agent 反复迭代直到满足某个退出条件（如内容合规 / 达到长度、
> 迭代修正），而不是固定步数时。

反复执行 subAgents，直到 `exitCondition` 满足或达到 `maxIterations`：

```java
UntypedAgent pipeline = AgenticWorkflows.loop()
        .subAgents(rewriteAgent)
        .maxIterations(5)
        .exitCondition(scope -> {
            String draft = (String) scope.readState("answer", "");
            return draft.endsWith("。");          // 满足条件即退出
        })
        .build();
```

### 4.3 conditional 条件编排

> **使用场景**：执行哪条分支取决于运行期状态（意图、分类、标记），如意图路由、
> 多策略择一——分支与条件可预先声明，但选哪条要等运行时才知道。

以「条件谓词 + agent」成对声明分支，按 scope 状态路由：

```java
UntypedAgent pipeline = AgenticWorkflows.conditional()
        .subAgents(
            scope -> "query".equals(scope.readState("intent")), queryAgent,
            scope -> "report".equals(scope.readState("intent")), reportAgent)
        .build();
```

### 4.4 parallel 并行编排（fan-out）

> **使用场景**：多个子任务**互不依赖**、可同时进行以缩短总耗时（如同一段文本同时翻译 +
> 写宣传语、多路检索后汇总）。有前后依赖的步骤不要放这里，改用 sequence。

多个独立 agent 同时执行（互不依赖），结果按各自 outputKey 写回：

```java
UntypedAgent pipeline = AgenticWorkflows.parallel()
        .subAgents(weatherAgent, stockAgent)      // 各带 outputKey
        .outputKey("digest")
        .build();
```

## 5. parallelMapper 并行映射编排

> **使用场景**：对一批同类元素做同一种处理——列表逐项映射、批量抽取 / 翻译 / 分类，
> fan-out 后聚合为一个结果列表。

对 `itemsProvider` 命名的列表逐项并行执行「映射 agent」（逐项输出按
`outputKey_索引` 写回后汇聚为列表，写回 `outputKey`）：

```java
// 映射 agent：非 AI，确定性变换（也支持 AI agent）
public static class UpperCaseMapper {
    @Agent(name = "upper", description = "把输入项转为大写", outputKey = "mapped")
    public String map(@V("item") String item) {
        return item.toUpperCase();
    }
}

UntypedAgent mapper = AgenticWorkflows.parallelMapper()
        .itemsProvider("items")                    // scope 中的列表键
        .subAgents(new UpperCaseMapper())
        .outputKey("results")                      // 汇聚后列表写回的键
        .executor(Executors.newFixedThreadPool(4)) // 可选：自定义线程池
        .build();

Object result = mapper.invoke(Map.of("items", List.of("a", "b", "c")));
// result == ["A", "B", "C"]
```

要点：映射 agent 必须带 `outputKey`；`itemsProvider` 指向的入参必须是 `List`；
结果顺序与输入一致。`AgenticPatternsTest.parallelMapperShouldMapEachItemAndWriteBackList`
为已验证示例。

## 6. supervisor / planner（LLM 自主编排）

两者都是「LLM 决定下一步执行哪个子 agent」的监督者模式，均**必须显式注入 ChatModel**。

### 6.1 supervisor 监督者

> **使用场景**：事先不确定该调用哪些 agent、以什么顺序，需要让「监督者」模型依据每次
> 请求自主决策与调度一批 subAgents（开放域问答、多工具协作）。

监督者 LLM 依据请求自主选择并调用 subAgents，产物为框架句柄 `AgenticSupervisor`：

```java
Object stepA = AgenticWorkflows.agent(StepA.class).chatModel(model).build();
Object stepB = AgenticWorkflows.agent(StepB.class).chatModel(model).build();

AgenticSupervisor supervisor = AgenticWorkflows.supervisor()
        .chatModel(model)                 // 必填
        .subAgents(stepA, stepB)
        .maxAgentsInvocations(6)          // 最多调用子 agent 次数
        .outputKey("answer")
        .build();

String answer = supervisor.invoke("用户请求");
// 或取结果+作用域：
ResultWithAgenticScope<String> r = supervisor.invokeWithAgenticScope("用户请求");
```

可配置项：`chatMemoryProvider` / `requestGenerator(Function<AgenticScope,String>)` /
`contextGenerationStrategy(SupervisorContextStrategy)`（默认 CHAT_MEMORY）/
`responseStrategy(SupervisorResponseStrategy)`（默认 LAST）/
`supervisorContext(String)` / `output(Function<AgenticScope,Object>)` /
`beforeCall(Consumer<AgenticScope>)` / `compensateOnError(boolean)` /
`errorHandler` / `listener`。

### 6.2 planner LLM 自主规划

> **使用场景**：与 supervisor 同属「模型自主编排」，但更强调**显式规划**——适合多步骤、
> 步骤间有依赖、需要模型先给出执行计划的场景。

产物为 `UntypedAgent`，可独立执行，也可作为 subAgent 挂入其他编排：

```java
UntypedAgent plannerPipeline = AgenticWorkflows.planner()
        .chatModel(model)                 // 必填，缺省 build() 抛 IllegalArgumentException
        .subAgents(stepA)
        .maxAgentsInvocations(4)
        .outputKey("answer")
        .build();

plannerPipeline.invoke(Map.of("request", "用户请求"));
```

要点（`AgenticPlannerBuilder` 实测语义）：
- `maxAgentsInvocations` 默认 10、`contextGenerationStrategy` 默认 CHAT_MEMORY、
  `responseStrategy` 默认 LAST；
- 不调用 `chatModel(...)` 直接 `build()` 会抛 `IllegalArgumentException`（防呆守卫）：
  `planner 需要由调用方经 chatModel(...) 注入 ChatModel`。

## 7. 人工介入 human-in-the-loop

> **使用场景**：流程中需要人工审批 / 补充信息 / 确认后才能继续，如审批流、工单确认、
> 敏感操作的二次确认。

编排执行到该节点时，采用外部提供的 `responseProvider` 响应继续流转（真实的
「人工审批」场景由业务把 provider 接到消息队列 / 审批系统 / WebSocket）。产物为
框架句柄 `AgenticHumanInTheLoop`，可直接作为 sequence / loop / parallel 的 subAgent：

```java
AgenticHumanInTheLoop human = AgenticWorkflows.humanInTheLoop()
        .responseProvider(() -> "人工响应")                // 固定响应
        // 或基于 scope 动态计算： .responseProvider(scope -> compute(scope))
        .outputKey("answer")
        .build();

UntypedAgent pipeline = AgenticWorkflows.sequence()
        .subAgents(human)                                  // 句柄自动拆包挂载
        .build();

Object out = pipeline.invoke(Map.of("request", "是否需要继续？")); // "人工响应"
```

挂起模式：底层节点以 `AgenticSystemSuspendedException` 抛出让执行挂起，
调用方可捕获后经 `ResultWithAgenticScope.completePendingResponse(...)` 补交响应。
`AgenticIsolationTest.businessCallerShouldBuildAndRunUsingOnlyFrameworkTypes` 验证了
「业务仅用框架类型（human 句柄）+ UntypedAgent」即可完成构建与执行。

## 8. A2A 远程 agent（Agent-to-Agent）

> **使用场景**：某个子 agent 部署在远端（另一个服务 / 团队 / 语言），需要通过 A2A 协议
> 跨进程调用——把能力外包，或与异构系统协作时。

把远端 A2A 服务暴露的 agent 作为本地句柄/代理使用，参与本地编排。框架刻意做成
**惰性**：配置阶段（`inputKeys/outputKey/async/...`）不触网，`build()` 时才创建
底层客户端并抓取 `/.well-known/agent-card.json` 确定对端能力（因此 build 需要可访问的对端）。

### 8.1 untyped 远程句柄

```java
UntypedAgent remote = AgenticWorkflows.a2a("http://host:port/agent")
        .inputKeys("request")
        .outputKey("response")
        .async(false)                    // 同步等待
        .tenant("tenantId")              // 可选：租户隔离
        .build();

// 作为远程子 agent 挂入本地编排
UntypedAgent pipeline = AgenticWorkflows.sequence()
        .subAgents(localStep, remote)
        .build();
```

### 8.2 typed 远程代理

```java
public interface RemoteDocAgent {
    @Agent(name = "doc", description = "远程文档处理", outputKey = "result")
    String process(@V("docId") String docId);
}

RemoteDocAgent remote = AgenticWorkflows.a2a("http://host:port/agent", RemoteDocAgent.class)
        .outputKey("result")
        .async(false)
        .build();
String r = remote.process("doc-123");
```

### 8.3 配置项与别名

- `a2a(url)` / `a2aBuilder(url)`（untyped，等价）；`a2a(url, Class)`（typed）；
- common：`inputKeys`（仅 untyped）/ `outputKey` / `async` / `listener(AgentListener)` /
  `clientCustomizer(Consumer<?>)` / `tenant(String)`；
- 服务端由 a2a-java-sdk（`org.a2aproject.sdk`，1.3.1.Final）与
  `langchain4j-agentic-a2a`（`DefaultA2AService / A2AClientInstance / A2ATaskId` 等）提供，
  本层只暴露客户端侧；`AgenticA2AServerTest` 用本地 stub 服务端验证了「只构建不 build()
  不发起网络请求」的惰性语义。

## 9. 声明式 createAgenticSystem + declarative suppliers

> **使用场景**：整条编排可在编译期用注解静态声明，希望「接口即系统」、少样板、易读易维护时。

用注解在接口上直接声明整条编排（不再手工串 builder），`createAgenticSystem` 装配成
接口代理直接调用。支持注解见下表（`dev.langchain4j.agentic.declarative`）：

| 类别 | 注解 |
|---|---|
| 编排 | `@SequenceAgent` `@LoopAgent` `@ConditionalAgent` `@ParallelAgent` `@ParallelMapperAgent` |
| LLM 自主 | `@SupervisorAgent` `@PlannerAgent` |
| 远程接入 | `@A2AClientAgent` `@McpClientAgent` |
| 注册中心引用 | `@RegistryAgent` |
| 运行时配置 | `@Output` `@BeforeCall` `@ErrorHandler` `@ExitCondition` `@ActivationCondition` `@LoopCounter` `@HumanInTheLoop` `@ParallelExecutor` `@K` `@TypedKey` `@SupervisorRequest` |
| suppliers | 见 9.2 |

### 9.1 最小示例（顺序编排）

```java
public interface DeclA {
    @Agent(name = "A", description = "第一步")
    @UserMessage("A: {{request}}")
    String run(@V("request") String request);
}

public interface DeclB {
    @Agent(name = "B", description = "第二步", outputKey = "finalOut")
    @UserMessage("B: {{request}}")
    String run(@V("request") String request);
}

public interface DeclarativeSequence {
    @SequenceAgent(subAgents = {DeclA.class, DeclB.class}, outputKey = "finalOut")
    String run(@V("request") String request);
}

DeclarativeSequence system = AgenticWorkflows.createAgenticSystem(DeclarativeSequence.class, chatModel);
String out = system.run("用户请求");        // 返回 DeclB 的 finalOut
```

其余重载（`langchain4j-agentic` 1.22.0-beta32 `AgenticServices`）：

```java
T createAgenticSystem(Class<T>);
T createAgenticSystem(Class<T>, ChatModel);                       // 共享模型
T createAgenticSystem(Class<T>, AgentConfigurator);               // 程序化再造
T createAgenticSystem(Class<T>, ChatModel, AgentConfigurator);
```

### 9.2 declarative suppliers（声明式依赖注入）

声明式装配里，模型/记忆/工具等依赖既可以经装配参数注入，也可以由 `Supplier` 注解
**声明一个静态/实例方法**提供，让装配器按名字注入：

| Supplier 注解 | 提供什么 |
|---|---|
| `@ChatModelSupplier` / `@StreamingChatModelSupplier` | 对话 / 流式模型 |
| `@ChatMemorySupplier` / `@ChatMemoryProviderSupplier` | 单条记忆 / 记忆提供者 |
| `@ToolProviderSupplier` / `@ToolsSupplier` | tool provider / 工具数组 |
| `@SystemMessageProviderSupplier` / `@UserMessageProviderSupplier` | 系统 / 用户消息模板 |
| `@RetrievalAugmentorSupplier` / `@ContentRetrieverSupplier` | RAG 增强 / 检索器 |
| `@McpClientSupplier` | MCP 客户端 |
| `@A2AServerUrlSupplier` / `@A2AClientCustomizer` | A2A 服务地址 / 客户端定制 |
| `@AgentListenerSupplier` / `@PlannerSupplier` | 监听器 / planner 实现 |

```java
public interface SupplierDeclarative {
    @ChatModelSupplier
    static ChatModel model() { return ChatModelFactory.create(); }

    @SequenceAgent(subAgents = {DeclA.class, DeclB.class}, outputKey = "finalOut")
    String run(@V("request") String request);
}

SupplierDeclarative sys = AgenticWorkflows.createAgenticSystem(SupplierDeclarative.class);
// 不再需要传 chatModel —— 装配器调用 model() 注入
```

## 10. 可观测性 observability

### 10.1 AgentListener（切片监听）

> **使用场景**：需要观测 / 审计编排执行——每个 agent 的输入输出、耗时、token、失败，
> 用于监控、日志、计费或根因分析时。

`AgentListener` 是一个 9 方法（含 `inheritedBySubagents()`)的监听器接口，挂在
builder 的 `.listener(...)` 上，贯穿调用、工具、scope 与挂起全生命周期：

```java
AgentListener listener = new AgentListener() {
    public void beforeAgentInvocation(AgentRequest r)      { /* 调用前 */ }
    public void afterAgentInvocation(AgentResponse r)      { /* 调用后 */ }
    public void onAgentInvocationError(AgentInvocationError e) { /* 出错 */ }
    public void afterAgenticScopeCreated(AgenticScope s)   { /* scope 创建 */ }
    public void beforeAgenticScopeDestroyed(AgenticScope s){ /* scope 销毁 */ }
    public void onAgenticSystemSuspended(AgenticScope s)   { /* 挂起 */ }
    public void beforeAgentToolExecution(BeforeAgentToolExecution t) { }
    public void afterAgentToolExecution(AfterAgentToolExecution t)   { }
    public boolean inheritedBySubagents()                  { return true; }
};

UntypedAgent pipeline = AgenticWorkflows.sequence()
        .subAgents(...)
        .listener(listener)
        .build();
```

### 10.2 AgentMonitor（内存会话监控）

> **使用场景**：需要开箱即用的内存会话监控（聚合最近 N 次 agent 会话），
> 不想自己实现完整监听器时。

`AgentMonitor implements AgentListener`：内置实现，聚合最近 N 次 agent 会话
（`setMaxRetainedSessions`），配合 `ComposedAgentListener` 可组合多个监听器。

### 11. agent-as-tool：ToolProvider 包装

> **使用场景**：想让上层主 LLM 自主决定何时调用「整条编排」——把流水线（如问数、
> 报表生成）当成一个工具按需触发。

编排产物（`UntypedAgent`）或 langgraph4j 编译图可包装为标准 `ToolProvider`，
挂入 `AssistantBuilder.toolProvider(...)`，让**主 LLM 把整条编排当工具调用**：

```java
// 1) agentic 编排 → tool
UntypedAgent pipeline = AgenticWorkflows.sequence()
        .subAgents(extractor, matcher, generator)
        .outputKey("sql")
        .build();

ToolProvider queryTool = AgenticToolProviders.agenticToolProvider(
        pipeline, "query_data", "按自然语言查询业务数据", "sql", "question");

assistantBuilder.toolProvider(queryTool);
```

```java
// 2) langgraph4j 编译图 → tool
ToolProvider graphTool = AgenticToolProviders.graphToolProvider(
        compiledGraph, "flow_engine", "运行 langgraph 流程");
```

```java
// 3) 按注册中心 key 取编排 → tool（见第 12 章）
ToolProvider tool = AgenticToolProviders.agenticToolProviderByKey(
        registry, "nl2sql-pipeline", "query_data", "按自然语言查询数据", "sql", "question");
```

语义：`inputKeys` 逐个声明为 JSON 字符串属性；工具参数解析为 Map 后调用
`pipeline.invoke(arguments)`；返回 Map 时按 `outputKey` 提取字符串值回传。

## 12. 编排注册中心 AgenticPipelineRegistry

> **使用场景**：多条编排需要统一注册 / 按 key 查找复用，或脚本 / 配置希望按名字取用
> 某条编排、再包装为工具挂入助手时。

注册中心让编排提供方（nl2sql 流水线、业务代码）把 `UntypedAgent` 按 key 注册，
脚本与 tool-provider 资源链按 key 引用，解耦构建与使用：

```java
AgenticPipelineRegistry registry = new AgenticPipelineRegistry();
registry.register("nl2sql-pipeline", pipeline);
registry.contains("nl2sql-pipeline");   // true
registry.get("nl2sql-pipeline");        // UntypedAgent
registry.keys();                        // 不可修改视图
registry.unregister("nl2sql-pipeline");
```

配合 tool 化：
`AgenticToolProviders.agenticToolProviderByKey(registry, "nl2sql-pipeline", ...)`
——key 未注册时抛 `IllegalArgumentException` 并列出已注册 key，便于排障。

## 13. magic 脚本集成

> **使用场景**：希望在 magic-script（不写 Java）里完成「构建 → 执行 → 工具化」编排时。

`Langchain4jModule.agentic()` 暴露门面实例给 magic 脚本（`langchain4j.agentic()`），
脚本内即可完成「构建 → 执行 → 工具化」，无需写 Java：

```js
// magic 脚本（.ms / api）
import langchain4j;
var fw = langchain4j.agentic();

// 例：nl2sql 顺序编排 + 工具化
var pipeline = fw.sequence()
    .subAgents(stepExtract, stepMatch, stepGenerate)
    .outputKey("sql")
    .build();

var rows = db['biz'].select(pipeline.invoke({question: request.body.question}));
return rows;
```

> 依赖提示：`langchain4j-agentic` / `langgraph4j` 为 beta 构件；未引入时调用
> `agentic()` 会抛类缺失错误（由模块 javadoc 明示）。

## 14. 常见问题与注意事项

1. **planner/supervisor 必须注入 ChatModel**：`build()` 缺 model 时 planner 抛
   `IllegalArgumentException`（防呆守卫）。
2. **A2A 惰性构建**：`a2a(...)` 配置阶段不触网，`build()` 才连服务端并抓取
   agent-card，因此真实调用前请确保对端可达。
3. **映射 agent 需带 outputKey**：`parallelMapper` 逐项写回按 `outputKey_索引`，
   不带 `outputKey` 的 agent 无法参与并行映射。
4. **未经白的反序列化**：`AgenticScopeSerializer.fromJson` 默认拒绝未放行的类型，
   需先 `allowDeserializationPackagePrefix(...)` 或 `allowDeserializationType(...)`。
5. **subAgents 混排**：typed agent / untyped agent / `AgenticSupervisor` /
   `AgenticHumanInTheLoop` 可任意混排，框架自动拆包（`AgenticAgents`）。
6. **隔离守卫**：业务 keep 使用上文类型即可；若被迫 import
   `dev.langchain4j.agentic.workflow.*` 等实验包，请评估是否借助门面扩展点规避。
7. **验收基线**：`AgenticPatternsTest` / `AgenticIsolationTest` /
   `AgenticA2AServerTest` / `AgenticToolProviderTest` 共 21 条用例覆盖本文全部模式，
   是门面行为的事实来源。

## 附录 A：门面工厂速查（AgenticWorkflows）

| 工厂 | 返回 | 说明 |
|---|---|---|
| `instance()` | `AgenticWorkflows` | 无状态门面实例 |
| `sequence()` | `AgenticSequenceBuilder` | 顺序编排 |
| `loop()` | `AgenticLoopBuilder` | 循环编排 |
| `conditional()` | `AgenticConditionalBuilder` | 条件分支 |
| `parallel()` | `AgenticParallelBuilder` | 并行 fan-out |
| `parallelMapper()` | `AgenticParallelMapperBuilder` | 列表并行映射 |
| `supervisor()` | `AgenticSupervisorBuilder` | 监督者（产物 `AgenticSupervisor`） |
| `planner()` | `AgenticPlannerBuilder` | LLM 自主规划 |
| `humanInTheLoop()` | `AgenticHumanInTheLoopBuilder` | 人工介入节点 |
| `agent(Class)` | `AgenticTypedAgentBuilder<T>` | typed agent |
| `agent()` / `agentBuilder()` | `AgenticUntypedAgentBuilder` | untyped agent |
| `agentBuilder(Class)` | `AgenticTypedAgentBuilder<T>` | typed agent 别名 |
| `createAgenticSystem(Class[,ChatModel])` | `<T>` | 声明式装配 |
| `a2a(url)` / `a2aBuilder(url)` | `AgenticA2ABuilder` | A2A 远程句柄 |
| `a2a(url, Class)` | `AgenticA2ATypedBuilder<T>` | A2A 远程代理 |
| `graph(stateFactory)` | `StateGraph<T>` | langgraph4j 图入口 |
| `agenticToolProvider(...)` | `ToolProvider` | 编排 → 工具 |
| `graphToolProvider(...)` | `ToolProvider` | 编译图 → 工具 |

## 附录 B：依赖坐标

```xml
<!-- fw-plus-ai-boot-dependencies 已统一版本，业务工程一般只需引 starter -->
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-boot-starter</artifactId>
    <version>0.4.0-SNAPSHOT</version>
</dependency>
<!-- 如需直接使用原生 API（应避免） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic</artifactId>
    <version>1.22.0-beta32</version>
</dependency>
```

**兼容性**：本文 API 针对 `langchain4j-agentic 1.22.0-beta32`；beta 版本 API 可能漂移，
框架隔离层（门面 + 13 个 Builder/句柄类）是业务侧的稳定契约，升级原生依赖时优先在
隔离层内适配。