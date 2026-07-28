# fw-plus-ai-langchain4j-model-spring-boot-starter

统一的 LangChain4j 模型 Starter，用于在 Spring Boot 2.7 / Java 8 项目中按
`fw-plus.ai.langchain4j.model` 配置创建多个模型，并通过
`LangChain4jModelRegistry` 统一获取。

本模块解决的是“模型注册和选择”问题：同一个应用可以同时配置 OpenAI、Azure
OpenAI、Qwen、Ollama、Anthropic、Embedding、Image 等多类模型，
业务代码或 Magic-API 脚本只按 key 获取模型，不再把 provider 初始化逻辑散落到各处。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-model-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

Starter 只作为依赖入口，不包含业务代码。自动配置位于：

```text
fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model
```

## 默认依赖策略

本 starter 默认保持轻量，只直接依赖：

- `fw-plus-ai-boot-starter`
- `fw-plus-ai-langchain4j-model`

不会默认传递引入 OpenAI、Ollama、DashScope、Anthropic、Azure OpenAI、Google
AI、Voyage AI 等具体 provider SDK，避免业务应用只引 starter 就带上大量厂商依赖。

如果业务要使用具体 provider，请在应用侧额外引入对应 LangChain4j 实现依赖。例如使用
OpenAI：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>
```

例如使用 Ollama：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
</dependency>
```

依赖版本优先交给 `fw-plus-ai-boot-dependencies` 中的 LangChain4j BOM 管理，当前项目锁定
`0.35.0`。

## 最小可用示例

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
```

启动后可注入统一注册表：

```java
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;

public class ChatService {

    @Autowired
    private LangChain4jModelRegistry modelRegistry;

    public String chat(String message) {
        LangChain4jModel model = modelRegistry.getDefault();
        ChatLanguageModel chatModel = (ChatLanguageModel) model.getModel();
        return chatModel.generate(message);
    }
}
```

## 基础配置

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        enabled: true
        default-model: qwen-chat
        models:
          qwen-chat:
            provider: qwen
            model-type: chat
            api-key: ${DASHSCOPE_API_KEY}
            model-name: qwen-plus
            properties:
              temperature: 0.7
              maxTokens: 2048
          ollama-embedding:
            provider: ollama
            model-type: embedding
            base-url: http://127.0.0.1:11434
            model-name: nomic-embed-text
          openai-image:
            provider: openai
            model-type: image
            api-key: ${OPENAI_API_KEY}
            model-name: dall-e-3
```

说明：

- `enabled`：是否启用统一模型自动配置，默认启用。
- `default-model`：默认模型 key，可不填；调用 `getDefault()` 时必须存在。
- `models`：多模型配置，map key 会作为模型 key。
- `provider`：模型供应商，用于匹配 `LangChain4jModelProviderAdapter`。
- `model-type`：模型类型，对应 `LangChain4jModelType`。
- `base-url`：通用服务地址字段，会自动映射到支持 `baseUrl(...)` 的 builder。
- `api-key`：通用认证字段，会自动映射到支持 `apiKey(...)` 的 builder。
- `model-name`：通用模型名字段；可以是单个值，也可以是列表。单个值会自动映射到
  `modelName(...)`，并兼容 `deploymentName(...)`、`model(...)`、`modelId(...)`。
- `default-model-name`：当同一个模型配置下有多个 `model-name` 时，指定该模型 key 的默认
  模型名；不填时默认使用 `model-name` 列表第一个。
- `model-configs`：按 `model-name` 覆盖单个模型的配置；支持覆盖 `provider`、`model-type`、
  `base-url`、`api-key` 和 `properties`。共享配置先应用，`model-configs` 后应用。
- `properties`：传给具体 LangChain4j model builder 的属性；属性名与 builder 方法名保持一致。

反射 adapter 会先设置通用字段，再设置 `properties`。`properties` 中的字段如果找不到同名
builder 方法，会直接抛出异常，便于启动期暴露配置错误。

## 默认模型与多模型

