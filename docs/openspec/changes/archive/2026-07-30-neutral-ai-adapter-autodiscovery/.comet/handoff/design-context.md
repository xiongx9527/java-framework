# Comet Design Handoff

- Change: neutral-ai-adapter-autodiscovery
- Phase: design
- Mode: compact
- Context hash: 620c57bbe78755061116165f1742fd0d55ff17b9727458facde6ece9c3ca82a0

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/neutral-ai-adapter-autodiscovery/proposal.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/proposal.md
- Lines: 1-31
- SHA256: 609781ec39668ea04846189f367351549d97fcffbfc931454cd5e05d90d9a07f

```md
## Why

当前中立 model/store starter 和 AI Boot 自动配置仍直接依赖、装配 LangChain4j，旧的 model/store 模块及 starter 也继续作为公开坐标存在。这违反了框架无关边界，使运行时不能根据已安装的 adapter 安全选择框架，也误导后续 Java 17 adapter 的接入方式。

## What Changes

- **BREAKING** 删除 4 个旧 LangChain4j model/store artifact 及其聚合、BOM、starter、Magic-API 和样例引用。
- 新增统一、可选的 LangChain4j adapter 模块，作为中立 API 的默认 Java 8 实现；它是唯一允许引用 LangChain4j 类型的实现层。
- 中立 Core 模块、中立 starter、AI Boot 公共自动配置和 Magic-API 只依赖中立 API，不传递或引用 LangChain4j、Spring AI、Spring AI Alibaba、AgentScope 类型或配置前缀。
- 新增 adapter 自动发现和选择规则：显式 `framework` 必须匹配已发现 adapter；未显式配置时仅一个候选可自动选用，零个或多个候选必须启动失败。
- **BREAKING** 重写迁移文档，说明旧坐标删除、依赖/配置替换、Magic-API 保持边界，以及 Java 17 / Boot 3.x adapter 仍未实现。

## Capabilities

### New Capabilities

- `ai-adapter-autodiscovery`: 定义中立模型/向量 adapter 的 classpath 发现、唯一候选自动选择和多候选失败规则。
- `ai-neutral-starter-boundary`: 规定中立 starter、公共自动配置和 Magic-API 不得携带任何具体 AI 框架依赖或类型。
- `ai-framework-breaking-migration-guide`: 提供旧 LangChain4j artifact 移除后的迁移、失败诊断和 Java 17 后续实现边界文档。

### Modified Capabilities

- `ai-framework-neutral-model-abstraction`: 强化模型 Core API 的零框架痕迹与 adapter 发现契约。
- `ai-framework-neutral-embedding-store-abstraction`: 强化向量 Core API 的零框架痕迹与 adapter 发现契约。
- `langchain4j-model-module`: 删除旧模型模块，改由统一 LangChain4j adapter 承担实现。
- `langchain4j-embedding-store-starter`: 删除旧向量 store 模块和 starter，改由统一 LangChain4j adapter 承担实现。
- `langchain4j-starter-modules`: 删除旧 model/store starter 聚合和 BOM 坐标。
- `magicapi-unified-vector-plugin`: 保持中立 facade，并使用自动发现后的中立 registry。
- `ai-framework-migration-guide`: 从兼容迁移改为破坏性坐标/配置迁移说明。

## Impact

```

## openspec/changes/neutral-ai-adapter-autodiscovery/design.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/design.md
- Lines: 1-74
- SHA256: 4227f0d325246a4cef68e20ca3ea7a5f514c1d544727b1fbeed5ee11ca96170a

```md
## Context

上一轮抽象已将模型和向量存储的业务契约放入 Java 8 的 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`。但当前 AI Boot 自动配置仍在同一装配路径中直接创建 LangChain4j registry、factory 与原生 `EmbeddingStore<TextSegment>`，中立 starter 也传递旧 LangChain4j 模块。四个旧 artifact 仍被 BOM、聚合 POM、Magic-API、AskData starter 和样例引用。

这使中立模块的依赖边界与运行时行为不一致：声明 `framework` 时不能明确验证适配器，未声明时也不能安全决定默认实现。未来的 Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 需要 Java 17 / Spring Boot 3.x；把它们加入当前 Java 8 / Boot 2.7 reactor 会破坏基线。

## Goals / Non-Goals

**Goals:**

