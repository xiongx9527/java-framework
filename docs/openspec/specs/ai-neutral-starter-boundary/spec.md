# ai-neutral-starter-boundary Specification

## Purpose
TBD - created by archiving change neutral-ai-adapter-autodiscovery. Update Purpose after archive.
## Requirements
### Requirement: 中立 starter 与公共运行时保持具体框架零痕迹
系统 SHALL 使 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、其所有中立 Starter、公共 API、Magic-API model/vector 实现以及 `fw-plus-ai-boot-autoconfigure` 中框架专属子包之外的代码只依赖中立 API 与 registry。具体框架自动配置 SHALL 隔离在统一 Boot autoconfigure artifact 的 `com.xiongx9527.fw.plus.ai.autoconfigure.<framework>.*` 子包，并通过 optional 依赖和 classpath 条件装配。

#### Scenario: 中立 artifact 没有具体框架依赖或公开类型
- **WHEN** 检查中立 Core artifact、中立 Starter、Magic-API、公开签名和框架专属子包之外的自动配置
- **THEN** 它们不得包含 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 类型、传递依赖、包引用或配置前缀。
- **AND** 它们 SHALL 保持 Java 8 兼容。

#### Scenario: 统一 autoconfigure 隔离具体框架实现
- **WHEN** `fw-plus-ai-boot-autoconfigure` 包含 LangChain4j 条件自动配置
- **THEN** 相关代码 SHALL 仅位于 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 子包。
- **AND** 具体框架与 provider 依赖 SHALL 标记为 optional，不得通过中立 Starter 传递。
- **AND** 缺少 adapter 或可选 SDK 时相关配置 SHALL 退让，不得导致中立应用启动时发生类链接失败。

#### Scenario: 应用显式安装 LangChain4j adapter
- **WHEN** 应用需要 LangChain4j 实现
- **THEN** 它 SHALL 显式引入 `fw-plus-ai-langchain4j-adapter`，并在 Spring Boot 应用中引入 `fw-plus-ai-boot-starter` 以启用自动配置。
- **AND** 中立 Starter 本身不得隐式传递该 adapter。

#### Scenario: Magic-API 使用中立 registry
- **WHEN** Magic-API 创建或访问 model/vector 动态资源
- **THEN** 它 SHALL 仅通过中立 registry 与门面执行操作。
- **AND** 它不得创建或暴露框架原生 model、document 或 embedding store 类型。
