# Comet Design Handoff

- Change: langchain4j-magicapi-model-plugins
- Phase: design
- Mode: compact
- Context hash: aea9717fc3389ea18551fd6b71e86dd49222553fff8f58d9d550fadd590550f8

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/langchain4j-magicapi-model-plugins/proposal.md

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/proposal.md
- Lines: 1-42
- SHA256: 450e41e6020241859f8994d2431424a95e296c63d0a3a2624ec42ecd0b93b9c8

```md
# Proposal

## Why

fw-plus-ai 已把 LangChain4j 以 magic-api `@MagicModule` 形式接入，但模型实例只能在脚本内用 `build*` 编程式自建并缓存，缺少可维护、可视化、可热更新的配置通道；langchain4j 生态的 20+ 模型供应商与 15+ 向量库在 magic-api 侧也没有统一接入面。同时 magic-api 的 `api` / `function` 资源已有元数据（与 MCP 插件共用），却无法作为工具交给大模型本地调用。

本 change 把七类 LangChain4j 模型资源做成 magic-api datasource 插件，补齐脚本只读取用通道，并把 magic-api 资源桥接为 LangChain4j 本地工具，使模型配置从「脚本硬编码」变为「控制台可视化 + 热更新」。

## What Changes

- **新增七类 datasource 型 magic-api 插件**：`langchain4j-chat-model`、`langchain4j-streaming-chat-model`、`langchain4j-language-model`、`langchain4j-streaming-language-model`、`langchain4j-embedding-model`、`langchain4j-tokenizer`、`langchain4j-embedding-store`。每类在控制台「数据源」页签注册，JSON 持久化于 `{type}/*.json`。
- **每类资源补齐标准三件套**：运行时实例持有者 `MagicDynamic*DataSource`（`DisposableBean`，按 key 持有/驱逐/统一关闭）、动态注册器 `*MagicDynamicRegistry`（遵循资源托管规则 A：先驱逐 → `enabled` 检查 → 工厂构建 → 成功才落库）、资源存储 `*MagicResourceStorage`（`key` 唯一与格式、`provider` 短名表、`embedding-store` 必填 `embeddingModelKey`）。
- **装配按资源类型拆为 7 个子 Configuration**：父类 `Langchain4jDatasourceConfiguration` 只保留 `plugin()` / `controllerRegister()` 与共享 bean，用 `@Import` 引入 7 个按类型的子配置类（对齐 `MagicAPIDruidConfiguration` 的 `@Import` 写法）。
- **JSON 结构 = 公共字段 + `options` 差异参数对象**：`key` / `name` / `provider` / `enabled`（`embedding-store` 另加 `embeddingModelKey`）提取到实体基类，其余连接与生成参数放入 `options` 自由 map；不同 provider 的 `options` 天然不一致。
- **provider 覆盖**：每家一个显式工厂（参照 core 层 `AutoConfig` 的 builder 链写法），每类资源有独立短名表，表外值校验失败并列出可用短名。
- **改造 `Langchain4jModule`（**BREAKING**）**：`@MagicModule("uid")` → `@MagicModule("langchain4j")`；删除全部 `build*` 编程式工厂与内部实例 map；`get*` 改为只读委托运行时实例持有者；`remove(type,key)` 改为驱逐语义；`destroy()` 清空全部持有者。旧脚本中的 `langchain4j.buildXxx(...)` 将失效。
- **新增连接测试 Controller**：`POST /langchain4j/{type}/datasource/test`，对未保存的配置即时构建并按类型做强弱不同的最小探测。
- **新增 console 前端子工程** `fw-plus-ai-magicapi-plugin-langchain4j`：7 个数据源组件 + `(资源类型 × provider)` 字段元数据表 + 连接测试 service + 中英文 i18n，构建产物 `magic-langchain4j.1.0.0.iife.js`。
- **新增 Magic-API → LangChain4j 本地工具桥**：把 magic-api 的 `api` / `function` 资源暴露为 LangChain4j `ToolProvider`，过滤范围（分组/路径/HTTP 方法/opt-in）通过 `fw.plus.magicapi.langchain4j.tool.*` 配置。
- **依赖**：AI 层 BOM 统一管理 langchain4j 全生态供应商版本，`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入。
- **硬约束**：core 层 `fw-plus-ai-langchain4j-*` 零修改（含修饰符），只作为 builder 参数写法的参考资料。

## Capabilities

### New Capabilities

- `langchain4j-model-datasource-plugins`: 七类 LangChain4j 模型资源作为 magic-api datasource 的注册、JSON 结构、校验、动态注册与生命周期、连接测试、控制台装配与前端资源。
- `langchain4j-script-module`: 脚本侧 `langchain4j` 模块对七类资源的只读取用语义、未配置时的错误行为与驱逐语义。
- `magicapi-langchain4j-tool-provider`: magic-api `api` / `function` 资源暴露为 LangChain4j 本地 `ToolProvider` 的曝光范围、过滤规则、入参 schema 与变更监听。

### Modified Capabilities

（无。本 change 不改变既有 `magicapi-mcp-plugin` / `mcp-server-starter` 的规格行为；工具桥只复用 MCP 插件既有的 `mcpInputSchema` / `mcpTool` 元数据约定。）

## Impact

- **代码**：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`（插件装配、实体、存储、注册器、工厂、Controller、脚本模块、工具桥）；新增前端子工程 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j`；样例工程 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample` 仅作为「静态配置路径不回归」验收对象。
- **依赖**：`fw-plus-ai-boot-dependencies` 新增 langchain4j 供应商版本管理；`fw-plus-ai-boot-autoconfigure/pom.xml` 以 `optional` 引入模型/向量库 jar。
- **配置**：新增 `fw.plus.magicapi.langchain4j.enabled` 与 `fw.plus.magicapi.langchain4j.tool.*`。
- **破坏性**：脚本侧 `langchain4j.build*` 系列方法移除；`@MagicModule` 名由 `uid` 改为 `langchain4j`。
- **交付物**：前端 `magic-langchain4j.1.0.0.iife.js` 需落在 `target/classes/magic-editor/plugins/`。
- **非目标**：不做 `AiServices` / `Assistant` / `ContentRetriever` / `RetrievalAugmentor` / `ModerationModel` 的 datasource 化；不做敏感参数加密存储；不做集群同步与参数 schema 强校验引擎；不修改 core 层 `fw-plus-ai-langchain4j-*`。

```

## docs/openspec/changes/langchain4j-magicapi-model-plugins/design.md

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/design.md
- Lines: 1-180
- SHA256: 3d27a5141861406abddbc85494b48f9e9e2bd716c7bf25e70f221d718e1bfdca

[TRUNCATED]

```md
---
comet_change: langchain4j-magicapi-model-plugins
role: technical-design
canonical_spec: openspec
---

# Design

## Context

动机与范围见 `proposal.md`；行为契约见 `specs/`。本节只记录影响实现方式的现状与约束。

**当前状态**：`fw-plus-ai` 已把 LangChain4j 以 magic-api `@MagicModule` 形式接入，但 `modules/langchain4j/core/Langchain4jModule` 在 `0e8af325e` 中被清空为占位类，工作区里另有一份尚未定案的 261 行版本（仍为 `@MagicModule("uid")` + 全套 `build*`）。七类 datasource 插件的后端骨架已提交（`2322ef973`、`5240c9f5b`），落在 `plugin/langchain4j/**`，但装配是一个 239 行的单类配置、运行时实例由一个跨七类的共享 holder 持有，且前端子工程、测试与 Module 改造尚未完成。`modules/langchain4j/tool/**` 下的工具桥为未提交新增，无既有设计覆盖。

**已确定的约束**：

- core 层 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-*` 只作参考，零修改（含修饰符）。
- Java 8：不得使用 `var`、`record`、`List.of()`、`Map.copyOf()` 等 Java 9+ API；注释与异常信息用简体中文。
- 供应商模型类来自 langchain4j 官方 jar，由 `fw-plus-ai-boot-autoconfigure` 以 `optional` 引入；版本由 AI 层 BOM 统一管理。
- 既有插件惯例（`MagicAPIMongoConfiguration`、`MagicAPIKafkaConfiguration`、`MagicAPIDruidConfiguration`）决定了目录分层、装配写法与运行时持有者的形态。

## Goals / Non-Goals

**Goals:**

- 七类模型资源以 magic-api datasource 形式可配置、可热更新、可停用，生命周期与数据源一致。
- 运行时实例持有层与仓库既有插件同构，便于后续维护与增量接入新供应商。
- 装配入口可读、可分层：单类型改动不影响其它类型。
- 脚本侧只有一条只读取用通道，实例来源唯一。
- magic-api 已有资源可作为本地工具交给模型调用，且曝光范围可控。

**Non-Goals:**

- 不做 `AiServices` / `Assistant` / `ContentRetriever` / `RetrievalAugmentor` / `ModerationModel` 的 datasource 化（本次仅七类）。
- 不做敏感参数加密存储（`api-key` 等与 magic-api 内建 datasource 密码字段行为一致）。
- 不做集群同步、参数 schema 强校验引擎。
- 不做 embedding-store 对 embedding-model 的级联删除。
- 不修改 core 层；不在本次处理 chat-sample 的会话级能力（另建 change）。

## Decisions

### D1. datasource 形态与 JSON 结构

每类资源在控制台「数据源」页签注册为一个数据源类型，JSON 经 magic-api 通用 `/resource/file/{type}/save` 接口持久化到 `{type}/*.json`，与官方 `mongo-datasource`、`redis-datasource` 同构。

JSON = 公共字段 + `options` 差异参数对象。公共字段 `key` / `name` / `provider` / `enabled` 提取到实体基类（`embedding-store` 扩展 `embeddingModelKey`）；其余一律入 `options` 自由 map，因为不同 provider 的参数天然不一致（qwen 有 `enable-search`，openai 没有）。

*替代方案*：把所有参数拍平成实体字段 → 字段数不可控且每加一个供应商都要改实体，放弃。

### D2. 每类资源的运行时持有者与注册器

七类资源各自拥有：

| 角色 | 形态 | 职责 |
| --- | --- | --- |
| 运行时持有者 | `MagicDynamic<Type>DataSource`，`implements DisposableBean` | 按 `key` 持有/驱逐/列举实例，`destroy()` 统一关闭；只依赖本类型实例 |
| 动态注册器 | `<Type>MagicDynamicRegistry`（`AbstractMagicDynamicRegistry<T>`） | 遵循 CLAUDE.md「MQ 插件动态注册器」规则 A：先驱逐 → `enabled` 检查 → 工厂构建 → 成功才落库 |
| 资源存储 | `<Type>MagicResourceStorage`（`MagicResourceStorage<T>`） | `folder()` = 该类型 datasource type；`key` 必填/格式/同类唯一；`provider` 命中本类型短名表；`embedding-store` 另校验 `embeddingModelKey` |

*决策依据*：与 `MagicDynamicKafkaDataSource`、`MagicDynamicMongoDataSource` 等 12 个既有插件同构（同样的持有者+注册器+存储三件套、同样的 `DisposableBean` 释放语义）。

*替代方案*：单一跨类型 `Langchain4jModelHolder`（当前已提交实现）→ 与仓库既有插件不同构，一个类承担七类职责，类型安全靠 `EnumMap` + 强转维持；已被否。

### D3. 装配拆为七个按类型的子 Configuration

父类 `Langchain4jDatasourceConfiguration implements MagicPluginConfiguration` 只保留跨类型共享内容（插件标识 `plugin()`、`controllerRegister()`、工厂注册表）与共享 bean，通过 `@Import` 引入七个按资源类型的顶层子配置类；每个子配置只声明本类型的持有者、存储与注册器 bean。

*决策依据*：与 `MagicAPIDruidConfiguration`（`@Import({DruidSpringAopConfiguration, ...})`）一致；单类型 bean 定义集中在一处，改动一个类型不触及其它六个。

*替代方案*：
- 内部 `static class` 嵌套子配置 → 写法与仓库既有高层级插件不一致，放弃。
- 七个子配置全部由自动配置入口直接注册 → 装配入口分散、`plugin()` / `controllerRegister()` 仍需一个实现类，放弃。

*补充要求*：单类型装配或构建失败不得阻止其它类型可用（已写入 `specs/langchain4j-model-datasource-plugins`）。

*实施补充*：与类型无关的共享内容（工厂注册表）单独放进 `Langchain4jModelFactoryConfiguration`；父装配构造注入该注册表与 embedding-model 持有者，供 `controllerRegister()` 直接构造连接测试 Controller——避免在 `@Configuration` 构造器里注入自身 `@Bean` 造成的循环依赖，同时去掉 Controller 的 `SpringUtils` 服务定位。选项读取器的错误路径使用传入的完整 datasource type，即 `langchain4j-chat-model[qwen-main].options.api-key`（类型串本身已含前缀，读取器不再二次拼接）。

### D4. provider 工厂 SPI 与短名表单一事实源

每家供应商一个显式工厂类（参照 core 层 `AutoConfig` 的 builder 链写法）。工厂实现 `ModelFactory` SPI：`provider()` 返回短名，七类 `build*` 方法有默认实现抛「不支持」。短名表不单独维护常量，而是**由已注册工厂的能力探测生成**，存储校验用的短名集合与工厂注册表同源，避免「表里有短名但无工厂」的漂移。

```

Full source: docs/openspec/changes/langchain4j-magicapi-model-plugins/design.md

## docs/openspec/changes/langchain4j-magicapi-model-plugins/tasks.md

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/tasks.md
- Lines: 1-50
- SHA256: 3c7581e2abaa2a69d66d14ca56da527d0800d15c20657e487389a311a5b084e1

```md
# Tasks

## 1. 结构对齐：运行时持有者与装配

- [x] 1.1 新增七个 `MagicDynamic<Type>DataSource`（`implements DisposableBean`，按 `key` 持有/驱逐/列举实例，`destroy()` 统一关闭），并让七个 `*MagicDynamicRegistry` 注入各自类型的持有者而非共享 holder；验证：新增 `MagicDynamicLangchain4j*DataSourceTest` 覆盖驱逐即关闭、关闭异常仅记日志不阻断、`destroy()` 清空，全部通过 <!-- comet-task:7c931e83-14a9-44a0-96d4-0b71a17409eb -->
- [x] 1.2 删除共享 `Langchain4jModelHolder` 及其全部引用，确认无残留；验证：`grep -r "Langchain4jModelHolder" fw-plus-ai` 无结果，且 `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure compile -nsu` 通过 <!-- comet-task:bb60100e-53ab-46ac-8c00-8bd6426f84b1 -->
- [x] 1.3 把 `Langchain4jDatasourceConfiguration` 拆为「父装配 + 七个按资源类型的顶层子 Configuration」：父类只保留 `plugin()` / `controllerRegister()` 与共享 bean，通过 `@Import` 引入七个子配置，每个子配置只声明本类型的持有者、存储与注册器；验证：新增 `Langchain4jDatasourceConfigurationTest` 断言七类 bean 齐备，并通过单类型故障隔离用例 <!-- comet-task:9711f454-4605-4a31-a66d-0c354cd91916 -->
- [x] 1.4 核对并修正目录分层：datasource 插件位于 `plugin/langchain4j/**`，脚本模块与工具桥位于 `modules/langchain4j/**`（修正设计初稿统一放在 `modules` 的偏差）；验证：目录清单与 design.md D7 一致，且模块编译通过 <!-- comet-task:e63751b7-027a-4532-a41d-2b63c11cff8b -->

## 2. 脚本模块只读化（BREAKING）

- [x] 2.1 `Langchain4jModule` 改为 `@MagicModule("langchain4j")`，删除全部 `build*` 与内部实例 map，七个 `get*` 只读委托对应类型持有者，未配置时抛中文异常并列出该类型已配置 `key`；验证：新增 `Langchain4jModuleTest` 覆盖取用委托、未配置异常含类型与 `key`、异常列出可用 `key` <!-- comet-task:0fa0f29a-16ed-479d-9843-5839d349d565 -->
- [x] 2.2 `remove(type, key)` 改为持有者驱逐语义、`destroy()` 清空全部持有者并关闭可关闭实例；验证：`Langchain4jModuleTest` 中驱逐后可重新加载、关闭时清空用例通过 <!-- comet-task:4f517478-07a8-4956-98c3-d647a85fb675 -->

## 3. provider 工厂与依赖

- [x] 3.1 AI 层 BOM 补齐 langchain4j 全生态供应商版本管理，`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入模型、分词器与向量库 jar（含内置向量模型）；验证：`./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-dependencies install -DskipTests -nsu` 与 autoconfigure 编译通过 <!-- comet-task:9d6b483c-fe1c-458b-8521-ad725e654262 -->
- [x] 3.2 短名表改为由已注册工厂的能力探测生成，使存储校验与工厂注册表同源；验证：新增 `Langchain4jModelFactoryRegistryTest` 覆盖「每个短名都有工厂」「工厂类可加载」「表外短名被拒」「工厂支持的方法集合与资源类型表一致」 <!-- comet-task:ef795f81-e186-42b7-bcab-e3d84189025d -->
- [x] 3.3 为每个资源类型的构建方法补代表工厂单测（builder 链参数透传、默认值语义、读取器缺参报错）；验证：`Langchain4jOpenAiModelFactoryTest`、`Langchain4jQwenModelFactoryTest` 通过 <!-- comet-task:733eaa78-07a3-4c71-b272-1ee357739f43 -->
- [x] 3.4 强类型选项读取器的完整路径报错；验证：新增 `Langchain4jOptionsReaderTest`，断言缺失必填项时异常信息含 `langchain4j-chat-model[qwen-main].options.api-key` 形式路径 <!-- comet-task:d3edaff6-4f2e-4411-846c-5b34edd6de4e -->

## 4. embedding-store 依赖与启动恢复

- [x] 4.1 实现 `embeddingModelKey` 必填校验与注册期依赖解析，引用不可用时注册失败并提示需先配置并启用对应 embedding-model；验证：`Langchain4jEmbeddingStoreMagicDynamicRegistryTest` 覆盖缺失/未启用依赖的失败用例 <!-- comet-task:c0ff527c-c590-47f7-86bf-f8f9cfc0f1fc -->
- [x] 4.2 实现维度决策链：`options.dimension` → 被引用模型维度 → 中文报错；验证：同上测试覆盖三条分支 <!-- comet-task:04164d3b-6afd-4331-ac53-6688534784bf -->
- [x] 4.3 启动完成钩子对启动期失败的 embedding-store 统一重试一次注册；验证：`Langchain4jEmbeddingStoreStartupRetryTest` 覆盖「store 先于 model 加载后自动恢复」与「已成功的不产生重复实例」 <!-- comet-task:cfcadf67-5b71-4009-ad99-14127edd096a -->

## 5. 连接测试 Controller

- [x] 5.1 实现 `POST /langchain4j/{type}/datasource/test`：七类各自最小探测、未保存配置可测、streaming 两类只做构建验证、embedding-store 探测结束后关闭临时实例、返回 `ok` 或含类型/`key`/选项路径的中文错误；验证：新增 `Langchain4jDatasourceControllerTest` 覆盖七类与错误/未保存/关闭分支 <!-- comet-task:0bb739d2-45e7-4b94-8353-4c16c14ae71d -->

## 6. 前端 console 子工程

- [x] 6.1 新建 `fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j`（pom 仿 mcp 子工程，`exec-maven-plugin` 执行 npm 构建，`maven-resources-plugin` 复制产物）；验证：`./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j package -nsu` 后 `target/classes/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js` 存在且非空 <!-- comet-task:0a8cecea-c192-47c0-b704-b20936305de1 -->
- [x] 6.2 实现 console 源码：`index` 注册七类 datasource、七个 Vue 组件、`(资源类型 × provider)` 字段元数据表、连接测试 service、中英文 i18n；验证：元数据表覆盖七类且每个已声明短名都有字段定义，`embedding-store` 组件含 `embeddingModelKey` 选择项 <!-- comet-task:141e9aec-cead-4f4a-a489-e9806fe6648f -->
- [x] 6.3 前端产物缺失时后端只按基础插件标识注册且不抛异常；验证：`Langchain4jDatasourceConfigurationTest` 中断言资源缺失的降级路径 <!-- comet-task:459d3315-bd41-44ad-bb45-2a9dd4c799bf -->

## 7. 工具桥（api / function → LangChain4j ToolProvider）

- [x] 7.1 工具快照注册表：两类资源独立启停、排除优先于包含、分组模式覆盖子分组、`api` 方法白名单与 opt-in（默认 `mcpTool`）、入参 schema（默认 `mcpInputSchema`，支持对象与 JSON 字符串并回退无参）；验证：`MagicApiToolRegistryTest` 全部用例通过 <!-- comet-task:b8e1540b-aa96-459f-a67f-cfeb09ae486f -->
- [x] 7.2 `ToolProvider` 与 `ToolExecutor`：工具名生成（`METHOD /path` 与函数名）、同名时函数优先、参数原样透传、映射/集合按 JSON 序列化、脚本异常与未知工具返回错误文本而不抛异常；验证：`MagicApiToolProviderTest`、`MagicApiToolExecutorTest` 全部用例通过 <!-- comet-task:a8c96d4c-3811-4556-84e3-64475222d7c2 -->
- [x] 7.3 资源变更监听使快照失效并按需重建（不使用 TTL），装配受工具开关与 magic-api 资源服务可用性约束；验证：`MagicApiToolChangeListenerTest`、`Langchain4jToolAutoConfigurationTest` 全部用例通过 <!-- comet-task:678cf728-124b-4228-b6c0-731b21687191 -->

## 8. 集成验收与审计

- [x] 8.1 集成测试：上下文拉起后七类存储/持有者/注册器 bean 齐备，以文件事件触发注册后按 `key` 可取、删除后消失；验证：扩展 `LangChain4jAutoConfigurationRegistrationTest` 并全绿 <!-- comet-task:a4873971-bad9-489b-9dfd-44ba509d5438 -->
- [x] 8.2 core 层零修改审计；验证：`git diff --name-only -- fw-plus-core` 输出为空 <!-- comet-task:769f981a-99ac-4ff3-83a9-f508432d0623 -->
- [x] 8.3 样例工程静态配置路径不回归（`langchain4j.open-ai.*`）；验证：`fw-plus-ai-chat-sample` 测试通过 <!-- comet-task:905777f1-c7d2-432e-ab03-88b2a42c4816 -->
- [x] 8.4 命名审计：folder、数据源类型、插件标识、包名、类名、脚本模块名均含 `langchain4j` 且无 `uid` 残留；验证：全仓检索无旧的 `uid` 模块名与旧命名 <!-- comet-task:29014e55-455a-4757-8b19-2cd9f090c771 -->
- [x] 8.5 控制台端到端验收：七类数据源的新建/保存/停用/删除/连接测试全链路可用，`options` 表单字段随 `provider` 变化；验证：手工操作记录（含所保存的 `{type}/*.json` 与测试返回） <!-- comet-task:1d5dc780-2200-4d85-826c-2bc9be97a8f7 -->

```

## docs/openspec/changes/langchain4j-magicapi-model-plugins/.openspec.yaml

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/.openspec.yaml
- Lines: 1-2
- SHA256: d707823457f58b4f938a99bc049bddd9b937affec2d09c976c9a16a9dcd03f77

```md
schema: spec-driven
created: 2026-09-28

```

## docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/langchain4j-model-datasource-plugins/spec.md

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/langchain4j-model-datasource-plugins/spec.md
- Lines: 1-207
- SHA256: b1821f494e997be9edfeef24c49c73577bbaf476e23d1517ff670a8e5d849ed6

[TRUNCATED]

```md
# Spec Delta

## Purpose

让 LangChain4j 的七类模型与向量库实例能够作为 magic-api 数据源在控制台可视化配置、热更新与停用，实例由插件按数据源生命周期创建和销毁，调用方按数据源 key 取用。

## ADDED Requirements

### Requirement: 七类 LangChain4j 数据源类型注册

插件 SHALL 向 magic-api 控制台的数据源页签注册七个数据源类型：`langchain4j-chat-model`、`langchain4j-streaming-chat-model`、`langchain4j-language-model`、`langchain4j-streaming-language-model`、`langchain4j-embedding-model`、`langchain4j-tokenizer`、`langchain4j-embedding-store`。

#### Scenario: 控制台列出七类数据源类型
- **WHEN** 插件已装配且打开 magic-api 控制台数据源页签
- **THEN** 页签中出现七个 `langchain4j-*` 类型，每个类型有对应的新建入口

#### Scenario: 每个类型独立的持久化目录
- **WHEN** 在某一类型下保存一个数据源
- **THEN** 该数据源的 JSON 持久化到以该数据源类型命名的目录中，扩展名为 `.json`

### Requirement: 数据源 JSON 的公共字段与 options 分离

每个数据源 JSON SHALL 由公共字段与 `options` 对象两部分组成。公共字段为 `key`、`name`、`provider`、`enabled`；`langchain4j-embedding-store` 额外包含必填公共字段 `embeddingModelKey`。除公共字段外的全部参数 SHALL 放在 `options` 对象中，且不同 provider 的 `options` 键集合允许不同。

#### Scenario: options 携带 provider 差异参数
- **WHEN** 保存一个 `provider` 为通义千问的 chat-model 数据源，其 `options` 含 `enable-search`
- **THEN** 该键被完整持久化，读取时原样返回

#### Scenario: options 允许为空
- **WHEN** 保存的数据源不含 `options` 或 `options` 为空对象
- **THEN** 保存成功，最小依赖默认值构建实例

#### Scenario: name 缺省
- **WHEN** 保存的数据源未提供 `name`
- **THEN** 该数据源的展示名回退为 `key`

#### Scenario: 实例在生命周期中唯一
- **WHEN** 同一数据源被多次保存或重载
- **THEN** 同一时刻只有一个同 `key` 的实例对外可用

### Requirement: key 校验

数据源 `key` SHALL 为必填、非空、仅由字母、数字、下划线、点、连字符组成，且在同一数据源类型内唯一。不同数据源类型之间允许使用相同 `key`。

#### Scenario: 缺少 key 被拒绝
- **WHEN** 保存的数据源 `key` 为空或未提供
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 非法字符被拒绝
- **WHEN** 保存的数据源 `key` 含约定字符集之外的字符（例如空格或斜杠）
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 同类型重复 key 被拒绝
- **WHEN** 在同一数据源类型下保存已存在 `key` 的数据源
- **THEN** 保存被拒绝并返回中文错误信息

#### Scenario: 跨类型同名 key 允许
- **WHEN** 在两个不同数据源类型下分别保存相同 `key`
- **THEN** 两次保存都成功，且各自取用时互不影响

### Requirement: provider 短名表校验

`provider` SHALL 为必填，且必须命中所属数据源类型的可用短名集合。每个数据源类型 SHALL 拥有独立的短名集合，表外取值 SHALL 被拒绝，错误信息 SHALL 列出该类型的全部可用短名。

#### Scenario: 表内 provider 通过
- **WHEN** 保存的数据源 `provider` 属于所属类型的短名集合
- **THEN** 保存通过

#### Scenario: 表外 provider 被拒绝
- **WHEN** 保存的数据源 `provider` 不属于所属类型的短名集合
- **THEN** 保存被拒绝，且错误信息列出该类型全部可用短名

#### Scenario: 各类型短名集合互相独立
- **WHEN** 使用某 provider 短名保存 chat-model 成功，而该短名不在 tokenizer 的短名集合内，则用同一短名保存 tokenizer
- **THEN** tokenizer 的保存被拒绝

#### Scenario: 每个已声明短名都有对应构建能力
- **WHEN** 遍历某一类型短名集合中的每个短名
- **THEN** 都能为该类型构建出实例，不存在声明了短名却无法构建的取值


```

Full source: docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/langchain4j-model-datasource-plugins/spec.md

## docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/langchain4j-script-module/spec.md

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/langchain4j-script-module/spec.md
- Lines: 1-71
- SHA256: 39518e896da4ab292a0cc959e1e79fba9f385e27df671f5faa72e8223f5f8a2f

```md
# Spec Delta

## Purpose

让 magic-api 脚本以一个只读入口按 key 取用已配置的 LangChain4j 模型与向量库实例，把实例来源统一收敛到数据源配置，脚本自身不再负责构建与缓存模型。

## ADDED Requirements

### Requirement: 脚本模块名与只读取用入口

magic-api 脚本 SHALL 通过模块名 `langchain4j` 引入该能力。模块 SHALL 为七类资源分别提供按 `key` 取用实例的方法：对话模型、流式对话模型、语言模型、流式语言模型、向量模型、分词器、向量库。取用方法 SHALL 是只读的，不产生、不注册、不替换实例。

#### Scenario: 引入模块
- **WHEN** 脚本执行 `import langchain4j`
- **THEN** 可以调用该模块的取用方法

#### Scenario: 模块名为 langchain4j
- **WHEN** 脚本引用该模块
- **THEN** 模块名为 `langchain4j`，旧的占位模块名不再可用

#### Scenario: 按 key 取到已配置实例
- **WHEN** 脚本用某个已存在且已启用的数据源 `key` 调用对应类型的取用方法
- **THEN** 返回该数据源对应的实例

#### Scenario: 取用不改变实例集合
- **WHEN** 脚本反复调用取用方法
- **THEN** 可用实例集合与实例本身保持不变，不产生新的实例

### Requirement: 不再支持脚本内自建模型

模块 SHALL 不提供脚本侧构建或缓存模型的编程式入口。模型实例的唯一来源 SHALL 是通过数据源配置注册进运行时的实例。

#### Scenario: 编程式构建入口不可用
- **WHEN** 脚本调用旧有的 `build*` 形式构建方法
- **THEN** 该调用不可用（方法不存在或调用报错），脚本需改用数据源配置与取用方法

### Requirement: 未配置 key 的错误行为

当取用的 `key` 在该类型下没有可用实例时，模块 SHALL 抛出中文异常，且异常信息 SHALL 便于定位，包含所请求的类型与 `key`，并列出该类型当前已配置的 `key`。

#### Scenario: key 不存在
- **WHEN** 脚本取用一个未配置或不存在的 `key`
- **THEN** 抛出中文异常，信息中包含请求的类型与 `key`

#### Scenario: 已停用的 key
- **WHEN** 脚本取用一个 `enabled=false` 数据源的 `key`
- **THEN** 行为与未配置一致，抛出中文异常

#### Scenario: 异常信息列出可用 key
- **WHEN** 该类型下已有其它可用数据源
- **THEN** 异常信息中列出这些已配置的 `key`

### Requirement: 驱逐语义

模块 SHALL 提供按类型与 `key` 移除实例的入口，其语义为从运行时可用集合中驱逐该实例，而不是删除已保存的数据源配置。被驱逐后再次取用该 `key` SHALL 表现为未配置。

#### Scenario: 驱逐后不可取用
- **WHEN** 调用移除入口移除某类型的某个 `key`
- **THEN** 之后取用该 `key` 抛中文异常

#### Scenario: 驱逐不删除配置
- **WHEN** 驱逐某 `key` 后重新加载其数据源
- **THEN** 该 `key` 可重新取用

### Requirement: 应用关闭时释放实例

模块 SHALL 在应用关闭时清空全部可用实例，并对可关闭的实例执行关闭，释放其占用的连接或资源。

#### Scenario: 关闭时释放
- **WHEN** 应用关闭
- **THEN** 各类可用集合被清空，可关闭的实例被关闭

```

## docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/magicapi-langchain4j-tool-provider/spec.md

- Source: docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/magicapi-langchain4j-tool-provider/spec.md
- Lines: 1-159
- SHA256: a2faed220d36e2c6038ccba204da3715c3d017d9ee0f2cb02018d363c8e980d3

[TRUNCATED]

```md
# Spec Delta

## Purpose

让 magic-api 中已存在的 `api` 与 `function` 资源可以作为一个 LangChain4j 本地工具提供者交给大模型调用，无需经过 MCP，并允许按分组、路径、HTTP 方法与显式 opt-in 控制曝光范围。

## ADDED Requirements

### Requirement: 以本地工具提供者暴露 magic-api 资源

插件 SHALL 提供一个 LangChain4j 工具提供者，把 magic-api 的 `function` 与 `api` 资源作为工具规格与执行器成对提供，执行时不经过网络或 MCP 通道。工具名 SHALL 为：`function` 使用函数名，`api` 使用「HTTP 方法 + 空格 + 以斜杠开头的路径」。

#### Scenario: 提供函数工具
- **WHEN** 存在一个已保存的 `function` 资源
- **THEN** 工具提供者返回以该函数名为工具名的工具规格

#### Scenario: 提供 api 工具
- **WHEN** 存在一个已保存的 `POST` 类型 `api` 资源，路径为 `/order/create`
- **THEN** 工具提供者返回工具名为 `POST /order/create` 的工具规格

#### Scenario: 同名冲突时函数优先
- **WHEN** 一个 `function` 与一个 `api` 派生出的工具名相同
- **THEN** 结果中只保留一个该名称的工具，且其行为为函数

#### Scenario: 路径规范化
- **WHEN** `api` 资源的路径未以斜杠开头
- **THEN** 生成的工具名中路径以斜杠开头

### Requirement: 按资源类型独立启停

`function` 与 `api` 两类资源的曝光 SHALL 可以分别启用或停用，默认均启用。

#### Scenario: 默认两类都曝光
- **WHEN** 未配置任何范围相关选项且两类资源都存在
- **THEN** 两类资源都出现在工具集合中

#### Scenario: 关闭一类
- **WHEN** 将 `function` 类的启用开关设为 `false`
- **THEN** 工具集合中不包含任何函数工具，`api` 工具不受影响

### Requirement: 分组与路径过滤

每类资源 SHALL 支持按分组与路径的包含和排除过滤，模式使用 Ant 风格匹配。排除规则优先于包含规则；包含列表为空表示不限制。分组模式不带通配符时 SHALL 同时覆盖该分组本身与其子分组。

#### Scenario: 包含分组只保留匹配项
- **WHEN** 配置只包含某个分组
- **THEN** 工具集合只包含落在该分组（及其子分组）下的资源

#### Scenario: 排除优先于包含
- **WHEN** 同一资源同时命中包含分组与排除分组
- **THEN** 该资源不出现在工具集合中

#### Scenario: 分组模式覆盖子分组
- **WHEN** 包含分组写为不带通配符的分组路径，且存在该分组下的子分组资源
- **THEN** 子分组下的资源也被保留

#### Scenario: 空列表表示不限
- **WHEN** 包含列表与排除列表均为空
- **THEN** 该范围内全部资源都被保留

### Requirement: api 的 HTTP 方法与显式 opt-in

`api` 类资源 SHALL 支持 HTTP 方法白名单，白名单为空表示不限制；还 SHALL 支持 opt-in 模式，启用后只有显式声明了 opt-in 选项的 `api` 资源才被曝光。opt-in 选项名 SHALL 可配置，默认与 MCP 插件共用同一选项名，使同一处元数据同时控制两侧曝光。

#### Scenario: 方法白名单限制曝光
- **WHEN** 方法白名单只包含 `GET`
- **THEN** 工具集合中不包含 `POST` 类型的 api 工具

#### Scenario: 未启用 opt-in 时不额外限制
- **WHEN** opt-in 模式关闭
- **THEN** 通过其它过滤条件的 `api` 资源全部曝光

#### Scenario: 启用 opt-in 后只曝光已声明项
- **WHEN** opt-in 模式开启且某 `api` 资源未携带 opt-in 选项（或该选项为假值）
- **THEN** 该 `api` 资源不曝光，携带真值的 `api` 资源正常曝光

#### Scenario: opt-in 选项来自资源元数据
- **WHEN** 某 `api` 资源在元数据中以属性形式声明 opt-in 选项为真
- **THEN** 该资源被判定为已 opt-in


```

Full source: docs/openspec/changes/langchain4j-magicapi-model-plugins/specs/magicapi-langchain4j-tool-provider/spec.md