- 删除 4 个旧 LangChain4j model/store artifact 及其全部聚合、BOM、starter、Magic-API、AskData 和样例引用。
- 提供 `fw-plus-ai-langchain4j-adapter`，作为唯一包含 LangChain4j 类型的 Java 8 默认实现，并通过独立 adapter starter 显式安装。
- 使模型与向量 store 的中立配置支持 adapter 发现：显式 framework 精确匹配；缺省 framework 时仅唯一候选可用；零或多候选必须失败。
- 保证中立 Core、中立 starter、公共自动配置和 Magic-API 只依赖中立 registry/SPI，不出现具体 AI 框架的依赖、类型、包名或配置前缀。
- 以可执行测试和中文迁移文档覆盖移除、自动选择、错误分支、Magic-API 与回滚；记录 Java 17 / Boot 3.x 的后续实现蓝图。

**Non-Goals:**

- 本次不实现 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java adapter，不升级 Java 8 / Spring Boot 2.7 基线，也不引入跨进程 AgentScope 协议。
- 不兼容旧 artifact 或 `fw-plus.ai.langchain4j.*` 配置；这是一次明确的破坏性迁移。
- 不改变具体 LangChain4j provider SDK 的现有职责，也不重写 Magic-API 的脚本语义。

## Decisions

### 1. 统一 LangChain4j adapter 替代两个旧 Core artifact

在 `fw-plus-ai-langchain4j` 聚合下新增 `fw-plus-ai-langchain4j-adapter`，承载原 `fw-plus-ai-langchain4j-model` 与 `fw-plus-ai-langchain4j-store-embeding` 的 LangChain4j 对象转换、provider 构造和中立 SPI 实现。新增 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 显式引入 adapter 与其隔离的自动配置；应用不再通过中立 starter 隐式获得 LangChain4j。

选择该方案而不是保留兼容 POM，是因为保留旧坐标会持续给调用方传递 LangChain4j 依赖，并无法满足“旧模块不应存在”的发布边界。选择单一 adapter 而不是 model/store 两个 adapter，可将一套框架生命周期、条件装配与版本管理集中维护。

### 2. 在中立 SPI 中定义可发现 framework，而非以 provider 推断框架

`AiModelAdapter` 与 `AiEmbeddingStoreAdapter` 扩展为能够声明其 framework 标识和支持能力的 SPI；中立工厂/选择器接收当前 classpath 已发现的 adapter 集合。选择算法对模型和 store 一致：

1. 定义显式给出 `framework` 时，只保留同名 adapter；不存在或无法支持 provider/type 时以包含 key、framework、provider、type 的受控异常失败。
2. 定义未给出 `framework` 时，先按 provider/type 计算候选；恰好一个候选时使用该 adapter，并将实际 framework 写入诊断元数据；零个或多个候选时启动失败，错误列出候选 framework。
3. 不以 adapter 排序、Bean 名称或 LangChain4j 作为隐式优先级，不回退到空实现。

这比“始终默认 LangChain4j”更安全：LangChain4j 仍是只安装一个 adapter 时的默认实现，但应用显式安装多个 adapter 后必须消除歧义。

### 3. 拆分中立与框架特定 Spring Boot 装配

中立 properties、registry、factory、动态 Magic-API 数据源只保留 `fw-plus.ai.model`、`fw-plus.ai.embedding-store` 与中立 API。LangChain4j adapter 的 properties、原生 Bean 适配和 provider 逻辑放入 adapter starter 的自动配置，条件为 adapter 类与 LangChain4j SDK 同时存在。公共自动配置不得导入、反射加载或以字符串检测任何具体 AI 框架。

替代方案是在现有 LangChain4j 自动配置中继续桥接旧配置。该方案被拒绝，因为会令中立装配仍需 LangChain4j，并使之后的 Java 17 adapter 复制桥接代码。

### 4. Magic-API 只消费中立 registry

model 与 vector 脚本模块、动态资源、控制器和 legacy support 一律使用 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 及中立门面。资源定义中的 `framework` 交给选择器处理；Magic-API 不创建 LangChain4j 原生对象，也不依赖 adapter 包。无 framework 的资源仅在唯一候选时可创建。

### 5. 将 Java 17 / Boot 3 作为独立后续 reactor

