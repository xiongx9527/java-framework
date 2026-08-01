# Comet Design Handoff

- Change: move-langchain4j-adapter-autoconfigure
- Phase: design
- Mode: compact
- Context hash: b444c6f0fa0598bf317015de5df9823a76592d93427907dd506c0cf4ac1dac89

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/move-langchain4j-adapter-autoconfigure/proposal.md

- Source: openspec/changes/move-langchain4j-adapter-autoconfigure/proposal.md
- Lines: 1-31
- SHA256: e3e33cc2621e66cad4612c868c489b62a3fc46d0f90cb2a32ba3f7e497facee3

```md
## Why

`fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 被单独发布为 Maven artifact，但其中源码已经属于统一的 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 自动配置命名空间。这一额外模块增加了 Reactor、BOM、Starter 和文档的维护边界，却没有形成独立的公共能力边界。

## What Changes

- **BREAKING** 删除 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` Maven 模块及公开坐标，不提供兼容空 POM。
- 将该模块的生产源码、自动配置注册资源和测试完整迁入 `fw-plus-ai-boot-autoconfigure`，保留现有 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包结构和运行时行为。
- 保留 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 作为业务应用显式安装 LangChain4j 默认实现的入口，并将其自动配置依赖改为 `fw-plus-ai-boot-autoconfigure`。
- 从 AI Reactor、BOM、Starter 文档、迁移文档和验证命令中移除旧 autoconfigure 坐标。
- 调整中立边界测试：允许统一 Boot autoconfigure artifact 在隔离的 `langchain4j` 子包中承载具体框架自动配置，同时继续禁止中立 Core、Starter、公共 API、Magic-API 和传递依赖暴露具体框架或 provider SDK。
- 保留 provider SDK 缺失时的条件装配和启动回归，不向 Starter 新增 provider SDK 依赖。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `langchain4j-starter-modules`：统一 LangChain4j adapter Starter 继续显式安装默认实现，但具体 Spring Boot 自动配置必须归属现有 `fw-plus-ai-boot-autoconfigure`，不得再发布独立 autoconfigure artifact。
- `ai-neutral-starter-boundary`：将无具体框架痕迹的约束精确限定到中立包、公开契约、Magic-API 和传递依赖；统一 autoconfigure artifact 可在隔离子包中包含条件化框架实现。

## Impact

- 受影响模块：`fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-langchain4j-adapter-spring-boot-starter`、`fw-plus-ai-boot-dependencies` 和 `fw-plus-ai-project`。
- 受影响资源：Spring Boot 2.7 `spring.factories` 与现有 `AutoConfiguration.imports` 注册清单。
- 受影响测试：自动配置注册、缺失 provider SDK 启动、依赖边界、BOM/Reactor 坐标和旧 artifact 不存在性检查。
- 受影响文档：Starter README、AI 迁移说明及包含旧模块构建参数的验证文档。
- 不改变 Java 8 / Spring Boot 2.7.18 基线、中立配置前缀、adapter 选择规则、Core adapter 实现或具体 provider 行为。

```

## openspec/changes/move-langchain4j-adapter-autoconfigure/design.md

- Source: openspec/changes/move-langchain4j-adapter-autoconfigure/design.md
- Lines: 1-80
- SHA256: 7e06691a1b17b36e229c55da9dc91099f55b3c2dd6ed9997a462508d6ce3b3e4

```md
## Context

当前 AI Boot Reactor 同时发布 `fw-plus-ai-boot-autoconfigure` 和 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`。后者包含 46 个生产类、4 个测试类、`spring.factories` 与 `AutoConfiguration.imports`，但所有代码已经位于 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 命名空间。

业务应用通过 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 安装默认 LangChain4j 实现。该 Starter 同时依赖中立 model/store Starter、Core LangChain4j adapter 和独立 autoconfigure artifact。独立 artifact 还被 AI Reactor、Boot BOM、README 和验证命令单独维护。

本仓库保持 Java 8、Spring Boot 2.7.18 与 LangChain4j 0.35.0 基线。具体 provider SDK 继续由业务按需安装，Starter 不得批量传递 provider SDK。

## Goals / Non-Goals

**Goals:**

- 删除独立 LangChain4j adapter autoconfigure Maven 模块和发布坐标。
- 将其源码、资源与测试无行为变化地迁入 `fw-plus-ai-boot-autoconfigure`。
- 保留 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包级隔离和现有条件装配。
- 保留 LangChain4j adapter Starter 作为唯一推荐安装入口。
- 保持 provider SDK 缺失时可安全启动，并保持中立 Starter 不传递具体框架或 provider SDK。

