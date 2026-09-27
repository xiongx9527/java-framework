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
ollama pull qwen2.5:7b        # 机器配置有限可换 llama3.2:3b，改 application.yaml 的 model-name
```

## 运行

```bash
# 仓库根目录
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml spring-boot:run
```

## 验证

```bash
# 同步对话
curl -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
     -d '{"message":"杭州和北京哪个更适合夏天出行？"}'
# {"reply":"...","toolCalls":0}

# 触发工具调用（模型会自行决定是否调用天气工具）
curl -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
     -d '{"message":"杭州现在天气怎么样？"}'
# {"reply":"杭州当前 29℃，湿度 78%...","toolCalls":1}

# SSE 流式
curl -N 'localhost:8080/api/chat/stream?message=用一句话介绍杭州'
# event:token
# data:杭州
# event:done
# data:[DONE]
```

`toolCalls` 为本次请求中天气工具被调用的次数，可直观验证"模型自主决定是否调用工具"。

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
| `ai/Assistant` | `AiServices` 声明式接口：`chat` / `stream` |
| `ai/ChatAssistantConfig` | 装配模型 + 工具 + RAG |
| `ai/WeatherTools` | `@Tool` 天气查询（本地 mock 数据） |
| `ai/knowledge/RemoteKnowledgeBaseContentRetriever` | 调远程知识库接口的 `ContentRetriever` |
| `ai/knowledge/KnowledgeBaseProperties` | 知识库配置项 |
| `ai/RagAutoConfig` | 知识库 Bean 装配（按 `base-url` 条件启用） |
| `web/ChatController` | `POST /api/chat` 与 `GET /api/chat/stream` |

## 约束

- JDK 1.8、Tab 缩进；不使用 `List.of` / `Map.of` / `var` / `Stream.toList()` / 文本块。
- LangChain4j 锁定 `0.35.0`，模型类型为 `ChatLanguageModel` / `StreamingChatLanguageModel`。
- 本工作区禁改依赖版本，`org.ssssssss` 组依赖禁止升版。

## 测试

```bash
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/pom.xml clean test
```

`RemoteKnowledgeBaseContentRetrieverTest` 用 `MockRestServiceServer` 在进程内模拟知识库接口，无需外部服务。
