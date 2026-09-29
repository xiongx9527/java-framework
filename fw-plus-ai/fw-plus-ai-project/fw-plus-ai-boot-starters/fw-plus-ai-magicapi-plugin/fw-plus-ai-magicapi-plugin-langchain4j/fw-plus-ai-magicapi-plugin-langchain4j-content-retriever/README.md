# fw-plus-ai-magicapi-plugin-langchain4j-content-retriever

Magic-API 的 LangChain4j **内容检索器** 控制台前端，对应 datasource type `langchain4j-content-retriever`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever`。
- 前端产物：`magic-langchain4j-content-retriever.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`；产物缺失时后端只按基础插件标识注册。
- 表单字段对应后端 `Langchain4jContentRetrieverInfo`（key/name/provider/enabled + options），五种 provider：
  - `embedding-store` —— 关联向量库 + 向量模型做向量检索；
  - `neo4j` —— 关联对话模型，把问题转成 Cypher 后在图库执行；
  - `azure-ai-search` —— Azure AI Search 向量/全文/混合检索；
  - `web-search` —— 联网检索，搜索引擎可由 magic-api 函数托管；
  - `filtering` —— 包在另一个检索器之外，按 score 与 metadata 过滤；
  - `aggregating` —— 组合多个检索器：并行发起、按声明顺序合并去重（可选条数上限）。
- 引用的同类数据源（向量库/向量模型/对话模型/另一个检索器）在表单里按已启用 key 下拉选择，取值落在 `options` 中。
- 连接测试：走后端 `/langchain4j/content-retriever/datasource/test`，按表单内容构建实例并真实检索一次。

```bash
npm install && npm run build   # dist/magic-langchain4j-content-retriever.1.0.0.iife.js
npm test                       # console 元数据与后端 provider 的交叉校验
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。
