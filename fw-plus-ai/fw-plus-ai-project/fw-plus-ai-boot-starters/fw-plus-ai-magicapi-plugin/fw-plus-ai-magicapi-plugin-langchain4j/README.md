# fw-plus-ai-magicapi-plugin-langchain4j

Magic-API 的 LangChain4j 模型资源插件控制台前端，把七类模型资源（对话/流式对话/语言/流式语言/向量/分词器/向量存储）注册为 magic-api datasource 类型，供控制台新建、保存、启停、测试。

本模块目录只负责 starter 依赖与控制台前端资源打包；后端实现位于 `fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 包。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-langchain4j</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

依赖 `fw-plus-ai-boot-autoconfigure`，提供七类 datasource 的装配、资源存储、动态注册与连接测试接口。

## 注册的 datasource 类型

| 短类型 | datasource type | 说明 |
| --- | --- | --- |
| `chat-model` | `langchain4j-chat-model` | `ChatLanguageModel` |
| `streaming-chat-model` | `langchain4j-streaming-chat-model` | `StreamingChatLanguageModel` |
| `language-model` | `langchain4j-language-model` | `LanguageModel` |
| `streaming-language-model` | `langchain4j-streaming-language-model` | `StreamingLanguageModel` |
| `embedding-model` | `langchain4j-embedding-model` | `EmbeddingModel` |
| `tokenizer` | `langchain4j-tokenizer` | `Tokenizer` |
| `embedding-store` | `langchain4j-embedding-store` | `EmbeddingStore<TextSegment>`，须搭配 `embedding-model` |

## 表单字段来源

`options` 表单字段按「数据源类型 × provider」渲染，元数据表位于 `src/metadata/providers.js`：

- 表内容由后端各 provider 工厂实际读取的 options 键生成，键名与后端 `Langchain4jOptionsReader` 读取的键一一对应。
- 字段类型：`text` / `number` / `switch` / `select` / `duration` / `tags`。
- 敏感字段（`api-key`、`secret-key`、`password`、`token` 等）由渲染器统一按密码输入框处理。
- 枚举字段（如 `index-type`、`metric-type`、`consistency-level`）给出常用候选值且允许直接输入，SDK 新增取值不会被阻塞。

表与后端的一致性由 `test/langchain4j-console-metadata.test.cjs` 交叉校验（解析后端工厂源码），运行：

```bash
npm test
```

## 连接测试

控制台「测试」按钮调用 `POST /langchain4j/{短类型}/datasource/test`，后端按提交的 JSON 构建临时实例并做最小探测（chat/language/embedding 发一次请求，streaming 只验证构建，embedding-store 校验依赖的 embedding-model 后关闭临时实例），返回 `ok` 或中文错误信息。

## 构建

前端产物由 Maven 在 `prepare-package` 阶段生成并复制到 `target/classes/magic-editor/plugins/`：

```bash
npm install && npm run build     # 产物：dist/magic-langchain4j.1.0.0.iife.js
```

`dist/`、`node_modules/`、`package-lock.json` 不入库（见 `.gitignore`）。插件前端产物缺失时后端只按基础插件标识注册，服务端装配不受影响。
