# Comet Design Handoff

- Change: add-langchain4j-model-magicapi
- Phase: design
- Mode: compact
- Context hash: 67b8e4a5ecdfed8105d663a7ffd8c821f551c1d8eb59f6ebc5acc7bae74495ef

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-langchain4j-model-magicapi/proposal.md

- Source: openspec/changes/add-langchain4j-model-magicapi/proposal.md
- Lines: 1-29
- SHA256: 1a10b127088ee9eac9cc15744377b108bfc9215888591404f4e33567ab2f1d9e

```md
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

```

## openspec/changes/add-langchain4j-model-magicapi/design.md

- Source: openspec/changes/add-langchain4j-model-magicapi/design.md
- Lines: 1-104
- SHA256: 6acbb34b3479edee5d909ec6e6ae137fdd31af71a9073b97af9c44a3c70a0043

[TRUNCATED]

```md
## Context

`fw-plus-ai-langchain4j-store-embeding` 已经在 core 侧提供了轻量的向量存储抽象，并通过 AI Boot starter 与 Magic-API vector 插件接入运行态动态资源。相比之下，现有 LangChain4j 模型相关类主要位于 `fw-plus-ai-boot-autoconfigure` 的 Magic-API 插件包中，包含模型包装、模型工厂、动态数据源和脚本模块等职责，导致普通 Spring Boot 应用无法在不依赖 Magic 插件实现细节的情况下复用通用模型契约。

本变更面向 Spring Boot 2.7.18、JDK 1.8、LangChain4j 0.35.0 和现有 Magic-API 2.2.2 自维护版本。实现时必须使用各顶层目录的 Maven Wrapper 验证，不升级 `org.ssssssss` 依赖，也不引入 Java 9+ API。

用户已确认旧 Magic LangChain4j 插件实现应删除或替换：它可以作为参考，但最终方案应使用更优雅、合理的一套 model 契约和插件入口。

## Goals / Non-Goals

**Goals:**

- 在 core 侧新增 `fw-plus-ai-langchain4j-model`，提供通用模型定义、模型类型、provider adapter、工厂和 registry 契约。
- 在 AI Boot 层新增 model starter，并通过自动配置把 Spring Boot 配置映射为 model registry。
- 在 Magic-API 中提供新的 `model-datasource` 动态资源与 model 脚本模块，使脚本能按 key 获取 Chat、Streaming Chat、Embedding、Image、Language 等模型。
- 参考但删除或替换现有 Magic-API LangChain4j 插件实现，避免继续维护两套模型定义、工厂、动态数据源和脚本入口。
- 保持 provider SDK 按需引入，starter 不默认传递所有模型供应商依赖。

**Non-Goals:**

- 不升级 LangChain4j、Spring Boot、Spring AI 或 Magic-API 版本。
- 不新增具体业务对话、RAG、Agent 工作流或 UI 大改版。
- 不改变已有 `fw-plus-ai-langchain4j-store-embeding` 的向量存储行为。
- 不默认内置所有第三方 provider SDK，也不要求应用必须使用 Magic-API 才能使用模型 registry。
- 不保留旧 Magic `langchain4j-datasource` 资源类型和旧 `langchain4j` 脚本入口兼容。

## Decisions

### 1. Core 模块承载通用模型契约

在 `fw-plus-ai-langchain4j` 聚合下新增 `fw-plus-ai-langchain4j-model`，包名建议为 `com.xiongx9527.fw.plus.ai.langchain4j.model`。模块内容仿照 store 模块组织：

- `LangChain4jModelDefinition`：保存 `key`、`provider`、`modelType`、`enabled`、`baseUrl`、`apiKey`、`modelName`、`properties` 等通用配置。
- `LangChain4jModelType`：定义 `CHAT`、`STREAMING_CHAT`、`EMBEDDING`、`IMAGE`、`LANGUAGE`、`STREAMING_LANGUAGE`。
- `LangChain4jModel`：包装原生 LangChain4j model，暴露 `getModelType()`、`getProvider()`、`getModel()`。
- `LangChain4jModelProviderAdapter`：以 provider + modelType 判断是否支持，并创建原生模型或包装模型。
- `LangChain4jModelFactory` / `DefaultLangChain4jModelFactory`：选择 adapter、统一异常语义、禁止返回 null。
- `LangChain4jModelRegistry` / `DefaultLangChain4jModelRegistry`：按 key 获取模型、默认模型、key 集合和包含判断。
- `LangChain4jModelException`：封装 provider 不支持、配置缺失、创建失败等运行期错误。

备选方案是在 AI Boot 自动配置层继续维护模型工厂。该方案改动小，但会让 Magic 插件实现继续成为通用模型能力的事实源，不利于 core 侧复用，因此不采用。

### 2. Provider adapter 放在 Boot 自动配置层，core 只定义 SPI

core 模块只依赖轻量 LangChain4j core 类型，不直接绑定 OpenAI、Qwen、Ollama、DashScope 等 provider SDK。AI Boot 自动配置层提供 provider adapter beans，并通过 `@ConditionalOnClass` 或创建时 class 检查避免缺少 provider jar 时启动失败。

备选方案是让 `fw-plus-ai-langchain4j-model` 内置全部 provider builder。该方案使用方便，但会扩大 core 依赖面，也与 store starter 当前“业务显式引入 provider”的模式不一致，因此不采用。

### 3. Spring Boot starter 只带抽象和自动配置入口

新增 `fw-plus-ai-langchain4j-model-spring-boot-starter`，依赖 `fw-plus-ai-boot-starter` 与 `fw-plus-ai-langchain4j-model`。它负责让业务应用获得自动配置能力，但 provider 具体依赖由业务引入已有 `fw-plus-ai-langchain4j-*` 模块或 LangChain4j provider artifact。

配置结构建议沿用 store 配置风格，例如：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: demo
            model-name: gpt-4o-mini
            properties: {}
```

