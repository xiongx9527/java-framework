# Task 7 执行报告：文档、README 与样例配置

## 文档内容摘要

- 新增 TinyEngine starter README：包含 Maven 坐标、`fw.plus.tiny-engine.ai.enabled` 总开关、chat/streaming/embedding model key、vector store key、RAG chunk/search 配置、`fail-fast`/`degrade` 缺失依赖策略和禁用行为。
- TinyEngine 文档明确模型走 `AiModelRegistry` / `fw-plus-ai-model`，向量走 `AiEmbeddingStoreRegistry` / `fw-plus-ai-store-embedding`；LangChain4j 仅作为默认 adapter/provider。
- TinyEngine 文档如实说明 Task 6 边界：知识库服务写入文本 chunk，中立 AiModel 不向 TinyEngine 服务直接暴露 embedding 生成调用，向量生成/批量写入策略由中立 store/provider 承担。
- 新增 AskData starter README：说明 `fw.plus.ai.askdata` 的模型 key、候选 key 和 `vector.store` 如何解析到两个中立 Registry；提供默认 LangChain4j adapter、provider SDK 和 `AskDataModelRegistry` / `AskDataVectorRetriever` / Registry Bean 覆盖示例。
- 两份 README 均加入旧 LangChain4j model/store starter 坐标不是当前 starter line 主路径的边界警告。

## grep 命令与结果

按 brief 执行：

```bash
rg -n "fw-plus-ai-langchain4j-model-spring-boot-starter|fw-plus-ai-langchain4j-store-embeding-spring-boot-starter|LangChain4jModelRegistry|EmbeddingStoreRegistry" \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter
```

结果：退出码 `0`，存在匹配。逐项检查如下：

- 两份 README 中的旧坐标只出现在明确的“当前 starter line 不使用/不是当前主路径/不要混用”警告中，没有作为新增依赖或主配置示例。
- 两个 starter 的既有 `*StarterDependencyTest.java` 中命中旧坐标，是断言旧坐标不得出现在 POM 的回归测试，不是文档主路径。
- `EmbeddingStoreRegistry` 的匹配来自合法中立名称 `AiEmbeddingStoreRegistry`；未发现 README 使用旧 registry 名称 `LangChain4jModelRegistry`。

补充检查：

```bash
git diff --check
```

结果：通过。

## 修改文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/README.md`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md`
- `.superpowers/sdd/task-7-report.md`

未修改源码、POM、版本号或无关文件；`.superpowers/sdd/task-7-brief.md` 仅按提交要求加入暂存范围。

## 自审结果

- 文档使用简体中文，并参考现有 model/store/adapter README 的配置风格。
- Maven artifact 坐标与当前 starter POM 及已有依赖回归测试一致。
- TinyEngine 的总开关、RAG 开关、model/store key、缺失依赖策略和 disable 行为均有说明和 YAML 示例。
- AskData 主路径明确为两个中立 Registry，并覆盖默认 LangChain4j adapter 与业务 Bean 覆盖点。
- 未将 LangChain4j 写成 TinyEngine/AskData 业务唯一实现，未夸大 embedding 生成能力。
- 本任务只涉及文档，未运行 Maven 测试；已完成 brief 指定 docs grep 与 `git diff --check`。

## 风险/疑问

- 具体 provider SDK、后端连接、向量维度和 schema 仍需业务应用按所选 adapter/provider 配置并进行真实环境验证；README 不替代 provider 文档。
- Task 6 的中立 AiModel 尚不提供 TinyEngine 侧 embedding 生成调用，文档已保留该边界；后续若实现发生变化，需要同步更新知识库流程说明。
- grep 因既有负向依赖断言和合法中立 Registry 名称而有匹配，未发现把旧 artifact/registry 当作当前主路径的文档内容。
