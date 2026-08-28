## Why

`fw-plus-ai-boot-autoconfigure` 的 Spring Boot 2 自动配置清单与 Spring Boot 3 导入清单不一致：`spring.factories` 声明了 10 个自动配置类，而 `AutoConfiguration.imports` 仅声明 1 个，导致迁移到 Spring Boot 3 时大部分 AI 自动配置无法被发现。Boot 与 Cloud 层也需要一并核对，避免相同问题被遗漏。

## What Changes

- 对比 `fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 中所有 `spring.factories` 的 `EnableAutoConfiguration` 条目与对应 `AutoConfiguration.imports` 条目。
- 保持已经一致的 Boot 和 Cloud 清单不变。
- 将 AI `spring.factories` 中缺失的条目补入 `AutoConfiguration.imports`。
- 明确排除 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration`：不将它从 `spring.factories` 同步到 `AutoConfiguration.imports`，并保持两份清单中的现状不变。
- 增加轻量集合一致性校验，验证排除项之外两种元数据机制没有缺失或多余条目。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本次仅修复自动配置发现元数据，不改变对外功能需求，因此 change 使用 `skip_specs: true`。

## Impact

- 受影响文件：`fw-plus-ai-boot-autoconfigure` 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 核对但预计不修改：Boot、Cloud 对应的两类自动配置元数据文件，以及 Cloud 聚合模块的空 `spring.factories`。
- 不新增或变更 public API、依赖、数据库 schema。
- `LangChain4jAutoConfiguration` 不参与本次同步；当前两份清单均未注册它，本次保持该现状。
