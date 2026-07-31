# fw-plus-ai-askdata-spring-boot-starter

## 当前支持状态

本 starter 提供 AskData 的 Spring Boot 2.7.x / Java 8 自动配置。业务配置使用 `fw.plus.ai.askdata`，模型和向量能力统一解析中立注册表：聊天/embedding 模型 key 解析到 `AiModelRegistry`（来自 `fw-plus-ai-model`），向量 store key 解析到 `AiEmbeddingStoreRegistry`（来自 `fw-plus-ai-store-embedding`）。AskData 业务代码不应把 LangChain4j 原生模型或 store 当作主接口。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-askdata-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

当前 starter 不默认带入任何具体 AI 框架 Adapter。AskData 的 key、业务流程和覆盖点保持中立；业务应用按所选 AI 框架显式组合对应的 Adapter Starter 与 provider SDK。

## 中立模型与向量配置

`model.default-chat-model`、`model.default-embedding-model` 是默认注册表 key；候选列表会优先选择已在 `AiModelRegistry` 中注册且类型匹配的 key。`vector.store` 解析到 `AiEmbeddingStoreRegistry` 的 store key。

默认模型桥接只接受同时实现 `AiModel` 与 `AskDataChatModel` / `AskDataEmbeddingModel` 的中立实现，不反射或转换具体 SDK 类型。LangChain4j 模型的转换由应用显式引入 Adapter Starter 后启用的可选适配配置负责。

```yaml
fw:
  plus:
    ai:
      askdata:
        enabled: true
        model:
          default-chat-model: chat-default
          default-embedding-model: embedding-default
          chat-candidates:
            - chat-default
          embedding-candidates:
            - embedding-default
          fallback-enabled: true
        vector:
          store: knowledge
          default-top-k: 5
        sql:
          execute-enabled: true
          read-only-connection-required: true
          default-max-rows: 100
```

需要 LangChain4j 时，由业务应用显式引入 Adapter Starter：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

具体 LangChain4j provider SDK 也由业务按需显式加入。例如 OpenAI 兼容模型：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

然后通过 `fw.plus.ai.model` 和 `fw.plus.ai.embedding-store` 定义同名中立 key。AskData 只消费注册表，不要求业务 Bean 暴露 LangChain4j 原生类型。

## 业务 Bean 覆盖点

自动配置使用 `@ConditionalOnMissingBean`，业务可以按中立契约覆盖默认行为：

- 提供自定义 `AskDataModelRegistry`，覆盖模型 key 解析、聊天模型和 embedding 模型适配。
- 提供自定义 `AskDataVectorRetriever`，覆盖向量查询、召回和重排策略；默认实现通过 `AiEmbeddingStoreRegistry` 查找 `vector.store`。
- 提供自定义 `AskDataChatModel`、`AskDataEmbeddingModel` 或其他 AskData SPI Bean，覆盖具体业务模型能力。
- 直接提供 `AiModelRegistry` / `AiEmbeddingStoreRegistry`，让 AskData 的默认桥接从中立注册表读取能力。

示例：

```java
@Bean
public AskDataModelRegistry askDataModelRegistry(AiModelRegistry models) {
    return new MyNeutralAskDataModelRegistry(models, "chat-default", "embedding-default");
}

@Bean
public AskDataVectorRetriever askDataVectorRetriever(AiEmbeddingStoreRegistry stores,
        AskDataModelRegistry models) {
    return new MyNeutralVectorRetriever(stores, models, "knowledge");
}
```

覆盖 `AskDataModelRegistry` 或 `AskDataVectorRetriever` 后，应用负责保证 key、模型类型、向量维度和后端 schema 一致。缺少中立注册表或对应 key 时，默认向量检索不会伪造可用结果，应在部署检查中确认 adapter、provider SDK 和后端连接均已就绪。

## 旧坐标边界

当前 starter line 不使用旧的 `fw-plus-ai-langchain4j-model-spring-boot-starter` 或 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 坐标。它们不是当前主路径，也不应在新应用中与中立 starter 混用；请使用本 starter、`fw-plus-ai-model` / `fw-plus-ai-store-embedding` 中立注册表，并在应用侧按需组合 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
