# fw-plus-ai

## 中立 AI adapter 破坏性迁移

本 Java 17 / Spring Boot 3.5.8 发布线删除旧 LangChain4j model/store 坐标，不保留 POM 别名或旧配置前缀。`fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 保持中立；LangChain4j 与 Spring adapter 均由业务显式安装，Boot 自动配置由 `fw-plus-ai-boot-autoconfigure` 按 classpath 条件加载。

| 已删除坐标 | 替代坐标 | 说明 |
| --- | --- | --- |
| `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model` | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | 统一的模型与向量实现层 |
| `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-store-embeding` | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | 注意旧 artifact 的 `embeding` 拼写 |
| `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter` | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | 统一 LangChain4j 核心适配 |
| `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | 统一 LangChain4j 核心适配 |

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

中立 starter `fw-plus-ai-model-spring-boot-starter` 与 `fw-plus-ai-store-embedding-spring-boot-starter` 仍可按需引入，但它们不会传递 LangChain4j 或任何 provider SDK。选择 OpenAI、Ollama、Qdrant 等 provider 时，应用仍需显式声明相应 SDK。

## 中立配置与自动发现

删除 `fw-plus.ai.langchain4j.model` 和 `fw-plus.ai.langchain4j.embedding-store`。使用 `fw-plus.ai.model`、`fw-plus.ai.embedding-store`；单一可支持 adapter 时可省略 `framework`，安装多个 adapter 后每一个定义都必须写明 framework。

```yaml
fw-plus:
  ai:
    model:
      enabled: true
      default-model: knowledge-chat
      models:
        knowledge-chat:
          framework: langchain4j
          provider: openai
          type: CHAT
          enabled: true
          properties:
            base-url: http://localhost:11434/v1
            api-key: ${AI_API_KEY}
            model-name: gpt-4o-mini
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
```

选择规则对模型和向量 store 一致：显式 `framework` 只匹配同名 adapter；省略时仅一个支持该 provider/type 的 adapter 才能自动选择；零个或多个候选都失败。诊断包含 key、framework、provider、type 与候选 framework，不得包含 apiKey、token、secret 或完整连接串。

## Magic-API 与 AskData

`model['knowledge-chat']`、`vector['knowledge']` 和默认 key 的语义保持稳定。资源定义也可给出 `framework`，并遵循同一选择规则；脚本只得到中立 facade，不能取得 `LangChain4jModel` 或 `EmbeddingStore<TextSegment>`。SDK 专有调用应留在 Java adapter/业务层。

## Spring AI 与 Spring AI Alibaba

同一个 Core artifact 支持两个 framework 的同步 `CHAT` 与 `EMBEDDING`：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-spring-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

应用还需显式引入官方 provider starter。通用 provider 使用 `framework: spring-ai`；DashScope 使用 `framework: spring-ai-alibaba`、`provider: dashscope`。通过 `properties.beanName` 精确选择 Spring 管理的 `ChatModel`/`EmbeddingModel`，或注册唯一 `SpringAiProviderBinding`。

首版不提供 Spring AI/Alibaba `AiEmbeddingStoreAdapter`：标准文本查询 `VectorStore` 不能安全替代中立预计算向量契约，因而 `vector[...]` 仍需显式安装兼容的 store adapter。

AskData 保持中立且不默认引入具体 adapter/provider；AgentScope 2 仍是后续需求。完整迁移、回滚限制、诊断与当前能力见 [迁移总览](../docs/ai-framework-migration/overview.md)。
