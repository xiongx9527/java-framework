# fw-plus-ai-store-embedding-spring-boot-starter

## 当前支持状态

本 starter 提供 Java 17 / Spring Boot 3.5.8 下的中立向量存储自动配置入口。业务代码应依赖 `fw-plus-ai-store-embedding` 的中立契约，不直接暴露 LangChain4j `EmbeddingStore<TextSegment>`、Spring AI `VectorStore` 或 AgentScope 2 Java SDK 类型。

当前可用的中立 VECTOR 路径仍需业务显式选择兼容的 LangChain4j Adapter：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-store-embedding-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

如果只引入本 starter，没有任何 framework adapter，应用不会获得真实向量 store 实例；这是为了保证后续 framework 可替换。

模型配置前缀是 `fw-plus.ai.model`，向量存储配置前缀是 `fw-plus.ai.embedding-store`；两者共享同一套中立 adapter 选择规则。

## 配置方式

向量存储统一使用 `fw-plus.ai.embedding-store` 前缀。业务选择 LangChain4j 时将 `framework` 写为 `langchain4j`，Qdrant、Milvus、Redis 等写在 `provider`。

```yaml
fw-plus:
  ai:
    model:
      enabled: false
    embedding-store:
      enabled: true
      default-store: knowledge
      stores:
        knowledge:
          framework: langchain4j
          provider: qdrant
          type: VECTOR
          enabled: true
          properties:
            host: localhost
            port: 6334
            collectionName: knowledge
```

注意：`type` 是中立能力类型，向量库统一使用 `VECTOR`；具体向量数据库写到 `provider`。

## 自动感知环境

启动时会读取中立 store 定义，并交给 classpath 中可用的 adapter 判断是否支持：

1. 配置了 `framework` 时，只匹配同名 adapter。
2. 未配置 `framework` 时，按 `provider + type` 查找候选 adapter。
3. 候选为 1 个时创建 store；候选为 0 个或多个时失败，并输出脱敏诊断。

Magic-API 的 `vector[...]` 保持按业务 key 访问，但返回中立 facade，不保证能拿到任何 framework 原生对象。

## Java 类型迁移

LangChain4j Store 仅保留中立链，旧 public 类型不提供兼容壳：

| 已删除类型 | 替代类型 |
| --- | --- |
| `EmbeddingStoreDefinition` | `AiEmbeddingStoreDefinition` |
| `EmbeddingStoreProperties` | `AiEmbeddingStoreProperties` |
| `EmbeddingStoreFactory` | `AiEmbeddingStoreFactory` |
| `EmbeddingStoreRegistry` | `AiEmbeddingStoreRegistry` |
| `DefaultEmbeddingStoreRegistry` | `DefaultAiEmbeddingStoreRegistry` |
| `EmbeddingStoreException` | `AiEmbeddingStoreException` |
| `DefaultEmbeddingStoreFactory` | `DefaultLangChain4jEmbeddingStoreAdapter`（由自动配置创建） |
| `EmbeddingStoreProviderAdapter` | `LangChain4jEmbeddingStoreProviderAdapter` |

业务代码应依赖中立 Store Interface；只有实现自定义 LangChain4j provider 创建逻辑时才实现新的 Provider Adapter Interface。

## Spring AI 首版边界

`fw-plus-ai-spring-adapter` 当前只实现同步 CHAT/EMBEDDING，**不注册** `AiEmbeddingStoreAdapter`。Spring AI 标准 `VectorStore` 接受文本查询并可能自行嵌入，而中立查询携带预计算向量；不得丢弃、字符串化或重新计算调用方向量来伪装兼容。

因此本 starter 的 Spring AI / Spring AI Alibaba VECTOR 切换尚不可用。只有后续 provider bridge 能保留预计算向量、ID、metadata、score/filter 与幂等 delete 语义时，才能新增 store adapter。业务 `vector[...]` key 可保持不变，但在此之前仍需显式安装已验证的兼容 store adapter。

AgentScope 2 Java 同样未实现；任何未来 adapter 都不得把 SDK 类型带入 `fw-plus-ai-store-embedding` 的公共签名。

## 回滚

如果迁移后无法启动，优先回滚到业务上一版 LangChain4j 组合：

1. 恢复 `fw-plus-ai-langchain4j-adapter`。
2. 将 `framework` 改回 `langchain4j`。
3. 确认 Qdrant、Milvus、Redis 等 provider SDK 已显式加入业务应用。
4. 保持 `fw-plus.ai.embedding-store` 的业务 key 不变。

不要在新版本中恢复已删除的旧 store artifact 或旧 starter。
