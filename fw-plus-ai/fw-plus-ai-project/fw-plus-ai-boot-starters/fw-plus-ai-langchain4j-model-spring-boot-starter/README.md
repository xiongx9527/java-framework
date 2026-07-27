# fw-plus-ai-langchain4j-model-spring-boot-starter

LangChain4j 模型配置 Starter，用于按 `fw-plus.ai.langchain4j.model` 配置创建模型 Bean。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-model-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

## 最小配置

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        enabled: true
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: demo
            model-name: gpt-4o-mini
            properties: {}
```

本 Starter 不会默认引入 OpenAI、Ollama、DashScope、Anthropic、Azure OpenAI 等 provider SDK。业务应用需要使用具体 provider 时，应显式引入对应的 LangChain4j provider 依赖。