文档定义未来 `java17` / Boot 3.x 线：独立 parent/BOM/reactor，使用 `jakarta.*`、Spring Boot 3 自动配置注册方式和 Java 17 编译级别；每个 Spring AI、Spring AI Alibaba、AgentScope 2 Java adapter 都实现相同中立 SPI。AgentScope 2 采用进程内 Java SDK，业务层决定是否启用其 agent/workflow 能力。该路线不向当前 Java 8 artifact 回传依赖。

## Risks / Trade-offs

- [删除 artifact 会导致旧依赖立即解析失败] → 文档给出逐项坐标替换、配置替换、回滚至前一发布版本和 Maven 依赖树诊断命令。
- [多个 adapter 让缺省 framework 的历史配置失败] → 错误列出候选，文档要求在定义中显式写 framework；单 adapter 部署保持自动选择。
- [provider 构造逻辑从旧模块迁移时遗漏能力] → 先补充 RED 测试，逐项迁移 model/store provider、默认 key、多实例和异常上下文，再删除旧目录。
- [Magic-API 仍隐式持有原生类型] → 编译依赖、源码导入和集成测试三层检查；测试从中立 registry 注入假实现。
- [未来 Java 17 adapter 与 Java 8 二进制混用] → 文档明确分仓库 reactor 与版本线，不承诺当前 Boot 2 应用可直接加载 Java 17 adapter。

## Migration Plan

1. 在 Core BOM、根 AI 聚合与 LangChain4j 聚合中注册统一 adapter，先建立中立 SPI 自动选择测试。
2. 将 LangChain4j model/store 的 provider 实现与自动配置迁入 adapter 与 adapter starter，迁移 Magic-API、AskData starter、BOM 和样例依赖。
3. 删除四个旧模块目录和所有坐标、旧配置绑定、兼容桥接代码；在构建中添加禁止旧 artifact/前缀/导入的断言。
4. 发布迁移文档：应用显式添加 adapter starter；把旧 `fw-plus.ai.langchain4j.*` 配置改为中立 key；多 adapter 时指定 framework。
5. 先运行 Core reactor 测试，再运行 AI Boot 自动配置与 Magic-API/AskData 目标测试；在新版本不可用时回退到包含旧模块的上一发行版本，而不是混装新旧 artifact。

## Open Questions

- Spring AI、Spring AI Alibaba 与 AgentScope 2 Java adapter 的 artifact 命名、BOM 版本和 provider 能力矩阵在 Java 17 / Boot 3 change 中确定。
- 当前 AskData 直接使用 LangChain4j 的内部路径是否也应在同一后续 change 中改为中立模型门面；本次仅消除对已删除 artifact 的编译依赖并保持当前 Java 8 行为。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/tasks.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/tasks.md
- Lines: 1-37
- SHA256: 16c9fd4e457bcc144acf35c9605b7b977660524b2f10447156771a50e6f9b99c

