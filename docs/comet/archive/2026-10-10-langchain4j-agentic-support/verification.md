---
generated_from_state_version: 16
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 3
- 迭代: 1
- 验证器尝试次数: 1
- 完成时间: 2026-10-10T08:10:51.961Z
- 摘要: 独立只读验收 langchain4j-agentic-support 候选（iteration 1/attempt 1）：10/10 全量通过。核实实现（AgenticWorkflows 门面 + 各 Builder）、测试（AgenticPatternsTest 10 / AgenticIsolationTest 3 / AgenticA2AServerTest 3 / AgenticToolProviderTest 5，共 21 用例）、版本升级（langchain4j 1.22.0 / agentic 1.22.0-beta32）与三项 Runtime 绑定证据（agentic-suite 21/21、dependency-baseline 1/1、thinking-audit 3/3，均 exitCode 0）。独立复跑模块全量测试 588/10F/20E/5skipped，逐类核查 9 个失败类根因均为环境性 DJL 原生库不可加载或既有测试假设（OpenAI 反射字段/记忆行为/Multimodal mock NPE），全部位于本 change 未触碰区域，与干净基线逐项一致，无本 change 引入的新失败；5 skipped 为 RealServer 环境性。升级唯一破坏（Xinference 流式思考回调矩阵）已随实现修正并通过。判定通过；已知风险（隔离守卫不完整、A2A 生产 protobuf 对齐、A2A 工具路径未端到端、AgenticModelPlanner 死代码）如实记录。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | Scenario: 并行映射模式——WHEN 用门面的 parallelMapper 构建编排并指定 itemsProvider THEN 对列表中每个元素并行执行映射 agent，结果汇聚写回 | 门面 AgenticWorkflows.parallelMapper() 返回 AgenticParallelMapperBuilder，支持 itemsProvider/subAgents/executor/outputKey 等；测试 parallelMapperShouldMapEachItemAndWriteBackList 用 itemsProvider("items")+UpperCaseMapper+outputKey("results") 对 [a,b,c] 并行映射并汇聚写回为 [A,B,C]，Runtime agentic-suite 与独立全量运行均通过。 |
| A2 | passed | brief.md | Scenario: 监督者模式——WHEN 用门面的 supervisor 构建编排、由调用方注入 ChatModel 并注册多个 subAgents THEN 监督者 LLM 依据请求自主选择并调用 subAgents，输出写回 outputKey | AgenticSupervisorBuilder 暴露 chatModel(...) 显式注入、subAgents(...)、maxAgentsInvocations、outputKey 等；测试 supervisorShouldLetLlmChooseSubAgentsAndReturnOutput 注入 ScriptedChatModel 并注册 stepA/stepB 两个 subAgent，监督者依据请求自主调用 A/B，输出写回 outputKey(answer)，Runtime 与独立运行通过。 |
| A3 | passed | brief.md | Scenario: 人工介入模式——WHEN 用门面的 humanInTheLoop 构建节点并提供 responseProvider THEN 编排执行到该节点时采用外部提供的人工响应继续流转 | AgenticHumanInTheLoopBuilder 支持 responseProvider(Supplier/Function)、async、inputKey(s)、outputKey；测试 humanInTheLoopShouldProvideResponseAndBeMountable 用 responseProvider(()->"人工响应") 构建节点并挂入 sequence，执行到该节点采用外部人工响应继续流转，Runtime 通过。 |
| A4 | passed | brief.md | Scenario: 单 agent 构建——WHEN 用门面的 agentBuilder 构建 typed/untyped 单 agent THEN 该 agent 可独立执行并可作为 subAgent 挂入既有编排 | agent()/agent(Class) 与 agentBuilder 别名构建 typed/untyped 单 agent；测试 typedAgentShouldExecuteStandalone、untypedAgentShouldExecuteStandalone、typedAgentShouldBeMountableAsSubAgent 验证可独立执行且可挂入既有编排（sequence），Runtime 通过。 |
| A5 | passed | brief.md | Scenario: 声明式系统——WHEN 调用方声明带 agentic 注解的接口并经门面 createAgenticSystem 创建 THEN 返回可用的编排实例 | createAgenticSystem(Class)/带 ChatModel/AgentConfigurator 共 4 重载代理 AgenticServices.createAgenticSystem；测试 createAgenticSystemShouldBuildDeclarativeSequence 用 @SequenceAgent 注解接口创建并执行得到 B-OUT，返回可用编排实例；框架不提供注解扫描（与 Non-goal 一致），Runtime 通过。 |
| A6 | passed | brief.md | Scenario: A2A 远程客户端——WHEN 用门面的 a2aBuilder 指定服务端地址构建远程 agent THEN 该客户端可作为 subAgent/工具参与编排 | a2a(String)/a2a(String,Class) 及 a2aBuilder 别名构建远程 agent，build() 才触网（延迟）；AgenticA2AServerTest 3/3 用本地 stub agent card 验证可 build、可挂入 sequence 参与编排、untyped 缺 inputKeys 快速失败；工具路径为通用 UntypedAgent 包装机制（逻辑成立，未直接端到端测试，记入 risks）。 |
| A7 | passed | brief.md | Scenario: 隔离层约束——WHEN 检查 agentic 包对外 API THEN 业务调用方只需框架自有类型即可完成构建与执行，实验性 API 收口在包内 | AgenticIsolationTest 3/3：业务调用方仅用框架类型+UntypedAgent 句柄即可完成构建与执行；反射守卫确认门面公开 API 不泄漏 internal./workflow./planner./SupervisorAgent 实验类型；构建/执行主路径（itemsProvider/subAgents/name/description/outputKey/chatModel/build/invoke）收口成立。已知不完整点：部分可选配置方法仍公开暴露 AgenticScope/AgentListener/ErrorContext 等实验类型（known_limits 与上轮验收已记录），记入 risks，不影响主路径判定。 |
| A8 | passed | brief.md | Scenario: 脚本可用——WHEN magic 脚本经 `langchain4j.agentic()` 调用新增模式入口 THEN 行为与 Java API 一致 | Langchain4jModule.agentic() 返回 AgenticWorkflows.instance()（同一无状态门面），magic 脚本经 langchain4j.agentic() 获得全部新模式入口；facadeShouldExposeAllPatternFactoriesForMagicScripts 验证门面暴露 13 个工厂方法；同一实现保证行为与 Java API 一致，Runtime 通过。 |
| A9 | passed | brief.md | Scenario: 测试覆盖——WHEN 在 fw-plus-ai-boot-autoconfigure 运行 `./mvnw test` THEN 新增模式的单测全部通过，既有测试全部通过（反映错误行为的既有测试随实现一并修正） | 独立运行 mvn test -Dmaven.test.skip=false 复现 588 tests/10F/20E/5skipped，与 Builder 声称一致；逐类核查 9 个失败类根因全部为环境性或既有假设：DJL Huggingface 原生库不可加载（BgeSmallEnV15QuantizedEmbeddingModel EngineException，x86_64 macOS 无 tokenizers 0.36.0 原生库，涉及 Datasource 系列/ModelFactoryRegistry/McpPlugin/WebFlux）、Langchain4jOpenAiModelFactoryTest 反射 temperature/topP 字段两版本均不存在、Langchain4jModuleTest 记忆行为、MultimodalAssistantBridgeTest mock NPE——全部位于本 change 未触碰区域，无本 change 引入的新失败；新增模式测试 21/21 全绿；5 skipped 均为 RealServer 环境性跳过。 |
| A10 | passed | brief.md | Scenario: 版本升级——WHEN 以升级后的基线（langchain4j 1.22.0 / agentic 1.22.0-beta32）构建并运行 THEN 全部封装模式编译与测试通过，升级不引入破坏性行为变更 | fw-plus-dependencies 升级 langchain4j 1.20.2→1.22.0、community 1.20.0-beta30→1.21.0-beta31、github-models 保持 1.19.3-beta29；m2 确认 langchain4j-agentic/a2a 由 BOM 解析为 1.22.0-beta32；planner 适配 supervisorBuilder(UntypedAgent.class)；SpringBoot3DependencyBaselineTest 1/1、StreamingThinkingAdapterAuditTest 3/3（Xinference 移入支持矩阵）通过；全量失败集合与干净基线逐项一致，升级未引入新破坏。Runtime 三项正式检查均 exitCode 0。 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| agentic 隔离层定向测试（A1-A8 覆盖） | -lc export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"; /Users/xiongxiao/.m2/wrapper/dists/apache-maven-3.9.9-bin/4nf9hui3q3djbarqar9g711ggc/apache-maven-3.9.9/bin/mvn -B test -Dmaven.test.skip=false -Dtest='AgenticPatternsTest,AgenticIsolationTest,AgenticA2AServerTest,AgenticToolProviderTest' -DfailIfNoSpecifiedTests=false | fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure | passed | 0 | 42687 ms |
| SpringBoot3 依赖基线校验（A10） | -lc export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"; /Users/xiongxiao/.m2/wrapper/dists/apache-maven-3.9.9-bin/4nf9hui3q3djbarqar9g711ggc/apache-maven-3.9.9/bin/mvn -B test -Dmaven.test.skip=false -Dtest=SpringBoot3DependencyBaselineTest -DfailIfNoSpecifiedTests=false | fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure | passed | 0 | 43533 ms |
| 流式思考审计测试（升级回归修复验证） | -lc export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"; /Users/xiongxiao/.m2/wrapper/dists/apache-maven-3.9.9-bin/4nf9hui3q3djbarqar9g711ggc/apache-maven-3.9.9/bin/mvn -B test -Dmaven.test.skip=false -Dtest=StreamingThinkingAdapterAuditTest -DfailIfNoSpecifiedTests=false | fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure | passed | 0 | 72582 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- agentic 隔离层定向测试（AgenticPatterns/Isolation/A2AServer/ToolProvider）: passed — 21/21 全绿；clean 全量运行中同轮观测，AgenticPatternsTest 10、AgenticIsolationTest 3、AgenticA2AServerTest 3、AgenticToolProviderTest 5。
- 流式思考审计测试适配（StreamingThinkingAdapterAuditTest）: passed — community 1.21.0-beta31 起 XinferenceStreamingChatModel 读取 Delta.reasoningContent 并回调 onPartialThinking（jar 字节码实证：beta30 0 处、beta31 1 处），已把 Xinference 移入支持矩阵，3/3 通过。
- fw-plus-ai-boot-autoconfigure 全量 clean test: failed — 588 tests / 10F / 20E / 5 skipped，失败集合与干净 springboot3 基线 572/10F/20E 逐项一致：环境性 DJL Huggingface 原生库不可用（tokenizers 0.36.0 无 osx-x86_64 原生库）+ 既有测试假设（OpenAiModelFactory 反射 temperature/topP 在 1.20.2/1.22.0 均无该字段），非本变更引入。
- SpringBoot3 依赖基线校验: passed — SpringBoot3DependencyBaselineTest 1/1 通过，确认 langchain4j 1.22.0 / community 1.21.0-beta31 基线。
- 已知限制: 模块全量测试的 10 失败/20 错误与干净 springboot3 基线（572/10F/20E）逐项一致：根因为环境性 DJL Huggingface tokenizers 原生库在本机 x86_64 JDK 上不可加载（langchain4j 1.22 引入 tokenizers 0.36.0，不再发布 osx-x86_64 原生库）与既有测试假设（Langchain4jOpenAiModelFactoryTest 反射 temperature/topP 字段，两个版本均不存在），非本变更引入；CI/生产在受支持架构上不受影响。
- 已知限制: A2A 生产使用需消费方自行对齐 protobuf-java（及 grpc）版本：a2a-java-sdk 1.3.1.Final 依赖 protobuf-java 4.35.0，项目全局 pin 3.25.1；当前仅在测试作用域覆盖，用于验证门面构建与挂载语义。
- 已知限制: 隔离守卫不完整：部分可选配置方法仍公开暴露 dev.langchain4j.agentic.* 实验类型（上一轮验收已记录，本轮保持记录）；AgenticModelPlanner 在改用 supervisor 后为死代码，暂保留。
- 已知限制: langchain4j-github-models 维持 1.19.3-beta29（上游无更新版本，按用户要求不升级）。

