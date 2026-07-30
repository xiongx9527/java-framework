# fw-plus-ai-magicapi-plugin-model

Magic-API 的 LangChain4j 模型插件，用于把 Spring Boot 中的 `LangChain4jModelRegistry` 和 Magic 动态模型资源统一暴露给 MagicScript。业务脚本可以通过 `model` 模块获取默认模型或指定 key 的模型对象。

本模块目录主要负责 starter 依赖和控制台前端资源打包，后端自动配置实现位于 `fw-plus-ai-boot-autoconfigure`。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-model</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

模块依赖：

- `fw-plus-ai-boot-autoconfigure`：提供 Magic-API 插件后端配置、控制器、资源存储和动态注册。
- `fw-plus-ai-langchain4j-adapter-spring-boot-starter`：提供统一 LangChain4j adapter；模型注册通过中立 `AiModelRegistry` 暴露。

具体 provider SDK 不由本插件一次性全部引入。使用 OpenAI、Ollama、DashScope、Anthropic、Azure OpenAI 等 provider 时，请在业务应用侧按需引入对应 LangChain4j 实现依赖。

## 启用条件

后端配置类为 `MagicAPIModelConfiguration`，满足以下条件时生效：

- 当前应用是 Magic-API 所在的 Servlet Web 应用，并启用了 AI Magic-API 基础自动配置。
- classpath 中存在 `LangChain4jModelRegistry`。
- Spring 容器中已经有 `LangChain4jModelRegistry` Bean。
- `magic-api.plugin.model.enabled=true` 或未配置该开关。

关闭插件：

```yaml
magic-api:
  plugin:
    model:
      enabled: false
```

插件启动后会注册：

- Magic 模块：`model`
- 动态资源类型：`model-datasource`
- 控制台插件：`model` / `MagicModel` / `magic-model.1.0.0.iife.js`
- 控制器接口：`/model/datasource/list`、`/model/datasource/test`

## Spring Boot 模型配置

如果模型由 Spring Boot 配置创建，使用 `fw-plus.ai.model` 前缀，并将 `framework` 设为 `langchain4j`：

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
            api-key: ${OPENAI_API_KEY:demo}
            model-name: gpt-4o-mini
            properties:
              temperature: 0.7
              maxTokens: 2048
```

`default-model` 指向 registry key。Magic 脚本中不指定 key 时，会使用 registry 的默认模型。

### 默认模型与多模型名

同一个配置 key 下可以配置多个 `model-name`。注意不是在 `chat-default` 下面再嵌套一层 `models`，而是把多个模型名写成列表：

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
            api-key: ${OPENAI_API_KEY:demo}
            default-model-name: gpt-4o-mini
            model-name:
              - gpt-4o-mini
              - qwen-plus
              - llama3.1
            properties:
              temperature: 0.3
              maxTokens: 1024
            model-configs:
              gpt-4o-mini:
                properties:
                  temperature: 0.2
                  maxTokens: 1024
              qwen-plus:
                properties:
                  temperature: 0.7
                  maxTokens: 2048
              "llama3.1":
                properties:
                  temperature: 0.4
                  maxTokens: 4096
```

上面会注册这些 key：

- `chat-default`：别名，指向 `default-model-name` 指定的模型；未配置时指向 `model-name` 列表第一个。
- `chat-default.gpt-4o-mini`
- `chat-default.qwen-plus`
- `chat-default.llama3.1`

父级 `properties` 是该组模型共享配置；`model-configs.<modelName>.properties` 是单个模型名自己的配置，同名字段会覆盖共享配置。模型名包含点号时，建议加引号，避免 YAML 解析歧义。

## Magic 动态资源

Magic 控制台中新增的数据源类型是 `model-datasource`，运行态资源通常位于 `model-datasource/*.json`。示例：

```json
{
  "name": "本地 OpenAI 兼容 Chat",
  "key": "local-chat",
  "provider": "openai",
  "modelType": "CHAT",
  "enabled": true,
  "baseUrl": "http://localhost:11434/v1",
  "apiKey": "demo",
  "defaultModelName": "gpt-4o-mini",
  "modelName": [
    "gpt-4o-mini",
    "qwen-plus"
  ],
  "modelConfigs": {
    "gpt-4o-mini": {
      "properties": {
        "temperature": 0.2,
        "maxTokens": 1024
      }
    },
    "qwen-plus": {
      "properties": {
        "temperature": 0.7,
        "maxTokens": 2048
      }
    }
  },
  "properties": {
    "timeout": "PT30S"
  }
}
```

字段说明：

- `name`：控制台显示名称。
- `key`：数据源 key，必填且唯一，已有资源不允许修改 key。
- `provider`：provider 名称，必填。
- `modelType`：模型类型，启用或测试时必填。
- `enabled`：是否注册运行态模型，默认启用。
- `baseUrl`、`apiKey`：通用模型连接字段。
- `modelName`：模型名列表，必须是 JSON 字符串数组；单模型也写成 `["gpt-4o-mini"]`。
- `defaultModelName`：多个模型名时 `key` 默认别名指向的模型名；不填时使用 `modelName` 列表第一个。
- `modelConfigs`：单个模型名自己的配置，支持覆盖 `provider`、`modelType`、`baseUrl`、`apiKey` 和 `properties`。
- `secretKey`、`accessToken`、`token`、`password`：兼容不同厂商的认证字段。
- `properties`：透传给 LangChain4j builder 或自定义 provider adapter 的扩展参数。

