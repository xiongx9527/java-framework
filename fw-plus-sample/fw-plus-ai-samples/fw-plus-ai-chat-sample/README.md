# fw-plus-ai-chat-sample

大模型对话样例：**Ollama 本地模型 + 远程知识库接口（RAG）+ 天气工具调用 + SSE 流式**。

一次请求的完整链路：

```
POST /api/chat
  → RemoteKnowledgeBaseContentRetriever 调远程知识库接口取上下文
  → DefaultRetrievalAugmentor 把上下文注入 prompt
  → OllamaChatModel 生成（需要时回调 WeatherTools）
  → { reply, toolCalls }
```

## 前置条件

本机已安装并启动 [Ollama](https://ollama.com/)：

```bash
ollama pull gemma4:31b-cloud  # 若走 Ollama Cloud，改用 'ollama run gemma4:31b-cloud' 并登录
```

## 运行

```bash
# 仓库根目录
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml spring-boot:run
```

## 验证

```bash
# 同步对话（sessionId 可省略，省略后按 default 处理）
curl -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
     -d '{"message":"杭州和北京哪个更适合夏天出行？","sessionId":"demo-1"}'
# {"reply":"...","toolCalls":0}

# 触发工具调用（模型会自行决定是否调用天气工具）
curl -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
     -d '{"message":"杭州现在天气怎么样？","sessionId":"demo-1"}'
# {"reply":"杭州当前 29℃，湿度 78%...","toolCalls":1}

# SSE 流式（POST，请求体与同步接口一致）
curl -N localhost:8080/api/chat/stream -H 'Content-Type: application/json' \
  -d '{"message":"用一句话介绍杭州","sessionId":"demo-1"}'
# event:token
# data:杭州
# event:done
# data:[DONE]
```

`toolCalls` 为本次请求中天气工具被调用的次数，可直观验证"模型自主决定是否调用工具"。

## 系统提示（systemMessageProvider）

系统提示由 `AssistantBuilder.systemMessageProvider` 提供（实现挂在 `SystemPromptBuilder` 上），**没有使用 `@SystemMessage` 注解**——注解是静态的，provider 才能按会话定制：

```java
new AssistantBuilder()
        ...
        .systemMessageProvider(systemPromptBuilder::build);   // 每次调用都执行
```

`Assistant` 的两个方法把 `sessionId` 作为第一个参数并标注 `@MemoryId`，LangChain4j 每次调用会把该实参透传给 provider，因此提示词末尾的「当前会话标识」是客户端真实传入的值：

```java
String chat(@MemoryId String sessionId, String message);
TokenStream stream(@MemoryId String sessionId, String message);
```

要点：

- `sessionId` 留空（或纯空白）时 Controller 兜底为 `default`，**不能传 null**——LangChain4j 0.35.0 的 `DefaultAiServices.findMemoryId` 对 `@MemoryId` 收到 null 会直接抛 `IllegalArgumentException`，兜底逻辑由 `ChatControllerTest` 的 sessionId 用例覆盖。
- 0.35.0 的 `AiServices` 上没有 `systemMessage(String)`，只有 `systemMessageProvider(Function<Object, String>)`；若方法上同时存在 `@SystemMessage` 注解，注解优先、provider 不再生效。
- provider 返回的字符串同样支持 `{{变量}}` 模板，变量取自方法参数上 `@V("name")` 标注的值。
- 同一个 `sessionId` 就是会话记忆键（`@MemoryId`），多轮会话上下文按它隔离。

## 打印模型上下文

排查 prompt 拼接效果（系统提示对不对、RAG 注入了什么、模型看到哪些工具、工具调用传了什么）靠日志，不靠猜。

样例内置 `ai/ChatContextLogger`（实现 LangChain4j `ChatModelListener`）。框架侧的 ollama 自动配置已用 `fw.plus.ai.langchain4j.ollama.enabled=false` 关掉，改由 `ChatAssistantConfig` 自己构造 `OllamaChatModel` / `OllamaStreamingChatModel`，并在 builder 上调用**官方** `.listeners(...)` 挂载——不动任何框架文件，代价是模型参数在样例里再声明一份（`ai/OllamaProperties`，yaml 键名与框架侧完全一致）。同步与流式两条链路都会经过它。

**流式模型的配置**：`/api/chat/stream` 的流式模型走的是 **Ollama 的 OpenAI 兼容端点**，因此配置在 `langchain4j.open-ai.streaming-chat-model`（OpenAI 集成的键名）而不是 `langchain4j.ollama`（`base-url` + `/v1`，装配时自动补 `/v1`），用的是 OpenAI 集成而不是 `OllamaStreamingChatModel`——langchain4j 0.35.0 的 ollama 流式实现没有「带工具」的重载，调了会抛 `Tools are currently not supported by this model`。换到 `/v1` 后流式链路是真流式且能调工具，与同步链路行为一致。

一次对话按七类标签输出：

| 标签 | 内容 | 数据来源 |
|---|---|---|
| `TOOLS` | 本轮可用工具清单 | `ChatModelRequest.toolSpecifications()` |
| `SYSTEM` | 系统提示词 | `ChatAssistantConfig.systemMessageProvider` |
| `USER` | 用户原话 | `UserMessage` |
| `RAG` | 注入的检索内容 | `DefaultContentInjector` 把检索结果拼在 user message 尾部，logger 按模板锚点拆开 |
| `TOOL_CALL` | 模型请求的工具与参数 | `AiMessage.toolExecutionRequests()` |
| `TOOL_RESPONSE` | 工具执行结果 | `ToolExecutionResultMessage` |
| `RESPONSE` | 模型回复与 token 用量 | `ChatModelResponse` |

实际效果：

```text
════════ 模型请求上下文（4 条消息） ════════
TOOLS        可用工具 1 个
TOOLS        - queryWeather : 查询指定城市当前天气，返回温度与湿度
[0] SYSTEM      你是 fw-plus 内置智能助手，遵守以下约定：... 当前会话标识：demo-1
[1] USER        杭州天气怎么样
[2] AI          上一轮回复: 杭州夏天更凉快一些。
TOOL_CALL    模型请求工具 queryWeather 参数 {"city":"杭州"}
[3] TOOL_RESPONSE 工具 queryWeather 返回: {"temperatureC":29,...}
RESPONSE     模型回复: 杭州当前 29℃，湿度 78%。
RESPONSE     token 用量 TokenUsage{inputTokenCount=412, outputTokenCount=38, totalTokenCount=450}
```

流式链路与同步链路走的是同一套消息拼装，日志输出一致，无需额外埋点。

要看发给模型的原始 JSON（含完整工具参数 schema），把 `log-requests` / `log-responses` 置 `true`（logger 级别已在 `application.yaml` 配好）：

```yaml
langchain4j:
  ollama:
    chat-model:
      log-requests: true
      log-responses: true
```

注意：若自定义了 `ContentInjector` 模板，需同步修改 `ChatContextLogger.RAG_ANCHOR`，否则 RAG 段拆不出来。

## 多轮记忆与个人信息

`Assistant` 首个参数 `@MemoryId String sessionId` 兼任会话记忆键，也决定读取哪份个人资料：`AssistantBuilder` 的内置记忆按它隔离多轮上下文，同一 sessionId 的多次对话会带上历史，不同 sessionId 互不干扰。

```java
new AssistantBuilder()
        .memory(MAX_MEMORY_MESSAGES)   // MessageWindowChatMemory，默认 20 条
        .memoryStore(chatMemoryStore)  // 共享存储 Bean
        ...                            // build() 时内置包一层 EphemeralChatMemory
```

实现要点：

- 内置记忆为 `MessageWindowChatMemory`（按 `memoryId` 建实例）外包 `EphemeralChatMemory`，窗口 `MAX_MEMORY_MESSAGES = 20` 条，超出自动淘汰最早的消息；**SystemMessage 不占窗口额度**（淘汰时跳过）。
- **store 必须是共享 Bean**：在 `.memoryStore(...)` 里传每次 new 的存储实例会让多轮上下文静默失效；`ChatAssistantConfig` 把它声明为独立 Bean，`assistant` Bean 注入使用。
- 默认用进程内 `InMemoryChatMemoryStore`，重启即丢。生产换成实现 `ChatMemoryStore` 的 Redis / JDBC 存储即可；注意 `updateMessages` 是读全量再全量写回，落库前最好先按窗口裁剪。

### 系统提示与个人信息放哪

系统提示由 `SystemPromptBuilder` 拼装（固定人设 + profile），挂在 `AiServices.systemMessageProvider` 上每轮生成：

```java
.systemMessageProvider(systemPromptBuilder::build)   // Function<Object, String>，入参是 memoryId
```

**provider 的入参只有 memoryId**（`Function<Object, String>`，0.35.0→1.x 未变），适合放按会话稳定的内容。检索资料不能放这里——它靠 query，provider 拿不到，那部分走 `retrievalAugmentor`，见下一节。

框架会把 provider 的产出写进 memory，`MessageWindowChatMemory.add` 对 SystemMessage 是「内容相同则跳过、不同则替换」，所以每轮只有一条、profile 变更自动生效、不堆积。**不要再从别处写第二条 SystemMessage**，否则互相顶掉。

- profile 由 `UserProfileProvider` 提供（内存 mock + 60 秒缓存），未注册的 sessionId 省略该段。
- **只放影响回答的字段**：称呼、时区、语言、角色。手机号、身份证、住址不要进 prompt。
- 系统提示（含 profile 与检索资料）会出现在 `ChatContextLogger` 的 `[0] SYSTEM` 行，接真实用户前先决定脱敏方案（或把该 logger 调至 WARN）。

### 临时上下文不进历史

两类数据是**本轮临时**的，不该沉淀为会话记忆：RAG 检索内容（按当前 query 检索）、工具往返（一次调用内的过程数据）。而框架把两者都无条件写进了 memory：检索结果被拼进 user message（`DefaultAiServices:136-139` → `:158`），工具往返在 `:248`/`:270` 直接 add。

`ai/EphemeralChatMemory` 补上这一层：

```java
add(USER 含 RAG)               → 只写用户原话，RAG 暂存
add(AI 带 toolExecutionRequests) → 不写 memory，暂存
add(TOOL_RESPONSE)             → 不写 memory，暂存
messages()                     → 历史 + RAG 拼回最后一条 user + 工具对原序插在其后
```

发给模型的请求与不剥时**完全一致**（本轮的 RAG 和工具往返都在，顺序不变），只是 store 里干净。下一轮 `provider.get` 拿到新实例，暂存为空，历史里只剩「用户原话 / 系统提示 / 最终回复」。

**代价（产品上要先接受）**：追问「刚才查到的结果」时，历史只剩最终回复文本——若回复未复述具体数值，该依据不可找回（工具只能重调）。这也是为什么 RAG 剥得、工具往返要权衡：RAG 随时可重新检索，工具结果拿回来是有成本的。

若只想去掉 RAG 残留、保留工具往返，把该类里两个 `TOOL_EXECUTION` 分支改回 `delegate.add(message)` 即可。

注入格式由 `ai/RagMessageCodec` 统一封装，调用方只面对三个方法，谁也碰不到字面量：

```java
template()               → RagAutoConfig 构造 injector 用（不用框架默认模板）
join(userText, rag)      → 拼成注入形态
split(text)              → 拆回 [用户原话, 检索内容]，EphemeralChatMemory 与 ChatContextLogger 用
```

`RagMessageCodecTest` 用真实 injector 走一遍，断言「模板产出能被 split 原样拆回」——格式只有一个地方定义，不存在改了一半的可能。

## 接入自有知识库

知识库通过实现 LangChain4j 的 `ContentRetriever` 接口接入，**不依赖向量数据库**：

```yaml
fw:
  plus:
    sample:
      chat:
        knowledge-base:
          base-url: http://your-knowledge-service   # 填写后即启用
```

接口约定：

```http
POST {base-url}/api/search
Content-Type: application/json

{"query": "用户原话", "topK": 3}
```

```json
{"items": [{"title": "文档标题", "content": "文档内容", "score": 0.91}]}
```

返回的 `content` 会由 `DefaultContentInjector` 注入 prompt。远程不可达、超时或返回空时自动降级为无上下文，不影响对话。

未配置 `base-url` 时知识库完全不装配，样例退化为纯对话。

## 目录结构

| 文件 | 作用 |
|---|---|
| `ChatSampleApplication` | 启动类 |
| `ai/Assistant` | `AiServices` 声明式接口：`chat` / `stream`，参数只有 `@MemoryId sessionId` 与 `@UserMessage message` |
| `ai/ChatAssistantConfig` | 自建两个模型 Bean（挂 `ChatModelListener`），经框架 `AssistantBuilder` 装配模型/工具/RAG/记忆/系统提示 |
| `ai/OllamaProperties` | `langchain4j.ollama.*` 配置绑定（框架侧类是包私有，样例需自声明） |
| `ai/SystemPromptBuilder` | 拼装系统提示：人设 + profile |
| `ai/SystemPromptBuilder` | 拼装系统提示：人设 + profile + 检索资料 |
| `ai/FilteringContentRetriever` | 挂在 augmentor 上：score 阈值 + metadata 过滤 + 《title》渲染 |
| `ai/ChatContextLogger` | `ChatModelListener`：打印系统提示 / 工具清单 / tool_call / tool_response |
| `ai/EphemeralChatMemory` | `ChatMemory` 装饰器：RAG 与工具往返都不进历史，只在本轮请求里拼回 |
| `ai/UserProfileProvider` | 个人信息提供者（内存 mock + 60 秒缓存），演示 profile 注入系统提示 |
| `ai/WeatherTools` | `@Tool` 天气查询（本地 mock 数据） |
| `ai/knowledge/RemoteKnowledgeBaseContentRetriever` | 调远程知识库接口的 `ContentRetriever` |
| `ai/knowledge/KnowledgeBaseProperties` | 知识库配置项 |
| `ai/RagAutoConfig` | 知识库 Bean 装配（按 `base-url` 条件启用） |
| `web/ChatController` | `POST /api/chat` 与 `POST /api/chat/stream`（SSE） |

## 约束

- JDK 1.8、Tab 缩进；不使用 `List.of` / `Map.of` / `var` / `Stream.toList()` / 文本块。
- LangChain4j 锁定 `0.35.0`，模型类型为 `ChatLanguageModel` / `StreamingChatLanguageModel`。
- 本工作区禁改依赖版本，`org.ssssssss` 组依赖禁止升版。

## 测试

```bash
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml clean test
```

`RemoteKnowledgeBaseContentRetrieverTest` 用 `MockRestServiceServer` 在进程内模拟知识库接口，无需外部服务。
