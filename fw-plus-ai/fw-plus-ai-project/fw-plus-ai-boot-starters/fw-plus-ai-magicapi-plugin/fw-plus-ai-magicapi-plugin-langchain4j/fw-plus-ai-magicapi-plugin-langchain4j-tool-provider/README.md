# fw-plus-ai-magicapi-plugin-langchain4j-tool-provider

Magic-API 的 LangChain4j **ToolProvider 工具集** 控制台前端，对应 datasource type `langchain4j-tool-provider`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider`。
- 前端产物：`magic-langchain4j-tool-provider.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`；产物缺失时后端只按基础插件标识注册。
- 表单字段对应后端 `Langchain4jToolProviderInfo`（key/name/enabled + tools{functions, api} 范围），无连接测试（工具集直接读取 magic-api 资源）。

```bash
npm install && npm run build   # dist/magic-langchain4j-tool-provider.1.0.0.iife.js
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