### 4. Magic-API 插件使用新的 model 契约并删除旧实现

Magic 插件侧新增模型动态资源能力，资源类型明确为 `model-datasource`。动态资源创建时转换为 core 的 `LangChain4jModelDefinition`，通过 `LangChain4jModelFactory` 创建模型，并与 Spring Boot registry 一起对外提供查询。

脚本模块建议提供 `model['key']` 访问方式，并根据模型类型暴露原生 LangChain4j model 或受控包装对象。现有 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下的重复实现仅作为参考，最终删除或替换为新的 `magicapi.plugin.model` 实现；不维护旧 `langchain4j` façade。

备选方案是保留旧 `langchain4j` 插件作为 façade 并委托新 registry。该方案迁移成本低，但会长期保留两套入口、两套资源命名和两套前端心智模型，不符合本次“通用模型模块 + 优雅统一插件”的目标，因此不采用。

### 5. OpenSpec 单 change 交付


```

Full source: openspec/changes/add-langchain4j-model-magicapi/design.md

## openspec/changes/add-langchain4j-model-magicapi/tasks.md

- Source: openspec/changes/add-langchain4j-model-magicapi/tasks.md
- Lines: 1-48
- SHA256: 1aa3bd1db7f754d68ebdb6c8abb294a8350107f1009674fd31ec3396154ffcc5

```md
## 1. Core 模型模块

- [ ] 1.1 新建 `fw-plus-ai-langchain4j-model` Maven 模块，保持父 POM、`main.basedir`、编码和 JDK 1.8 约束与相邻 LangChain4j 模块一致。
- [ ] 1.2 实现 `LangChain4jModelDefinition`、`LangChain4jModelType`、`LangChain4jModel`、`LangChain4jModelProviderAdapter`、`LangChain4jModelFactory`、`LangChain4jModelRegistry` 和模型异常类型。
- [ ] 1.3 实现默认模型工厂和默认 registry，覆盖 adapter 选择、默认 key 校验、空返回防御和错误上下文。
- [ ] 1.4 为 core 模型工厂和 registry 增加单元测试，覆盖成功创建、不支持 provider、adapter 异常、默认模型 key 无效等场景。

## 2. Maven 聚合与 BOM

- [ ] 2.1 在 `fw-plus-ai-langchain4j` 聚合 POM 注册 `fw-plus-ai-langchain4j-model`，保留现有用户未提交模块改动。
- [ ] 2.2 在 core dependencies BOM 中管理 `fw-plus-ai-langchain4j-model`，版本使用 `${project.version}`。
- [ ] 2.3 在 AI Boot starters 聚合 POM 注册 `fw-plus-ai-langchain4j-model-spring-boot-starter`。
- [ ] 2.4 在 AI Boot dependencies BOM 中管理 model starter，版本使用 `${project.version}`。

## 3. Spring Boot 自动配置与 starter