```md
## 1. 基线与回归测试准备

- [ ] 1.1 盘点四个旧 artifact 在 Core/AI BOM、聚合 POM、starter、Magic-API、AskData、样例和文档中的引用，建立删除清单。
- [ ] 1.2 为模型和向量 store adapter 选择器编写 RED 单元测试：显式 framework 匹配、唯一候选、零候选和多候选。
- [ ] 1.3 为中立 starter/公共自动配置/Magic-API 的零具体框架依赖、导入、公开签名和配置前缀建立静态回归测试。

## 2. 中立 SPI 与选择器

- [ ] 2.1 扩展 `AiModelAdapter` 与 `AiEmbeddingStoreAdapter` 的 framework/能力声明，并保持 Java 8 API。
- [ ] 2.2 实现中立模型 adapter 选择与受控异常上下文，令缺省 framework 仅接受唯一候选。
- [ ] 2.3 实现中立向量 store adapter 选择与受控异常上下文，令缺省 framework 仅接受唯一候选。
- [ ] 2.4 使中立 properties、factory、registry 与默认 key 校验不依赖任何具体 AI framework，并将第 1 组 RED 测试转为 GREEN。

## 3. LangChain4j 统一适配层

- [ ] 3.1 新建 `fw-plus-ai-langchain4j-adapter`，迁移旧 model/store 的 LangChain4j 对象转换、provider 构造和中立 SPI 实现。
- [ ] 3.2 新建 LangChain4j adapter starter/隔离自动配置，使 LangChain4j SDK 与 adapter 均在 classpath 时才注册实现。
- [ ] 3.3 保持现有 LangChain4j provider、多模型别名、多个 store、默认 key 与错误上下文行为，并为 adapter 编写目标测试。

## 4. 中立 Boot 与 Magic-API 边界

- [ ] 4.1 将中立 model/store properties 与公共自动配置迁出 LangChain4j 包和依赖路径。
- [ ] 4.2 重构 Magic-API model/vector 动态资源、控制器和 legacy support，使其只使用中立 registry/门面并复用 adapter 选择规则。
- [ ] 4.3 更新 AskData starter 与相关测试，移除对已删除 model/store artifact 的编译依赖并保留 Java 8 行为。
- [ ] 4.4 执行并补齐 Magic-API 与自动配置集成测试：唯一 LangChain4j adapter、多个 adapter 歧义、显式 framework 和无原生类型暴露。

## 5. 构建坐标与破坏性清理

- [ ] 5.1 从 Core/AI 聚合、Core/AI Boot BOM、starter 聚合、Magic-API 与样例 POM 中移除四个旧 artifact，注册统一 adapter 与 starter。
- [ ] 5.2 删除 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding` 及两个旧 starter 目录和所有旧自动配置/兼容桥接代码。
- [ ] 5.3 验证仓库不再包含旧 artifact、`fw-plus.ai.langchain4j.model`、`fw-plus.ai.langchain4j.embedding-store` 或中立模块中的具体框架痕迹。

## 6. 迁移文档与验证

- [ ] 6.1 编写中文破坏性迁移指南：旧坐标/配置映射、LangChain4j adapter 安装、自动发现错误诊断、Magic-API key 边界、回滚限制和测试命令。
- [ ] 6.2 编写 Java 17 / Spring Boot 3.x 后续实施蓝图：独立 reactor/BOM、Jakarta 迁移、Spring AI、Spring AI Alibaba、AgentScope 2 进程内 SDK adapter SPI 与测试要求。
- [ ] 6.3 运行 Core 目标 reactor、AI Boot 自动配置、Magic-API/AskData 目标测试、OpenSpec 严格校验和 Maven 依赖/源码边界检查，记录结果。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-adapter-autodiscovery/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-adapter-autodiscovery/spec.md
- Lines: 1-24
- SHA256: 1e5d9f3e3bc04e9f49bb3ed011d2a29f23c2954b5f18557e9ebae985a67be198

```md
## ADDED Requirements

### Requirement: 中立 AI adapter 的自动发现与确定性选择
系统 SHALL 从应用 classpath 与 Spring 容器发现已安装的 `AiModelAdapter` 和 `AiEmbeddingStoreAdapter`，并对每个模型或向量 store 定义独立选择 adapter。

#### Scenario: 显式 framework 匹配已安装 adapter
- **WHEN** 定义给出 `framework` 且同名 adapter 支持其 provider 与 type
- **THEN** 系统 SHALL 使用该 adapter 创建中立门面。
- **AND** 注册表元数据 SHALL 保留显式 framework。

#### Scenario: 显式 framework 没有匹配 adapter
- **WHEN** 定义给出 `framework` 但 adapter 不存在、SDK 缺失或不支持 provider/type
- **THEN** 应用启动 SHALL 失败。
- **AND** 错误 SHALL 包含 key、framework、provider、type 和已发现 framework，且不得包含凭证。

#### Scenario: 缺省 framework 只有一个候选
- **WHEN** 定义未给出 `framework` 且仅一个已发现 adapter 支持其 provider 与 type
- **THEN** 系统 SHALL 自动选择该 adapter。
- **AND** LangChain4j 是仅安装 LangChain4j adapter 时的默认实现。

#### Scenario: 缺省 framework 没有或存在多个候选
- **WHEN** 定义未给出 `framework` 且候选数量为零或大于一
- **THEN** 应用启动 SHALL 失败而不得按排序或 Bean 名称选择。
- **AND** 错误 SHALL 指示用户安装 adapter 或显式配置 framework。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-breaking-migration-guide/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-breaking-migration-guide/spec.md
- Lines: 1-18
- SHA256: 83b5def95287e139bc70abe62569d4155ec9b3032045647121000970cbdd5c06

```md
## ADDED Requirements

