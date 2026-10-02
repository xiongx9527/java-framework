# 目标

参照 `fw-plus-magicapi-plugin-langchain4j-system-message` 插件的模式（资源内容即脚本、magic 控制台维护、运行时按需求值、热更新），为 agentic 编排提供**脚本节点**能力：编排（AgenticWorkflows 的 sequence/loop/conditional/parallel）中的一个 agent 节点可以用一段 magic 脚本实现——不会写 Java 的业务/脚本作者也能实现编排节点。

并扩展两项配套能力（用户明确 2026-10-01）：
1. **编排注册中心**：编排提供方（nl2sql 流水线、agentic-script 组合、业务代码）把编排句柄按 key 注册，供资源链与其他模块按 key 引用；
2. **tool-provider 资源链支持 agentic 来源**：tool-provider 资源（functions / api 之外）新增 agentic 来源，绑定注册中心的编排 key，把编排包装为 ToolProvider 即插即用。

需求来源：agentic-basic Shape 讨论中识别的 magic×agentic 结合点（用户明确：像 system-message 一样提供特殊脚本）。

# 范围

- 新资源类型 `langchain4j-agentic-script`（独立 magic 插件 `fw-plus-magicapi-plugin-langchain4j-agentic-script`，与 system-message 插件同构）：**单一资源类型，用 type 字段区分形态**
  - 资源定义：key、**type（node=编排节点 / tool=编排工具）**、名称/描述、输入变量声明（入参名列表）、脚本正文（magic-script，输入变量已注入脚本上下文，返回值即输出）
  - type=node：经 `MagicScriptAgent` 适配器挂入 `AgenticWorkflows` 编排 subAgents（入参注入、返回值写回 outputKey）
  - type=tool：经 `agenticToolProvider` 包装为 ToolProvider 挂入助手
  - agentic 门面补齐**两个工厂**（用户确认 2026-10-01「都要支持」）：`agenticToolProvider(UntypedAgent, ...)`（agentic 编排当工具）与 `graphToolProvider(CompiledGraph, ...)`（langgraph4j 编译图当工具）——agentic-basic 遗留缺口
  - 资源链 6 文件（Info/DataSource/Module/Registry/Storage/Configuration，参照 system-message）+ 控制台前端 iife（magic-textarea 编辑脚本，type 下拉选择）
  - 热更新：资源变更即重建脚本节点/工具（Registry 语义与现有资源链一致）
- `MagicScriptAgent` 适配器：把脚本节点包装为可挂入 `AgenticWorkflows` 编排 subAgents 的 agent（invoke：入参 Map → 按声明的输入变量注入脚本上下文 → 求值 → 返回值写回 outputKey）
- 脚本入口暴露：`Langchain4jModule`（或 agentic 门面）提供按 key 取脚本节点/工具的方法；**同时补齐 agentic 门面的 `agenticToolProvider(...)` 方法**（agentic-basic 遗留缺口）
- **编排注册中心** `AgenticPipelineRegistry`：key → 编排句柄（UntypedAgent）的注册/查询/注销；nl2sql 等编排提供方启动/配置变更时注册，脚本与资源链按 key 引用
- **tool-provider 资源链支持 agentic 来源**：`Langchain4jToolProviderInfo`/`Langchain4jToolProperties` 新增 agentic 来源（绑定注册中心的编排 key），运行时经 `AgenticToolProviders.agenticToolProvider(...)` 包装为 ToolProvider；控制台前端表单增加来源类型

# 非目标

- 不做可视化编排画布/编排定义资源化（编排结构仍由 Java/脚本代码构建）
- 不做脚本节点类型系统/强类型输出（首版输入均字符串、输出为脚本返回值的文本形态）
- 不做多语言脚本（仅 magic-script）

# 验收示例

- Scenario: 脚本节点——WHEN 登记一个 agentic-node 脚本资源（声明输入变量 question，脚本返回处理后文本） THEN 编排构建时可将该节点挂入 subAgents，入参自动注入、返回值写回 outputKey
- Scenario: 热更新——WHEN 控制台修改脚本正文 THEN 下次编排执行立即使用新逻辑，无需重启
- Scenario: 与 system-message 同模式——WHEN 资源新增/修改/删除 THEN 注册器重建/注销脚本节点（与 system-message 资源生命周期一致）
- Scenario: tool 形态——WHEN 登记 type=tool 的脚本资源并挂入助手 toolProvider THEN 主 LLM 可调用该脚本工具并获得返回值
- Scenario: 编排注册——WHEN nl2sql 流水线（或业务编排）按 key 注册到编排注册中心 THEN 资源链与其他模块可按 key 查询到该编排
- Scenario: tool-provider agentic 来源——WHEN tool-provider 资源配置 source=agentic 并绑定注册中心的编排 key THEN 该资源实例可作为 ToolProvider 挂入助手，调用即执行对应编排
- Scenario: 脚本异常——WHEN 脚本求值抛出异常 THEN 按编排节点失败语义传播（可被 agentic errorHandler 捕获）
- Scenario: 多节点协作——WHEN 脚本节点与 LLM agent 混合编排（sequence） THEN 脚本节点的输出经 AgenticScope 流向下游 LLM agent

# 约束与不变量

- 脚本求值经框架既有脚本调用机制（MagicToolInvoker/ScriptManager 模式），不直接依赖 magicapi 引擎内部类（对齐 API 服务与 magic 解耦的架构：脚本求值能力属于 magic 管理端/同进程部署形态）
- 脚本节点的入参/输出均为文本形态（首版不做结构化类型）
- 资源 key 唯一性校验与现有资源链一致

# 决策

- D1: 参照 system-message 插件模式（资源即脚本、控制台维护、热更新）（用户明确 2026-10-01）
- D2: 脚本节点经 MagicScriptAgent 适配器挂入 AgenticWorkflows 编排（agentic-basic 的扩展能力，依赖其隔离层）
- D3: 独立 magic 插件承载（与 system-message 插件同构，不并入现有插件）
- D4: **单一资源类型 `langchain4j-agentic-script` + type 字段区分形态**（node=编排节点 / tool=编排工具；用户明确 2026-10-01「同一个类型字段区分」），后续新形态按 type 扩展，不再新增资源类型；同时补齐 agentic 门面的 agenticToolProvider 方法（agentic-basic 遗留缺口）
- D5: **通用编排注册中心**（用户明确 2026-10-01，选项「通用编排注册中心」）：tool-provider 资源的 agentic 来源通过 agenticKey 指向注册中心中任意已注册编排；首版注册方为 nl2sql 流水线，后续 agentic-script 组合编排、业务代码编排按同模式注册
- D6: tool-provider 资源链的 agentic 来源经 `AgenticToolProviders.agenticToolProvider(...)` 包装（复用 agentic-basic 适配器），不重复实现

# 待解决问题

（无 [blocking] 项）

# 验证预期

- 单测：脚本节点求值（入参注入/返回值提取）、异常传播、资源生命周期
- 集成：sequence 编排中脚本节点与 LLM agent 协作的端到端验证（mock ChatModel）
- 前端插件构建产物校验（iife 进 jar）
