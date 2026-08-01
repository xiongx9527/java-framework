## Why

当前 Magic-API 的 LangChain4j 模型动态管理能力主要沉在 AI Boot 自动配置层，模型定义、工厂和运行态注册无法像 `fw-plus-ai-langchain4j-store-embeding` 一样作为 core 侧通用能力复用。新增通用模型模块可以让普通 Spring Boot 应用和 Magic-API 插件共享同一套模型定义、创建、注册和查找契约，降低后续接入 Chat、Streaming Chat、Embedding、Image、Language 等模型的重复成本。

## What Changes

- 在 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 下新增 `fw-plus-ai-langchain4j-model` 模块，仿照 `fw-plus-ai-langchain4j-store-embeding` 提供轻量模型定义、工厂、provider adapter、registry、异常等 core 抽象。
- 在 AI Boot starter 层新增 `fw-plus-ai-langchain4j-model-spring-boot-starter`，仿照 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 引入 boot 自动配置和 core 模型抽象，但不默认传递所有 provider SDK。
- 在 AI Boot 自动配置层新增或调整 LangChain4j 模型自动配置，负责读取 Spring Boot 配置、注册默认模型 registry、暴露 provider adapter，并复用 core 模型工厂创建真实 LangChain4j model。
- **BREAKING** 为 Magic-API 新增统一模型插件能力，提供 `model-datasource` 资源、动态注册/销毁/查询、脚本侧模块访问，以及基础连接/创建测试接口。
- **BREAKING** 删除或替换现有 Magic-API LangChain4j 插件的重复模型工厂、动态数据源、资源存储、控制器、脚本模块和前端资源；旧实现仅作为迁移参考，不保留旧 `langchain4j-datasource` 和旧 `langchain4j` 脚本入口兼容。
- 增加单元测试和必要集成测试，覆盖工厂适配、registry 行为、自动配置条件、Magic model 插件动态注册和旧实现替换。

## Capabilities

### New Capabilities
- `langchain4j-model-module`: 通用 LangChain4j 模型模块、Spring Boot starter 与 Magic-API model 插件集成，覆盖模型定义、创建、注册、动态数据源和脚本访问。

### Modified Capabilities

无。

## Impact

- 影响 core 侧 Maven 聚合：`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml` 需要注册新模块，core BOM 需要管理新 artifact。
- 影响 AI Boot 层 Maven 聚合和 BOM：新增 starter 模块，并在 AI Boot 自动配置中注册模型自动配置与 Magic model 插件配置。
- 影响 Magic-API AI 插件：新增 `model` 插件配置、动态资源存储、控制器、脚本模块与前端资源注册，并删除或替换旧 `langchain4j` 插件实现。
- 影响依赖边界：starter 默认只带通用抽象和自动配置，具体 OpenAI、Qwen、Ollama、DashScope 等 provider SDK 由业务或已有 fw-plus LangChain4j provider 模块显式引入。
- 影响兼容性：保持 Spring Boot 2.7.18、JDK 1.8、LangChain4j 0.35.0 和项目现有 Maven Wrapper 构建方式；不升级 Magic-API `org.ssssssss` 依赖版本。旧 Magic `langchain4j-datasource` 资源和脚本入口需要迁移到新的 `model-datasource` 与 model 脚本模块。
