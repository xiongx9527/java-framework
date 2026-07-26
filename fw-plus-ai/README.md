# fw-plus-ai

## LangChain4j Models and Magic-API

`fw-plus-ai-langchain4j-model-spring-boot-starter` creates reusable LangChain4j
model beans from `fw-plus.ai.langchain4j.model`. The
`fw-plus-ai-magicapi-plugin-model` artifact exposes those models through the
Magic-API `model` script module and supports runtime model resources in
`model-datasource`.

### Minimal dependencies and configuration

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-model-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-model</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

The model starter does not pull in provider SDKs for OpenAI, Ollama, DashScope,
Anthropic, or Azure OpenAI. Applications must declare the LangChain4j SDK for
the selected provider explicitly.

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
            enabled: true
            properties: {}

magic-api:
  plugin:
    model:
      enabled: true
```

### Magic-API resources and scripts

Create a model resource in the Magic-API `model-datasource` directory, for
example `model-datasource/chat-default.json`:

```json
{
  "name": "Local OpenAI-compatible model",
  "key": "chat-default",
  "provider": "openai",
  "modelType": "CHAT",
  "enabled": true,
  "baseUrl": "http://localhost:11434/v1",
  "apiKey": "demo",
  "modelName": "gpt-4o-mini",
  "properties": {
    "temperature": 0.2
  }
}
```

Use the `model` script module to retrieve a configured model:

```javascript
import model;
return model['chat-default'].getModel();
```

The former Magic `langchain4j` plugin and `langchain4j-datasource` resource type
were removed. Migrate resources to `model-datasource`, import `model` in scripts,
and replace the dependency with `fw-plus-ai-magicapi-plugin-model`. Do not retain
`MagicLangchain4j` or `@MagicModule("langchain4j")` at runtime.

### LangChain4j compatibility

This module is compiled against LangChain4j `0.35.0`: `CHAT` maps to
`ChatLanguageModel` and `STREAMING_CHAT` maps to `StreamingChatLanguageModel`.
Later LangChain4j versions use the names `ChatModel` and `StreamingChatModel`;
those types are not available in the current version.

LangChain4j `0.35.0` already provides `TextContent`, `ImageContent`,
`AudioContent`, `VideoContent`, and `PdfFileContent` for multimodal Chat input.
Later versions continue to carry multimodal input in Chat requests. Audio, video,
and PDF are not standalone model types: there is no `AudioModel` or `VideoModel`,
and `AUDIO`, `VIDEO`, and `PDF` must not be added to `modelType`.
