# fw-plus-ai-langchain4j-adapter-spring-boot-starter

## 当前支持状态

本 starter 是当前默认 LangChain4j 实现，负责把 LangChain4j 的模型和向量 store 接入中立 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` SPI。它同时依赖：

- `fw-plus-ai-model-spring-boot-starter`
- `fw-plus-ai-store-embedding-spring-boot-starter`
- `fw-plus-ai-langchain4j-adapter`
- `fw-plus-ai-boot-autoconfigure`

原独立 LangChain4j adapter autoconfigure artifact 已删除，其自动配置已合并到 `fw-plus-ai-boot-autoconfigure`。业务 Spring Boot 2.7 / Java 8 应用如果要使用默认实现，通常只需要引入本 starter，再按需引入具体 LangChain4j provider SDK。

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

## 配置方式

模型配置使用 `fw-plus.ai.model`，向量存储配置使用 `fw-plus.ai.embedding-store`：

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

旧的模型/store 专用 artifact 和 starter 已删除，不要再使用旧坐标或旧前缀。新版本统一由中立配置驱动，LangChain4j 只是其中一个 adapter。

## 自动感知环境

当 classpath 中只有本 LangChain4j adapter 时，`framework` 可以省略；但为了后续切换到 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java，建议业务配置始终显式写：

```yaml
framework: langchain4j
```

如果后续同时安装多个 adapter，系统会按 `framework + provider + type` 精确选择。候选为 0 个或多个都会失败，不会按 Bean 顺序或 `@Primary` 隐式选择。

## Provider SDK

本 starter 不打包所有 LangChain4j provider SDK。业务需要哪个 provider，就显式加入对应依赖。例如 OpenAI 兼容 provider：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

缺少 provider SDK 时，对应 adapter Bean 不会注册，启动或创建实例时会给出脱敏错误。错误只能包含 key、framework、provider、type 和候选信息，不应包含 `api-key`、token、secret 或完整 properties。

## LangChain4j 切换到其他实现

当前发布物只交付默认 LangChain4j adapter。后续 Spring AI、Spring AI Alibaba、AgentScope 2 Java adapter 会在 Java 17 / Spring Boot 3.x 高版本线交付。切换时按以下方式处理：

1. 删除本 starter。
2. 保留 `fw-plus-ai-model-spring-boot-starter` 和 `fw-plus-ai-store-embedding-spring-boot-starter`，或由目标 adapter starter 传递它们。
3. 引入目标 framework 的 Java 17 / Spring Boot 3.x adapter starter。
4. 将 `framework: langchain4j` 改为目标 framework。
5. 保留业务 key，例如 `chat-default`、`knowledge`，保证 Java API、`model[...]`、`vector[...]` 的 key 不变。
6. 运行目标 SDK 版本探针、Boot 3 条件装配测试、真实 provider/model/store 回归和 Java 8 依赖隔离检查。

## Java 17 / Spring Boot 3.x 后续边界

Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 当前没有可解析 starter、自动配置或真实 adapter。本 starter 也不会在 Java 8 / Boot 2.7 路径中传递 Boot 3、`jakarta.*` 或目标 SDK。

AgentScope 2 Java 必须按进程内 SDK adapter 实现，不能用外部 HTTP、MCP 或 A2A 桥接替代。

## Magic-API 影响

`model[...]` 和 `vector[...]` 继续按业务 key 访问。稳定的是 key 和中立 facade，不是 LangChain4j 原生对象。脚本不能依赖 `LangChain4jModel` 或 `EmbeddingStore<TextSegment>` 的原生方法；需要原生能力时应在 Java adapter 或业务服务层封装。

## 回滚

回滚默认实现时，保留或恢复本 starter，并确认：

1. `fw-plus.ai.model` 和 `fw-plus.ai.embedding-store` 的 `framework` 为 `langchain4j`。
2. provider SDK 已显式加入业务应用。
3. 业务没有混入已删除的旧 model/store artifact 或旧 starter。

如果必须回到旧 artifact，只能回滚整个 fw-plus-ai 发布版本；不要在新版本中手工混装旧 jar。
