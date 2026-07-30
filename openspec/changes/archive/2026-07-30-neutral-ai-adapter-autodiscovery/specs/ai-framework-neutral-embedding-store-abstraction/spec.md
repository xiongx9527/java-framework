## MODIFIED Requirements

### Requirement: 提供框架无关的向量存储核心模块
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 下提供 Java 8 兼容的 `fw-plus-ai-store-embedding`，承载中立文档、向量、查询、匹配结果、store 门面、工厂、注册表和不含具体框架类型的 adapter SPI。store 定义的 `framework` 可显式指定；未指定时 SHALL 按已发现的 adapter 执行唯一候选选择。

#### Scenario: 公共 store API 不暴露原生文档类型
- **WHEN** 检查 `fw-plus-ai-store-embedding` 的公开签名
- **THEN** 它不得暴露 LangChain4j `EmbeddingStore<TextSegment>`、Spring AI `Document` 或其他框架原生类型。
- **AND** 文档内容、metadata、向量和查询结果 SHALL 使用中立数据类型表示。

#### Scenario: 支持多个 store 和默认 store
- **WHEN** 应用配置多个启用的向量 store 并指定默认 key
- **THEN** registry SHALL 按 key 返回独立的中立 store 门面。
- **AND** 未指定 key 时 SHALL 返回默认 store。

#### Scenario: 默认 store 配置无效
- **WHEN** 默认 store key 不存在或对应定义未启用
- **THEN** 应用启动 SHALL 失败。
- **AND** 错误 SHALL 包含无效 key 且不得泄漏凭证。

#### Scenario: store adapter 选择失败
- **WHEN** 没有 adapter、多个 adapter 或显式 framework 无法匹配
- **THEN** 应用启动 SHALL 失败。
- **AND** 错误 SHALL 包含选择上下文且不得泄漏凭证。
