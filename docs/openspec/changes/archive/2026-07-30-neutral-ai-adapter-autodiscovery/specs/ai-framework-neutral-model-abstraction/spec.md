## MODIFIED Requirements

### Requirement: 提供框架无关的 AI 模型核心模块
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 下提供 Java 8 兼容的 `fw-plus-ai-model`，承载模型定义、能力类型、统一模型门面、工厂、注册表、异常和不含具体框架类型的 adapter SPI。模型定义的 `framework` 可显式指定；未指定时 SHALL 按已发现的 adapter 执行唯一候选选择。

#### Scenario: 公共 API 不泄漏框架类型
- **WHEN** 检查 `fw-plus-ai-model` 的编译依赖和公开签名
- **THEN** 它不得依赖或暴露 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 类型。
- **AND** 它仅使用 Java 8 兼容的语言和集合 API。

#### Scenario: 通过 key 获取默认或指定模型
- **WHEN** 应用注册多个启用的中立模型定义并配置默认 key
- **THEN** 调用方 SHALL 能按 key 获取对应模型门面。
- **AND** 未提供 key 时 SHALL 获取默认模型。

#### Scenario: 模型定义有唯一候选 adapter
- **WHEN** 模型定义未提供 framework 且一个已发现 adapter 支持其 provider 和能力
- **THEN** 工厂 SHALL 使用该 adapter 创建并注册中立模型门面。
- **AND** 未提供 key 时 registry SHALL 获取默认模型。

#### Scenario: 模型定义的 adapter 选择不确定
- **WHEN** 没有 adapter、多个 adapter 或显式 framework 没有可用 adapter
- **THEN** 系统 SHALL 抛出受控模型异常。
- **AND** 异常 SHALL 包含 key、framework、provider、能力类型和候选信息。

#### Scenario: 未支持模型能力在启动期失败
- **WHEN** 模型定义请求的 framework、provider 或能力没有匹配 adapter
- **THEN** 系统 SHALL 抛出受控模型异常。
- **AND** 异常 SHALL 包含 key、framework、provider 和能力类型。
