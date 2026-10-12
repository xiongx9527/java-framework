# langchain4j-agentic 封装（agentic 包隔离层）

本规格描述 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic` 包的完整目标行为：把 langchain4j-agentic（1.22.0-beta32）提供的全部 agentic 模式封装为框架自有隔离层 API。配套将 langchain4j 核心依赖升级至 1.22.0、agentic 实验构件升级至 1.22.0-beta32。

## 0. 依赖版本基线（本次升级）

- `fw-plus-dependencies` 统一版本管理：`langchain4j.version` 1.20.2 → **1.22.0**。
- `langchain4j-agentic` 实验构件随 langchain4j-bom 对应升至 **1.22.0-beta32**；autoconfigure 模块的 `langchain4j-agentic` / `langchain4j-agentic-a2a` optional 依赖无显式版本，由 BOM 统一管理。
- 兼容性：1.20.2 → 1.22.0-beta32 仅新增 API（`a2aBuilder` 重载、`nonAiAgentBuilder`、`streamingClientListener`），对既有编排模式（sequence/loop/conditional/parallel/supervisor/planner/agent 等）无破坏性变更，升级适配成本≈0；封装范围与行为不变。

## 1. 隔离层总则

- 业务代码与 magic 脚本只使用框架自有类型（门面类、builder、`UntypedAgent` 句柄、`ToolProvider`）即可完成全部 agentic 模式的构建、执行与挂载；`dev.langchain4j.agentic.*` 实验性 API 收口在 agentic 包内部。
- 门面无状态、不依赖 Spring 容器；需要 ChatModel 的模式由调用方经 builder 的 `chatModel(...)` 显式注入。
- `langchain4j-agentic` 保持 optional + ConditionalOnClass 收口：未引入依赖时其他功能不受影响。
- 既有实现不要求兼容（用户明确 2026-10-09「不用兼容，不对的都可以推翻重来」）：确认错误或设计不合理的既有封装（含方法签名、脚本入口形态、反映错误行为的测试）可推翻重构，并同步修正全部调用方。

## 2. 已有编排模式（可重构并补齐）

- `sequence()`：顺序编排，subAgents 输出经 AgenticScope 按变量名流转。
- `loop()`：循环编排，maxIterations + exitCondition。
- `conditional()`：条件路由分支。
- `parallel()`：并行 fan-out。
- 配套对齐：各模式 builder 统一暴露 errorHandler / listener 等共有配置项（底层 API 支持即暴露）。

## 3. 新增编排模式

### 3.1 parallelMapper

- 入口：`AgenticWorkflows.parallelMapper()`。
- 支持 `itemsProvider`（指定 AgenticScope 中的列表键）、`executor`（自定义线程池）、subAgents、outputKey、name/description、listener。
- 执行语义：对列表每个元素并行执行映射 agent，结果汇聚写回。

### 3.2 supervisor

- 入口：`AgenticWorkflows.supervisor()`。
- 支持 `chatModel(...)`（必填，由调用方注入）、`subAgents(...)`、`maxAgentsInvocations`、`contextGenerationStrategy` / `responseStrategy` / `requestGenerator`、outputKey、errorHandler、listener。
- 执行语义：监督者 LLM 依据请求自主选择并调用 subAgents，输出写回 outputKey。

### 3.3 planner

- 入口：`AgenticWorkflows.planner()`。
- 支持 `chatModel(...)`（由调用方注入）、planner/拓扑相关配置的最小暴露、outputKey、listener。
- 执行语义：由 Planner 依据 AgenticSystemTopology 自主编排 agents 执行。

### 3.4 humanInTheLoop

- 入口：`AgenticWorkflows.humanInTheLoop()`。
- 支持 `responseProvider(...)`（Supplier 或 Function<AgenticScope,?>）、`async`、inputKey(s)、outputKey、description、listener。
- 执行语义：编排执行到该节点时采用外部提供的人工响应继续流转。

### 3.5 agentBuilder（单 agent）

- 入口：`AgenticWorkflows.agent()`。
- 支持 typed（接口 Class）与 untyped 两种形态，暴露模型、工具、消息等常用配置。
- 产物可独立执行，也可作为 subAgent 挂入任意编排。

## 4. 声明式注解系统

- 入口：`AgenticWorkflows.createAgenticSystem(Class<T>)` 及带 ChatModel / AgentConfigurator 的重载。
- 调用方自行声明带 agentic 注解（@SequenceAgent、@SupervisorAgent、@LoopAgent 等）的接口，门面负责创建并返回编排实例。
- 框架不提供注解扫描或自动注册基础设施。

## 5. A2A 远程客户端

- 入口：`AgenticWorkflows.a2a(String url)`（typed / untyped 重载）。
- 产物为远程 agent 客户端句柄，可作为 subAgent 或工具参与编排；构建与执行不要求调用方接触 `dev.langchain4j.agentic.internal.*` 类型。

## 6. 脚本暴露

- `Langchain4jModule.agentic()` 返回同一 `AgenticWorkflows` 门面，上述全部新增入口对 magic 脚本可见，行为与 Java API 一致。

## 7. 验收场景

- Scenario: 并行映射模式——WHEN 用门面的 parallelMapper 构建编排并指定 itemsProvider THEN 对列表中每个元素并行执行映射 agent，结果汇聚写回
- Scenario: 监督者模式——WHEN 用门面的 supervisor 构建编排、由调用方注入 ChatModel 并注册多个 subAgents THEN 监督者 LLM 依据请求自主选择并调用 subAgents，输出写回 outputKey
- Scenario: 人工介入模式——WHEN 用门面的 humanInTheLoop 构建节点并提供 responseProvider THEN 编排执行到该节点时采用外部提供的人工响应继续流转
- Scenario: 单 agent 构建——WHEN 用门面的 agentBuilder 构建 typed/untyped 单 agent THEN 该 agent 可独立执行并可作为 subAgent 挂入既有编排
- Scenario: 声明式系统——WHEN 调用方声明带 agentic 注解的接口并经门面 createAgenticSystem 创建 THEN 返回可用的编排实例
- Scenario: A2A 远程客户端——WHEN 用门面的 a2aBuilder 指定服务端地址构建远程 agent THEN 该客户端可作为 subAgent/工具参与编排
- Scenario: 隔离层约束——WHEN 检查 agentic 包对外 API THEN 业务调用方只需框架自有类型即可完成构建与执行，实验性 API 收口在包内
- Scenario: 脚本可用——WHEN magic 脚本经 `langchain4j.agentic()` 调用新增模式入口 THEN 行为与 Java API 一致
- Scenario: 测试覆盖——WHEN 在 fw-plus-ai-boot-autoconfigure 运行 `./mvnw test` THEN 全部测试通过（反映错误行为的既有测试随实现一并修正）
