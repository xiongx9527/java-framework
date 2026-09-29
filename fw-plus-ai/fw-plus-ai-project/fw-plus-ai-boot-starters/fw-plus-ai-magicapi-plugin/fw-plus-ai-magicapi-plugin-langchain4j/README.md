# fw-plus-ai-magicapi-plugin-langchain4j

Magic-API 的 LangChain4j 模型资源插件集合（Maven 聚合工程）。七类模型资源各为一个独立 starter 子工程，各自带控制台前端产物与类型装配条件：

| 子工程 | datasource type | 说明 |
| --- | --- | --- |
| `fw-plus-ai-magicapi-plugin-langchain4j-chat-model` | `langchain4j-chat-model` | `ChatLanguageModel` |
| `…-streaming-chat-model` | `langchain4j-streaming-chat-model` | `StreamingChatLanguageModel` |
| `…-language-model` | `langchain4j-language-model` | `LanguageModel` |
| `…-streaming-language-model` | `langchain4j-streaming-language-model` | `StreamingLanguageModel` |
| `…-embedding-model` | `langchain4j-embedding-model` | `EmbeddingModel` |
| `…-tokenizer` | `langchain4j-tokenizer` | `Tokenizer` |
| `…-embedding-store` | `langchain4j-embedding-store` | `EmbeddingStore<TextSegment>`，须搭配 `embedding-model` |

## 后端

统一位于 `fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 包：

- 每个类型子配置继承公共类 `AbstractLangchain4jPluginConfiguration`，各自声明插件标识（`langchain4j-{type}`）与前端产物名，并带 `@ConditionalOnClass`（该类型接口）与 `@ConditionalOnProperty`（`fw.plus.magicapi.langchain4j.{type}.enabled`，缺省开启）。
- 总开关 `fw.plus.magicapi.langchain4j.enabled` 控制整体装配；总开关关闭或前端产物缺失时，服务端装配与脚本取用不受影响（后者只降级为「按基础插件标识注册」）。
- 连接测试接口 `POST /langchain4j/{type}/datasource/test` 由 `Langchain4jChatModelConfiguration` 统一承载（路径含 `{type}`，一个 Controller 服务七类）。

## 引入方式

按需引入对应类型的 starter；每个子工程显式声明该类型涉及的 langchain4j 依赖（autoconfigure 中的 provider/store 依赖为 `optional`，不向下游传递）：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-langchain4j-chat-model</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

## 构建

在聚合器目录执行（父工程默认跳过前端构建，七个子工程各自 `npm install` + `vite build` 并复制产物到 `target/classes/magic-editor/plugins/`）：

```bash
mvn package -DskipTests
# 单个子工程的前端调试
cd fw-plus-ai-magicapi-plugin-langchain4j-chat-model/src/console && npm install && npm run build && npm test
```
