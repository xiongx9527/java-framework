# 目标

在 `fw-plus-sample/fw-plus-ai-samples/` 下新增样例模块 `fw-plus-ai-chat-sample`，演示 fw-plus 上一次完整的大模型对话链路：

```
用户消息 → 调用远程知识库接口取上下文 → 注入 prompt → Ollama 本地模型生成 → （按需）工具调用 → 返回
                ↘ 流式逐 token 推送（SSE）
```

目标读者是框架使用者。看完这个样例应能回答：怎么发起一次对话、怎么把自有知识库接进 prompt、怎么加一个工具、怎么做流式输出。

# 范围

1. **模块坐标**：`com.xiongx9527.fw.plus.ai:fw-plus-ai-chat-sample:0.1.1-SNAPSHOT`，父 POM 为 `fw-plus-ai-samples`，并在父 POM `<modules>` 注册。
2. **模型接入**：Ollama 本地模型，使用 owner 模块 `dev.langchain4j.ollama.spring.AutoConfig` 提供的 `OllamaChatModel` 与 `OllamaStreamingChatModel`（由 `langchain4j.ollama.chat-model.base-url` / `streaming-chat-model.base-url` 控制启用）。不使用 `embedding-model`。
3. **知识库接入（核心）**：自定义 `RemoteKnowledgeBaseContentRetriever implements dev.langchain4j.rag.content.retriever.ContentRetriever`，用 `RestTemplate` 调用远程 HTTP 知识库接口，把返回条目转成 `Content.from(TextSegment)`，经 `DefaultRetrievalAugmentor` + `DefaultContentInjector` 注入 prompt。检索失败或空结果时降级为无知识库上下文，不打断对话。
4. **工具调用**：`WeatherTools` 提供 `@Tool("查询指定城市的实时天气")`，返回本地构造的 JSON（静态 mock 数据，注释标明），演示模型自主决定是否调用工具。
5. **对外接口**：
   - `POST /api/chat`，入参 `{"message":"..."}`，返回 `{"reply":"...","toolCalls":1}`
   - `GET /api/chat/stream?message=...`，返回 `text/event-stream`，若干 token 事件后以 `done` 事件结束
6. **配置集中**：`application.yaml` 顶部集中模型名、base-url、知识库开关（`fw.plus.sample.chat.knowledge-base.base-url`，未配置则知识库 Bean 不装配）。

# 非目标

- 不做前端页面，用 curl / 浏览器 SSE 验证。
- 不接真实天气 API，不做外部服务编排。
- **不使用向量数据库**：不做 embedding、不做文档切分灌库、不用 `InMemoryEmbeddingStore`、不引入 `EmbeddingStoreContentRetriever`。知识检索完全由远程知识库服务负责。
- 不引入多轮会话记忆（`ChatMemory`）。
- 不使用 MCP / A2A（Java 8 线上不可用）。
- 不启动真实远程知识库服务；测试用 `MockRestServiceServer` 在进程内模拟该 HTTP 接口。
- 不改造任何现有模块，只新增样例文件与父 POM 的一行 module 注册。

# 验收示例

