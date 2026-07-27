## Why

`fw-plus-ai-langchain4j-model` 与 `fw-plus-ai-langchain4j-store-embeding` 承担的是框架通用的模型和向量存储能力，但其模块边界、公共类型、starter 和 Magic-API 插件直接暴露 LangChain4j 类型。这使 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 无法以同一配置、注册和脚本访问模型接入，也会把未来框架支持扩大为重复实现。

现在需要将这两项能力下沉为稳定的框架无关抽象，并保留 LangChain4j 作为默认、完整且兼容的首个实现，为其他进程内 Java SDK 适配提供统一扩展点和可验证的契约。

## What Changes

- 将模型定义、模型类型、工厂、注册表、异常和 provider 适配 SPI 从 `fw-plus-ai-langchain4j-model` 移至框架无关的 AI 模型模块；LangChain4j 模块保留为默认适配实现。
- 将向量存储定义、工厂、注册表和 provider 适配 SPI 从 `fw-plus-ai-langchain4j-store-embeding` 移至框架无关的 AI 向量存储模块；LangChain4j `EmbeddingStore<TextSegment>` 仅存在于 LangChain4j 适配层。
- 为 LangChain4j、Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 定义统一的进程内 SDK 适配契约、能力声明和 starter 装配方式；本次默认实现为 LangChain4j。
- 将 AI starter 和 Magic-API model/vector 插件改为仅依赖中立注册表，并保持现有 `fw-plus.ai.langchain4j.*` 配置、资源 key 与脚本使用方式兼容。
- 补齐从 LangChain4j 切换到其他适配框架的迁移文档，覆盖 Maven 依赖、配置、注册表访问、Magic-API 动态资源和脚本调用。
- 补齐契约、默认适配、自动配置、Magic-API 插件和分层 Maven 构建的测试矩阵。

## Capabilities

### New Capabilities

- `ai-framework-neutral-model-abstraction`: 提供不依赖具体 AI 框架的模型契约、注册、创建和进程内 SDK 适配 SPI。
- `ai-framework-neutral-embedding-store-abstraction`: 提供不依赖具体 AI 框架的向量存储契约、注册、创建和 provider 适配 SPI。
- `ai-framework-adapter-integration`: 定义 LangChain4j、Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 接入中立抽象、starter 与配置的规则。
- `ai-framework-migration-guide`: 规定从 LangChain4j 切换到其他适配框架时必须提供的依赖、配置、资源和 API 迁移说明。
- `ai-abstraction-contract-testing`: 提供跨抽象、默认实现、自动配置和 Magic-API 插件的兼容性测试要求。

### Modified Capabilities

- `langchain4j-model-module`: 将原模型模块从公共能力承载者调整为中立模型抽象的默认 LangChain4j 适配实现，并保留既有配置和访问兼容性。
- `langchain4j-embedding-store-starter`: 将原向量 store starter 调整为中立向量存储抽象的默认 LangChain4j 适配和 starter。
- `langchain4j-starter-modules`: 调整聚合与依赖边界，使 LangChain4j starter 不再拥有框架通用模型和向量存储契约。
- `magicapi-unified-vector-plugin`: 改为委托中立向量注册表和工厂，而非直接依赖 LangChain4j store 类型。

## Impact

- 受影响模块包括 Core AI 的 LangChain4j 模型与向量 store 聚合、Core BOM、AI Boot 自动配置、AI Boot starter、AI Magic-API model/vector 插件及相应样例和测试。
- 需要新增 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的可选依赖管理和进程内 SDK 适配模块；不得把它们的具体 SDK 传递到默认 LangChain4j 使用路径。
- 现有配置前缀、动态资源 key、脚本访问方式和 LangChain4j 行为需要回归验证；本次不迁移用户业务配置或运行态 Magic-API 资源。
