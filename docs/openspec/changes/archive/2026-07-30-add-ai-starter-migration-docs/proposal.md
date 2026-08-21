## Why

AI framework 迁移说明目前集中在仓库级文档中，业务开发者从 Maven starter 入口看不到默认 LangChain4j、后续 Spring AI / Spring AI Alibaba / AgentScope 2 Java 切换方式和 Java 17 / Spring Boot 3.x 边界。

本次补丁把关键迁移说明放到对应 starter 目录，避免使用方只看到 POM 而错过破坏性升级、不兼容点和回滚要求。

## What Changes

- 为中立模型 starter 新增 README，说明 `fw-plus.ai.model`、默认 LangChain4j adapter、自动感知与后续替换路径。
- 为中立向量 store starter 新增 README，说明 `fw-plus.ai.embedding-store`、`VECTOR`/provider 边界、Magic-API `vector[...]` 影响与回滚。
- 为统一 LangChain4j adapter starter 新增 README，说明它是当前默认实现、provider SDK 需要业务显式引入，以及后续 Java 17 / Boot 3.x adapter 切换步骤。
- 扩展迁移文档校验脚本，要求三个 starter README 必须存在并覆盖核心主题。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `ai-framework-migration-guide`: 迁移说明必须在 starter 目录内也可发现。
- `ai-framework-breaking-migration-guide`: 破坏性迁移说明必须覆盖中立 starter 与统一 adapter starter。

## Impact

- 影响文档与校验脚本，不修改 Java 代码、POM 依赖或自动配置行为。
- 不属于 `add-tiny-engine-*`，不处理 AskData 迁移。
