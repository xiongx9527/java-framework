# Comet Design Handoff

- Change: abstract-ai-model-and-embedding
- Phase: design
- Mode: compact
- Context hash: 873415265c83a5389f104b90838e3927139e81c2fdefa77b62c0c48f296939f0

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/abstract-ai-model-and-embedding/proposal.md

- Source: openspec/changes/abstract-ai-model-and-embedding/proposal.md
- Lines: 1-37
- SHA256: 8601d3032baae8ca03844b2ad4abb7a85710fa767db5568be07e12c1a364c01e

```md
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

```

## openspec/changes/abstract-ai-model-and-embedding/design.md

- Source: openspec/changes/abstract-ai-model-and-embedding/design.md
- Lines: 1-94
- SHA256: aaae3e5accc062c6c2096a1ec7c960b3d5edadb5fbc10ef8636fb436f547c6c2

[TRUNCATED]

```md
## Context

当前 Core AI 层将 `LangChain4jModel`、`LangChain4jModelRegistry`、`EmbeddingStore<TextSegment>`、`EmbeddingStoreRegistry` 等框架类型放在 `fw-plus-ai-langchain4j-model` 和 `fw-plus-ai-langchain4j-store-embeding` 中。AI Boot 自动配置和 Magic-API model/vector 插件因此直接依赖 LangChain4j；已有配置分别使用 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store`。

本次是跨 Core、AI Boot、starter、Magic-API 与 BOM 的架构调整。框架基线仍受 Core Java 8 与 Spring Boot 2.7.18 约束；Spring AI、Spring AI Alibaba、AgentScope 2 Java 的实际版本兼容性必须通过有效 POM、依赖树和编译验证确认，不能仅依据 BOM 属性推断。

## Goals / Non-Goals

**Goals:**

