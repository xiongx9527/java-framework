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
- 总开关 `fw.plus.magicapi.langchain4j.enabled` 控制整体装配；总开关关闭时，服务端装配与脚本取用不受影响。
- 插件注册**始终**上报完整三元组（插件标识 + 全局名 `globalName` + 前端产物名 `javascriptFilename`），不按前端产物是否落在 classpath 上做降级。本工程自身是 `pom` 聚合父，不在 `<build><plugins>` 里启用前端构建，只通过 pluginManagement 下传；14 个子工程各自在自己的 pom 里启用 `exec-maven-plugin`（`npm install` / `npm run build`，绑 `prepare-package`）与 `maven-resources-plugin:copy-resources`。`copy-console-dist` 绑两个阶段：`process-resources`（与聚合层 `clean-stale-console-assets` 的 `initialize` 清理配对，否则 `mvn compile` 会把 `target/classes` 里的产物删光且不重建）与 `prepare-package`（npm 重建完 dist 再拷一次，保证 jar 里是本次构建的产物）。行为与 boot 层 `fw-plus-magicapi-plugin` 的 28 个插件子工程一致。
- **谁用插件面板，谁声明产物模块**：这些子工程不含 Java 源码，只把 `src/console/dist/*.iife.js` 打成 `magic-editor/plugins/` 静态资源。使用方（如 `fw-plus-ai-magicapi-sample`）必须显式声明对应子工程为依赖，版本由 `fw-plus-ai-boot-dependencies` 纳管；漏声明时控制台会请求到 404。
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
