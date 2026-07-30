# fw-plus-ai-tiny-engine-spring-boot-starter

## 当前支持状态

本 starter 为 TinyEngine 提供 Spring Boot 2.7.x / Java 8 下的 AI 自动配置入口。业务侧的模型和向量存储应使用中立契约：模型通过 `AiModelRegistry`（`fw-plus-ai-model`），向量通过 `AiEmbeddingStoreRegistry`（`fw-plus-ai-store-embedding`）。LangChain4j 只是当前默认的 adapter/provider，不是 TinyEngine 业务代码必须直接依赖的唯一实现。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-tiny-engine-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

starter 已接入中立 model/store starter，并带入默认 LangChain4j adapter。使用具体 provider 时仍需按 provider 文档显式加入对应 SDK；starter 不会把所有 provider SDK 打包进来。

## 最小配置

总开关是 `fw.plus.tiny-engine.ai.enabled`，默认开启。模型 key 和向量 store key 是注册表中的业务逻辑 key，不是 LangChain4j 原生对象名称。

```yaml
fw:
  plus:
    tiny-engine:
      ai:
        enabled: true
        provider: langchain4j
        missing-dependency-policy: fail-fast
        model:
          chat-key: chat-default
          streaming-chat-key: chat-default
          embedding-key: embedding-default
        vector:
          enabled: true
          store-key: knowledge
        store:
          store-key: knowledge
        chat:
          enabled: true
        rag:
          enabled: true
          chunk-size: 500
          chunk-overlap: 50
          max-results: 5
          min-score: 0.0
```

`model.embedding-key` 必须解析到可用的 embedding 模型；未配置时，RAG 会尝试使用 `AiModelRegistry` 的默认 key。`vector.store-key` 优先于 `store.store-key`；未配置时，RAG 会尝试使用 `AiEmbeddingStoreRegistry` 的默认 key。

## LangChain4j 默认 adapter 示例

如果应用采用当前默认实现，可显式声明 adapter，并在中立配置中声明 provider：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

```yaml
fw-plus:
  ai:
    model:
      enabled: true
      default-model: chat-default
      models:
        chat-default:
          framework: langchain4j
          provider: openai
          type: CHAT
          enabled: true
          properties:
            base-url: http://localhost:11434/v1
            api-key: ${AI_API_KEY}
            model-name: gpt-4o-mini
        embedding-default:
          framework: langchain4j
          provider: openai
          type: EMBEDDING
          enabled: true
          properties:
            base-url: http://localhost:11434/v1
            api-key: ${AI_API_KEY}
            model-name: text-embedding-3-small
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

上例中的 `framework: langchain4j` 只选择默认 adapter；业务仍通过中立注册表按 `chat-default`、`embedding-default` 和 `knowledge` 访问能力。后续切换到其他 framework 时，保持业务 key 不变并替换 adapter/provider。

## RAG 与知识库边界

开启 `rag.enabled` 后，TinyEngine RAG 需要同时具备 `AiModelRegistry` 中可用的 embedding 模型和 `AiEmbeddingStoreRegistry` 中可用的向量 store。知识库服务负责读取文档、切分文本并写入文本 chunk；当前中立 `AiModel` 不向 TinyEngine 服务直接暴露 embedding 生成调用。chunk 的向量生成、批量写入和具体后端策略由配置的中立 store/provider 承担，不应把当前实现描述为服务端已直接生成 embedding。

## 缺失依赖与禁用行为

`missing-dependency-policy` 支持：

- `fail-fast`：默认值。RAG 缺少中立模型注册表、向量注册表或对应 key 时，启动失败并给出缺失能力诊断。
- `degrade`：RAG Bean 保留为不可用状态，`isAvailable()` 返回 `false`，检索返回空结果；适合暂时只启用聊天的部署。

设置 `fw.plus.tiny-engine.ai.enabled=false` 时，TinyEngine AI 自动配置整体不生效，不创建 TinyEngine Chat、RAG、知识库服务和默认 provider Bean。单独设置 `chat.enabled=false` 或 `rag.enabled=false` 时，只关闭对应能力；聊天关闭不会要求向量依赖，RAG 关闭也不会创建 RAG/知识库服务。

## 旧坐标边界

当前 starter line 不使用旧的 `fw-plus-ai-langchain4j-model-spring-boot-starter` 或 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 坐标。它们只能作为历史版本迁移提示，不能作为当前主路径或新增业务依赖；请使用 `fw-plus-ai-model-spring-boot-starter`、`fw-plus-ai-store-embedding-spring-boot-starter` 与统一的 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
