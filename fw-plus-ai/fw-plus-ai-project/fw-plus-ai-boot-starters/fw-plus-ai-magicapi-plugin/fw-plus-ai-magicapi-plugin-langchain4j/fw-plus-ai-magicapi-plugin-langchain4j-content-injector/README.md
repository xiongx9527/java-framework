# fw-plus-ai-magicapi-plugin-langchain4j-content-injector

Magic-API 的 LangChain4j **内容注入器** 控制台前端，对应资源类型 `langchain4j-content-injector`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector`。
- 前端产物：`magic-langchain4j-content-injector.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`，并打进 jar；后端始终以「插件标识 + 全局名 + 产物名」三元组注册，控制台据此加载本插件面板，因此使用方必须把本模块声明为依赖（版本由 `fw-plus-ai-boot-dependencies` 纳管），否则控制台请求该产物会 404。
- 资源为路径型 `.ms` 文件：`script` 即注入逻辑，可用变量 `contents`（检索到的 `List<Content>`）、`texts`（正文列表）、`chatMessage`、`query`；返回 String（作为注入后的用户消息文本）或 `ChatMessage`。
- 脚本模块：`langchain4jContentInjector.get('/injector/assistant')` 返回 `ContentInjector`，供 `DefaultRetrievalAugmentor.builder().contentInjector(...)` 装配；`langchain4jContentInjector.inject(path, contents, chatMessage)` 可直接执行一次。
- 控制台「执行测试」走后端 `/langchain4j/content-injector/execute`，支持断点与日志（资源上的「测试参数」会注入脚本上下文，可用于给 `query` / `texts` 造数据）。

```bash
npm install && npm run build   # dist/magic-langchain4j-content-injector.1.0.0.iife.js
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
