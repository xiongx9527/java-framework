# ai-framework-neutral-embedding-store-abstraction Specification

## Purpose
TBD - created by archiving change abstract-ai-model-and-embedding. Update Purpose after archive.
## Requirements
### Requirement: 提供框架无关的向量存储核心模块
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 下提供 `fw-plus-ai-store-embedding`，承载中立文档、向量、查询、匹配结果、store 门面、工厂、注册表和 provider adapter SPI。

#### Scenario: 公共 store API 不暴露原生文档类型
- **WHEN** 检查 `fw-plus-ai-store-embedding` 的公开签名
- **THEN** 它不得暴露 `EmbeddingStore<TextSegment>`、Spring AI `Document` 或其他框架原生类型。
- **AND** 文档内容、metadata、向量和查询结果 SHALL 使用中立数据类型表示。

#### Scenario: 支持多个 store 和默认 store
- **WHEN** 应用配置多个启用的向量 store 并指定默认 key
- **THEN** registry SHALL 按 key 返回独立的中立 store 门面。
- **AND** 未指定 key 时 SHALL 返回默认 store。

#### Scenario: 默认 store 配置无效
- **WHEN** 默认 store key 不存在或对应定义未启用
- **THEN** 应用启动 SHALL 失败。
- **AND** 错误 SHALL 包含无效 key 且不得泄漏凭证。

### Requirement: 向量 provider adapter 隔离框架与连接构造
系统 SHALL 通过 `AiEmbeddingStoreAdapter` SPI 构建框架或 provider 原生 store，并向调用方暴露统一操作。

#### Scenario: adapter 构建已支持的 store
- **WHEN** framework/provider adapter 位于 classpath、声明支持该定义且配置有效
- **THEN** 工厂 SHALL 使用该 adapter 创建中立 store 门面。

#### Scenario: adapter 或依赖缺失
- **WHEN** 配置引用的 framework/provider adapter 不存在或其可选 SDK 不在 classpath
- **THEN** 自动配置 SHALL 失败并说明缺失 adapter、SDK 或 provider。
