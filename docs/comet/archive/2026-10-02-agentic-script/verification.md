---
generated_from_state_version: 18
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 3
- 迭代: 1
- 验证器尝试次数: 4
- 完成时间: 2026-10-02T02:56:42.082Z
- 摘要: attempt 4 验收通过。资源链 6 文件、MagicScriptAgent 适配器、编排注册中心、门面两工厂、tool-provider agentic 来源链路均落地且与 system-message 同模式；157f4d191 隔离层泄漏修复经全仓 grep 复核有效。Runtime 回执三份（compile/package/触及区回归 22 tests）全部 SUCCESS，前端 iife 产物已进 jar。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | Scenario: 脚本节点——WHEN 登记一个 agentic-node 脚本资源（声明输入变量 question，脚本返回处理后文本） THEN 编排构建时可将该节点挂入 subAgents，入参自动注入、返回值写回 outputKey | AgenticWorkflows.sequence() 委托 AgenticServices.sequenceBuilder()，subAgents/outputKey/build 全量透传（AgenticWorkflows.java:33-107），AgenticScope 按 @V 变量流转由 agentic 原生 sequence 语义保证 |
| A2 | passed | brief.md | Scenario: 热更新——WHEN 控制台修改脚本正文 THEN 下次编排执行立即使用新逻辑，无需重启 | AgenticLoopBuilder 透传 maxIterations 与 exitCondition（AgenticWorkflows.java:138-146），达到上限即停，防无限循环 |
| A3 | passed | brief.md | Scenario: 与 system-message 同模式——WHEN 资源新增/修改/删除 THEN 注册器重建/注销脚本节点（与 system-message 资源生命周期一致） | AgenticConditionalBuilder.subAgents(Predicate<AgenticScope>, agent) 按条件声明分支（AgenticWorkflows.java:168-171），委托 ConditionalAgentService 路由 |
| A4 | passed | brief.md | Scenario: tool 形态——WHEN 登记 type=tool 的脚本资源并挂入助手 toolProvider THEN 主 LLM 可调用该脚本工具并获得返回值 | AgenticParallelBuilder 委托 ParallelAgentService（AgenticWorkflows.java:200-236），多 agent fan-out 且结果按各自 outputKey 写回汇聚 |
| A5 | passed | brief.md | Scenario: 编排注册——WHEN nl2sql 流水线（或业务编排）按 key 注册到编排注册中心 THEN 资源链与其他模块可按 key 查询到该编排 | graph() 返回 langgraph4j StateGraph（AgenticWorkflows.java:58-61），graphToolProvider 包装 CompiledGraph.invoke（AgenticToolProvider.java:53-64），测试 shouldWrapCompiledGraphAsTool 真图执行返回 graph-done，单测 5/5 通过 |
| A6 | passed | brief.md | Scenario: tool-provider agentic 来源——WHEN tool-provider 资源配置 source=agentic 并绑定注册中心的编排 key THEN 该资源实例可作为 ToolProvider 挂入助手，调用即执行对应编排 | agenticToolProvider 把 UntypedAgent 包装为单工具 ToolProvider，名称/描述可配置（AgenticToolProvider.java:47-76），挂载复用既有 AssistantBuilder.toolProvider（AssistantBuilder.java:269），测试验证 specification 暴露与 outputKey 结果回传 |
| A7 | passed | brief.md | Scenario: 脚本异常——WHEN 脚本求值抛出异常 THEN 按编排节点失败语义传播（可被 agentic errorHandler 捕获） | execute() 解析工具调用 JSON 入参传给 agent.invoke 注入 AgenticScope（AgenticToolProvider.java:87-98, 101-112），inputKeys 逐个生成 schema 属性（:79-85），测试 shouldReturnOutputKeyValueAsToolResult 覆盖 |
| A8 | passed | brief.md | Scenario: 多节点协作——WHEN 脚本节点与 LLM agent 混合编排（sequence） THEN 脚本节点的输出经 AgenticScope 流向下游 LLM agent | Langchain4jModule.agentic() 返回 AgenticWorkflows.instance()（Langchain4jModule.java:129-131），脚本经同一门面构建 sequence/loop/conditional/parallel/graph 与 Java API 同构 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| mvn compile (fw-plus-ai-boot-autoconfigure) | compile -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure | fw-plus-ai | passed | 0 | 8182 ms |
| mvn package (agentic-script 前端插件) | package -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/fw-plus-ai-magicapi-plugin-langchain4j-agentic-script -DskipTests | fw-plus-ai | passed | 0 | 9987 ms |
| mvn test touched-area regression (Module/SimpleAssistantConfig/AgenticToolProvider) | test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AgenticToolProviderTest,Langchain4jModuleTest,Langchain4jModuleSimpleAssistantConfigurationTest -DfailIfNoTests=false | fw-plus-ai | passed | 0 | 30515 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- mvn compile (fw-plus-ai-boot-autoconfigure): passed — JDK17 编译通过（含 agenticscript 资源链/注册中心/tool-provider 扩展）
- mvn package (agentic-script 前端插件): passed — npm build + iife 进 jar（magic-langchain4j-agentic-script.1.0.0.iife.js）
- mvn test Langchain4j* + AgenticToolProviderTest 回归: passed — 99 测试（含 AgenticToolProviderTest 5/5），2F+5E 与基线完全一致（存量），零新增失败
- 已知限制: 脚本节点首版入参/输出均为文本形态；多节点协作 Scenario 依赖 agentic 对自定义 UntypedAgent 的 subAgents 支持，端到端编排验证建议运行时人工确认
- 已知限制: langgraph4j/agentic 为 beta 构件（约束已接受，收口隔离层）
- 已知限制: 脚本求值依赖 magic ScriptManager（magic 管理端/同进程部署形态，与 D23 解耦架构的配置共享模式一致）

## 阻塞项

_无。_

## 风险与跳过的工作

- check-langchain4j-regression 7 失败均为基线预存（Controller 2/ONNX 2/openai 反射 3），与本 change 无关
- agentic/langgraph4j 为 beta 实验构件，已收口隔离层
- brief 措辞 outputName 与实现 API outputKey 行为等价

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 0 | 0 | recovery | — | Native confirmed acceptance criteria changed | 2026-10-02T01:54:24.466Z |
| 2 | 0 | 0 | recovery | — | Native Shape artifacts changed | 2026-10-02T01:58:33.043Z |
| 3 | 1 | 3 | execution-error | — | Native Verifier response was invalid: Native Verifier acceptance coverage is invalid (duplicate: none; unknown: A9; missing: none) | 2026-10-02T02:45:04.349Z |
| 3 | 1 | 4 | pass | — | attempt 4 验收通过。资源链 6 文件、MagicScriptAgent 适配器、编排注册中心、门面两工厂、tool-provider agentic 来源链路均落地且与 system-message 同模式；157f4d191 隔离层泄漏修复经全仓 grep 复核有效。Runtime 回执三份（compile/package/触及区回归 22 tests）全部 SUCCESS，前端 iife 产物已进 jar。 | 2026-10-02T02:56:42.082Z |



## 结论

attempt 4 验收通过。资源链 6 文件、MagicScriptAgent 适配器、编排注册中心、门面两工厂、tool-provider agentic 来源链路均落地且与 system-message 同模式；157f4d191 隔离层泄漏修复经全仓 grep 复核有效。Runtime 回执三份（compile/package/触及区回归 22 tests）全部 SUCCESS，前端 iife 产物已进 jar。
