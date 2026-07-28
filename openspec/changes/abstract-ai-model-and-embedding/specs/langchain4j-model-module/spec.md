## MODIFIED Requirements

### Requirement: 提供 Core 侧 LangChain4j 默认模型适配模块
系统 SHALL 保留 `fw-plus-ai-langchain4j-model` 作为 `fw-plus-ai-model` 的默认 LangChain4j 适配模块，而不再让它承载框架通用模型定义、工厂和 registry 契约。

#### Scenario: 聚合 POM 同时注册中立模型模块和 LangChain4j adapter
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml` 及 `fw-plus-ai-langchain4j/pom.xml`
- **THEN** 根 AI 聚合 SHALL 列出 `fw-plus-ai-model`。
- **AND** LangChain4j 聚合 SHALL 继续列出 `fw-plus-ai-langchain4j-model`。

#### Scenario: BOM 管理新模块和兼容 artifact
- **WHEN** 检查 Core BOM 的 dependencyManagement
- **THEN** 它 SHALL 管理 `fw-plus-ai-model` 和 `fw-plus-ai-langchain4j-model`。
- **AND** 两个 artifact SHALL 使用 `${project.version}`。

#### Scenario: LangChain4j adapter 保持默认模型行为
- **WHEN** 应用提供启用的 LangChain4j 模型定义
- **THEN** LangChain4j adapter SHALL 通过中立模型工厂和 registry 创建可用的中立模型门面。
- **AND** 既有 LangChain4j provider、默认模型 key 和多模型别名行为 SHALL 继续生效。

#### Scenario: 公共模型类型已移出 LangChain4j 包
- **WHEN** 检查框架无关模型定义、类型、工厂、registry、异常和 SPI
- **THEN** 它们 SHALL 位于中立模型模块和中立包中。
- **AND** `fw-plus-ai-langchain4j-model` SHALL 只包含 LangChain4j 原生对象转换、provider adapter 和兼容入口。
