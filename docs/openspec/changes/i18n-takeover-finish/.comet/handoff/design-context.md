# Comet Design Handoff

- Change: i18n-takeover-finish
- Phase: design
- Mode: compact
- Context hash: 73341c029c7605c9b6953601bb9710891fbace2d4ad7ea8833498524c57d4b85

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/i18n-takeover-finish/proposal.md

- Source: docs/openspec/changes/i18n-takeover-finish/proposal.md
- Lines: 1-27
- SHA256: ffa0235393d6c0d6804f4df2c105ed74ec23c6638ed72116196d824f1311eca8

```md
# Proposal

## Why

`dev` 分支上已提交多轮国际化基础设施与模块文案迁移（搭建 MessageSource 合并、异常按状态码解析文案、各模块 messages 资源），但工作树仍残留一批未提交改动：`I18nAutoConfiguration` 以 `@PostConstruct` + `SpringUtils.getBean` 注入 `MessageSource`，在自动配置初始化时静态 context 尚未就绪会 NPE；另有 `boot-dependencies`/`magicapi-plugin` 的 pom 调整与 `magicapi-spring-boot-starter-sample` 的集成测试改动尚未收尾。本 change 接管这批半成品，消除 NPE 隐患并让样例集成测试通过。

## What Changes

- 将 `I18nAutoConfiguration.initResolver` 由 `@PostConstruct` + `SpringUtils.getBean(MessageSource.class)` 改为 `@Autowired(required = false) MessageSource`，由容器注入，避免静态 context 未就绪导致的 NPE。
- 同步 `boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml` 中已在工作树调整但未提交的依赖/版本关系。
- 修正 `fw-plus-magicapi-spring-boot-starter-sample` 中因 i18n 迁移而需更新的集成测试（`MagicApiAllPluginsApplicationContextTest`、`MagicApiAllPluginsClasspathTest`），使其通过。
- 不新增尚未迁移模块的 i18n 文案迁移；不改动与 i18n 无关的代码。

## Capabilities

### New Capabilities
- `i18n`: 框架级国际化消息解析能力——合并多 Bundle 的 `MessageSource`、异常按状态码解析用户可见文案、自动配置以容器方式注入 `MessageSource`。本 change 落定自动配置注入策略这一行为契约。

### Modified Capabilities
<!-- 无既有 capability 的需求级别变更 -->

## Impact

- 受影响代码：`fw-plus-boot-autoconfigure` 的 `I18nAutoConfiguration`；`fw-plus-boot-dependencies`、`fw-plus-magicapi-plugin` 的 pom；`fw-plus-magicapi-spring-boot-starter-sample` 测试。
- 受影响 API：无对外 API 签名变更；`I18nMessageResolver.setMessageSource` 调用方由自动配置内部使用，行为不变。
- 依赖：无新增依赖。
- 风险：自动配置注入时机变化，需样例集成测试验证容器能正确注入 `MessageSource`。

```

## docs/openspec/changes/i18n-takeover-finish/design.md

- Source: docs/openspec/changes/i18n-takeover-finish/design.md
- Lines: 1-67
- SHA256: ee0ecb325d7893352947f1dce72b1b9c6b0aaa9beebf7c34a0e254d6000ba8b0

```md
---
comet_change: i18n-takeover-finish
role: technical-design
canonical_spec: openspec
---

# Design: i18n-takeover-finish

## 背景

本 change 接管 `dev` 工作树中已存在但未提交的国际化收尾改动，目标是消除 `I18nAutoConfiguration` 的 NPE 隐患并让样例集成测试通过，不引入新的 i18n 行为设计。

## 确认的技术方案

### 1. MessageSource 注入方式

原实现：

```java
@PostConstruct
public void initResolver() {
    I18nMessageResolver.setMessageSource(SpringUtils.getBean(MessageSource.class));
}
```

问题：`@PostConstruct` 执行时 `SpringUtils` 静态持有的 `ApplicationContext` 可能尚未设置（自动配置早于部分上下文回调），`getBean` 会 NPE。

改为容器注入：

```java
@Autowired(required = false)
public void initResolver(MessageSource messageSource) {
    I18nMessageResolver.setMessageSource(messageSource);
}
```

由 Spring 在填充 `I18nAutoConfiguration` Bean 属性阶段直接注入 `MessageSource`，不依赖静态 context。`required = false` 容忍缺失，缺失时 `I18nMessageResolver` 退回无 MessageSource 的默认行为（与现状一致）。

### 2. 范围边界（已确认）

- 仅收尾工作树既有改动：`I18nAutoConfiguration.java`、`boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml`、样例集成测试。
- 不新增任何模块级的 i18n 文案迁移（既有迁移已在先前提交完成）。
- 不改动 `I18nMessageResolver`/`ComposableMessageSource`/`BundleLoader` 的解析逻辑。
- **排除无关改动**：工作树中另有 86 个 `magicapi-plugin-word` console 资源删除、以及 `AGENTS.md`/`CLAUDE.md` 改动，与本次无关，归档时按 dirty-worktree 协议排除，不纳入本 change。

### 3. 验证策略

- `fw-plus-boot-autoconfigure` 的 `BootAutoconfigureI18nTest` 验证注入行为。
- `fw-plus-magicapi-spring-boot-starter-sample` 的 `MagicApiAllPluginsApplicationContextTest` 与 `MagicApiAllPluginsClasspathTest` 必须绿灯，验证自动配置注入与全插件 classpath 在 i18n 收尾后仍正常。

## 关键取舍与风险

- 取舍：`required = false` 容忍无 `MessageSource` 场景，回退默认行为，与现状一致；不引入新的静态解析路径。
- 风险：注入时机变化需样例集成测试验证；无关工作树改动须在归档时精确排除，避免污染提交。

## 数据流

```
Spring 容器
  └─> I18nAutoConfiguration Bean 属性填充
        └─> @Autowired MessageSource -> I18nMessageResolver.setMessageSource()
              └─> 异常/文案解析时由 MessageSource 解析多语言文案
