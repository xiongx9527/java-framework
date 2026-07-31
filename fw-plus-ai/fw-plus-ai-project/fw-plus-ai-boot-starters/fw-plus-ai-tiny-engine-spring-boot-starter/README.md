# fw-plus-ai-tiny-engine-spring-boot-starter

## 当前支持状态

本 starter 为 TinyEngine 提供 Spring Boot 2.7.x / Java 8 下的 AI 自动配置入口。业务侧的模型和向量存储应使用中立契约：模型通过 `AiModelRegistry`（`fw-plus-ai-model`），向量通过 `AiEmbeddingStoreRegistry`（`fw-plus-ai-store-embedding`）。具体 framework adapter 由业务应用显式选择；LangChain4j 只是当前可用的默认实现之一，不由本 starter 传递绑定。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-tiny-engine-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

业务引入本 starter 后，可同时获得现有 `fw-plus-tiny-engine-spring-boot-starter` 提供的 TinyEngine Boot、Web 与 MyBatis-Plus 基础装配，以及中立 model/store starter。Boot Starter 仍保持 AI-free，本 starter 仅通过 Maven 依赖聚合它，不复制或重新实现 Boot 配置。

业务必须按目标 framework/provider 显式加入对应 adapter starter 和 provider SDK；starter 不会替业务选择 LangChain4j，也不会把所有 provider SDK 打包进来。

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

## LangChain4j adapter 配置示例

如果业务选择 LangChain4j，需要在应用 POM 中显式加入 adapter starter，并按 provider 文档额外添加对应 SDK：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter-spring-boot-starter</artifactId>
</dependency>
```

下面是在中立配置中选择 LangChain4j adapter 的示例：

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

上例中的 `framework: langchain4j` 只选择业务显式安装的 LangChain4j adapter；业务仍通过中立注册表按 `chat-default`、`embedding-default` 和 `knowledge` 访问能力。后续切换到其他 framework 时，保持业务 key 不变并替换 adapter/provider。

## RAG 与知识库边界

开启 `rag.enabled` 后，TinyEngine RAG 需要同时具备 `AiModelRegistry` 中可用的 embedding 模型和 `AiEmbeddingStoreRegistry` 中可用的向量 store。知识库服务负责读取文档、切分文本并写入文本 chunk；当前中立 `AiModel` 不向 TinyEngine 服务直接暴露 embedding 生成调用。chunk 的向量生成、批量写入和具体后端策略由配置的中立 store/provider 承担，不应把当前实现描述为服务端已直接生成 embedding。

## 缺失依赖与禁用行为

`missing-dependency-policy` 支持：

- `fail-fast`：默认值。RAG 缺少中立模型注册表、向量注册表或对应 key 时，启动失败并给出缺失能力诊断。
- `degrade`：对应 Bean 保留为不可用状态，`isAvailable()` 返回 `false`，请求时抛出包含不可用原因的异常；适合暂时只启用部分能力但不伪造成功的部署。

设置 `fw.plus.tiny-engine.ai.enabled=false` 时，TinyEngine AI 自动配置整体不生效，不创建 TinyEngine Chat、RAG、知识库服务和默认 provider Bean。单独设置 `chat.enabled=false` 或 `rag.enabled=false` 时，只关闭对应能力；聊天关闭不会要求向量依赖，RAG 关闭也不会创建 RAG/知识库服务。

## REST 兼容入口

REST 路由默认关闭。启用后，Controller 仅在 Servlet Web 应用中且 Chat、RAG 服务均已注册时生效；总开关关闭时不会注册任何 TinyEngine AI 路由。所有路径可单独配置，默认前缀为 `/tiny-engine/ai`，不会占用宿主应用的既有 API 前缀。

```yaml
fw:
  plus:
    tiny-engine:
      ai:
        web:
          enabled: true
          base-path: /tiny-engine/ai
          chat-path: /chat
          chat-completions-path: /chat/completions
          encrypt-key-path: /encrypt-key
          search-path: /search
```

启用后提供 `POST` Chat、Chat Completions 和 Search 路由。Chat Completions 的 `stream=true` 返回现有服务生成的 chunk 列表；`stream=false` 时与 Chat 一样返回普通响应。Encrypt Key 路由当前明确返回 HTTP 501，因为 starter 未实现密钥解密，调用方不得将其当作成功结果。

## 旧坐标边界

当前 starter line 不使用旧的 `fw-plus-ai-langchain4j-model-spring-boot-starter` 或 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 坐标。它们只能作为历史版本迁移提示，不能作为当前主路径或新增业务依赖。`fw-plus-ai-model-spring-boot-starter` 与 `fw-plus-ai-store-embedding-spring-boot-starter` 由本 Starter 聚合；`fw-plus-ai-langchain4j-adapter-spring-boot-starter` 或未来其他 adapter starter 由业务应用按需显式声明。