- [ ] 3.1 新增 model properties，支持 `default-model`、`models.<key>.provider`、`model-type`、`base-url`、`api-key`、`model-name`、`enabled` 和扩展 `properties`。
- [ ] 3.2 新增 model 自动配置，创建 `LangChain4jModelFactory`、provider adapters 和 `LangChain4jModelRegistry`。
- [ ] 3.3 参考现有 Magic-API LangChain4j 模型工厂，将可复用的 provider 创建逻辑迁移到新的 provider adapter 体系，并移除旧工厂依赖。
- [ ] 3.4 新建 `fw-plus-ai-langchain4j-model-spring-boot-starter`，依赖 `fw-plus-ai-boot-starter` 与 `fw-plus-ai-langchain4j-model`，不默认传递全部 provider SDK。
- [ ] 3.5 为自动配置增加测试，覆盖条件装配、配置绑定、registry 创建、默认模型 key 校验和缺少 provider SDK 的隔离行为。

## 4. Magic-API 模型插件

- [ ] 4.1 新增 Magic model 动态资源信息对象、资源存储和动态注册器，资源类型使用 `model-datasource`，资源定义转换为 `LangChain4jModelDefinition`。
- [ ] 4.2 新增 Magic 动态模型数据源，合并 Spring Boot registry 与 Magic 运行态模型，明确 key 冲突和优先级规则。
- [ ] 4.3 新增或调整 Magic 脚本模块，使脚本可按 key 获取 Chat、Streaming Chat、Embedding、Image、Language 和 Streaming Language 模型。
- [ ] 4.4 新增 Magic 控制器测试接口和资源列表能力，响应与日志过滤 apiKey、secretKey、accessToken 等敏感字段。
- [ ] 4.5 注册新的 Magic model 插件配置和前端资源，删除或替换旧 `langchain4j` 插件配置、脚本入口和 `langchain4j-datasource` 前端资源。

## 5. 旧实现迁移与删除

- [ ] 5.1 删除或替换旧 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下重复的模型工厂、动态数据源、资源存储、控制器、脚本模块和配置类。
- [ ] 5.2 更新 Magic 插件聚合、AI Boot BOM、starter 依赖和示例项目，使运行入口迁移到新的 model 插件 artifact。
- [ ] 5.3 明确旧 `fw-plus-ai-magicapi-plugin-langchain4j` artifact 的处理方式：删除模块或改为指向新 model 插件的空壳迁移模块，避免同一运行态出现两套入口。

## 6. 兼容性与文档

- [ ] 6.1 保持现有 `fw-plus-ai-langchain4j-store-embeding`、Magic vector 插件和已有 LangChain4j provider 模块行为不变。
- [ ] 6.2 补充 README 或示例配置，说明 model starter 最小依赖、provider SDK 显式引入方式、旧 Magic LangChain4j 插件迁移方式和 `model-datasource` 配置示例。
- [ ] 6.3 检查所有新增 Java 代码避免使用 Java 9+ API，并保持包名、缩进和现有命名风格一致。

## 7. 验证

- [ ] 7.1 在 `fw-plus-core` 下使用 Maven Wrapper 构建 `fw-plus-ai-langchain4j-model` 及受影响 core 模块。
- [ ] 7.2 在 `fw-plus-ai` 下使用 Maven Wrapper 构建受影响的 boot autoconfigure、model starter 和 Magic model 插件模块。
- [ ] 7.3 运行新增和受影响测试，至少覆盖 core model、boot autoconfigure、Magic model 插件、旧插件删除/替换迁移、现有 vector 插件兼容场景。
- [ ] 7.4 记录验证命令、结果和任何环境限制，避免把依赖解析失败误判为功能失败。

```

## openspec/changes/add-langchain4j-model-magicapi/specs/langchain4j-model-module/spec.md

- Source: openspec/changes/add-langchain4j-model-magicapi/specs/langchain4j-model-module/spec.md
- Lines: 1-117
- SHA256: ff889dca708eed3e43ccb65fe37b0f2b8dd75e945ed8832b1f8378268f50968d

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 提供 core 侧 LangChain4j 模型抽象模块
系统 SHALL 在 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 下提供 `fw-plus-ai-langchain4j-model` Maven 模块，用于承载通用 LangChain4j 模型定义、创建、注册和查询契约。

#### Scenario: 聚合 POM 注册模型模块
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- **THEN** 它列出 `fw-plus-ai-langchain4j-model` 子模块。

#### Scenario: core BOM 管理模型模块 artifact
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-dependencies` 的 dependencyManagement
- **THEN** 它包含 `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model`。
- **AND** 该依赖使用 `${project.version}` 作为版本。