**Non-Goals:**

- 不改变 Core `fw-plus-ai-langchain4j-adapter` 的 SPI 实现。
- 不修改 `fw-plus.ai.model`、`fw-plus.ai.embedding-store` 或 adapter 选择算法。
- 不增加、升级或替换任何 LangChain4j/provider SDK 版本。
- 不扁平化 `langchain4j` Java 子包，不迁移 AskData 业务实现。
- 不保留旧 autoconfigure 坐标的兼容空 POM。

## Decisions

### 1. 完整合并到现有 Boot autoconfigure artifact

将独立模块的 `src/main/java`、`src/test/java` 和 `src/main/resources` 合并到 `fw-plus-ai-boot-autoconfigure`，随后删除整个旧模块目录和 Reactor module 声明。保留 Java 包与类名，因此应用代码、条件注解和 Bean 名称不发生变化。

未采用兼容空 POM，因为它仍会保留一个没有独立职责的公开坐标；未采用只迁移入口配置类，因为 provider 配置、adapter 配置与测试仍会形成分裂所有权。

### 2. 以包级条件装配代替 artifact 级隔离

统一 artifact 的中立自动配置继续位于 `com.xiongx9527.fw.plus.ai.autoconfigure.model`、`store`、`magicapi` 等包；具体框架实现限定在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*`。LangChain4j 根配置继续以 adapter/Core SDK 类作为 classpath 条件，直接引用可选 provider SDK 类型的 Bean 方法继续保留精确 `@ConditionalOnClass(name = ...)`。

原“整个公共 autoconfigure artifact 不得包含 LangChain4j”测试改为验证包级隔离和传递依赖边界。中立 Core、Starter、Magic-API 和公共 API 仍不得引用具体框架。

### 3. 合并 POM 依赖但保持 optional 边界

旧模块的编译依赖迁入 `fw-plus-ai-boot-autoconfigure`，其中 LangChain4j provider wrapper、provider SDK 和 Core adapter 依赖继续标记为 `optional`。这样统一模块自身可以编译全部配置类，但依赖该 jar 的中立 Starter 不会传递这些依赖。

`fw-plus-ai-langchain4j-adapter-spring-boot-starter` 删除对旧 autoconfigure 坐标的依赖，显式依赖 `fw-plus-ai-boot-autoconfigure`，并继续显式依赖 Core adapter 与中立 model/store Starter。其 Maven Enforcer 规则继续只允许 `langchain4j-core`，禁止批量 provider SDK。

### 4. 合并自动配置元数据并保持注册兼容

把 `LangChain4jAdapterAutoConfiguration` 追加到统一模块的 `spring.factories`。旧模块的 `AutoConfiguration.imports` 路径与内容迁入统一模块，保留 Spring Boot 2.7 当前的双注册行为。注册测试改为从统一模块资源读取并断言目标类可加载、只注册一次且条件装配不变。

### 5. 测试随所有权迁移，保留缺 SDK 回归

四个 adapter 测试类原样迁入统一模块，再调整模块目录定位和边界断言。重点保留：

- 缺少 Milvus、Qdrant、Weaviate 等可选 SDK 时上下文可启动且相关 Bean 不注册。
- 直接链接 provider 类型的 Bean 方法仍声明精确 classpath 条件。
- Starter 的解析依赖不包含被禁止的 provider SDK。
- Reactor、BOM、Starter POM、文档和源码中不再出现旧 artifact 坐标。

## Risks / Trade-offs

- [Risk] 统一 autoconfigure jar 体积增加，并包含具体框架类名。 → 通过 `langchain4j` 子包隔离、optional Maven 依赖和 classpath 条件避免中立应用加载或传递具体实现。
- [Risk] 合并 `spring.factories` 与 `AutoConfiguration.imports` 后重复注册同一配置。 → 元数据测试同时读取两种注册文件，并通过上下文测试确认配置只产生一组 Bean。
- [Risk] POM 依赖迁移时遗漏 provider 编译依赖，导致统一模块无法编译。 → 以旧模块 POM 为清单逐项迁移，并运行统一模块全量测试与 dependency tree/enforcer 检查。
- [Risk] 删除公开坐标导致直接依赖旧 autoconfigure artifact 的应用解析失败。 → README 和迁移文档要求改用保留的 adapter Starter；不承诺兼容旧坐标。
- [Risk] 原边界测试被放宽过度。 → 仅对 `autoconfigure.langchain4j` 子包建立例外，继续扫描其他中立包、Starter POM、公开签名和 Magic-API。

## Migration Plan

1. 先补充/调整结构测试，使其期望旧模块和 BOM 坐标删除、统一模块拥有 LangChain4j 自动配置。
2. 将旧模块源码、资源和测试移动到 `fw-plus-ai-boot-autoconfigure`，合并 POM optional 依赖与注册元数据。
3. 修改 adapter Starter、AI Reactor 和 Boot BOM，删除旧模块目录与坐标。
4. 更新 README、迁移文档和验证命令，统一只引用 Boot autoconfigure 与 adapter Starter。
5. 运行 JDK 8 下的统一模块测试、Starter Maven Enforcer、AI Reactor validate/test、文档检查和 `git diff --check`。

回滚时整体恢复旧模块目录、Reactor/BOM 坐标和 Starter 依赖；不得只恢复旧 POM 而不恢复其源码与资源。

## Open Questions

无。模块归属、Starter 保留、包结构和兼容策略均已由用户确认。

```

