# fw-plus-ai-chat-sample 设计文档

- 日期：2026-09-24
- 状态：待评审
- 分支基线：`dev`

## 1. 背景与目标

`fw-plus-sample/fw-plus-ai-samples/` 下现有两个样例（`fw-plus-ai-magicapi-sample`、`fw-plus-ai-mcp-server-sample`），都偏"接入框架能力"，没有一个演示**完整的大模型对话链路**。

本样例要演示的最小完整链路是：

```
用户消息 → 调用远程知识库接口取上下文 → 注入 prompt → 大模型生成 → （按需）工具调用 → 返回
                ↘ 流式逐 token 推送
```

技术基线：Java 8、Spring Boot 2.7.18、LangChain4j `0.35.0`（本仓库 `dev.langchain4j.*` owner 覆盖模块）、Spring MVC（非 WebFlux）。

目标读者是框架使用者：看完这个样例应当能回答"fw-plus 里怎么发起一次对话、怎么加工具、怎么把自有知识库接进 prompt"。

## 2. 范围

**包含**

- Ollama 本地模型（`chat-model` 与 `streaming-chat-model`）
- REST 同步接口 + SSE 流式接口
- 知识库：自定义 `ContentRetriever` 调用远程 HTTP 知识库接口，检索结果由 `DefaultContentInjector` 注入 prompt
- 工具调用：一个本地 mock 的天气查询 `@Tool`

**明确不做（YAGNI）**

- 前端页面（curl / 浏览器 SSE 验证即可）
- 真实天气 API（mock 固定数据，避免样例依赖外部服务与 key）
- **向量数据库**：不做 embedding、不做文档切分灌库、不用 `InMemoryEmbeddingStore`。知识检索完全交由远程知识库服务负责，本地不持有任何向量数据
- 多轮会话记忆（`ChatMemory`）——避免与检索、流式的演示目标混在一起
- MCP / A2A —— Java 8 线上不可用（`fw-plus-ai-langchain4j-agentic` 已删除）
- 真实远程知识库服务部署：只实现客户端；测试用 `MockRestServiceServer` 在进程内模拟该 HTTP 接口，不启动真实服务、不依赖任何外部进程

## 3. 模块坐标

```
groupId    com.xiongx9527.fw.plus.ai
artifactId fw-plus-ai-chat-sample
version    0.1.1-SNAPSHOT
parent     fw-plus-ai-samples
```

父 POM `fw-plus-ai-samples` 已提供 `spring-boot-starter-web`、`knife4j-openapi3-spring-boot-starter`、`fw-plus-ai-boot-starter` 与两个 BOM 导入。样例模块只需补 Ollama 覆盖模块与测试依赖。

属性需显式覆盖父级的 `maven.test.skip=true`（参照 `fw-plus-ai-mcp-server-sample`，本模块要跑单测）。

## 4. 文件结构

```
fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/xiongx9527/fw/plus/ai/sample/chat/
    │   ├── ChatSampleApplication.java
    │   ├── ai/
    │   │   ├── Assistant.java                        # AiServices 声明式接口
    │   │   ├── WeatherTools.java                     # @Tool 天气查询（mock）
    │   │   ├── knowledge/
    │   │   │   ├── RemoteKnowledgeBaseContentRetriever.java  # implements ContentRetriever
    │   │   │   ├── KnowledgeBaseProperties.java      # base-url / path / top-k / timeout
    │   │   │   └── dto/                             # 远程接口请求/响应 DTO
    │   │   │       ├── KnowledgeSearchRequest.java
    │   │   │       └── KnowledgeSearchResponse.java
    │   │   └── RagAutoConfig.java                    # 装配 ContentRetriever + RetrievalAugmentor
    │   └── web/ChatController.java
    └── main/resources/
        ├── application.yaml
        └── prompts/assistant-system.txt
    └── test/java/.../web/ChatControllerTest.java
    └── test/java/.../ai/knowledge/RemoteKnowledgeBaseContentRetrieverTest.java
```

同时在 `fw-plus-sample/fw-plus-ai-samples/pom.xml` 的 `<modules>` 中注册新模块。

## 5. 组件设计

