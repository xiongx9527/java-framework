# fw-plus-ai-magicapi-plugin-langchain4j-embedding-store

Magic-API 的 LangChain4j **向量存储** 控制台前端，对应 datasource type `langchain4j-embedding-store`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j`。
- 前端产物：`magic-langchain4j-embedding-store.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`；产物缺失时后端只按基础插件标识注册。
- `options` 表单字段按 provider 元数据渲染（`src/metadata/providers.js`，与后端工厂实际读取的 options 键一致）。
- 连接测试：`POST /langchain4j/embedding-store/datasource/test`。

```bash
npm install && npm run build   # dist/magic-langchain4j-embedding-store.1.0.0.iife.js
npm test                       # 元数据与后端工厂交叉校验
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