注意这里说的是 Magic 动态资源 JSON。插件动态资源不兼容旧的单字符串 `modelName` 写法；旧配置必须迁移为 JSON 数组，单模型也要写成 `["gpt-4o-mini"]`。

列表输出会脱敏或移除 `apiKey`、`secretKey`、`accessToken`、`token`、`password`，`properties` 内同名敏感字段也会过滤。创建模型时这些凭证会合并到 `LangChain4jModelDefinition.properties`，供 provider adapter 使用。

### 配置中心覆盖

可通过 `magic.api.model.<key>.*` 覆盖运行态资源中的 `provider`、`modelType`、`baseUrl` 以及 `apiKey`、`secretKey`、`accessToken`、`token`、`password`。例如：

```yaml
magic:
  api:
    model:
      local-chat:
        baseUrl: http://localhost:11434/v1
        apiKey: ${OPENAI_API_KEY:demo}
        properties: '{"temperature":0.3,"maxTokens":1024}'
```

配置中心存在 `properties` JSON 时，其内容会完整替代资源文件中的 `properties`。其他字段仍按各自配置键覆盖资源值。

## MagicScript 用法

```javascript
import model;

// 默认模型的中立 facade，来自 Spring Boot registry 的 default-model
var chatModel = model.getModel();

// 与 getModel() 相同，返回中立 AiModel facade
var wrapped = model.getModelObject();

// 指定 Spring Boot registry 或 model-datasource 中的 key
var qwen = model['chat-default.qwen-plus'].getModel();
var dynamicDefault = model['local-chat'].getModel();
var dynamicQwen = model['local-chat.qwen-plus'].getModel();
```

`getModel()` 与 `getModelObject()` 返回中立 `AiModel` facade，只提供稳定的 `key`、`framework`、`provider`、`type`
元数据；不再返回真实 LangChain4j 模型对象，也不提供 chat、embedding、image 等 SDK 专有快捷方法。

这是一次破坏性变更：旧脚本中对 `model.getModel()` 或 `model['key'].getModel()` 返回值强转并调用
LangChain4j API 的写法必须移除。若业务必须调用 LangChain4j 专有能力，应在 Java 应用层注入
`LangChain4jModelRegistry` 或相应 adapter；不要把原生 SDK 对象经 Magic-API facade 传递。未来切换到
Spring AI、Spring AI Alibaba 或 AgentScope 2 时，`model['key']` 保持不变，facade 元数据中的 `framework`
才表示实际实现。需要立即回滚脚本兼容性时，回退到本抽象改造前的插件版本和旧 LangChain4j 契约，不能在
当前 facade 上恢复原生对象暴露。

## 控制器接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/model/datasource/list` | GET/POST | 返回运行态模型数据源脱敏列表。 |
| `/model/datasource/test` | POST | 接收 `ModelDataSourceInfo` JSON，调用 `LangChain4jModelFactory.create(...)` 验证能否创建。 |

测试接口成功返回 Magic-API 标准响应中的 `ok`；失败返回 code `-1` 和“模型数据源测试失败”。

## 支持的 modelType

当前枚举包含：

- `CHAT`
- `STREAMING_CHAT`
- `EMBEDDING`
- `IMAGE`
- `LANGUAGE`
- `STREAMING_LANGUAGE`
- `MODERATION`
- `SCORING`
- `AUDIO`
- `VIDEO`
- `PDF`

项目当前以 LangChain4j `0.35.0` 编译，Chat 类型对应 `ChatLanguageModel`，Streaming Chat 对应 `StreamingChatLanguageModel`。高版本 LangChain4j 中常见的 `ChatModel` / `StreamingChatModel` 类名不要直接写进 0.35.0 编译代码。

`AUDIO`、`VIDEO`、`PDF` 是 fw-plus 为 Magic 资源和业务适配器预留的扩展 modelType。LangChain4j 0.35.0 以及当前 1.x 并没有统一的独立 `AudioModel` / `VideoModel` / `PdfModel` 接口；音频、视频、PDF 通常作为多模态 Chat 输入内容或厂商 SDK 能力接入。需要使用这些类型时，请注册自定义 `LangChain4jModelProviderAdapter` Bean。

## 前端资源

前端源码位于 `src/console`：

- 包名：`magic-model`
- 构建：`npm run build`，底层为 `vite build`
- 测试：`npm test`，底层为 `node --test test/*.test.cjs`
- 输出：IIFE library，global name `MagicModel`，文件 `magic-model.1.0.0.iife.js`

Maven 在 `prepare-package` 阶段执行 `npm install` 和 `npm run build`，并把 `src/console/dist` 复制到 `target/classes/magic-editor/plugins`。

## 注意事项

- 资源类型是 `model-datasource`，不要继续使用旧的 `langchain4j-datasource` 命名。
- 控制台插件全局变量是 `MagicModel`，不是旧的 `MagicLangchain4j`。
- Magic 动态资源与 Spring Boot registry 共存；按 key 获取时会先查动态资源，再回退到 Spring Boot registry。
- 默认模型来自 Spring Boot registry 的 `default-model`，动态资源不会单独覆盖 registry 的默认 key。