#### Scenario: 模型模块提供通用契约
- **WHEN** 检查 `fw-plus-ai-langchain4j-model` 源码
- **THEN** 它提供模型定义、模型类型、provider adapter、模型工厂、模型 registry 和模型异常等通用类型。
- **AND** 这些类型位于 `com.xiongx9527.fw.plus.ai.langchain4j.model` 包下。

#### Scenario: 模型模块保持轻量依赖
- **WHEN** 检查 `fw-plus-ai-langchain4j-model/pom.xml`
- **THEN** 它依赖 LangChain4j core 模型类型所需的基础 artifact。
- **AND** 它不默认依赖 OpenAI、Qwen、Ollama、DashScope、Azure、Anthropic 等具体 provider SDK。

### Requirement: 模型工厂按 provider adapter 创建模型
系统 SHALL 通过通用模型工厂按 provider 和 modelType 选择合适的 provider adapter，并返回可按 key 注册和获取的 LangChain4j 模型包装对象。

#### Scenario: 使用匹配 adapter 创建模型
- **WHEN** 调用默认模型工厂创建 provider 和 modelType 均被某个 adapter 支持的模型定义
- **THEN** 工厂返回非 null 的 LangChain4j 模型包装对象。
- **AND** 包装对象保留 provider、modelType 和原生 LangChain4j model。

#### Scenario: provider 不支持时返回明确错误
- **WHEN** 调用默认模型工厂创建没有任何 adapter 支持的模型定义
- **THEN** 系统抛出模型异常。
- **AND** 异常信息包含 provider、modelType 和 key。

#### Scenario: adapter 创建失败时保留上下文
- **WHEN** 匹配的 adapter 在创建模型时抛出运行期异常
- **THEN** 系统抛出模型异常。
- **AND** 异常保留原始 cause，并在消息中包含 provider、modelType 和 key。

### Requirement: 提供 Spring Boot model starter 和自动配置
系统 SHALL 提供 `fw-plus-ai-langchain4j-model-spring-boot-starter`，并在 AI Boot 自动配置层把应用配置转换为模型 registry。

#### Scenario: starter 注册到 AI Boot 聚合
- **WHEN** 检查 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters` 聚合 POM
- **THEN** 它列出 `fw-plus-ai-langchain4j-model-spring-boot-starter` 子模块。

#### Scenario: AI Boot BOM 管理 starter artifact
- **WHEN** 检查 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies` 的 dependencyManagement
- **THEN** 它包含 `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter`。
- **AND** 该依赖使用 `${project.version}` 作为版本。

#### Scenario: starter 只传递抽象和自动配置
- **WHEN** 检查 `fw-plus-ai-langchain4j-model-spring-boot-starter/pom.xml`
- **THEN** 它依赖 `fw-plus-ai-boot-starter` 和 `fw-plus-ai-langchain4j-model`。
- **AND** 它不默认传递所有具体 provider SDK。

#### Scenario: 自动配置创建模型 registry
- **WHEN** 应用配置了启用状态的 LangChain4j model 定义
- **THEN** 自动配置使用模型工厂创建模型并注册到 `LangChain4jModelRegistry`。
- **AND** 默认模型 key 配置存在时，registry 的默认模型指向对应模型。

#### Scenario: 自动配置校验默认模型 key
- **WHEN** 应用配置了不存在的默认模型 key
- **THEN** Spring Boot 启动阶段抛出明确异常。
- **AND** 异常信息包含无效默认模型 key。

### Requirement: Magic-API 支持动态模型资源和脚本访问
系统 SHALL 为 Magic-API 提供新的 LangChain4j model 插件能力，使脚本可以通过 `model-datasource` 动态资源 key 获取模型，并与 Spring Boot registry 共存。

#### Scenario: Magic 插件注册模型动态资源
- **WHEN** Magic-API 启动且模型插件启用
- **THEN** 系统注册模型动态资源存储和动态注册器。
- **AND** 动态资源类型为 `model-datasource`。
- **AND** 动态资源可以被创建、销毁、列举和按 key 获取。

#### Scenario: Magic 脚本按 key 获取模型
- **WHEN** Magic 脚本通过 model 模块访问已注册的模型 key

```

Full source: openspec/changes/add-langchain4j-model-magicapi/specs/langchain4j-model-module/spec.md