```

## Spec Patch

无。现有 `specs/i18n/spec.md` 已覆盖「容器注入 MessageSource」与「按状态码解析文案」验收场景，无需回写。

```

## docs/openspec/changes/i18n-takeover-finish/tasks.md

- Source: docs/openspec/changes/i18n-takeover-finish/tasks.md
- Lines: 1-7
- SHA256: e621b520c5a7847aebd97f3a392709d77831f0a768f25e9de1ab7cbe5e28c4ea

```md
# Tasks: i18n-takeover-finish

- [ ] 1. 将 `I18nAutoConfiguration.initResolver` 改为 `@Autowired(required = false) MessageSource` 注入，移除 `SpringUtils`/`@PostConstruct` 用法
- [ ] 2. 同步工作树中 `boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml` 的未提交依赖/版本调整（仅保留与 i18n 收尾相关项）
- [ ] 3. 更新 `fw-plus-magicapi-spring-boot-starter-sample` 的 `MagicApiAllPluginsApplicationContextTest` / `MagicApiAllPluginsClasspathTest`，适配 i18n 收尾
- [ ] 4. 运行 `fw-plus-boot-autoconfigure` 的 i18n 测试与样例集成测试，确保全绿
- [ ] 5. 校验改动仅含本 change 范围，排除 word 插件 console 删除、AGENTS.md/CLAUDE.md 等无关改动

```

## docs/openspec/changes/i18n-takeover-finish/specs/i18n/spec.md

- Source: docs/openspec/changes/i18n-takeover-finish/specs/i18n/spec.md
- Lines: 1-27
- SHA256: 021eb2cf00b53d8ca9205f8cc27d9da0435ffd37c5d7786da85b49a84bf2fc33

```md
# Spec: i18n

国际化消息解析能力：合并多 Bundle 的 `MessageSource`、异常按状态码解析用户可见文案，自动配置以容器方式注入 `MessageSource`。

## Requirement: 自动配置以容器方式注入 MessageSource

`I18nAutoConfiguration` 必须把容器中的 `MessageSource` 注入到静态解析器 `I18nMessageResolver`，且注入过程不得依赖尚未就绪的静态 `SpringUtils` context。

### Scenario: 自动配置初始化时注入
- **WHEN** Spring 容器完成 `I18nAutoConfiguration` 的 Bean 初始化且容器中存在 `MessageSource`
- **THEN** `I18nMessageResolver` 持有的 `MessageSource` 为容器注入的实例，且初始化过程不抛出 NPE

### Scenario: 容器中不存在 MessageSource
- **WHEN** 容器中没有 `MessageSource` Bean
- **THEN** 自动配置以 `required = false` 容忍缺失，不阻断应用启动

## Requirement: 用户可见文案经 i18n 解析

业务与框架抛出的异常、插件返回文案、校验提示、WebSocket 原因、展示名等用户可见中文文案，应走 `I18nMessageResolver`/`ComposableMessageSource` 按状态码或 key 解析，支持 `messages.properties` 与 `messages_en.properties` 双语言。

### Scenario: 按状态码解析异常文案
- **WHEN** 框架内置异常携带 8 位状态码被抛出
- **THEN** 其用户可见消息由 i18n 文案资源解析，而非写死中文

### Scenario: 默认语言回退
- **WHEN** 请求语言无对应翻译
- **THEN** 回退到 `messages.properties` 默认文案

```
