# fw-plus-ai-native

真实原子能力层：把 `fw-plus-ai-base` 定义的端口落成可直接运行的实现。
与 `fw-plus-ai-support`（组合 / 装饰）、`fw-plus-ai-trace`（观测导出）平级；
装配阶段由业务选定具体后端，不污染 base 与 support。

只依赖 JDK 的 `HttpURLConnection` 与 okhttp3，不引入任何厂商模型 SDK。

## 能力

| 包 | 实现 | 端口 | 说明 |
| --- | --- | --- | --- |
| `nativeimpl.llm` | `OpenAiCompatibleLlmPort` | `LlmPort` | OpenAI Chat Completions 兼容协议（OpenAI / vLLM / Ollama / Qwen 等）；okhttp-sse 自研流式解析 |
| `nativeimpl.llm` | `AzureOpenAiLlmPort` | `LlmPort` | Azure OpenAI deployment URL、`api-version`、`api-key` 认证 |
| `nativeimpl.llm` | `AnthropicLlmPort` | `LlmPort` | Claude Messages API（`x-api-key`、`anthropic-version`、`tool_use` 块） |
| `nativeimpl.embedding` | `OpenAiCompatibleEmbeddingPort` | `EmbeddingPort` | `/v1/embeddings`，批量编码并记录维度 |
| `nativeimpl.rerank` | `RerankModelPort` | `RerankPort` | TEI `/rerank` 与 Cohere/Jina `/v1/rerank` 两种报文；分数按输入下标回填 |
| `nativeimpl.store.vector` | `QdrantVectorStore` | `VectorStorePort` | Qdrant REST，支持 `api-key` |
| `nativeimpl.store.vector` | `MilvusVectorStore` | `VectorStorePort` | Milvus 2.5+/2.6 的 `/v2/vectordb/...` REST |
| `nativeimpl.store.graph` | `Neo4jGraphStore` | `GraphStorePort` | Neo4j HTTP transactional Cypher（`/db/{db}/tx/commit`） |
| `nativeimpl.http` | `JdkHttp` | — | Qdrant / Milvus / Neo4j / rerank 共用的轻量 HTTP + JSON 传输 |

## 模型目录与校验

三个模型端口都提供同一组能力，供配置页选型与启动自检使用：

```java
List<ModelInfo> listModels();              // 枚举可用模型；不支持枚举时返回空列表，不抛异常
ModelValidation validateModel(String model); // 校验模型可用性；失败返回原因，不抛异常
```

| 端口 | `listModels` | `validateModel` |
| --- | --- | --- |
| `LlmPort` | OpenAI 兼容走 `GET /models`；Azure 走 `GET /openai/models`；Anthropic 走 `GET /v1/models` | 目录非空时按目录判定；目录为空时发一次 `max_tokens=1` 的真实补全探测 |
| `EmbeddingPort` | 默认空列表；OpenAI 兼容走 `GET /models` 并按模型名关键字（embed/bge/gte/m3e/e5/jina/voyage）过滤 | 用目标模型真实编码一条探针文本，拿到非空向量即通过，并记录维度 |
| `RerankPort` | TEI / Cohere 无标准枚举端点，返回当前配置的模型（单条） | 用目标模型真实打一次分，返回结果非空即通过 |

`ModelValidation` 不抛异常：`isValid()` + `getMessage()` + `getLatencyMillis()` 足以在配置页展示连通性。
装饰器（`TracedLlmPort`、`CachingEmbeddingPort`、`ListeningRerankPort` 等）一律透传给内层端口。

## 接入

```java
// 对话
LlmPort llm = new OpenAiCompatibleLlmPort("https://api.openai.com/v1", apiKey, "gpt-4o");
LlmPort claude = new AnthropicLlmPort("https://api.anthropic.com", apiKey, "claude-3-5-sonnet");

// 向量与重排
EmbeddingPort embedding = new OpenAiCompatibleEmbeddingPort(baseUrl, apiKey, "text-embedding-3-small");
RerankPort rerank = new RerankModelPort(baseUrl + "/rerank", RerankModelPort.Style.TEI);

// 存储
VectorStorePort vector = new QdrantVectorStore("http://localhost:6333", apiKey, "default");
VectorStorePort milvus = new MilvusVectorStore("http://localhost:19530", "root:Milvus", "default");
GraphStorePort graph = new Neo4jGraphStore("http://localhost:7474", "neo4j", password);
```

密钥、地址一律从配置传入，不要硬编码。

## 约定

- **失败语义**：模型端口的流式通道不抛异常，错误经 `onError` 交付；检索 / 存储端口的查询失败返回空结果或记录日志，
  由上层决定降级；`validateModel` 一律以结果对象表达失败。
- **过滤在客户端完成**：base 的 `Filter` 不暴露内部结构，Qdrant / Milvus 的元数据过滤与候选 id 收敛都在客户端做，
  查询时按 `topK * 10`（下限 200）放大召回再裁剪。
- **字符串主键映射**：Milvus 简化建表形态的主键是 Int64，写入时把字符串 id 映射为稳定的 64 位代理键，
  原始 id 存入动态字段 `pk`，检索时据此还原。
- **图存储属性类型**：Neo4j 节点统一打 `__Node` 标签，关系统一用 `__EDGE` 类型并把业务类型放进 `r.type`
  （Cypher 的关系类型不能参数化）；metadata 序列化进 `__meta` 字符串属性，规避非原生属性类型不兼容。

## 测试

```bash
# 契约测试（进程内 HTTP 桩，无需外部服务）
mvn -pl fw-plus-ai-native -am test

# 真实后端冒烟（默认跳过，需显式给出地址/凭据）
mvn -pl fw-plus-ai-native -am test -Dqdrant.apiKey=... 
mvn -pl fw-plus-ai-native -am test -Dmilvus.url=http://localhost:19530 -Dmilvus.token=root:Milvus
```

- 契约测试用 `FakeHttpServer` 模拟真实 REST 报文，覆盖请求路径、鉴权头、请求体字段与响应解析。
- 真实后端测试用 `Assumptions` 探测可用性，不可用时跳过而不是失败；Qdrant 需要 `apiKey`，Milvus 需要 v2 REST 已开启。