- 将模型和向量存储的公共 API 下沉到 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`，不暴露任何特定 AI 框架类型。
- 保留 LangChain4j 作为默认完整实现；原 `fw-plus-ai-langchain4j-*` 模块变为适配模块和兼容入口。
- 使 Spring AI、Spring AI Alibaba、AgentScope 2 Java 可作为进程内 SDK 适配器接入同一注册、配置、starter 与 Magic-API 生命周期。
- 保持旧 LangChain4j 配置、资源 key 与脚本访问可用，并提供从 LangChain4j 切换到其他框架的明确文档和回滚路径。
- 为 API 契约、每个适配器能力、兼容层、自动配置、Magic-API 和 Maven 分层构建建立自动化测试。

**Non-Goals:**

- 不迁移用户已有运行态 Magic-API 资源、密钥或业务代码。
- 不把所有框架的原生 agent/workflow、tool、memory、graph 等高阶能力强行抽象为统一接口。
- 不在公共模块传递具体厂商 SDK，也不承诺把不满足 Java 8/Spring Boot 2.7 前置条件的 SDK 编译进默认 LangChain4j 路径。
- 不改变向量数据库的 provider 连接协议或现有 Magic-API 资源格式，除非规格明确增加框架选择字段。

## Decisions

### 1. 模块分层采用中立核心和可选框架适配器

新增 Core 模块：

```
fw-plus-ai/
├── fw-plus-ai-model/
├── fw-plus-ai-store-embedding/
├── fw-plus-ai-langchain4j/
│   ├── fw-plus-ai-langchain4j-model/
│   └── fw-plus-ai-langchain4j-store-embeding/
├── fw-plus-ai-spring-ai/                 # 新增可选适配聚合
├── fw-plus-ai-spring-ai-alibaba/         # 新增可选适配聚合
└── fw-plus-ai-agentscope/                # 新增可选适配聚合
```

`fw-plus-ai-model` 提供模型定义、能力类型、统一模型门面、工厂、注册表、异常和 `AiModelAdapter` SPI；`fw-plus-ai-store-embedding` 提供中立文档、向量、查询、匹配结果、store 门面、工厂、注册表和 `AiEmbeddingStoreAdapter` SPI。公共 API 使用 Java 8 可用类型，禁止导入 `dev.langchain4j.*`、`org.springframework.ai.*`、Spring AI Alibaba 或 AgentScope 类型。

各框架适配模块负责原生对象构建和中立门面转换，并通过 `supports`/能力声明公开可用模型和向量能力。调用者只能通过中立门面访问通用能力；需要框架专有能力时必须显式、受控地获取原生对象，不能让框架类型重新渗入公共模块、starter 或 Magic-API 脚本边界。

选择此方案而不是直接抽取 `Object` 型注册表，是为了保留可测试的输入输出、能力校验和异常上下文；选择此方案而不是同时统一 agent/workflow，是为了避免把能力差异最大的 API 固化为最低公分母。

### 2. LangChain4j 是默认适配器，其他适配器按能力显式注册

`fw-plus-ai-langchain4j-model` 与 `fw-plus-ai-langchain4j-store-embeding` 保留 artifact 兼容入口，但不再定义公共契约。它们依赖新的中立模块并注册 LangChain4j 实现，维持现有 provider、默认模型、store key 和 `EmbeddingStore<TextSegment>` 的适配行为。

Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 各自注册独立 adapter。每个 adapter 必须声明实际支持的模型/向量能力；配置请求未支持能力时必须在启动期报告框架、provider、key 和能力类型。禁止通过空实现伪造支持，也禁止由默认 LangChain4j adapter 隐式处理其他框架的配置。

### 3. 配置先中立、后兼容映射，且中立配置优先

新增中立配置前缀 `fw-plus.ai.model` 与 `fw-plus.ai.embedding-store`，其中每个定义显式包含 `framework`、provider、能力类型、key、连接参数和扩展 properties。旧 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 继续被读取，并在未声明同 key 中立定义时映射为 `framework=langchain4j` 的中立定义。

同一 key 同时存在时，中立配置优先，启动日志必须给出可操作迁移提示但不得输出密钥。旧配置在一个兼容周期内保留；移除兼容入口只能通过后续破坏性 change 并附迁移公告完成。

### 4. starter 与 Magic-API 只依赖中立注册表

新增中立 model/store starter；每个框架 adapter starter 仅带入其 adapter、可选 SDK 和条件自动配置。原 LangChain4j starter 继续可用，并转调中立 starter 加 LangChain4j adapter，保证默认用户无需改依赖即可升级。

Magic-API model 和 vector 插件改为注入中立 registry/factory。既有 `model-datasource`、向量 store key、`model[...]` 和 `vector[...]` 访问形式维持不变；资源定义可通过可选 `framework` 指定 adapter，未指定时按已解析的默认模型/store 选择。插件返回中立门面，provider 连接逻辑只允许位于对应 adapter 中。

### 5. 切换迁移文档是交付物，不是附注

文档必须按目标框架分别给出：依赖替换、BOM/版本前置条件、配置映射、Java 注入 API 替换、Magic-API 资源与脚本影响、测试方式、回滚步骤和未支持能力。LangChain4j 到 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的示例必须使用相同业务 key，展示仅替换 adapter/starter 与 `framework` 后模型和 vector 脚本如何保持访问方式稳定。

### 6. 以契约测试隔离 SDK 差异

中立核心使用假 adapter/store 覆盖注册、默认选择、重复 key、能力不支持和异常上下文。每个真实 adapter 用 SDK mock 或本地 fake 覆盖其声明能力，禁止测试依赖真实 API key 或远程向量库。自动配置测试分别验证旧 LangChain4j 配置兼容、中立配置优先、SDK 缺失的条件退化和框架冲突。Magic-API 集成测试验证资源动态注册、按 key/default 访问及错误脱敏；Maven 测试按 Core 后 AI Boot 顺序执行。

## Risks / Trade-offs

- [Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的最低 JDK/Spring 基线高于当前项目] → 在开始实现前生成有效 POM 和依赖树并执行独立编译探针；不兼容时将 adapter 保持可选且隔离，记录支持基线，不污染默认路径。
- [中立 API 不能覆盖所有原生能力] → 仅规范模型和向量存储共同能力，adapter 声明能力，框架专有高级功能留在各自模块。
- [配置兼容映射导致同 key 行为不明确] → 定义中立配置优先、旧配置仅补全、启动期提示的固定优先级，并以测试锁定。
- [旧 artifact 过早删除破坏下游] → 旧 LangChain4j artifact 保留为薄兼容/适配模块；迁移文档提供逐步替换和回滚。
- [Magic-API 动态资源可能携带明文凭证] → 所有控制器、异常和迁移示例均按脱敏规则处理密钥，测试覆盖错误输出。

```