### Requirement: 破坏性 AI framework 迁移文档
系统 SHALL 提供中文迁移文档，明确旧 LangChain4j model/store 坐标和旧配置已删除，并说明如何安装 adapter、配置 framework、诊断选择失败和回滚。

#### Scenario: 文档列出删除的 artifact 与替代关系
- **WHEN** 开发者从旧版本升级
- **THEN** 文档 SHALL 列出四个被删除 artifact、统一 LangChain4j adapter 及其 starter 的替代坐标。
- **AND** 文档 SHALL 明确新旧 artifact 不得混用。

#### Scenario: 文档说明自动发现和多 adapter 失败
- **WHEN** 开发者使用缺省 framework 或同时安装多个 adapter
- **THEN** 文档 SHALL 说明唯一候选自动选择、零候选和多候选启动失败，以及显式 framework 的修复示例。

#### Scenario: 文档限定 Java 17 / Boot 3 后续实现
- **WHEN** 开发者评估 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java
- **THEN** 文档 SHALL 说明其 adapter 本次未实现，必须在独立 Java 17 / Spring Boot 3.x reactor 中实现。
- **AND** 文档 SHALL 说明 AgentScope 2 Java 使用进程内 SDK，业务决定是否采用其能力。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-migration-guide/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-migration-guide/spec.md
- Lines: 1-21
- SHA256: 0c96337b309320601f1b205ae05d0aa46d3c48123cebcd02f0fd918e1c419f6b

```md
## REMOVED Requirements

### Requirement: 保持旧 LangChain4j 配置的迁移兼容
**Reason**: 旧 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 配置及其四个依赖坐标将被明确删除；继续静默读取会掩盖不兼容升级。

**Migration**: 将配置迁移到 `fw-plus.ai.model` 和 `fw-plus.ai.embedding-store`，显式安装所需 adapter，并在多个 adapter 可用时配置 framework。需要无代码回滚时使用包含旧模块的上一发布版本。

## ADDED Requirements

### Requirement: 文档说明破坏性升级与 Java 17 后续实施
系统 SHALL 用中文文档说明被删除 artifact、配置前缀、替代 adapter、自动发现失败诊断、Magic-API key 保持边界、测试命令和回滚限制，并提供 Java 17 / Spring Boot 3.x adapter 的实施蓝图。

#### Scenario: 文档提供可执行的 LangChain4j 升级路径
- **WHEN** 开发者从旧 LangChain4j model/store 模块升级
- **THEN** 文档 SHALL 给出旧依赖和配置到统一 adapter/中立配置的逐项映射。
- **AND** 文档 SHALL 说明 `model[...]`、`vector[...]` 的业务 key 可保持稳定但框架原生 API 不兼容。

#### Scenario: 文档隔离 Java 17 适配器实现
- **WHEN** 开发者计划接入 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java
- **THEN** 文档 SHALL 指定独立 Java 17 / Boot 3.x reactor、BOM、`jakarta.*` 迁移和 adapter SPI 测试要求。
- **AND** 文档 SHALL 声明当前 Java 8 / Boot 2.7 release 不提供这些 adapter。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-neutral-embedding-store-abstraction/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-neutral-embedding-store-abstraction/spec.md
- Lines: 1-19
- SHA256: 61581c2d85030fe75cb55d31d36d23433e15bcb6219fff0a84d508a1205e7d5e

```md
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

#### Scenario: store adapter 选择失败
- **WHEN** 默认 store key 无效、没有 adapter、多个 adapter 或显式 framework 无法匹配
- **THEN** 应用启动 SHALL 失败。
- **AND** 错误 SHALL 包含无效 key 或选择上下文且不得泄漏凭证。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-neutral-model-abstraction/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-framework-neutral-model-abstraction/spec.md
- Lines: 1-19
- SHA256: 8f15e1f642cb505e781ec836c25151faaad656c63c9b4a98e47b5db61f122404

```md
## MODIFIED Requirements

### Requirement: 提供框架无关的 AI 模型核心模块
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai` 下提供 Java 8 兼容的 `fw-plus-ai-model`，承载模型定义、能力类型、统一模型门面、工厂、注册表、异常和不含具体框架类型的 adapter SPI。模型定义的 `framework` 可显式指定；未指定时 SHALL 按已发现的 adapter 执行唯一候选选择。