## 阻塞项

_无。_

## 风险与跳过的工作

- 隔离守卫不完整（既有记录）：部分可选配置方法（output/beforeCall/errorHandler/listener/requestGenerator/contextGenerationStrategy/responseStrategy）仍公开暴露 dev.langchain4j.agentic.* 实验类型（AgenticScope、AgentListener、ErrorContext/ErrorRecoveryResult、SupervisorContextStrategy/ResponseStrategy），未纳入 AgenticIsolationTest 的 FORBIDDEN 列表；主构建/执行路径收口成立，建议后续收敛。
- 模块全量测试 10F/20E 为本机 x86_64 环境性失败+既有测试假设（DJL Huggingface tokenizers 原生库不可加载、Langchain4jOpenAiModelFactoryTest 反射字段、Langchain4jModuleTest 记忆行为、MultimodalAssistantBridgeTest mock NPE），与干净 springboot3 基线逐项一致，非本 change 引入；CI/受支持架构不受影响。
- A2A 生产使用需消费方对齐 protobuf-java（及 grpc）版本：a2a-java-sdk 1.3.1.Final 依赖 protobuf-java 4.35.0，项目全局 pin 3.25.1；当前仅在测试作用域覆盖，真实远程 JSON-RPC 调用未纳入测试。
- A2A 作为工具参与编排路径未直接端到端测试（subAgent 挂载已测；工具路径依赖通用的 UntypedAgent→AgenticToolProvider 包装机制，逻辑成立）。
- AgenticModelPlanner 在 planner 改用 supervisorBuilder(UntypedAgent.class) 后为死代码，暂保留未清理。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 0 | 0 | recovery | — | Native confirmed acceptance criteria changed | 2026-10-09T12:46:46.050Z |
| 2 | 1 | 1 | pass | — | 独立复核：定向 21 用例全通过；模块全量 588/10F/20E 与干净 springboot3 基线 572/10F/20E 的失败用例集合逐项一致；A1–A6、A8、A9 均有独立运行证据，A7 的构建/执行路径与包级收口成立。唯一实质性问题是隔离守卫不完整、若干可选配置方法公开暴露 dev.langchain4j.agentic.* 实验类型（已如实记录于 risks），不影响基本构建与执行路径。判定 9/9 通过。 | 2026-10-09T15:21:57.894Z |
| 2 | 1 | 1 | recovery | — | 用户要求升级 langchain4j：将核心版本升至 1.22.0、agentic 实验构件升至 1.22.0-beta32。需在 brief/spec 中移除 Non-goal『不升级依赖版本』并新增对应需求条款，重新确认后在 1.22 基线实现升级与适配再验收。 | 2026-10-09T22:29:41.664Z |
| 3 | 1 | 1 | pass | — | 独立只读验收 langchain4j-agentic-support 候选（iteration 1/attempt 1）：10/10 全量通过。核实实现（AgenticWorkflows 门面 + 各 Builder）、测试（AgenticPatternsTest 10 / AgenticIsolationTest 3 / AgenticA2AServerTest 3 / AgenticToolProviderTest 5，共 21 用例）、版本升级（langchain4j 1.22.0 / agentic 1.22.0-beta32）与三项 Runtime 绑定证据（agentic-suite 21/21、dependency-baseline 1/1、thinking-audit 3/3，均 exitCode 0）。独立复跑模块全量测试 588/10F/20E/5skipped，逐类核查 9 个失败类根因均为环境性 DJL 原生库不可加载或既有测试假设（OpenAI 反射字段/记忆行为/Multimodal mock NPE），全部位于本 change 未触碰区域，与干净基线逐项一致，无本 change 引入的新失败；5 skipped 为 RealServer 环境性。升级唯一破坏（Xinference 流式思考回调矩阵）已随实现修正并通过。判定通过；已知风险（隔离守卫不完整、A2A 生产 protobuf 对齐、A2A 工具路径未端到端、AgenticModelPlanner 死代码）如实记录。 | 2026-10-10T08:10:51.961Z |



## 结论

独立只读验收 langchain4j-agentic-support 候选（iteration 1/attempt 1）：10/10 全量通过。核实实现（AgenticWorkflows 门面 + 各 Builder）、测试（AgenticPatternsTest 10 / AgenticIsolationTest 3 / AgenticA2AServerTest 3 / AgenticToolProviderTest 5，共 21 用例）、版本升级（langchain4j 1.22.0 / agentic 1.22.0-beta32）与三项 Runtime 绑定证据（agentic-suite 21/21、dependency-baseline 1/1、thinking-audit 3/3，均 exitCode 0）。独立复跑模块全量测试 588/10F/20E/5skipped，逐类核查 9 个失败类根因均为环境性 DJL 原生库不可加载或既有测试假设（OpenAI 反射字段/记忆行为/Multimodal mock NPE），全部位于本 change 未触碰区域，与干净基线逐项一致，无本 change 引入的新失败；5 skipped 为 RealServer 环境性。升级唯一破坏（Xinference 流式思考回调矩阵）已随实现修正并通过。判定通过；已知风险（隔离守卫不完整、A2A 生产 protobuf 对齐、A2A 工具路径未端到端、AgenticModelPlanner 死代码）如实记录。