`models` 下可以配置多个模型，每个 map key 就是模型 key。也可以在同一个模型 key 下把
`model-name` 配成多个值，用一套 provider/base-url/api-key 创建多个真实模型：

注意这里不是在 `chat-default` 下再嵌套一层 `models`；同组多个模型名统一写到
`chat-default.model-name` 列表里。

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: ${OPENAI_API_KEY:demo}
            properties:
              temperature: 0.2
              maxTokens: 1024
            model-name:
              - gpt-4o-mini
              - qwen-plus
              - llama3.1
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

上面配置会创建 3 个模型实例，并暴露 4 个可访问 key：

- `modelRegistry.getDefault()` 返回 `chat-default` 的默认模型名，默认是列表第一个 `gpt-4o-mini`。
- `modelRegistry.get("chat-default")` 同样返回 `chat-default.gpt-4o-mini`，可作为默认别名。
- `modelRegistry.get("chat-default.gpt-4o-mini")` 返回 `gpt-4o-mini` 模型。
- `modelRegistry.get("chat-default.qwen-plus")` 返回 `qwen-plus` 模型。
- `modelRegistry.get("chat-default.llama3.1")` 返回 `llama3.1` 模型。

`chat-default.properties` 是该组所有模型的共享默认值；`model-configs.<modelName>.properties`
是单个模型名自己的配置，会覆盖共享 `properties` 中的同名字段。模型名中包含 `.` 时，
建议像 `"llama3.1"` 这样加引号，避免配置解析产生歧义。

`chat-default` 不是固定字段名，只是一个模型 key。顶层 `default-model` 可以指向模型 key
`chat-default`，也可以直接指向完整模型名 key，例如 `chat-default.qwen-plus`。

如果不配置顶层 `default-model`，仍然可以通过 `get("<key>")` 获取指定模型，但调用
`getDefault()` 会抛出异常。

如果希望 `chat-default` 默认指向第二个模型名，可以配置 `default-model-name`：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: ${OPENAI_API_KEY:demo}
            default-model-name: qwen-plus
            model-name:
              - gpt-4o-mini
              - qwen-plus
```

这种写法中，`modelRegistry.get("chat-default")` 和 `modelRegistry.getDefault()` 都会返回
`chat-default.qwen-plus`。

## 获取模型

```java
@Autowired
private LangChain4jModelRegistry modelRegistry;

public LangChain4jModel defaultModel() {
    return modelRegistry.getDefault();
}

