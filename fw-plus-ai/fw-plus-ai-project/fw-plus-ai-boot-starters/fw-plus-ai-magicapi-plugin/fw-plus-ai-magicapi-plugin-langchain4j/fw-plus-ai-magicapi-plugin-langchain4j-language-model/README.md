# fw-plus-ai-magicapi-plugin-langchain4j-language-model

Magic-API 的 LangChain4j **语言模型** 控制台前端，对应 datasource type `langchain4j-language-model`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j`。
- 前端产物：`magic-langchain4j-language-model.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`，并打进 jar；后端始终以「插件标识 + 全局名 + 产物名」三元组注册，控制台据此加载本插件面板，因此使用方必须把本模块声明为依赖（版本由 `fw-plus-ai-boot-dependencies` 纳管），否则控制台请求该产物会 404。
- `options` 表单字段按 provider 元数据渲染（`src/metadata/providers.js`，与后端工厂实际读取的 options 键一致）。
- 连接测试：`POST /langchain4j/language-model/datasource/test`。

```bash
npm install && npm run build   # dist/magic-langchain4j-language-model.1.0.0.iife.js
npm test                       # 元数据与后端工厂交叉校验
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
