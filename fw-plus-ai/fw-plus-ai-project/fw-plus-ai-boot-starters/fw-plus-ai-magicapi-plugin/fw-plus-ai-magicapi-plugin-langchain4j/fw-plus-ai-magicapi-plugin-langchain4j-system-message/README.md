# fw-plus-ai-magicapi-plugin-langchain4j-system-message

Magic-API 的 LangChain4j **系统提示词** 控制台前端，对应资源类型 `langchain4j-system-message`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage`。
- 前端产物：`magic-langchain4j-system-message.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`；产物缺失时后端只按基础插件标识注册。
- 资源为路径型 `.ms` 文件，`script` 即系统提示词文本，脚本模块 `langchain4jSystemMessageProvider.get(path)` 返回 `Function<Object, String>`。

```bash
npm install && npm run build   # dist/magic-langchain4j-system-message.1.0.0.iife.js
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