public LangChain4jModel embeddingModel() {
    return modelRegistry.get("ollama-embedding");
}
```

`LangChain4jModel` 是一个轻量包装对象：

- `getProvider()`：返回 provider。
- `getModelType()`：返回 `LangChain4jModelType`。
- `getModel()`：返回真实 LangChain4j 模型实例，业务侧按 `model-type` 转成对应接口。

当前项目使用 LangChain4j `0.35.0`，常见转换示例：

```java
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.moderation.ModerationModel;
```

在 `0.35.0` 下不要 import 或强转 `dev.langchain4j.model.chat.ChatModel`；这个接口属于
LangChain4j 高版本主 API，不适用于当前项目锁定版本。

## 支持的 model-type

| model-type | LangChain4j 0.35 常见接口/对象 | 说明 |
| --- | --- | --- |
| `chat` | `ChatLanguageModel` | 对话模型，当前项目 0.35 使用该旧命名 |
| `streaming_chat` | `StreamingChatLanguageModel` | 流式对话模型，当前项目 0.35 使用该旧命名 |
| `embedding` | `EmbeddingModel` | 文本向量模型 |
| `image` | `ImageModel` | 图片生成模型 |
| `language` | `LanguageModel` | 旧式纯文本语言模型 |
| `streaming_language` | `StreamingLanguageModel` | 旧式流式纯文本语言模型 |
| `moderation` | `ModerationModel` | 内容审核模型，当前默认未内置 provider adapter |
| `scoring` | provider 专有对象 | 评分/重排类模型，如 Voyage AI scoring |
| `audio` | 框架扩展类型 | 预留给自定义音频模型 adapter |
| `video` | 框架扩展类型 | 预留给自定义视频模型 adapter |
| `pdf` | 框架扩展类型 | 预留给自定义 PDF 模型 adapter |

注意：`AUDIO`、`VIDEO`、`PDF` 是 fw-plus 扩展的 `model-type`，不是 LangChain4j
`0.35.0` 中的独立 `AudioModel`、`VideoModel`、`PdfModel` 接口。LangChain4j
高版本中的 `AudioContent`、`VideoContent`、`PdfFileContent` 属于 Chat 多模态输入内容，
应通过支持多模态的 chat model 使用，而不是作为独立模型类型理解。

## 支持的 provider

当前自动配置会注册以下 provider adapter。adapter 使用反射创建模型：只有应用侧存在对应
LangChain4j 类时才会匹配成功。

| Provider | model-type | LangChain4j 0.35 类 |
| --- | --- | --- |
| `openai` | `chat` | `dev.langchain4j.model.openai.OpenAiChatModel` |
| `openai` | `streaming_chat` | `dev.langchain4j.model.openai.OpenAiStreamingChatModel` |
| `openai` | `embedding` | `dev.langchain4j.model.openai.OpenAiEmbeddingModel` |
| `openai` | `image` | `dev.langchain4j.model.openai.OpenAiImageModel` |
| `openai` | `language` | `dev.langchain4j.model.openai.OpenAiLanguageModel` |
| `openai` | `streaming_language` | `dev.langchain4j.model.openai.OpenAiStreamingLanguageModel` |
| `azure-openai` | `chat` | `dev.langchain4j.model.azure.AzureOpenAiChatModel` |
| `azure-openai` | `streaming_chat` | `dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel` |
| `azure-openai` | `embedding` | `dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel` |
| `azure-openai` | `image` | `dev.langchain4j.model.azure.AzureOpenAiImageModel` |
| `azure-openai` | `language` | `dev.langchain4j.model.azure.AzureOpenAiLanguageModel` |
| `azure-openai` | `streaming_language` | `dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel` |
| `qwen` | `chat` | `dev.langchain4j.model.dashscope.QwenChatModel` |
| `qwen` | `streaming_chat` | `dev.langchain4j.model.dashscope.QwenStreamingChatModel` |
| `qwen` | `embedding` | `dev.langchain4j.model.dashscope.QwenEmbeddingModel` |
| `qwen` | `image` | `dev.langchain4j.model.dashscope.WanxImageModel` |
| `qwen` | `language` | `dev.langchain4j.model.dashscope.QwenLanguageModel` |
| `qwen` | `streaming_language` | `dev.langchain4j.model.dashscope.QwenStreamingLanguageModel` |
| `ollama` | `chat` | `dev.langchain4j.model.ollama.OllamaChatModel` |
| `ollama` | `embedding` | `dev.langchain4j.model.ollama.OllamaEmbeddingModel` |
| `ollama` | `language` | `dev.langchain4j.model.ollama.OllamaLanguageModel` |
| `anthropic` | `chat` | `dev.langchain4j.model.anthropic.AnthropicChatModel` |
| `anthropic` | `streaming_chat` | `dev.langchain4j.model.anthropic.AnthropicStreamingChatModel` |
| `qianfan` | `chat` | `dev.langchain4j.model.qianfan.QianfanChatModel` |
| `qianfan` | `streaming_chat` | `dev.langchain4j.model.qianfan.QianfanStreamingChatModel` |
| `qianfan` | `embedding` | `dev.langchain4j.model.qianfan.QianfanEmbeddingModel` |
| `qianfan` | `language` | `dev.langchain4j.model.qianfan.QianfanLanguageModel` |
| `zhipu` | `chat` | `dev.langchain4j.model.zhipu.ZhipuAiChatModel` |
| `zhipu` | `streaming_chat` | `dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel` |
| `zhipu` | `embedding` | `dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel` |
| `zhipu` | `image` | `dev.langchain4j.model.zhipu.ZhipuAiImageModel` |
| `hugging-face` | `chat` | `dev.langchain4j.model.huggingface.HuggingFaceChatModel` |
| `hugging-face` | `embedding` | `dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel` |
| `hugging-face` | `language` | `dev.langchain4j.model.huggingface.HuggingFaceLanguageModel` |
| `local-ai` | `chat` | `dev.langchain4j.model.localai.LocalAiChatModel` |
| `local-ai` | `embedding` | `dev.langchain4j.model.localai.LocalAiEmbeddingModel` |
| `local-ai` | `language` | `dev.langchain4j.model.localai.LocalAiLanguageModel` |
| `gemini` | `chat` | `dev.langchain4j.model.googleai.GoogleAiGeminiChatModel` |
| `gemini` | `embedding` | `dev.langchain4j.model.googleai.GoogleAiEmbeddingModel` |
| `voyage-ai` | `embedding` | `dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel` |
| `voyage-ai` | `scoring` | `dev.langchain4j.model.voyageai.VoyageAiScoringModel` |
| `all-mini-lm-l6-v2-quantized` | `embedding` | `dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel` |

`moderation`、`audio`、`video`、`pdf` 目前没有内置 provider adapter。需要这些类型时，
在业务应用侧注册自定义 `LangChain4jModelProviderAdapter` Bean。

## 反射 adapter 属性规则

默认 adapter 会按以下顺序创建模型：

1. 查找 LangChain4j model class 的静态 `builder()` 方法；`all-mini-lm-l6-v2-quantized`
   这类直接模型会使用无参构造。
2. 自动设置通用字段：`baseUrl`、`endpoint`、`apiKey`、`accessToken`、`secretKey`、
   `modelName`、`deploymentName`、`model`、`modelId`。
3. 设置 `properties` 中的每个 key，要求 builder 存在同名单参方法。
4. 调用 builder 的 `build()` 方法。

支持的简单类型转换：

- `String`
- `Integer` / `int`
- `Long` / `long`
- `Double` / `double`
- `Float` / `float`
- `Boolean` / `boolean`
- `Duration`

`Duration` 字符串需要使用 `Duration.parse(...)` 支持的 ISO-8601 格式，例如 `PT30S`；
也可以直接传数字，按毫秒处理。

## 扩展自定义 provider

当 LangChain4j `0.35.0` 没有某类模型，或某个 provider 需要外部 SDK client、复杂认证对象、
非标准 builder 字段时，可以注册自定义 `LangChain4jModelProviderAdapter` Bean。

```java
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.springframework.context.annotation.Bean;

