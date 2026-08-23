# fw-plus-ai-model-spring-boot-starter

## 当前支持状态

本 starter 提供 Java 17 / Spring Boot 3.5.8 下的中立模型自动配置入口。业务代码应依赖 `fw-plus-ai-model` 的中立契约，不直接依赖 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java SDK。

业务显式选择 framework adapter；选择 LangChain4j 时同时引入：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-model-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

如果只引入本 starter，没有任何 framework adapter，应用不会获得真实模型实例；这是预期行为，避免中立 starter 隐式绑定到某个实现。

## 配置方式

模型统一使用 `fw-plus.ai.model` 前缀。`framework` 表示业务选择的实现框架；选择 LangChain4j 时填写 `langchain4j`，`provider` 表示厂商或后端。

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
            baseUrl: http://localhost:11434/v1
            apiKey: ${AI_API_KEY}
            modelName: gpt-4o-mini
    embedding-store:
      enabled: false
```

`fw-plus.ai.embedding-store` 是向量存储 starter 的配置前缀，模型 starter 不创建向量 store，但两个前缀都属于同一套中立命名。

## 自动感知环境

启动时会读取中立模型定义，并交给 classpath 中可用的 adapter 判断是否支持：

1. 配置了 `framework` 时，只匹配同名 adapter。
2. 未配置 `framework` 时，按 `provider + type` 查找候选 adapter。
3. 候选为 1 个时创建模型；候选为 0 个或多个时失败，并输出脱敏诊断。

从 LangChain4j 换成其他框架时，业务代码优先依赖中立 `AiModelRegistry` / `AiModelFactory`，不要依赖 LangChain4j 原生对象。

## Spring AI / Alibaba 与 LangChain4j 切换

当前已实现 LangChain4j 及共享 artifact `fw-plus-ai-spring-adapter`。切换步骤：

1. 保留本 starter。
2. 显式引入一个 adapter：`fw-plus-ai-langchain4j-adapter` 或 `fw-plus-ai-spring-adapter`。
3. Spring AI 应用另行引入官方 provider starter；DashScope 使用 `spring-ai-alibaba-starter-dashscope`。
4. 将 `fw-plus.ai.model.models.<key>.framework` 设置为 `langchain4j`、`spring-ai` 或 `spring-ai-alibaba`。
5. Spring 管理的模型通过 `properties.beanName` 或唯一 `SpringAiProviderBinding` 选择。
6. 保持业务 key 不变，让 `model[...]` 和 Java API 调用不改 key，并运行 adapter 启动、契约和脱敏回归。

Spring adapter 首版只支持同步 CHAT/EMBEDDING，不提供 VectorStore。AgentScope 2 Java 仍未实现；未来 adapter 也不得把目标 SDK 类型带入 `fw-plus-ai-model` 的公共签名。

## 回滚

如果迁移后无法启动，优先回滚到业务上一版 LangChain4j 组合：

1. 恢复 `fw-plus-ai-langchain4j-adapter`。
2. 将 `framework` 改回 `langchain4j`。
3. 确认 provider SDK 已显式加入业务应用。
4. 保持 `fw-plus.ai.model` 的业务 key 不变。

不要在新版本中恢复已删除的旧 model artifact 或旧 starter。