### 5.1 Assistant（AiServices 声明式接口）

```java
public interface Assistant {
    String chat(@V("message") String message);
    TokenStream stream(@V("message") String message);
}
```

系统提示词放 `prompts/assistant-system.txt`，由 `@SystemMessage(fromResource = ...)` 或 `AiServices.builder().systemMessage(...)` 装载。装配时注入 `chatModel` / `streamingChatModel` / `tools` / `contentRetriever`。

> `@MemoryId` 与 `ChatMemory` 均不使用（见范围说明）。参数只保留 `message` 一项，保持样例最小。

### 5.2 模型 Bean

来自 owner 模块 `dev.langchain4j.ollama.spring.AutoConfig`，配置前缀 `langchain4j.ollama.*`：

```yaml
langchain4j:
  ollama:
    chat-model:
      base-url: http://localhost:11434
      model-name: qwen2.5:7b
      temperature: 0.3
    streaming-chat-model:
      base-url: http://localhost:11434
      model-name: qwen2.5:7b
```

两个 Bean 的启用条件分别是 `chat-model.base-url` 与 `streaming-chat-model.base-url`。本样例**不需要 `embedding-model`**（无本地向量化）。

### 5.3 知识库接入（核心）

`ContentRetriever` 接口只有一个方法（0.35.0 核实）：

```java
public interface ContentRetriever {
    List<Content> retrieve(Query query);   // Query.text() 即用户原话
}
```

`RemoteKnowledgeBaseContentRetriever` 实现它：

| 步骤 | 说明 |
|---|---|
| 取查询文本 | `query.text()` |
| 调远程接口 | `RestTemplate.postForObject(baseUrl + path, request, KnowledgeSearchResponse.class)` |
| 请求体 | `{"query": "...", "topK": 3}` |
| 响应体 | `{"items": [{"title": "...", "content": "...", "score": 0.87}, ...]}` |
| 转换 | 每个 item → `Content.from(TextSegment.from(content, Metadata.from("title", title)))` |
| 空结果 | 返回空 `List`，`DefaultContentInjector` 自然跳过注入，对话不受影响 |
| 失败 | 捕获异常记 warn 并返回空列表——**不让知识库故障打断对话**（样例策略，已在 README 说明） |

实现用 `RestTemplate` + Jackson（Java 8 / Spring Boot 2.7 原生，不引 WebClient）。

装配：

```java
@Bean
@ConditionalOnProperty(prefix = "fw.plus.sample.chat.knowledge-base", name = "base-url")
ContentRetriever contentRetriever(KnowledgeBaseProperties props) {
    return new RemoteKnowledgeBaseContentRetriever(props, restTemplate);
}

@Bean
RetrievalAugmentor retrievalAugmentor(ContentRetriever retriever) {
    return DefaultRetrievalAugmentor.builder()
            .contentRetriever(retriever)
            .contentInjector(new DefaultContentInjector())  // 默认模板，无需自写 prompt 拼接
            .build();
}
```

未配置 `knowledge-base.base-url` 时两个 Bean 都不创建，`AiServices` 退化为纯对话——保证样例开箱可跑。

**接口验证方式**：单元测试用 `MockRestServiceServer` 绑定样例自己的 `RestTemplate`，在进程内模拟知识库 HTTP 响应，无需外部服务即可跑通全链路；README 中给出该接口的请求/响应 JSON 样例，便于接到真实知识库时对照。

### 5.4 工具调用：天气查询

```java
public class WeatherTools {
    @Tool("查询指定城市的实时天气")
    public String queryWeather(@ToolArg("城市名，例如：北京") String city) { ... }
}
```

返回本地构造的 JSON 字符串（温度、湿度、风力、描述），数据来自静态 `Map`，注释明确标为 mock。由模型自行决定是否调用、传什么参数，演示工具调用的自主性。

`WeatherTools` 内部维护一个 `AtomicInteger callCount`，同步接口据此在响应中回报 `toolsUsed`，用于直观验证"模型这次是否选择了调用工具"。

### 5.5 流式

`Assistant.stream(message)` 返回 `TokenStream`，Controller 用 Spring MVC 的 `SseEmitter` 桥接：