@Bean
public LangChain4jModelProviderAdapter customAudioModelProviderAdapter() {
    return new LangChain4jModelProviderAdapter() {
        @Override
        public boolean supports(LangChain4jModelDefinition definition) {
            return definition != null
                    && "custom-audio".equalsIgnoreCase(definition.getProvider())
                    && LangChain4jModelType.AUDIO == definition.getModelType();
        }

        @Override
        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            Object audioModel = createAudioModel(definition);
            return new LangChain4jModel(definition.getProvider(), definition.getModelType(), audioModel);
        }
    };
}
```

配置中即可使用：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        models:
          audio-asr:
            provider: custom-audio
            model-type: audio
            properties:
              endpoint: http://127.0.0.1:8080
              modelName: asr-large
```

## Magic-API 插件

模型插件通过 `fw-plus-ai-magicapi-plugin-model` 暴露模型能力，并复用本
starter/自动配置创建的 `LangChain4jModelRegistry`。

如业务侧需要 Magic-API 模型插件，同时引入：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-model</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

脚本示例：

```js
import model;

// 使用 Spring Boot 配置的默认模型
var defaultModel = model.getModel();

// 使用指定 key 的模型；优先运行时 model-datasource，找不到再回退 Spring Boot 配置
var chatModel = model['qwen-chat'].getModel();
```