#### Scenario: 公共 API 不泄漏框架类型
- **WHEN** 检查 `fw-plus-ai-model` 的编译依赖和公开签名
- **THEN** 它不得依赖或暴露 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 类型。
- **AND** 它仅使用 Java 8 兼容的语言和集合 API。

#### Scenario: 模型定义有唯一候选 adapter
- **WHEN** 模型定义未提供 framework 且一个已发现 adapter 支持其 provider 和能力
- **THEN** 工厂 SHALL 使用该 adapter 创建并注册中立模型门面。
- **AND** 未提供 key 时 registry SHALL 获取默认模型。

#### Scenario: 模型定义的 adapter 选择不确定
- **WHEN** 没有 adapter、多个 adapter 或显式 framework 没有可用 adapter
- **THEN** 系统 SHALL 抛出受控模型异常。
- **AND** 异常 SHALL 包含 key、framework、provider、能力类型和候选信息。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-neutral-starter-boundary/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/ai-neutral-starter-boundary/spec.md
- Lines: 1-19
- SHA256: 2729e16bed6d0bcb21f86d7ea37168c2f8be0fb19f2611dd5b0865d21a4da3b0

```md
## ADDED Requirements

### Requirement: 中立 starter 与公共运行时保持具体框架零痕迹
系统 SHALL 使 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、其所有中立 starter、公共自动配置和 Magic-API model/vector 实现只依赖中立 API 与 registry。

#### Scenario: 中立 artifact 没有具体框架依赖或公开类型
- **WHEN** 检查中立 artifact 的 POM、编译依赖、公开签名和自动配置
- **THEN** 它们不得包含 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 类型、依赖、包引用或配置前缀。
- **AND** 它们 SHALL 保持 Java 8 兼容。

#### Scenario: 应用显式安装 LangChain4j adapter
- **WHEN** 应用需要 LangChain4j 实现
- **THEN** 它 SHALL 显式引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 或等价 adapter artifact。
- **AND** 中立 starter 本身不得隐式传递该 adapter。

#### Scenario: Magic-API 使用中立 registry
- **WHEN** Magic-API 创建或访问 model/vector 动态资源
- **THEN** 它 SHALL 仅通过中立 registry 与门面执行操作。
- **AND** 它不得创建或暴露框架原生 model、document 或 embedding store 类型。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/langchain4j-embedding-store-starter/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/langchain4j-embedding-store-starter/spec.md
- Lines: 1-21
- SHA256: c6d10b30d2ae42c192144384dbb606dd81cc5589f60fe591562d92cc7a2de285

```md
## REMOVED Requirements

### Requirement: 多向量存储 starter
**Reason**: `fw-plus-ai-langchain4j-store-embeding` 与 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 将被统一 LangChain4j adapter 替代，不能继续作为兼容坐标存在。

**Migration**: 应用改为引入统一 LangChain4j adapter starter，并使用 `fw-plus.ai.embedding-store` 中立配置。多个 adapter 同时存在时必须显式设置 `framework=langchain4j`。

### Requirement: 支持多个向量实例
**Reason**: 多实例能力已由中立 `AiEmbeddingStoreRegistry` 承担，不应继续通过旧原生 registry 暴露。

**Migration**: 通过中立 registry 按稳定 key 获取 store。

### Requirement: 支持默认向量存储
**Reason**: 默认 store 由中立配置与 registry 统一校验。

**Migration**: 使用中立 `default-store`，不要引用旧 `EmbeddingStoreRegistry`。

### Requirement: provider adapter 可扩展
**Reason**: provider 构造逻辑迁入统一 LangChain4j adapter。

**Migration**: 使用中立 SPI 和 adapter 内部 provider 实现，不在 Magic-API 或公共自动配置中构造原生 store。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/langchain4j-model-module/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/langchain4j-model-module/spec.md
- Lines: 1-16
- SHA256: fdcc4b4bffe1dd310a14814e12a2fb3f7708455251587720d0e799b6f9b70640