Full source: openspec/changes/abstract-ai-model-and-embedding/design.md

## openspec/changes/abstract-ai-model-and-embedding/tasks.md

- Source: openspec/changes/abstract-ai-model-and-embedding/tasks.md
- Lines: 1-40
- SHA256: a9c61177bb6c74e27abe901f94769183f1cd084c9ca6656b3deaf578d88018dc

```md
## 1. 依赖与兼容性基线

- [ ] 1.1 读取四层父 POM 和有效 POM，确认当前 Java、Spring Boot、Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 的版本约束。
- [ ] 1.2 为 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 分别执行最小 Maven 依赖/编译探针，记录 JDK、Spring 版本和传递依赖冲突结果。
- [ ] 1.3 根据探针结果确定各 adapter 的可发布基线、可选依赖边界和不支持能力，并更新 BOM 版本管理。

## 2. 中立 Core 模型与向量模块

- [ ] 2.1 在根 AI 聚合和 Core BOM 中新增 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 模块及 artifact 管理。
- [ ] 2.2 将模型定义、能力类型、门面、工厂、registry、异常和 adapter SPI 迁移到中立模型模块，确保公开签名不含框架类型。
- [ ] 2.3 将向量文档、向量、查询、匹配、store 门面、工厂、registry 和 adapter SPI 迁移到中立向量模块，确保公开签名不含框架类型。
- [ ] 2.4 为中立模型/store 增加重复 key、默认 key、adapter 匹配、能力不支持与异常上下文的单元契约测试。

## 3. LangChain4j 默认适配与兼容入口

- [ ] 3.1 将 `fw-plus-ai-langchain4j-model` 改造为基于中立模型 SPI 的 LangChain4j adapter，保留既有 provider、模型别名和默认 key 行为。
- [ ] 3.2 将 `fw-plus-ai-langchain4j-store-embeding` 改造为基于中立向量 SPI 的 LangChain4j adapter，并把 `EmbeddingStore<TextSegment>` 限制在该模块内部。
- [ ] 3.3 调整原 LangChain4j artifact、starter、聚合和 BOM，使既有下游依赖仍可解析且不引入其他框架 SDK。
- [ ] 3.4 为 LangChain4j adapter 增加模型/store 行为、旧配置映射和原有测试回归覆盖。

## 4. Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 适配

- [ ] 4.1 创建 Spring AI model/store adapter 与 starter，按实际 SDK 支持能力注册并覆盖缺失 SDK 的条件装配测试。
- [ ] 4.2 创建 Spring AI Alibaba model/store adapter 与 starter，复用中立 SPI 而不向公共模块泄漏 Alibaba 类型。
- [ ] 4.3 创建 AgentScope 2 Java 进程内 SDK model/store adapter 与 starter，显式拒绝 SDK 未提供的能力。
- [ ] 4.4 为三个 adapter 使用 mock 或本地 fake 编写契约测试，禁止测试依赖真实密钥、模型服务或向量数据库。

## 5. AI Boot、starter 与 Magic-API 集成

- [ ] 5.1 新增中立 model/store starter 与属性绑定，并实现中立配置优先、旧 LangChain4j 配置兼容映射和敏感字段脱敏。
- [ ] 5.2 改造 AI Boot 自动配置，使其仅依赖中立 registry/factory，并覆盖多 framework、默认 key 和不支持能力的启动期错误。
- [ ] 5.3 改造 Magic-API model/vector 插件和动态资源注册器，使其返回中立门面并保持 `model[...]`、`vector[...]`、资源 key 和默认访问兼容。
- [ ] 5.4 为 Magic-API model/vector 插件增加动态资源、按 key/default 访问、adapter 缺失和错误脱敏的集成测试。

## 6. 迁移文档与端到端验证

- [ ] 6.1 编写从 LangChain4j 切换到 Spring AI 的迁移指南，包含依赖、配置、Java API、Magic-API、测试与回滚示例。
- [ ] 6.2 编写从 LangChain4j 切换到 Spring AI Alibaba 和 AgentScope 2 Java 的迁移指南，并逐项声明运行基线和能力差异。
- [ ] 6.3 为旧配置、中立配置和同 key 冲突配置补齐自动配置回归测试，验证中立配置优先且旧路径可回滚。
- [ ] 6.4 按 Core → AI Boot → sample 的依赖顺序执行 Maven 编译与相关测试，记录有效 POM、依赖树和测试结果。

```