- **A1｜Scenario**：同步对话可用 —— 配置 `langchain4j.ollama.chat-model.base-url` 与 `model-name` 启动应用后，`POST /api/chat` 返回 200，响应 JSON 含非空 `reply` 字段；未配置知识库 `base-url` 时同样返回 200 且不报错。
- **A2｜Scenario**：知识库接口接入生效 —— 配置 `fw.plus.sample.chat.knowledge-base.base-url` 后，一次提问会对该地址发起 HTTP POST，请求体含 `query`（取用户原话）与 `topK`；知识库返回的条目内容出现在注入给模型的 prompt 中（用 `MockRestServiceServer` 断言请求体，用捕获断言注入内容）。
- **A3｜Scenario**：工具调用装配且 mock 数据可控 —— `WeatherTools.queryWeather("北京")` 返回包含温度/湿度/风力的 JSON 字符串且调用计数自增；`Assistant` 装配后其工具集合包含该方法对应的 `ToolSpecification`（名称即查询天气），即 `AiServices` 确实注册了该工具。
- **A4｜Scenario**：SSE 流式输出 —— `GET /api/chat/stream?message=...` 响应 `Content-Type` 为 `text/event-stream`，至少收到 1 条 token 数据事件，随后收到 `done` 事件且连接正常关闭；流式中途模型异常时通过 `SseEmitter.completeWithError` 结束。
- **A5｜Scenario**：构建与 Java 8 合规 —— `./mvnw -o -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml clean test` 在 JDK 8 下 BUILD SUCCESS，0 failed / 0 errors；新增源码中不存在 `List.of(`、`Map.of(`、`var `、`Stream.toList()`、文本块等 Java 9+ 写法。
- **A6｜Scenario**：知识库故障降级 —— 远程知识库返回 500 或读取超时时 `retrieve` 返回空列表且不抛出异常；此时同步接口仍返回 200，`reply` 不含知识库上下文，应用已记录 warn 日志。

# 约束与不变量

- JDK 1.8 语法，UTF-8，Tab 缩进；不新增任何 Maven 依赖版本改动（版本由父 POM 导入的两个 BOM 托管）。
- LangChain4j 版本锁定 `0.35.0`，一律使用 0.35 命名（`ChatLanguageModel` / `StreamingChatLanguageModel`），不得引用 1.x 的 `ChatModel` / `StreamingChatModel`。
- `ContentRetriever` 只实现 `retrieve(Query)` 一个方法；不修改任何 LangChain4j 接口或 owner 模块代码。
- 远程知识库接口约定：请求 `{"query":"...","topK":3}`，响应 `{"items":[{"title":"...","content":"...","score":0.87}]}`，样例内不硬编码任何真实地址。
- 假数据与 mock 只出现在 `WeatherTools` 与测试代码；`src/main` 不得包含依赖外部服务的初始化逻辑。
- 不改变任何现有模块行为；样例是纯新增。

# 决策

- **D1 隔离方式**：用户最初选择 `current`，Runtime 因当前目录已存在 active change `env-risk-cleanup` 而要求独立 worktree，最终使用 `worktree`（`.worktrees/ai-chat-sample`，分支 `comet/ai-chat-sample`，目标分支 `dev`）。
- **D2 模型提供方**：Ollama 本地，无需 API key，样例可离线验证。
- **D3 交互形态**：REST 同步 + SSE 流式，两条链路共用同一个 `Assistant`，保证行为一致。
- **D4 知识库走接口而非向量数据库**：知识检索交由远程知识库服务，本地不持有向量数据；因此本样例不需要 embedding 模型。
- **D5 全部走 LangChain4j 原生 API**：不使用 `fw-plus-ai-model` 的 `AiChatModel`，避免同一功能出现两套写法。
- **D6 流式用 Spring MVC `SseEmitter`**：不引入 reactor，尽管 `fw-plus-ai-langchain4j-reactor` 提供 `TokenStreamToFluxAdapter`。
- **D7 默认模型名 `qwen2.5:7b`**（Q1 结论）：样例演示链路而非性能，中文效果差会让工具调用是否被触发不稳定；README 注明可改 `llama3.2:3b` 降低门槛。
- **D8 天气工具保持纯 mock**（Q2 结论）：零外部依赖、A3 的 mock 数据可控；真实天气 API 属另一需求。
- **D9 同步与流式都走知识库增强**（Q3 结论）：两条链路共用同一 `Assistant`，行为一致；知识库延迟由 `RestTemplate` 超时控制。

# 待解决问题

无。本轮三个问题已由用户确认并落入 D7–D9。

# 验证预期

- 模块级：`./mvnw -o -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml clean test` 通过。
- 样例层不破坏既有聚合：`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate` 通过（新 module 注册不破坏聚合）。
- 手动验证（需要本机 Ollama）：按 README 拉取模型并启动后执行 curl，验证同步、SSE、知识库、工具四条路径。