```md
## REMOVED Requirements

### Requirement: 提供 core 侧 LangChain4j 模型抽象模块
**Reason**: `fw-plus-ai-langchain4j-model` 会继续暴露框架专属的公开坐标和兼容入口，违反统一 adapter 与中立 starter 的依赖边界。

**Migration**: 删除该 artifact；应用改为显式引入 `fw-plus-ai-langchain4j-adapter` 及其 Spring Boot starter。模型定义改用 `fw-plus.ai.model` 中立配置，并在多个 adapter 存在时显式设置 `framework=langchain4j`。

### Requirement: 模型工厂按 provider adapter 创建模型
**Reason**: 该能力随旧模型模块一同迁入统一 LangChain4j adapter。

**Migration**: 使用中立 `AiModelFactory` 与统一 adapter 的 SPI 实现，不再引用 `LangChain4jModelRegistry` 或旧包装类型。

### Requirement: 提供 Spring Boot model starter 和自动配置
**Reason**: 旧 model starter 已被移除，避免中立 starter 隐式传递 LangChain4j。

**Migration**: 使用统一 adapter starter；其框架专属自动配置只在 LangChain4j SDK 和 adapter 同时存在时装配。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/langchain4j-starter-modules/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/langchain4j-starter-modules/spec.md
- Lines: 1-21
- SHA256: 6a563b07c1a167a4a000bd0dec10ad261775fa2102f27b2944aebe26f2109a3c

```md
## REMOVED Requirements

### Requirement: 暴露指定 LangChain4j starter 本地模块
**Reason**: 该要求继续把已删除的 LangChain4j store starter 作为聚合与 BOM 的公开模块，和统一 adapter 方案冲突。

**Migration**: LangChain4j 聚合保留 provider/vector-store 专用模块，但删除旧 model/store artifact；新增统一 `fw-plus-ai-langchain4j-adapter` 与其 starter，且仅该 adapter 层依赖 LangChain4j。

## ADDED Requirements

### Requirement: LangChain4j 聚合暴露统一可选 adapter
系统 SHALL 在 LangChain4j 聚合和相关 BOM 中注册 `fw-plus-ai-langchain4j-adapter` 及其 adapter starter，并删除四个旧 model/store artifact 的模块、坐标和传递依赖。

#### Scenario: 聚合和 BOM 不再发布旧坐标
- **WHEN** 检查 Core AI 聚合、LangChain4j 聚合和 Core/AI Boot BOM
- **THEN** 它们不得列出四个已删除 artifact。
- **AND** 它们 SHALL 管理统一 adapter 及其 starter。

#### Scenario: 默认 LangChain4j 仍可显式安装
- **WHEN** Boot 2.7 / Java 8 应用引入统一 LangChain4j adapter starter
- **THEN** 它 SHALL 获得模型和向量 store 的 LangChain4j SPI 实现。
- **AND** 它不得传递 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java SDK。

```

## openspec/changes/neutral-ai-adapter-autodiscovery/specs/magicapi-unified-vector-plugin/spec.md

- Source: openspec/changes/neutral-ai-adapter-autodiscovery/specs/magicapi-unified-vector-plugin/spec.md
- Lines: 1-19
- SHA256: ede040df2b579d5610db310fd0a2305d5ba0bb070e90854b92a703924f251ab2

```md
## MODIFIED Requirements

### Requirement: 统一插件复用 Core 多向量 registry
系统 SHALL 让统一 Magic-API vector 插件委托中立 `AiEmbeddingStoreRegistry` 和中立 store 门面获取实例，而不得直接依赖 LangChain4j `EmbeddingStore<TextSegment>`、旧 factory/registry 或任何具体 AI framework 类型。

#### Scenario: 插件不重复构造 provider 连接
- **WHEN** 插件需要访问已注册向量 store
- **THEN** 插件 SHALL 通过中立 registry 获取中立 store 门面。
- **AND** 插件自身不得复制 provider 或框架原生连接构建逻辑。

#### Scenario: 脚本资源触发 adapter 自动发现
- **WHEN** Magic-API 资源定义给出 framework 或未给出 framework
- **THEN** 系统 SHALL 使用与普通 starter 相同的显式匹配或唯一候选选择规则。
- **AND** 零候选或多候选 SHALL 返回不含凭证的明确 Magic-API 错误。

#### Scenario: 旧脚本按 key 和默认 store 继续访问
- **WHEN** Magic-API 脚本使用 `vector['knowledge']` 或未指定 key 的默认访问方式
- **THEN** 插件 SHALL 使用相同 key 或默认 store 返回对应中立操作对象。
- **AND** 脚本侧不得获得框架原生对象。

```