## openspec/changes/abstract-ai-model-and-embedding/specs/ai-abstraction-contract-testing/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/ai-abstraction-contract-testing/spec.md
- Lines: 1-34
- SHA256: 0fd7e0fc11f21751241da103ae8f724e0fe7d15c8314f1686b5b80684517d195

```md
## ADDED Requirements

### Requirement: 中立抽象、默认 adapter 与 Java 17 starter 骨架必须具备自动化测试
系统 SHALL 为中立模型/store 核心、LangChain4j 默认 adapter 以及 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的 Java 17 starter 骨架提供不依赖真实 API key 或远程向量库的自动化测试。

#### Scenario: 核心契约测试覆盖注册与失败路径
- **WHEN** 执行中立核心模块测试
- **THEN** 测试 SHALL 覆盖按 key/default 获取、重复 key、无效默认 key、adapter 不支持能力和异常上下文。

#### Scenario: LangChain4j 默认 adapter 验证完整声明能力
- **WHEN** 执行 LangChain4j adapter 测试
- **THEN** 测试 SHALL 使用 SDK mock 或本地 fake 验证其声明支持的模型和向量能力。
- **AND** 测试 SHALL 验证未声明能力被明确拒绝。

#### Scenario: Java 17 starter 骨架验证发布边界
- **WHEN** 执行 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的首版测试
- **THEN** 测试 SHALL 验证 Java 17 编译、Spring Boot 3 条件装配、Java 8 依赖隔离和未实现 adapter 的明确诊断。
- **AND** 测试 SHALL 不把真实 SDK model/store 契约伪装为已通过。

### Requirement: 自动配置和 Magic-API 兼容性必须回归验证
系统 SHALL 对配置兼容层、starter 条件装配和 Magic-API model/vector 插件建立集成测试。

#### Scenario: 旧配置和中立配置优先级受测试保护
- **WHEN** 分别加载旧 LangChain4j 配置、仅中立配置和同 key 混合配置
- **THEN** 测试 SHALL 验证对应 registry 内容和中立配置优先级。

#### Scenario: Magic-API 使用中立 registry
- **WHEN** Magic-API 注册模型或向量动态资源并通过 key/default 调用插件
- **THEN** 测试 SHALL 验证插件只依赖中立 registry/factory。
- **AND** 错误响应和日志 SHALL 不包含密钥。

#### Scenario: 分层 Maven 构建通过
- **WHEN** 按 Core 后 AI Boot 的依赖顺序执行 Maven Wrapper 测试
- **THEN** 新增模块、LangChain4j 兼容路径和相关自动配置测试 SHALL 编译并通过。

```

## openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-adapter-integration/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-adapter-integration/spec.md
- Lines: 1-27
- SHA256: 9c0618a224ddf6eb8c6e2edadb1be1fe4aac29704d7e17cdba13cdfa438d994d

```md
## ADDED Requirements

### Requirement: 框架 adapter 以独立可选 starter 集成
系统 SHALL 为 LangChain4j 提供完整默认 adapter/starter，并为 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 提供 Java 17+ / Spring Boot 3+ 独立 Maven artifact 与可验证 starter 骨架；中立 starter 只依赖中立核心模块。

#### Scenario: 默认 LangChain4j 路径不引入其他 SDK
- **WHEN** 应用只引入既有 LangChain4j starter 或兼容 artifact
- **THEN** LangChain4j SHALL 作为默认完整实现完成模型和向量 store 装配。
- **AND** Spring AI、Spring AI Alibaba 和 AgentScope 2 Java SDK 不得被传递引入。

#### Scenario: Java 17 目标框架 starter 骨架隔离
- **WHEN** 构建 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的首版 starter artifact
- **THEN** 它 SHALL 使用 Java 17+ / Spring Boot 3+ 独立 parent。
- **AND** 它 SHALL 不进入 Java 8 默认 LangChain4j 路径的传递依赖图。

#### Scenario: 首版目标 adapter 未实现时拒绝启用
- **WHEN** 应用尝试使用首版 Java 17 artifact 启用 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的原生 model/store adapter
- **THEN** 条件自动配置 SHALL 不创建半初始化 Bean。
- **AND** 启动诊断 SHALL 指出该 artifact 当前仅提供 starter 骨架、真实 adapter 尚未实现及迁移文档入口。

### Requirement: 同一业务 key 为后续 adapter 切换保留稳定契约
系统 SHALL 让中立定义中的 `framework`、中立 registry key 和 Magic-API 访问 key 保持稳定，使后续真实 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java adapter 可在不改变业务 key 的前提下接入。

#### Scenario: 真实目标 adapter 实现后保持业务 key
- **WHEN** 后续 change 实现 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的真实进程内 SDK adapter
- **THEN** 中立定义 SHALL 允许以 `framework` 选择该 adapter。
- **AND** 既有模型/store key、`model[...]` 和 `vector[...]` 访问方式 SHALL 不要求改变。

```

## openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-migration-guide/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-migration-guide/spec.md
- Lines: 1-26
- SHA256: dcfc5173a43a9b3cdcc26db4130737921aebe7eb301c77800272d097129f3291

```md
## ADDED Requirements

### Requirement: 提供从 LangChain4j 切换到目标框架的迁移指南
系统 SHALL 在项目文档中提供从 LangChain4j 切换到 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 的独立迁移章节。

#### Scenario: 文档覆盖完整切换步骤
- **WHEN** 开发者阅读任一目标框架的迁移章节
- **THEN** 文档 SHALL 说明 Maven artifact/BOM、JDK 与 Spring 基线、starter 选择、中立配置映射、Java registry/API 替换、Magic-API 资源与脚本影响、测试命令和回滚步骤。
- **AND** 文档 SHALL 明确列出目标 adapter 未支持的能力。

#### Scenario: 文档使用稳定业务 key 示例
- **WHEN** 文档展示从 LangChain4j 切换的配置示例
- **THEN** 示例 SHALL 以同一个模型或 store key 展示旧配置、框架无关配置和目标框架选择。
- **AND** 示例 SHALL 说明 `model[...]`、`vector[...]` 与既有动态资源 key 如何保持稳定。

### Requirement: 保持旧 LangChain4j 配置的迁移兼容
系统 SHALL 在兼容周期内继续读取 `fw-plus.ai.langchain4j.model` 和 `fw-plus.ai.langchain4j.embedding-store`。

#### Scenario: 仅存在旧配置
- **WHEN** 应用只提供旧 LangChain4j 配置
- **THEN** 系统 SHALL 将其映射为 `framework=langchain4j` 的中立定义并保持原有行为。

#### Scenario: 中立配置与旧配置存在相同 key
- **WHEN** 同一 key 同时存在中立配置和旧 LangChain4j 配置
- **THEN** 中立配置 SHALL 优先。
- **AND** 系统 SHALL 输出不含敏感字段的迁移提示。

```

## openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-neutral-embedding-store-abstraction/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-neutral-embedding-store-abstraction/spec.md
- Lines: 1-30
- SHA256: 37168f5e915436b61a9e951c6274159698838e924174db47e4cc466a1514602d

```md
## ADDED Requirements

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

```

## openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-neutral-model-abstraction/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/ai-framework-neutral-model-abstraction/spec.md
- Lines: 1-32
- SHA256: 21fb36ca3a99657dd0b12b2d0078947d220a22a8688968958cc79a3e6588ab8c

```md
## ADDED Requirements

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

```

## openspec/changes/abstract-ai-model-and-embedding/specs/langchain4j-embedding-store-starter/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/langchain4j-embedding-store-starter/spec.md
- Lines: 1-19
- SHA256: e091108501e4e197af7dc2e9354026019e47904e46fda8b8bf8ca54bbd1ec535

