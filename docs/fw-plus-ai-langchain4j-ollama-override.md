# fw-plus-ai-langchain4j-ollama 覆盖说明

**模块路径**：`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-ollama`

---

## 为什么覆盖

langchain4j-ollama 0.35.0 的上游实现有以下缺陷，导致 fw-plus 在挂 `ChatModelListener` 或使用工具调用时无法正常工作：

### 缺陷 1：OllamaStreamingChatModel 不支持带工具的流式调用

上游 0.35.0 的 `OllamaStreamingChatModel` 只有不带工具的 `generate(messages, handler)` 重载。当通过 `StreamingChatLanguageModel` 接口调用 `generate(messages, toolSpecifications, handler)` 时，会命中接口 default 方法并抛：

```
Tools are currently not supported by this model
```

**覆盖方式**：fw-plus 直接把官方 `OllamaStreamingChatModel` 源码拷进来，补齐两个带工具的 `generate` 重载。工具列表写入 `ChatRequest.tools` 后走上游的四参 `OllamaClient.streamingChat`，不改变底层通信逻辑。

### 缺陷 2：OllamaStreamingResponseBuilder 不累积 toolCalls，且 build() 可能返回 null

上游 `OllamaStreamingResponseBuilder` 在流式累积阶段只收集 `message.content`，完全忽略 `message.toolCalls`。Ollama 原生 `/api/chat` 端点把工具调用以完整 JSON 结构随 `ChatResponse` 下发，丢失后模型回调的函数调用信息就丢了。

更严重的是，当模型完全不产文本（比如只回调了工具调用就结束），`build()` 会因为 `contentBuilder` 为空而返回 `null`。挂 listener 后，`ChatModelResponseContext` 构造器收到 null response 直接抛：

```
IllegalArgumentException: response cannot be null
```

**覆盖方式**：fw-plus 版 `OllamaStreamingResponseBuilder` 在 `append()` 中同时收集 `content` 和 `toolCalls`，`build()` 整合为 `AiMessage.from(content, toolExecutionRequests)` 后返回，永不返回 `null`。

---

## 覆盖类清单

所有覆盖采用 **同包名同 FQCN 类路径覆盖**（fw-plus 编译出的 class 在 classpath 中优先于 `langchain4j-ollama-0.35.0.jar` 内的同名 class），用户无需改任何 import，又提供了 fw-plus 增强。

| FQCN | 上游 0.35.0 行为 | fw-plus 覆盖行为 |
|------|-----------------|-----------------|
| `dev.langchain4j.model.ollama.OllamaStreamingChatModel` | 仅支持不带工具的流式调用，工具重载命中 default 抛异常 | 补齐两个带工具的 `generate` 重载，工具写入 `ChatRequest`，其余逻辑一致 |
| `dev.langchain4j.model.ollama.OllamaStreamingResponseBuilder` | 只累积 content，未收 toolCalls，`build()` 可能返回 null | 同时累积 content + toolCalls，`build()` 永不返回 null，工具调用完整传递 |
| `dev.langchain4j.ollama.spring.AutoConfig` | 上游为 package-private，fw-plus 版为 public 以便应用侧排除或引用 | `public class AutoConfig`，Spring 自动配置类可访问 |
| `dev.langchain4j.ollama.spring.*Properties` | 上游属性类 | 同 FQCN 覆盖，扩展字段以对齐样例配置 |

---

## 同 OpenAI 集成类似的修复

OpenAI 兼容端点（Ollama 的 `/v1/chat/completions`、Azure OpenAI、GitHub Models 等）存在同模式问题：

- `OpenAiStreamingResponseBuilder` 在 `delta.content() != null` 时提前 `return`，同帧 `tool_calls` 被丢弃 → `build()` 返回 null → 挂 listener 抛 NPE。
- 上游只取 `delta.toolCalls().get(0)`，多工具调用只保留第一个。

fw-plus 的修复版本位于 `fw-plus-ai-langchain4j-openai`：

| FQCN | 修复点 |
|------|--------|
| `dev.langchain4j.model.openai.OpenAiStreamingChatModel` | 官方源码复制，无改动（仅同 FQCN 覆盖，保持 API 一致） |
| `dev.langchain4j.model.openai.OpenAiStreamingResponseBuilder` | 去掉 content 分支的 `return`，同帧 content + tool_calls 都累积；多 tool_calls 按 index 排序遍历；`build()` 永不返回 null |

---

## Dashscope 对照

`QwenStreamingResponseBuilder`（langchain4j-dashscope 0.35.0）**没有同类问题**：其 `append()` 不做提前 `return`，逐帧追加内容，`build()` 始终返回非 null 的 `Response`。但也因此**不支持流式工具调用**——Dashscope 流式模式下工具调用需要走非流式路径。

---

## 升级指引

langchain4j 升到 1.x 后，上游应已修复上述问题（或 API breaking change 需要迁移），届时应：

1. 删除 `fw-plus-ai-langchain4j-ollama/src/main/java/dev/langchain4j/model/ollama/` 下的覆盖类
2. 删除 `fw-plus-ai-langchain4j-openai/src/main/java/dev/langchain4j/model/openai/` 下的覆盖类
3. 验证样例中 `ChatAssistantConfig` 的流式调用能正常携带工具链路
4. 详见 `docs/springboot3-upgrade-notes.md`