Spring Boot 默认模型配置示例：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: qwen-chat
        models:
          qwen-chat:
            provider: qwen
            model-type: chat
            api-key: ${DASHSCOPE_API_KEY}
            model-name: qwen-plus
```

Magic-API 控制台中新增的 `model-datasource` 会保存为
`model-datasource/*.json`，用于运行时追加或覆盖指定 key 的模型；它不会修改
`application.yml` 中的 Spring Boot 配置。

`model-datasource/qwen-chat.json` 示例：

```json
{
  "name": "通义千问",
  "key": "qwen-chat",
  "provider": "qwen",
  "modelType": "CHAT",
  "enabled": true,
  "apiKey": "${DASHSCOPE_API_KEY}",
  "modelName": "qwen-plus",
  "properties": {
    "temperature": 0.7
  }
}
```

数据源接口：

```text
GET  /model/datasource/list
POST /model/datasource/test
```

测试请求体示例：

```json
{
  "key": "qwen-chat",
  "provider": "qwen",
  "modelType": "CHAT",
  "apiKey": "${DASHSCOPE_API_KEY}",
  "modelName": "qwen-plus"
}
```

`apiKey`、`secretKey`、`accessToken`、`token`、`password` 是敏感字段。
列表接口和运行态节点视图会脱敏这些字段；示例配置中不要提交真实密钥。

旧的 Magic 入口已经迁移为：

- 脚本模块：`model`
- 运行态数据源：`model-datasource`

不要再使用旧的模型运行入口。

## 版本边界

- Java 8
- Spring Boot 2.7.x
- LangChain4j 0.35.0
- 自动配置使用 `META-INF/spring.factories`
- 当前 starter 面向 Spring Boot 2.7 / Java 8，不提供 Spring Boot 3 的
  `AutoConfiguration.imports`

需要特别注意 LangChain4j 模型命名：

- 当前项目锁定 `0.35.0`，核心对话接口是 `ChatLanguageModel` /
  `StreamingChatLanguageModel`。
- LangChain4j 高版本 `1.x` 已改用 `ChatModel` / `StreamingChatModel`；
  `ChatLanguageModel` / `StreamingChatLanguageModel` 不再作为当前高版本主接口使用。
- 升级 LangChain4j 时，需要同步检查自动配置中的 provider class、业务代码里的类型转换、
  AskData 集成和 Magic-API 插件脚本示例。

多模态也要按版本区分：

- LangChain4j `0.35.0` 没有独立的 `AudioModel`、`VideoModel`、`PdfModel` 通用接口。
- 高版本提供 `AudioContent`、`VideoContent`、`PdfFileContent` 等 Chat 多模态内容类型，
  使用方式是把这些内容放进 `UserMessage` 后交给支持多模态的 `ChatModel`。
- 本 starter 的 `AUDIO`、`VIDEO`、`PDF` 是 fw-plus 统一注册表的扩展类型，适合业务侧
  通过自定义 adapter 接入专有 SDK；默认不会注册内置实现。

## 常见问题

**启动报 `No adapter supports key=..., provider=..., modelType=...`**

检查 `provider`、`model-type` 是否在支持列表中，且应用侧是否引入了对应 LangChain4j
provider jar。adapter 会在目标 class 存在时才匹配成功。

**启动报 `Builder property is not supported: xxx`**

`properties.xxx` 找不到同名 builder 方法。请核对当前 LangChain4j `0.35.0` 对应模型的
builder API，或改用自定义 `LangChain4jModelProviderAdapter`。

**`getDefault()` 报默认模型不存在**

`default-model` 必须等于一个启用模型 key。多个 `model-name` 时，`chat-default` 会作为默认
模型名别名；也可以直接配置成 `chat-default.qwen-plus` 这类完整模型名 key。模型设置
`enabled: false` 后不能作为默认模型。

**能不能直接配置 `audio`、`video`、`pdf`？**

可以配置这些 `model-type`，但默认没有内置 provider adapter。它们是扩展点，不代表
LangChain4j `0.35.0` 已存在独立通用模型接口。