```java
SseEmitter emitter = new SseEmitter(0L);
tokenStream.onPartialResponse(t -> emitter.send(SseEmitter.event().data(t)))
           .onCompleteResponse(r -> emitter.complete())
           .onError(emitter::completeWithError)
           .start();
return emitter;
```

不引入 reactor（`fw-plus-ai-langchain4j-reactor` 的 `TokenStreamToFluxAdapter` 适用于返回 `Flux<String>` 的接口，与 `SseEmitter` 方案二选一即可，样例取更直接的 `SseEmitter`）。

## 6. 接口契约

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| 同步 | `POST /api/chat` | `{"message":"杭州和北京哪个更适合夏天出行？"}` | `{"reply":"...","toolCalls":1}` |
| 流式 | `GET /api/chat/stream?message=...` | query 参数 `message` | SSE：若干 `data:{token}`，末条 `event:done` |

`toolCalls` 为本次请求中 `WeatherTools` 被调用的次数（0 表示模型未选择工具）。

## 7. 错误处理

| 场景 | 行为 |
|---|---|
| Ollama 未启动 / 模型名不存在 | 启动阶段不失败（Bean 由 `@ConditionalOnProperty` 建立），请求阶段 503 + `{"error":"model unavailable"}` |
| 未配置知识库 `base-url` | RAG Bean 不装配，warn 提示，对话仍可用 |
| 知识库接口超时 / 5xx | `retrieve` 返回空列表 + warn，对话继续（知识降级） |
| SSE 进行中异常 | `emitter.completeWithError(e)`，连接中断可见 |

不做重试与熔断，超出演示范围。知识库超时通过 `RestTemplate` 的 `ClientHttpRequestFactory` 简单设置（如 `HttpComponentsClientHttpRequestFactory.setReadTimeout`）。

## 8. 测试

`ChatControllerTest`（`@SpringBootTest` + MockMvc，JDK 8）：

- `POST /api/chat` 返回 200 且 JSON 含 `reply` / `toolCalls`（`Assistant` 用 `@MockBean` 隔离真实模型）
- `GET /api/chat/stream` 返回 `text/event-stream`，断言至少一条 token 事件与 `done` 事件
- 知识库未配置时同步接口仍返回 200（验证降级路径）

`RemoteKnowledgeBaseContentRetrieverTest`：

- 用 `MockRestServiceServer` 断言请求体含 `query` 与 `topK`
- 响应含 2 条 item 时返回 2 个 `Content`，且 `TextSegment` 带上 `title` 元数据
- 远程返回 500 时 `retrieve` 返回空列表且不抛异常
- 远程返回空 `items` 时返回空列表

测试须在 JDK 8 下真实执行（样例层 `maven.test.skip` 显式置 `false`）。

## 9. 构建与运行

```bash
# 前置：本机 Ollama 已启动
ollama pull qwen2.5:7b

# 构建（JDK 8，样例层统一入口）
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml test

# 运行
cd fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample
../../../../fw-plus-core/mvnw spring-boot:run

# 验证
curl -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
     -d '{"message":"杭州和北京哪个更适合夏天出行？"}'
curl -N 'localhost:8080/api/chat/stream?message=用一句话介绍杭州'
```

模型名、base-url、知识库开关集中在 `application.yaml` 顶部并附注释。

## 10. 备注与约束

- LangChain4j `0.35.0` 用 `ChatLanguageModel` / `StreamingChatLanguageModel`，1.x 已改名 `ChatModel` / `StreamingChatModel`。本仓库锁定 0.35.0，样例一律使用 0.35 命名（依据 `fw-plus-ai-langchain4j/README.md`「模型 API 版本边界」）。
- 所有源码保持 Java 8 兼容：不得使用 `List.of()`、`Map.of()`、`var`、`Stream.toList()`、文本块。
- 版本由父 POM 导入的两个 BOM 托管，模块内不写死版本号。
- 本样例不依赖向量数据库；若后续要演示向量检索，应另建样例（`EmbeddingStore` 自定义实现 + `EmbeddingStoreContentRetriever`），不与本例混在一起。
