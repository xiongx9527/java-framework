# 目标

为框架补充 **langchain4j-agentic 基本能力**（两件套）：

1. **通用编排构建 API**：提供与 AssistantBuilder 同等使用体验的链式构建器——顺序编排（sequence）与循环编排（loop），业务/脚本用框架自有 API 组装多 agent 协作，不直接接触 agentic 实验性 API：

```java
// 顺序编排：输出经 AgenticScope 按 @V 变量名流向下一个 agent
UntypedAgent pipeline = AgenticWorkflows.sequence()
        .subAgents(agent(KeywordExtractor.class), agent(TableMatcher.class), agent(SqlGenerator.class))
        .outputName("sql")
        .build();

// 循环编排：SQL 生成+校验自愈
UntypedAgent sqlLoop = AgenticWorkflows.loop()
        .subAgents(agent(SqlGenerator.class), agent(SqlValidator.class))
        .maxIterations(3)
        .exitCondition(scope -> "OK".equals(scope.getState("validation", String.class)))
        .outputName("sql")
        .build();
```

2. **编排当工具**：`agenticToolProvider(pipeline)` 工厂，把编排产物包装为标准 `ToolProvider`，经现有 `assistant.toolProvider(...)` 挂入助手的 ReAct 循环——主 LLM 自主决定何时调用编排（agent-as-tool）。
3. **graph 编排**：集成 langgraph4j（MIT，`org.bsc.langgraph4j:langgraph4j-core`）——`AgenticWorkflows.graph()` 暴露 StateGraph 构建入口（节点/条件边/checkpoint），`graphToolProvider(compiledGraph)` 把编译图挂为工具。

需求来源：nl2sql（智能问数）Shape 讨论中拆出的独立通用能力；目标分支 springboot3。

# 范围

- 新增 `AgenticWorkflows` 通用构建器（框架自有 API，隔离层包装 langchain4j-agentic 与 langgraph4j）：
  - `sequence()`: subAgents / outputName / build
  - `loop()`: subAgents / maxIterations / exitCondition / outputName / build
  - `conditional()`: 按条件路由到不同 agent 分支
  - `parallel()`: 多 agent 并行 fan-out（/ 并行汇聚）
  - `graph()`: langgraph4j StateGraph 构建入口（节点/条件边）
  - subAgents 支持类型化 agent（`@Agent` 注解接口）与 untyped agent
- 新增 `AgenticToolProvider` 适配器 + `agenticToolProvider(...)` 工厂：
  - 工具名/描述可配置（供主 LLM 决策调用时机）
  - 主 LLM 调用工具时，参数按编排 inputKeys 注入 AgenticScope，outputKey 结果作为工具返回值
  - `graphToolProvider(compiledGraph)`：把 langgraph4j 编译图包装为工具（图入参 state → 工具参数，执行结果 → 工具返回值）
  - 支持 `AgentListener` 观测透传
- magic 脚本暴露：经 langchain4j 模块提供 `langchain4j.agentic()` 入口（sequence/loop/agenticToolProvider 与 Java API 同构），使用体验与 AssistantBuilder 一致
- `langchain4j-agentic` 与 `langgraph4j-core` 依赖均 optional；构建器与适配器按依赖存在性提供，未引入对应依赖的能力不可用且不影响其他功能

# 非目标

- 不做编排定义的资源化/管理界面/持久化编排
- 不做编排定义的资源化/管理界面/持久化编排
- 不做「编排即助手」（SimpleAssistant 心脏替换）
- 不在框架 API 面暴露 agentic 原生类型之外的高级特性（Planner 自定义等）

# 验收示例

- Scenario: 通用顺序编排——WHEN 用 AgenticWorkflows.sequence() 串联多个 agent 并指定 outputName THEN 各 agent 输出经 AgenticScope 按 @V 变量名流向下一个 agent，最终产出 outputName 结果
- Scenario: 通用循环编排——WHEN 用 AgenticWorkflows.loop() 配置 maxIterations 与 exitCondition THEN 反复执行 subAgents 直到条件满足或达上限
- Scenario: 条件路由——WHEN 用 AgenticWorkflows.conditional() 配置分支条件 THEN 按条件路由到对应 agent
- Scenario: 并行编排——WHEN 用 AgenticWorkflows.parallel() 配置多个 agent THEN 并行执行并汇聚结果
- Scenario: graph 编排——WHEN 用 AgenticWorkflows.graph() 构建 StateGraph（节点+条件边）并编译 THEN graphToolProvider 可将其挂为工具执行
- Scenario: 编排当工具——WHEN 将编排产物经 agenticToolProvider 包装并挂入助手 toolProvider THEN 主 LLM 可按工具描述调用该工具并获得编排结果
- Scenario: 入参映射——WHEN 主 LLM 调用工具传参 THEN 参数按编排 inputKeys 注入 AgenticScope
- Scenario: 脚本可用——WHEN magic 脚本经 langchain4j.agentic() 构建 sequence/loop/conditional/parallel/graph 并挂入助手 THEN 行为与 Java API 一致
- Scenario: 依赖可选——WHEN 应用未引入 langchain4j-agentic THEN 构建器与适配器不装配，助手其余功能不受影响

# 约束与不变量

- langchain4j-agentic（beta 实验构件）与 langgraph4j 的使用**收口在 AgenticWorkflows/AgenticToolProvider 及其 builder 类内**（隔离层），不扩散到 AssistantBuilder/SimpleAssistant 等稳定 API；必要时可退化为手写编排
- 工具描述必须充分（主 LLM 依据描述决策调用时机）
- 编排执行异常按工具执行异常语义抛出（主 LLM 可观察并自行调整）
- loop 必须支持轮数上限，防止无限循环

# 决策

- D1: agent-as-tool 模式（编排当工具，主 LLM 自主决策调用时机）+ 通用编排构建器，两件套构成 agentic 基本能力（用户明确 2026-10-01）
- D2: 挂载复用现有 `AssistantBuilder.toolProvider(...)`，不新增 builder 方法（用户示例：`.toolProvider(agenticToolProvider(pipeline))`）
- D3: 框架自有 `AgenticWorkflows` 构建器作隔离层，链式使用体验与 AssistantBuilder 一致（用户明确：「和 AssistantBuilder 一样」）
- D4: 脚本暴露经 langchain4j magic 模块（与 AssistantBuilder 同等暴露方式）
- D5: agentic 依赖 optional + ConditionalOnClass 收口（对齐框架既有 optional 策略）
- D6: 首版编排原语 sequence / loop / conditional / parallel（agentic 内置，用户确认 2026-10-01）；graph 编排集成 langgraph4j（MIT，用户确认「langgraph4j 加吧」），不做自研 GOAP Planner

# 待解决问题

（无 [blocking] 项）

# 验证预期

- 单测：sequence 变量流转、loop 退出条件与上限、工具参数映射与结果返回、未引依赖时的条件装配行为
- 编译验证：fw-plus-ai 层全量编译（含 optional 依赖存在/缺失两种形态）
