# fw-plus-ai-magicapi-plugin-langchain4j-chat-memory-store

Magic-API 的 LangChain4j **对话记忆存储** 控制台前端，对应 datasource type `langchain4j-chat-memory-store`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore`。
- 前端产物：`magic-langchain4j-chat-memory-store.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`，并打进 jar；后端始终以「插件标识 + 全局名 + 产物名」三元组注册，控制台据此加载本插件面板，因此使用方必须把本模块声明为依赖（版本由 `fw-plus-ai-boot-dependencies` 纳管），否则控制台请求该产物会 404。
- 表单字段对应后端 `Langchain4jChatMemoryStoreInfo`（key/name/enabled + getFunction/updateFunction/deleteFunction），无连接测试。

```bash
npm install && npm run build   # dist/magic-langchain4j-chat-memory-store.1.0.0.iife.js
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