```md
## MODIFIED Requirements

### Requirement: LangChain4j 向量 store starter 作为中立向量抽象的默认适配
系统 SHALL 提供 `fw-plus-ai-store-embedding-spring-boot-starter` 作为中立向量 store starter，并保留 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 作为默认 LangChain4j adapter 和兼容入口。

#### Scenario: 聚合和 BOM 注册中立 starter 与 LangChain4j starter
- **WHEN** 检查 Core AI 聚合和 Core BOM
- **THEN** 它们 SHALL 注册中立 store 模块/starter 与既有 LangChain4j store starter。
- **AND** 既有 LangChain4j starter artifact SHALL 继续可解析。

#### Scenario: 既有 LangChain4j store 配置保持行为
- **WHEN** 应用继续使用 LangChain4j 向量 store 配置并引入既有 starter
- **THEN** 系统 SHALL 通过 LangChain4j adapter 注册对应中立 store。
- **AND** 既有 provider、多实例、重复 key 和默认 store 校验行为 SHALL 保持。

#### Scenario: 原生 EmbeddingStore 不跨越中立边界
- **WHEN** 检查中立 starter、自动配置和 Magic-API vector 插件
- **THEN** 它们 SHALL 不要求 `EmbeddingStore<TextSegment>` 作为公开 Bean 参数或返回类型。
- **AND** 该原生类型只允许存在于 LangChain4j adapter 内部。

```

## openspec/changes/abstract-ai-model-and-embedding/specs/langchain4j-model-module/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/langchain4j-model-module/spec.md
- Lines: 1-24
- SHA256: 6811e5da89fdfde9a30eefc90e7eba5998f111997245b8fa42b69e68aedb6492

```md
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

```

## openspec/changes/abstract-ai-model-and-embedding/specs/langchain4j-starter-modules/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/langchain4j-starter-modules/spec.md
- Lines: 1-14
- SHA256: a19746f316a238b26f83294ee33395019f5e70f92ca997760836911cb64c1538

```md
## MODIFIED Requirements

### Requirement: LangChain4j starter 模块只承载 LangChain4j 专用能力
系统 SHALL 保留既有 LangChain4j provider/vector-store starter 模块，但框架通用模型和向量 store 契约 SHALL 由根 AI 聚合下的中立模块和 starter 提供。

#### Scenario: LangChain4j 聚合保持现有专用 starter
- **WHEN** 检查 `fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它 SHALL 继续列出既有 LangChain4j core、OpenAI、provider/vector-store 和兼容 store starter 模块。
- **AND** 新增的框架无关模型/store 模块 SHALL 位于根 AI 聚合而不是 LangChain4j 聚合。

#### Scenario: 默认 LangChain4j starter 组合中立核心和适配器
- **WHEN** 应用引入 LangChain4j 模型或向量 store starter
- **THEN** starter SHALL 引入所需中立核心/中立 starter以及对应 LangChain4j adapter。
- **AND** starter SHALL 不传递 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java SDK。

```

## openspec/changes/abstract-ai-model-and-embedding/specs/magicapi-unified-vector-plugin/spec.md

- Source: openspec/changes/abstract-ai-model-and-embedding/specs/magicapi-unified-vector-plugin/spec.md
- Lines: 1-19
- SHA256: 0f83d6d2e02e3acb8dcf0cf4456aa5a8611001d7f7d4245da0e6270392763dec

```md
## MODIFIED Requirements

### Requirement: 统一插件复用中立多向量 registry
系统 SHALL 让统一 Magic-API vector 插件委托框架无关的多向量 registry/factory 获取向量存储实例，而不直接依赖 LangChain4j `EmbeddingStore<TextSegment>` 或其 factory/registry。

#### Scenario: 插件不重复构造 provider 或框架连接
- **WHEN** 插件需要访问 Milvus、Qdrant、RedisStack、Weaviate 或其他已注册向量 store
- **THEN** 插件 SHALL 通过中立 registry/factory 获取中立 store 门面。
- **AND** 插件自身不得复制 provider 或框架原生连接构建逻辑。

#### Scenario: 旧脚本按 key 和默认 store 继续访问
- **WHEN** Magic-API 脚本使用 `vector['knowledge']` 或未指定 key 的默认访问方式
- **THEN** 插件 SHALL 使用相同 key 或默认 store 返回对应中立操作对象。
- **AND** 资源定义未指定 `framework` 时 SHALL 保持已解析默认 store 的行为。

#### Scenario: provider 配置异常
- **WHEN** 脚本访问的向量 store key 未注册、framework adapter 缺失或 provider 配置无效
- **THEN** 插件 SHALL 返回包含缺失 key、framework 或 provider 的明确 Magic-API 运行时错误。
- **AND** 错误不得暴露凭证。

```
