# fw-plus-ai-store-embedding-spring-boot-starter

## 当前支持状态

本 starter 提供 Java 8 / Spring Boot 2.7.x 下的中立向量存储自动配置入口。业务代码应依赖 `fw-plus-ai-store-embedding` 的中立契约，不直接暴露 LangChain4j `EmbeddingStore<TextSegment>`、Spring AI `VectorStore` 或 AgentScope 2 Java SDK 类型。

业务选择当前 Java 8 已实现的 LangChain4j Adapter 时，同时引入：

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

## LangChain4j 切换到其他实现

当前 Java 8 版本只实现 LangChain4j Adapter。后续接入 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 时，切换步骤应是：

1. 保留本 starter。
2. 移除 `fw-plus-ai-langchain4j-adapter`。
3. 引入目标实现的 Java 17 / Spring Boot 3.x adapter starter。
4. 将 `fw-plus.ai.embedding-store.stores.<key>.framework` 改成目标 framework。
5. 保持业务 key 不变，例如 `knowledge`，让 `vector[...]` 和 Java API 调用不改 key。
6. 按目标 adapter 文档补齐 provider SDK、连接参数、维度、schema 和真实后端测试。

## Java 17 / Spring Boot 3.x 后续边界

Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 不在当前 Java 8 / Boot 2.7 reactor 中实现。后续需要在高版本线新增独立 Java 17 / Spring Boot 3.x parent、BOM、starter 和自动配置，目标 SDK 类型不得进入 `fw-plus-ai-store-embedding` 的公共签名。

AgentScope 2 Java 必须按进程内 SDK adapter 适配，不能用外部 HTTP、MCP 或 A2A 桥接替代。

## 回滚

如果迁移后无法启动，优先回滚到业务上一版 LangChain4j 组合：

1. 恢复 `fw-plus-ai-langchain4j-adapter`。
2. 将 `framework` 改回 `langchain4j`。
3. 确认 Qdrant、Milvus、Redis 等 provider SDK 已显式加入业务应用。
4. 保持 `fw-plus.ai.embedding-store` 的业务 key 不变。

不要在新版本中恢复已删除的旧 store artifact 或旧 starter。
