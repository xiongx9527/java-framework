# Outcome

在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic` 包内，把 langchain4j-agentic（1.22.0-beta32）提供的**全部** agentic 模式封装为框架自有隔离层 API，业务与 magic 脚本不直接接触实验性原生 API，使用体验与既有 `AgenticWorkflows` / `AssistantBuilder` 一致；同时把 langchain4j 核心依赖升级至最新稳定版 1.22.0、agentic 实验构件升级至 1.22.0-beta32，并完成 API 适配与测试对齐。

# Scope

- 目标包：`fw-plus-ai-boot-autoconfigure` 的 `...langchain4j.support.agentic`
- 现状（已封装）：sequence / loop / conditional / parallel 四种编排 + graph（langgraph4j）+ agent-as-tool（AgenticToolProvider）+ 编排注册中心（AgenticPipelineRegistry）——可随本次重构，不要求保持既有形态
- 本次新增封装（全量覆盖 langchain4j-agentic 1.20.2 模式，D2）：
  - `parallelMapper`：对 AgenticScope 中的列表逐项并行映射（fan-out/map）
  - `supervisor`：监督者 LLM 自主路由/调度多个 subAgents
  - `planner`：基于 Planner（AgenticSystemTopology）的自主编排
  - `humanInTheLoop`：人工介入节点（responseProvider / 挂起恢复）
  - `agentBuilder`：单 agent（typed/untyped）构建，可作为 subAgent 挂入编排
  - `createAgenticSystem`：声明式注解系统（@SequenceAgent/@SupervisorAgent 等注解接口一键创建）
  - `a2aBuilder`：A2A（Agent-to-Agent）远程 agent 客户端
- ChatModel 注入（D4）：supervisor / planner 等需要模型的 builder 暴露 `chatModel(...)`，由调用方显式传入；门面保持无状态、不依赖 Spring 容器
- 配套：既有 builder 补齐模式间缺失能力（errorHandler / listener 等对齐）
- 脚本暴露（D3）：新增方法加入 `AgenticWorkflows` 门面，magic 脚本经 `langchain4j.agentic()` 同步获得全部新模式
- 依赖升级：`langchain4j.version` 1.20.2 → 1.22.0（`fw-plus-dependencies` 统一版本管理），`langchain4j-agentic` 实验构件随 langchain4j-bom 对应升至 1.22.0-beta32；核对 API 变更并对齐封装与测试
- 测试：每种新增模式至少一个单测（mock ChatModel）

# Non-goals

- 不做编排定义的资源化/可视化画布/持久化
- 不做「编排即助手」（SimpleAssistant 心脏替换）
- 不做声明式注解的扫描/自动注册基础设施（`createAgenticSystem` 仅提供门面入口，注解接口由调用方声明）

# Acceptance examples

- Scenario: 并行映射模式——WHEN 用门面的 parallelMapper 构建编排并指定 itemsProvider THEN 对列表中每个元素并行执行映射 agent，结果汇聚写回
- Scenario: 监督者模式——WHEN 用门面的 supervisor 构建编排、由调用方注入 ChatModel 并注册多个 subAgents THEN 监督者 LLM 依据请求自主选择并调用 subAgents，输出写回 outputKey
- Scenario: 人工介入模式——WHEN 用门面的 humanInTheLoop 构建节点并提供 responseProvider THEN 编排执行到该节点时采用外部提供的人工响应继续流转
- Scenario: 单 agent 构建——WHEN 用门面的 agentBuilder 构建 typed/untyped 单 agent THEN 该 agent 可独立执行并可作为 subAgent 挂入既有编排
- Scenario: 声明式系统——WHEN 调用方声明带 agentic 注解的接口并经门面 createAgenticSystem 创建 THEN 返回可用的编排实例
- Scenario: A2A 远程客户端——WHEN 用门面的 a2aBuilder 指定服务端地址构建远程 agent THEN 该客户端可作为 subAgent/工具参与编排
- Scenario: 隔离层约束——WHEN 检查 agentic 包对外 API THEN 业务调用方只需框架自有类型即可完成构建与执行，实验性 API 收口在包内
- Scenario: 脚本可用——WHEN magic 脚本经 `langchain4j.agentic()` 调用新增模式入口 THEN 行为与 Java API 一致
- Scenario: 测试覆盖——WHEN 在 fw-plus-ai-boot-autoconfigure 运行 `./mvnw test` THEN 新增模式的单测全部通过，既有测试全部通过（反映错误行为的既有测试随实现一并修正）
- Scenario: 版本升级——WHEN 以升级后的基线（langchain4j 1.22.0 / agentic 1.22.0-beta32）构建并运行 THEN 全部封装模式编译与测试通过，升级不引入破坏性行为变更

# Constraints and invariants

- langchain4j-agentic（beta 实验构件）使用收口在 agentic 包隔离层内，不扩散到 AssistantBuilder/SimpleAssistant 等稳定 API
- 沿用既有约定：链式 builder 风格与 AssistantBuilder 一致；依赖 optional + ConditionalOnClass 收口
- 门面无状态、不依赖 Spring 容器；ChatModel 由调用方注入
- 源文件 UTF-8、Tab 缩进与周围文件一致；fw-plus-ai 层语言级别遵循模块现状
- 既有 agentic-basic / agentic-script 的实现**不要求兼容**：用户已明确（2026-10-09「不用兼容，不对的都可以推翻重来」），凡确认错误或设计不合理的实现（含既有 builder、方法签名、脚本入口形态）均可推翻重构，同步修正全部调用方与测试

# Decisions

- D1: 工作区隔离：Runtime 拒绝 current（同目录存在活跃 change nl2sql），采用唯一可行的 worktree 隔离（2026-10-09）
- D2: 范围为全量封装——编排类（parallelMapper/supervisor/planner/humanInTheLoop/agentBuilder）+ 声明式 createAgenticSystem + A2A 远程客户端（用户确认 2026-10-09）
- D3: 新增模式同步暴露到 magic 脚本入口 `langchain4j.agentic()`（用户确认 2026-10-09）
- D4: supervisor/planner 等模式的 ChatModel 由调用方经 builder `chatModel(...)` 显式注入，门面不做 Spring 自动装配（用户确认 2026-10-09）
- D5: 用户明确取消兼容约束（「不用兼容，不对的都可以推翻重来」，2026-10-09）：以正确性与设计合理性优先，可自由推翻重构既往实现，同步修正调用方、脚本入口与测试；既有测试如反映的是错误行为也一并修正
- D6: 模式清单经逐类复核确认为 11 个公开工厂入口（4 已有 + 7 新增），`WorkflowAgentsBuilder` SPI、`AgenticScope` 执行机制、观测工具类不计为独立模式（2026-10-09）
- D7: 升级 langchain4j 至最新稳定版：核心 1.22.0、agentic 1.22.0-beta32（用户 2026-10-09 追加要求「顺便升级下langchain4j的版本，目前最新的稳定版是1.22」）。此前 Non-goal「不升级依赖版本」据此移除。已核实 1.20.2→1.22 仅新增 API（a2aBuilder 重载、nonAiAgentBuilder、streamingClientListener），对既有编排模式无破坏性变更，适配成本≈0

# Open questions

（无 [blocking] 项）

# Verification expectations

- 单测：每种新增模式至少一个用例（mock ChatModel），验证构建与执行语义
- 编译：fw-plus-ai 层全量编译通过；全部测试通过（反映错误行为的既有测试随实现一并修正）
- 隔离层核验：新增 API 不要求调用方 import `dev.langchain4j.agentic.*` 类型（构建与执行路径）
- 版本升级核验：`fw-plus-dependencies` 的 `langchain4j.version` 为 1.22.0、`langchain4j-agentic` 构件为 1.22.0-beta32；升级后 fw-plus-ai 层编译与测试结果与升级前基线一致（无新增失败）
