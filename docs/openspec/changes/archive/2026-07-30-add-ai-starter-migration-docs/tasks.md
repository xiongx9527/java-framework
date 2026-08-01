## 1. Starter 文档

- [x] 1.1 为 `fw-plus-ai-model-spring-boot-starter` 新增 README，说明默认 LangChain4j、中立配置、自动感知、后续 Java 17 / Boot 3.x 切换和回滚。
- [x] 1.2 为 `fw-plus-ai-store-embedding-spring-boot-starter` 新增 README，说明 `fw-plus.ai.embedding-store`、`VECTOR`/provider 边界、Magic-API `vector[...]` 影响、后续切换和回滚。
- [x] 1.3 为 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 新增 README，说明当前默认实现、provider SDK 显式引入、从 LangChain4j 换到后续实现的步骤和不兼容点。

## 2. 验证

- [x] 2.1 扩展 `docs/ai-framework-migration/verify-migration-docs.sh`，要求三个 starter README 存在并覆盖默认 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 2 Java、Java 17 / Boot 3.x 和回滚说明。
- [x] 2.2 运行 `zsh -n docs/ai-framework-migration/verify-migration-docs.sh` 与 `zsh docs/ai-framework-migration/verify-migration-docs.sh`。
- [x] 2.3 运行 `git diff --check`，确认文档无空白错误。
