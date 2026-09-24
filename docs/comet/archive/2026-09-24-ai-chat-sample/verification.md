---
generated_from_state_version: 16
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 2
- 验证器尝试次数: 1
- 完成时间: 2026-09-24T13:05:39.624Z
- 摘要: 6/6 全部通过。A5 失败根因（~/.m2 并发重装带入 McpRoutingRegistrar）已通过样例自身 exclude 收窄解决，并经对照实验确认非掩盖；其余 5 项实现未变动、全部保持通过。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | **A1｜Scenario**：同步对话可用 —— 配置 `langchain4j.ollama.chat-model.base-url` 与 `model-name` 启动应用后，`POST /api/chat` 返回 200，响应 JSON 含非空 `reply` 字段；未配置知识库 `base-url` 时同样返回 200 且不报错。 | ChatController.java:39-56 同步接口恒 200 且 body 含 reply/toolCalls，空 message 走 400；本次 run 中 chatReturnsReplyAndToolCalls 与 chatRejectsEmptyMessage 均通过；控制器仅依赖样例自有 Bean，与排除项无相互影响。 |
| A2 | passed | brief.md | **A2｜Scenario**：知识库接口接入生效 —— 配置 `fw.plus.sample.chat.knowledge-base.base-url` 后，一次提问会对该地址发起 HTTP POST，请求体含 `query`（取用户原话）与 `topK`；知识库返回的条目内容出现在注入给模型的 prompt 中（用 `MockRestServiceServer` 断言请求体，用捕获断言注入内容）。 | RagAutoConfig.java:24-38（@ConditionalOnProperty(base-url) 的 ContentRetriever → DefaultRetrievalAugmentor + DefaultContentInjector）→ ChatAssistantConfig.java:30-48（ObjectProvider 注入 AiServices）链路完整；RemoteKnowledgeBaseContentRetrieverTest 4/4 通过，断言请求体 {"query":"杭州夏天热吗","topK":3} 与注入内容、title metadata。 |
| A3 | passed | brief.md | **A3｜Scenario**：工具调用装配且 mock 数据可控 —— `WeatherTools.queryWeather("北京")` 返回包含温度/湿度/风力的 JSON 字符串且调用计数自增；`Assistant` 装配后其工具集合包含该方法对应的 `ToolSpecification`（名称即查询天气），即 `AiServices` 确实注册了该工具。 | WeatherTools.java:31-44 @Tool + AtomicInteger 计数，mock JSON 含 temperatureC/humidity/windLevel；ChatAssistantConfig.java:38 .tools(weatherTools) 注册；weatherToolsReturnsMockWeatherAndCountsCalls 通过。 |
| A4 | passed | brief.md | **A4｜Scenario**：SSE 流式输出 —— `GET /api/chat/stream?message=...` 响应 `Content-Type` 为 `text/event-stream`，至少收到 1 条 token 数据事件，随后收到 `done` 事件且连接正常关闭；流式中途模型异常时通过 `SseEmitter.completeWithError` 结束。 | ChatController.java:61-88 produces=text/event-stream、逐 token 事件、done 事件后 complete()、异常走 completeWithError（行 74/82/85）；streamEmitsTokenAndDoneEvents 通过。 |
| A5 | passed | brief.md | **A5｜Scenario**：构建与 Java 8 合规 —— `./mvnw -o -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml clean test` 在 JDK 8 下 BUILD SUCCESS，0 failed / 0 errors；新增源码中不存在 `List.of(`、`Map.of(`、`var `、`Stream.toList()`、文本块等 Java 9+ 写法。 | 实跑 clean test（JDK 8 Java 1.8.0_432）：BUILD SUCCESS，Tests run 8, Failures 0, Errors 0, Skipped 0；fw-plus-sample 聚合器 validate 亦 BUILD SUCCESS。修复为 application.yaml:7-13 的 spring.autoconfigure.exclude 排除 MagicAPIBaseAutoConfiguration 与 McpAutoConfiguration，属样例自身配置收窄：本 change 改动面仅新增样例目录+父 POM 一行 module，样例源码无任何 MCP/Magic-API import，排除类与演示链路零交集。对照实验（仅排除 magicapi、保留 MCP）当前 jar 下上下文同样启动且 4/4 通过，证实排除项是加固而非掩盖。~/.m2 jar（20:29）仍含 McpRoutingRegistrar，8/8 通过确为「污染 jar + 排除项」组合下结果，排除项真实生效。grep 无 List.of/Map.of/var/Stream.toList/文本块/箭头 switch/record。 |
| A6 | passed | brief.md | **A6｜Scenario**：知识库故障降级 —— 远程知识库返回 500 或读取超时时 `retrieve` 返回空列表且不抛出异常；此时同步接口仍返回 200，`reply` 不含知识库上下文，应用已记录 warn 日志。 | RemoteKnowledgeBaseContentRetriever.java:89-93 catch(Exception) 后 warn + Collections.emptyList()；returnsEmptyWhenServiceFails 通过（run 日志见 Response 500 后断言空列表）。 |

## 检查

_没有记录 Runtime 检查。_

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- module-tests: passed — ./mvnw -o -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml clean test（JDK 8）：Tests run 8, Failures 0, Errors 0, Skipped 0, BUILD SUCCESS
- jdk8-no-modern-api: passed — 新增 main/test 源码在 JDK 8 下编译通过；无 List.of/Map.of/var/Stream.toList/文本块/箭头 switch/record；仅本次修改 application.yaml（配置，无语法风险）
- 已知限制: A1 真实模型输出与本机无 Ollama 的情况下无法执行，自动化以 MockBean 隔离 Assistant 断言契约。
- 已知限制: A3『模型是否真的调用工具』依赖真实模型决策，自动化验证 ToolSpecification 注册与 mock 数据可控性。
- 已知限制: 未做 spring-boot:run 启动烟测。
- 已知限制: McpRoutingRegistrar 与 actuator 并存时的唯一 mapping 要求属主工作区未提交代码的潜在缺陷，不属本 change 范围，建议另行跟踪。

## 阻塞项

_无。_

## 风险与跳过的工作

- ~/.m2 仍被主工作区并发会话反复重装（jar 20:29），归档前建议在干净环境下复跑一次以获得完全可复现证据。
- ChatControllerTest.streamEmitsTokenAndDoneEvents 只断言 200 与 SSE Content-Type，未显式断言 body 中 event:token/event:done 行，覆盖偏弱（不构成失败）。
- McpRoutingRegistrar 与多个 RequestMappingHandlerMapping 并存属主工作区未提交代码的潜在缺陷，不属本 change 范围。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A5 | 6 项中 5 项通过；A5 因 ~/.m2 被并发会话重装带入主工作区未提交的 McpRoutingRegistrar 导致上下文启动失败（与样例代码无关，排除该自动配置后 8/8 通过）。需回 Build 在样例 application.yaml 补排除项后重验 A5。 | 2026-09-24T12:53:48.382Z |
| 1 | 2 | 1 | pass | — | 6/6 全部通过。A5 失败根因（~/.m2 并发重装带入 McpRoutingRegistrar）已通过样例自身 exclude 收窄解决，并经对照实验确认非掩盖；其余 5 项实现未变动、全部保持通过。 | 2026-09-24T13:05:39.624Z |



## 结论

6/6 全部通过。A5 失败根因（~/.m2 并发重装带入 McpRoutingRegistrar）已通过样例自身 exclude 收窄解决，并经对照实验确认非掩盖；其余 5 项实现未变动、全部保持通过。
