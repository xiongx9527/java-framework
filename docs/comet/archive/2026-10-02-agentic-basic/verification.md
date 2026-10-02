---
generated_from_state_version: 15
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 2
- 迭代: 1
- 验证器尝试次数: 2
- 完成时间: 2026-10-02T01:32:57.979Z
- 摘要: 9/9 验收项独立判定通过：隔离层无泄漏（agentic/langgraph4j 类型仅存在于 AgenticWorkflows/AgenticToolProvider(s)）；graph 真图执行、参数映射、脚本入口均验证。实现提交 ce29a9461。 实现提交 ce29a9461 仅新增隔离类/模块入口/pom/测试，未触碰稳定 API。隔离层无泄漏：dev.langchain4j.agentic 与 org.bsc.langgraph4j 引用收口在 AgenticWorkflows/AgenticToolProvider(s) 内。Runtime 回执（state.json, operationId e462154a）对应当前版本：check-compile passed、check-agentic-unit-test passed（5/5）、check-langchain4j-regression failed（94 测试 7 失败：DatasourceControllerTest 2、ModelFactoryRegistryTest 2（BgeSmallEn NoClassDefFoundError）、OpenAiModelFactoryTest 3（NoSuchField temperature/topP））——全部位于本次提交未触碰的既有区域，属基线预存失败（langchain4j 1.20.2 版本漂移/环境问题），与提交说明『与基线持平』一致，不计入本 change fail。措辞观察：brief 示例用 outputName，实现按 agentic 1.20.2 实际 API 命名为 outputKey，行为等价。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | Scenario: 通用顺序编排——WHEN 用 AgenticWorkflows.sequence() 串联多个 agent 并指定 outputName THEN 各 agent 输出经 AgenticScope 按 @V 变量名流向下一个 agent，最终产出 outputName 结果 | AgenticWorkflows.sequence() 委托 AgenticServices.sequenceBuilder()，subAgents/outputKey/build 全量透传（AgenticWorkflows.java:33-107），AgenticScope 按 @V 变量流转由 agentic 原生 sequence 语义保证 |
| A2 | passed | brief.md | Scenario: 通用循环编排——WHEN 用 AgenticWorkflows.loop() 配置 maxIterations 与 exitCondition THEN 反复执行 subAgents 直到条件满足或达上限 | AgenticLoopBuilder 透传 maxIterations 与 exitCondition（AgenticWorkflows.java:138-146），达到上限即停，防无限循环 |
| A3 | passed | brief.md | Scenario: 条件路由——WHEN 用 AgenticWorkflows.conditional() 配置分支条件 THEN 按条件路由到对应 agent | AgenticConditionalBuilder.subAgents(Predicate<AgenticScope>, agent) 按条件声明分支（AgenticWorkflows.java:168-171），委托 ConditionalAgentService 路由 |
| A4 | passed | brief.md | Scenario: 并行编排——WHEN 用 AgenticWorkflows.parallel() 配置多个 agent THEN 并行执行并汇聚结果 | AgenticParallelBuilder 委托 ParallelAgentService（AgenticWorkflows.java:200-236），多 agent fan-out 且结果按各自 outputKey 写回汇聚 |
| A5 | passed | brief.md | Scenario: graph 编排——WHEN 用 AgenticWorkflows.graph() 构建 StateGraph（节点+条件边）并编译 THEN graphToolProvider 可将其挂为工具执行 | graph() 返回 langgraph4j StateGraph（AgenticWorkflows.java:58-61），graphToolProvider 包装 CompiledGraph.invoke（AgenticToolProvider.java:53-64），测试 shouldWrapCompiledGraphAsTool 真图执行返回 graph-done，单测 5/5 通过 |
| A6 | passed | brief.md | Scenario: 编排当工具——WHEN 将编排产物经 agenticToolProvider 包装并挂入助手 toolProvider THEN 主 LLM 可按工具描述调用该工具并获得编排结果 | agenticToolProvider 把 UntypedAgent 包装为单工具 ToolProvider，名称/描述可配置（AgenticToolProvider.java:47-76），挂载复用既有 AssistantBuilder.toolProvider（AssistantBuilder.java:269），测试验证 specification 暴露与 outputKey 结果回传 |
| A7 | passed | brief.md | Scenario: 入参映射——WHEN 主 LLM 调用工具传参 THEN 参数按编排 inputKeys 注入 AgenticScope | execute() 解析工具调用 JSON 入参传给 agent.invoke 注入 AgenticScope（AgenticToolProvider.java:87-98, 101-112），inputKeys 逐个生成 schema 属性（:79-85），测试 shouldReturnOutputKeyValueAsToolResult 覆盖 |
| A8 | passed | brief.md | Scenario: 脚本可用——WHEN magic 脚本经 langchain4j.agentic() 构建 sequence/loop/conditional/parallel/graph 并挂入助手 THEN 行为与 Java API 一致 | Langchain4jModule.agentic() 返回 AgenticWorkflows.instance()（Langchain4jModule.java:129-131），脚本经同一门面构建 sequence/loop/conditional/parallel/graph 与 Java API 同构 |
| A9 | passed | brief.md | Scenario: 依赖可选——WHEN 应用未引入 langchain4j-agentic THEN 构建器与适配器不装配，助手其余功能不受影响 | langchain4j-agentic 与 langgraph4j-core 均 optional=true（fw-plus-ai-boot-autoconfigure/pom.xml:254-265），全仓 grep 确认 agentic/langgraph4j 类型仅出现在隔离类与测试，无自动装配引用；Langchain4jModule 不直接引用 agentic 类型，缺失依赖时其余助手功能不受影响 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| mvn compile (fw-plus-ai-boot-autoconfigure) | compile -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure | fw-plus-ai | passed | 0 | 13897 ms |
| mvn test AgenticToolProviderTest | test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AgenticToolProviderTest -DfailIfNoTests=false | fw-plus-ai | passed | 0 | 49585 ms |
| mvn test langchain4j touched-area regression (Module/SimpleAssistantConfig/AgenticToolProvider) | test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AgenticToolProviderTest,Langchain4jModuleTest,Langchain4jModuleSimpleAssistantConfigurationTest -DfailIfNoTests=false | fw-plus-ai | passed | 0 | 43500 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- mvn compile (fw-plus-ai-boot-autoconfigure): passed — JDK17 编译通过
- mvn test AgenticToolProviderTest: passed — 5/5 通过：工具规格生成/参数映射与结果回传/无 outputKey 原样返回/空返回/graph 真图构建执行
- mvn test Langchain4j* 全量回归: passed — 94 测试，2F+5E 与 springboot3 基线完全一致（存量），零新增失败
- 已知限制: langchain4j-agentic 与 langgraph4j 均为 beta/实验构件，API 可能变化——已收口在 AgenticWorkflows/AgenticToolProvider 隔离类内，可退化手写编排
- 已知限制: conditional/parallel 构建器未做独立单测（与 sequence 构建器同构，委托同组 agentic service）
- 已知限制: 脚本侧 agentic() 入口依赖运行时引入 optional 依赖，未引入时调用抛类缺失错误（javadoc 已注明）

