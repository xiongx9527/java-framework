# fw-plus-ai

## LangChain4j 模型与 Magic-API

`fw-plus-ai-langchain4j-model-spring-boot-starter` 根据
`fw-plus.ai.langchain4j.model` 配置创建可复用的 LangChain4j 模型 Bean；
`fw-plus-ai-magicapi-plugin-model` 将这些模型暴露给 Magic-API 的 `model`
脚本模块，并支持在 `model-datasource` 资源中覆盖或新增模型配置。

### 最小依赖与配置

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

模型 Starter 不会传递引入 OpenAI、Ollama、DashScope、Anthropic 或 Azure OpenAI
等 Provider SDK。应用选择某个 Provider 时，必须显式声明对应的 LangChain4j
Provider 依赖。

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

### Magic-API 资源与脚本

在 Magic-API 的 `model-datasource` 资源目录创建模型数据源，例如
`model-datasource/chat-default.json`：

```json
{
  "name": "本地 OpenAI 兼容模型",
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

脚本通过 `model` 模块取得指定模型：

```javascript
import model;
return model['chat-default'].getModel();
```

旧版 Magic `langchain4j` 插件及其 `langchain4j-datasource` 资源类型已移除。
迁移时将资源目录改为 `model-datasource`，脚本导入改为 `model`，并将依赖替换为
`fw-plus-ai-magicapi-plugin-model`；不要继续使用 `MagicLangchain4j` 或
`@MagicModule("langchain4j")`。

### LangChain4j 版本兼容性

本模块锁定 LangChain4j `0.35.0`：`CHAT` 对应 `ChatLanguageModel`，
`STREAMING_CHAT` 对应 `StreamingChatLanguageModel`。LangChain4j 高版本改用
`ChatModel` / `StreamingChatModel` 命名，当前版本不能引用这些类。

LangChain4j `0.35.0` 已提供 `TextContent`、`ImageContent`、`AudioContent`、
`VideoContent`、`PdfFileContent` 等 Chat 多模态内容类型；高版本仍通过 Chat
请求承载多模态输入。音频、视频和 PDF 不是独立模型类型，不存在独立的
`AudioModel` / `VideoModel`，因此不要在 `modelType` 中新增 `AUDIO`、`VIDEO`
或 `PDF`。
