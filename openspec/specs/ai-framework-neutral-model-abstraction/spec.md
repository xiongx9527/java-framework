# ai-framework-neutral-model-abstraction Specification

## Purpose
TBD - created by archiving change abstract-ai-model-and-embedding. Update Purpose after archive.
## Requirements
### Requirement: 提供框架无关的 AI 模型核心模块
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 下提供 `fw-plus-ai-model`，承载模型定义、能力类型、统一模型门面、工厂、注册表、异常和 adapter SPI。

#### Scenario: 公共 API 不泄漏框架类型
- **WHEN** 检查 `fw-plus-ai-model` 的编译依赖和公开签名
- **THEN** 它不得依赖或暴露 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 类型。
- **AND** 它仅使用 Java 8 兼容的语言和集合 API。

#### Scenario: 通过 key 获取默认或指定模型
- **WHEN** 应用注册多个启用的中立模型定义并配置默认 key
- **THEN** 调用方 SHALL 能按 key 获取对应模型门面。
- **AND** 未提供 key 时 SHALL 获取默认模型。

#### Scenario: 未支持模型能力在启动期失败
- **WHEN** 模型定义请求的 framework、provider 或能力没有匹配 adapter
- **THEN** 系统 SHALL 抛出受控模型异常。
- **AND** 异常 SHALL 包含 key、framework、provider 和能力类型。

### Requirement: 模型 adapter 必须声明可用能力
系统 SHALL 通过 `AiModelAdapter` SPI 将框架原生模型转换为中立模型门面，并要求 adapter 显式声明其支持的模型能力。

#### Scenario: 支持能力的模型被创建
- **WHEN** 模型定义与某个 adapter 声明的 framework、provider 和能力匹配
- **THEN** 工厂 SHALL 使用该 adapter 创建并注册中立模型门面。
- **AND** 该门面 SHALL 保留可诊断的 key、framework、provider 和能力元数据。

#### Scenario: 不伪造框架未支持的能力
- **WHEN** Spring AI、Spring AI Alibaba 或 AgentScope 2 Java adapter 未声明某种能力
- **THEN** 系统 SHALL 不得回退到 LangChain4j adapter 或返回空实现。
- **AND** 系统 SHALL 报告该 adapter 不支持请求能力。