## openspec/changes/move-langchain4j-adapter-autoconfigure/tasks.md

- Source: openspec/changes/move-langchain4j-adapter-autoconfigure/tasks.md
- Lines: 1-26
- SHA256: cd58d2870334d20b57769d48e56763d6051ec3916d4185be9f7462e4bdedbf84

```md
## 1. 结构回归与迁移准备

- [ ] 1.1 调整结构测试，先断言 AI Reactor、Boot BOM 和 Starter 不再引用 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`，且统一 Boot autoconfigure 拥有 LangChain4j 注册入口。
- [ ] 1.2 调整中立边界测试，仅允许 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包承载具体框架代码，并继续禁止中立 Starter 传递 adapter/provider SDK。
- [ ] 1.3 运行目标结构测试并记录预期 RED，确认失败原因是旧模块与旧边界仍存在。

## 2. 自动配置模块合并

- [ ] 2.1 将独立模块的生产源码迁入 `fw-plus-ai-boot-autoconfigure`，保留现有 Java 包、类名、Bean 名称和条件注解。
- [ ] 2.2 将旧模块 POM 的编译依赖合并进统一 autoconfigure POM，并保持 adapter、LangChain4j wrapper 和 provider SDK 依赖为 optional。
- [ ] 2.3 合并 `spring.factories` 与 `AutoConfiguration.imports`，确保 `LangChain4jAdapterAutoConfiguration` 在 Spring Boot 2.7 下保持注册。
- [ ] 2.4 将四个 adapter 测试类迁入统一模块并修正模块目录定位，不削弱缺失 provider SDK 和精确 `@ConditionalOnClass` 回归。

## 3. 坐标与文档清理

- [ ] 3.1 修改 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`，保留其公开坐标并将旧 autoconfigure 依赖替换为 `fw-plus-ai-boot-autoconfigure`。
- [ ] 3.2 从 AI Reactor 和 Boot BOM 删除旧 autoconfigure module/坐标，并删除独立模块目录。
- [ ] 3.3 更新 Starter README、AI 迁移文档和验证命令，删除旧坐标并说明自动配置已归入统一 Boot autoconfigure。
- [ ] 3.4 扩展旧 artifact 扫描，确保源码、POM、BOM、README 和当前验证命令不再把旧坐标当作可发布依赖。

## 4. 验证与交付证据

- [ ] 4.1 在 JDK 8 下运行 `fw-plus-ai-boot-autoconfigure` 全量测试，确认迁移后的注册、模型/store adapter 和缺 SDK 场景全部通过。
- [ ] 4.2 运行 LangChain4j adapter Starter 的 Maven Enforcer 与依赖树检查，确认只允许 `langchain4j-core` 且不传递 provider SDK。
- [ ] 4.3 运行 AI 目标 Reactor validate/test、OpenSpec 严格校验、文档引用检查和 `git diff --check`。
- [ ] 4.4 记录验证报告，列出删除坐标的破坏性影响、通过的测试数量和未触及的 Core/provider 行为。

```

## openspec/changes/move-langchain4j-adapter-autoconfigure/specs/ai-neutral-starter-boundary/spec.md

- Source: openspec/changes/move-langchain4j-adapter-autoconfigure/specs/ai-neutral-starter-boundary/spec.md
- Lines: 1-25
- SHA256: 3a250b023ea9f3e2279ee7d623b1a6074f288ea4d856d14c02ee05c40e661afc

