## Why

`fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 被单独发布为 Maven artifact，但其中源码已经属于统一的 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 自动配置命名空间。这一额外模块增加了 Reactor、BOM、Starter 和文档的维护边界，却没有形成独立的公共能力边界。

## What Changes

- **BREAKING** 删除 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` Maven 模块及公开坐标，不提供兼容空 POM。
- 将该模块的生产源码、自动配置注册资源和测试完整迁入 `fw-plus-ai-boot-autoconfigure`，保留现有 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包结构和运行时行为。
- 保留 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 作为业务应用显式安装 LangChain4j 默认实现的入口，并将其自动配置依赖改为 `fw-plus-ai-boot-autoconfigure`。
- 从 AI Reactor、BOM、Starter 文档、迁移文档和验证命令中移除旧 autoconfigure 坐标。
- 调整中立边界测试：允许统一 Boot autoconfigure artifact 在隔离的 `langchain4j` 子包中承载具体框架自动配置，同时继续禁止中立 Core、Starter、公共 API、Magic-API 和传递依赖暴露具体框架或 provider SDK。
- 保留 provider SDK 缺失时的条件装配和启动回归，不向 Starter 新增 provider SDK 依赖。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `langchain4j-starter-modules`：统一 LangChain4j adapter Starter 继续显式安装默认实现，但具体 Spring Boot 自动配置必须归属现有 `fw-plus-ai-boot-autoconfigure`，不得再发布独立 autoconfigure artifact。
- `ai-neutral-starter-boundary`：将无具体框架痕迹的约束精确限定到中立包、公开契约、Magic-API 和传递依赖；统一 autoconfigure artifact 可在隔离子包中包含条件化框架实现。

## Impact

- 受影响模块：`fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-langchain4j-adapter-spring-boot-starter`、`fw-plus-ai-boot-dependencies` 和 `fw-plus-ai-project`。
- 受影响资源：Spring Boot 2.7 `spring.factories` 与现有 `AutoConfiguration.imports` 注册清单。
- 受影响测试：自动配置注册、缺失 provider SDK 启动、依赖边界、BOM/Reactor 坐标和旧 artifact 不存在性检查。
- 受影响文档：Starter README、AI 迁移说明及包含旧模块构建参数的验证文档。
- 不改变 Java 8 / Spring Boot 2.7.18 基线、中立配置前缀、adapter 选择规则、Core adapter 实现或具体 provider 行为。