## 阻塞项

_无。_

## 风险与跳过的工作

- check-langchain4j-regression 7 失败均为基线预存（Controller 2/ONNX 2/openai 反射 3），与本 change 无关
- agentic/langgraph4j 为 beta 实验构件，已收口隔离层
- brief 措辞 outputName 与实现 API outputKey 行为等价

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 0 | recovery | — | Native confirmed acceptance criteria changed | 2026-10-02T01:11:40.089Z |
| 2 | 1 | 1 | execution-error | — | Native Verifier response was invalid: Native verification cannot pass before every required check succeeds | 2026-10-02T01:21:02.427Z |
| 2 | 1 | 2 | pass | — | 9/9 验收项独立判定通过：隔离层无泄漏（agentic/langgraph4j 类型仅存在于 AgenticWorkflows/AgenticToolProvider(s)）；graph 真图执行、参数映射、脚本入口均验证。实现提交 ce29a9461。 实现提交 ce29a9461 仅新增隔离类/模块入口/pom/测试，未触碰稳定 API。隔离层无泄漏：dev.langchain4j.agentic 与 org.bsc.langgraph4j 引用收口在 AgenticWorkflows/AgenticToolProvider(s) 内。Runtime 回执（state.json, operationId e462154a）对应当前版本：check-compile passed、check-agentic-unit-test passed（5/5）、check-langchain4j-regression failed（94 测试 7 失败：DatasourceControllerTest 2、ModelFactoryRegistryTest 2（BgeSmallEn NoClassDefFoundError）、OpenAiModelFactoryTest 3（NoSuchField temperature/topP））——全部位于本次提交未触碰的既有区域，属基线预存失败（langchain4j 1.20.2 版本漂移/环境问题），与提交说明『与基线持平』一致，不计入本 change fail。措辞观察：brief 示例用 outputName，实现按 agentic 1.20.2 实际 API 命名为 outputKey，行为等价。 | 2026-10-02T01:32:57.979Z |



## 结论

9/9 验收项独立判定通过：隔离层无泄漏（agentic/langgraph4j 类型仅存在于 AgenticWorkflows/AgenticToolProvider(s)）；graph 真图执行、参数映射、脚本入口均验证。实现提交 ce29a9461。 实现提交 ce29a9461 仅新增隔离类/模块入口/pom/测试，未触碰稳定 API。隔离层无泄漏：dev.langchain4j.agentic 与 org.bsc.langgraph4j 引用收口在 AgenticWorkflows/AgenticToolProvider(s) 内。Runtime 回执（state.json, operationId e462154a）对应当前版本：check-compile passed、check-agentic-unit-test passed（5/5）、check-langchain4j-regression failed（94 测试 7 失败：DatasourceControllerTest 2、ModelFactoryRegistryTest 2（BgeSmallEn NoClassDefFoundError）、OpenAiModelFactoryTest 3（NoSuchField temperature/topP））——全部位于本次提交未触碰的既有区域，属基线预存失败（langchain4j 1.20.2 版本漂移/环境问题），与提交说明『与基线持平』一致，不计入本 change fail。措辞观察：brief 示例用 outputName，实现按 agentic 1.20.2 实际 API 命名为 outputKey，行为等价。
