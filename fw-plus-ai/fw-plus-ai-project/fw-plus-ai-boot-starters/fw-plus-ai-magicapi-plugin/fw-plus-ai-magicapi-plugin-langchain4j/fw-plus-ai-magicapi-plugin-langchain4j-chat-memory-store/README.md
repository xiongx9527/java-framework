# fw-plus-ai-magicapi-plugin-langchain4j-chat-memory-store

Magic-API 的 LangChain4j **对话记忆存储** 控制台前端，对应 datasource type `langchain4j-chat-memory-store`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore`。
- 前端产物：`magic-langchain4j-chat-memory-store.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`；产物缺失时后端只按基础插件标识注册。
- 表单字段对应后端 `Langchain4jChatMemoryStoreInfo`（key/name/enabled + getFunction/updateFunction/deleteFunction），无连接测试。

```bash
npm install && npm run build   # dist/magic-langchain4j-chat-memory-store.1.0.0.iife.js
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
