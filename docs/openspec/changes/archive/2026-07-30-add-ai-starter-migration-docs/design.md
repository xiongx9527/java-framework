## Design

本次使用轻量文档补丁，不引入新的框架代码。

文档落点固定在三个 starter 目录：

- `fw-plus-ai-model-spring-boot-starter/README.md`
- `fw-plus-ai-store-embedding-spring-boot-starter/README.md`
- `fw-plus-ai-langchain4j-adapter-spring-boot-starter/README.md`

三个 README 采用相同口径：

- 当前 Java 8 / Spring Boot 2.7.x 只交付中立 API 与默认 LangChain4j adapter。
- 中立 starter 不隐式携带具体 framework SDK。
- 使用默认实现时引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
- 后续 Spring AI、Spring AI Alibaba、AgentScope 2 Java 放到 Java 17 / Spring Boot 3.x 高版本线。
- AgentScope 2 Java 必须是进程内 SDK adapter。
- 保留业务 key，切换 `framework` 字段，按目标 adapter 运行验证；失败时回滚到默认 LangChain4j。

校验脚本 `docs/ai-framework-migration/verify-migration-docs.sh` 增加 starter README 必备短语检查，确保后续改动不会再次漏掉 starter 内文档。