```md
## MODIFIED Requirements

### Requirement: 中立 Starter 与公共运行时保持具体框架零痕迹
系统 SHALL 使 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、其所有中立 Starter、公共 API、Magic-API model/vector 实现以及 `fw-plus-ai-boot-autoconfigure` 中框架专属子包之外的代码只依赖中立 API 与 registry。具体框架自动配置 SHALL 隔离在统一 Boot autoconfigure artifact 的 `com.xiongx9527.fw.plus.ai.autoconfigure.<framework>.*` 子包，并通过 optional 依赖和 classpath 条件装配。

#### Scenario: 中立 artifact 没有具体框架依赖或公开类型
- **WHEN** 检查中立 Core artifact、中立 Starter、Magic-API、公开签名和框架专属子包之外的自动配置
- **THEN** 它们不得包含 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 类型、传递依赖、包引用或配置前缀。
- **AND** 它们 SHALL 保持 Java 8 兼容。

#### Scenario: 统一 autoconfigure 隔离具体框架实现
- **WHEN** `fw-plus-ai-boot-autoconfigure` 包含 LangChain4j 条件自动配置
- **THEN** 相关代码 SHALL 仅位于 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 子包。
- **AND** 具体框架与 provider 依赖 SHALL 标记为 optional，不得通过中立 Starter 传递。
- **AND** 缺少 adapter 或可选 SDK 时相关配置 SHALL 退让，不得导致中立应用启动时发生类链接失败。

#### Scenario: 应用显式安装 LangChain4j adapter
- **WHEN** 应用需要 LangChain4j 实现
- **THEN** 它 SHALL 显式引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 或等价 adapter artifact。
- **AND** 中立 Starter 本身不得隐式传递该 adapter。

#### Scenario: Magic-API 使用中立 registry
- **WHEN** Magic-API 创建或访问 model/vector 动态资源
- **THEN** 它 SHALL 仅通过中立 registry 与门面执行操作。
- **AND** 它不得创建或暴露框架原生 model、document 或 embedding store 类型。

```

## openspec/changes/move-langchain4j-adapter-autoconfigure/specs/langchain4j-starter-modules/spec.md

- Source: openspec/changes/move-langchain4j-adapter-autoconfigure/specs/langchain4j-starter-modules/spec.md
- Lines: 1-21
- SHA256: 840a354c809dcc259bd689a74a47da7fb2c8d6e72577a15ad8a570ceaec5e721

```md
## MODIFIED Requirements

### Requirement: LangChain4j 聚合暴露统一可选 adapter
系统 SHALL 在 LangChain4j 聚合和相关 BOM 中注册 `fw-plus-ai-langchain4j-adapter` 及其 adapter Starter，并删除四个旧 model/store artifact 以及 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 的模块、坐标和传递依赖。具体 LangChain4j Spring Boot 自动配置 SHALL 归属现有 `fw-plus-ai-boot-autoconfigure`，并保留在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包下。

#### Scenario: 聚合和 BOM 不再发布旧坐标
- **WHEN** 检查 Core AI 聚合、AI Boot 聚合、LangChain4j 聚合和 Core/AI Boot BOM
- **THEN** 它们不得列出四个已删除 model/store artifact 或 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`。
- **AND** 它们 SHALL 管理统一 adapter 及其 adapter Starter。

#### Scenario: 默认 LangChain4j 仍可显式安装
- **WHEN** Boot 2.7 / Java 8 应用引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`
- **THEN** 它 SHALL 通过 `fw-plus-ai-boot-autoconfigure` 获得模型和向量 store 的 LangChain4j 条件自动配置。
- **AND** 它 SHALL 获得 `fw-plus-ai-langchain4j-adapter` 提供的中立 SPI 实现。
- **AND** 它不得传递 Spring AI、Spring AI Alibaba、AgentScope 2 Java SDK 或未显式允许的 LangChain4j provider SDK。

#### Scenario: 统一自动配置保留包结构和注册行为
- **WHEN** 检查 `fw-plus-ai-boot-autoconfigure` 的源码和 Spring Boot 自动配置元数据
- **THEN** LangChain4j 配置类 SHALL 保留在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包下。
- **AND** `LangChain4jAdapterAutoConfiguration` SHALL 通过 Spring Boot 2.7 支持的元数据注册。
- **AND** 缺少可选 provider SDK 时对应 Bean SHALL 不注册且应用上下文 SHALL 可启动。

```
