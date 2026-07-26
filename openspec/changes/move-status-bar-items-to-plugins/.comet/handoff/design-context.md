# Comet Design Handoff

- Change: move-status-bar-items-to-plugins
- Phase: design
- Mode: compact
- Context hash: c7aa929a416a68d1fa8ca9e2e9d67a9c39eb93704e6050d9dc69404b139e862a

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/move-status-bar-items-to-plugins/proposal.md

- Source: openspec/changes/move-status-bar-items-to-plugins/proposal.md
- Lines: 1-31
- SHA256: d6e1b91f5a08a04dea42b763582f69e7621dc048d632c4e49323970d38f92e92

```md
## Why

`magic-status-bar.vue` 仍通过 13 个 `hasXxxPlugin` 响应式变量、插件名判断和专属点击函数维护可选插件入口，导致宿主持续依赖插件业务细节。Kafka、Pulsar 已证明状态栏项可以由插件通过 `statusBarItems` 自注册，因此其余可选插件也应采用同一边界。

## What Changes

- 将 Config、Freemarker、DTP、LangChain4j、WebSocket、MQTT、Debezium、Vue3、Sa-Token 的状态栏项迁入各自前端插件。
- 为 Druid、Spring Boot Admin、Swagger、Eureka 增加插件自有的最小前端模块和 IIFE，并补齐后端加载元数据。
- 从 `magic-status-bar.vue` 删除全部 `hasXxxPlugin` 状态、插件存在性检查、专属图标项和专属点击函数。
- 保留宿主对 `plugin.statusBarItems` 的通用收集、配置过滤、排序、渲染与事件刷新机制。
- 保持所有入口原有图标、标题、`displayKey`、跳转地址和在线人数后的展示位置。
- 为状态栏项和所有现有静态插件 toolbar 增加明确的前端 `order`，由宿主执行稳定排序。
- 通过通用 `Plugin.attributes` 向前端工厂传递 Admin、Eureka 等运行时路径，不在宿主保留插件名分支。
- 增加插件契约、宿主边界、构建产物与跨层元数据测试。

## Capabilities

### New Capabilities

- `magic-plugin-status-bar-extension`: 规范可选插件状态栏入口的所有权、注册协议、排序、插件 toolbar 排序、宿主边界和兼容行为。

### Modified Capabilities

无。

## Impact

- 影响 Magic-API 宿主控制台的状态栏与插件加载逻辑。
- 影响 Boot 插件 console、AI LangChain4j 插件资源、Cloud Eureka 插件元数据，以及 Druid/Admin/Swagger 等后端插件描述。
- 扩展 `Plugin` 的通用运行时属性和前端工厂参数，但不改变业务接口、数据源格式或脚本侧 API。
- 用户已确认因跨 Boot、Cloud、AI 多模块协调而升级为完整 Comet 流程，并选择在当前 `dev` 分支直接工作。

```

## openspec/changes/move-status-bar-items-to-plugins/design.md

- Source: openspec/changes/move-status-bar-items-to-plugins/design.md
- Lines: 1-66
- SHA256: 047a8ba215ae19930eca1cd1b99be3a245911b9c058884c93d725671e5b52a31

```md
## Context

宿主状态栏已经能够从 `constants.PLUGINS` 通用收集 `plugin.statusBarItems`，但除 Kafka/Pulsar/S3 外，其余可选插件仍由宿主通过 `hasXxxPlugin` 和 `checkPlugin` 分支控制。Druid、Admin、Swagger、Eureka 目前没有前端 IIFE，LangChain4j 只有预构建 IIFE，没有可维护的 console 源码。

## Goals / Non-Goals

**Goals:**

- 所有可选插件状态栏入口均由插件自身声明。
- 宿主状态栏不包含任何具体插件名、资源类型或跳转地址。
- 保持原入口显示条件、图标、标题和点击行为，并通过 `order` 固化展示顺序。
- 所有现有静态插件 toolbar 通过同一排序语义获得稳定顺序。

**Non-Goals:**

- 不调整 Gitee、GitHub、QQ群、帮助和退出等宿主固定入口。
- 不改变插件业务页面、后端管理端点或权限模型。
- 不新增 toolbar，不改变动态 `ADD_FOOTER_TOOLBAR` 的点击打开行为。
- 不重做状态栏视觉样式。

## Decisions

### 1. IIFE 插件直接注册状态栏项

有 console 源码的插件在入口返回对象中增加 `statusBarItems`，使用插件自己的 SVG symbol 和原有 `displayKey`。外链点击函数由插件闭包持有，宿主只负责调用。

### 2. 无 IIFE 集成补充最小前端插件

Druid、Spring Boot Admin、Swagger、Eureka 分别增加独立前端插件模块和 IIFE。对应 Java `plugin()` 只提供 `globalName`、`javascriptFilename` 和通用运行时属性，不提供状态栏描述。LangChain4j 恢复 console 构建来源，禁止直接维护压缩产物。

### 3. 通用运行时属性传给前端工厂

`Plugin` 增加通用 `attributes`，加载器调用 IIFE 工厂时传入当前 `pluginData`。Admin、Eureka 使用 `attributes.path` 传递配置路径，替代过去复用 `globalName` 存放路径的行为。

### 4. 状态栏和静态插件 toolbar 统一排序

`statusBarItems` 和静态插件 `toolbars` 使用前端 `order`。宿主稳定排序：有效 `order` 升序，缺失或非法值排在最后，同序保持插件加载顺序。内置 toolbar 和动态 toolbar 不参与该排序。

### 5. 宿主只保留通用扩展点

`magic-status-bar.vue` 删除所有 `hasXxxPlugin`、`checkPlugin`、插件专属 `icons` 项和 `openXxx` 函数。插件加载器在 `constants.PLUGINS` 变化后发出通用事件，状态栏据此刷新，按 `config.header[displayKey]` 过滤和排序，并在在线人数之后渲染；不再依赖状态文案或定时轮询。

### 6. 分区实施

按 Boot 现有插件、四个新增插件与后端装配、AI LangChain4j、宿主通用机制和 toolbar 排序五个互不重叠写入分区实施，主任务负责协议审查和集成验证。

## Risks / Trade-offs

- [Admin/Eureka 自定义路径丢失] -> 使用 `Plugin.attributes.path`，覆盖默认路径和自定义路径测试。
- [插件 IIFE 图标不可用] -> 复用插件现有 SVG symbol，构建产物检查 symbol 与 `statusBarItems` 同时存在。
- [加载时序导致入口缺失] -> 使用专用通用事件刷新，并覆盖初始加载、失败隔离和多插件场景。
- [排序改变内置或动态 toolbar] -> 排序范围限定为静态插件 toolbar，并增加边界测试。
- [跨层契约不一致] -> Java 元数据测试、前端契约测试和实际 IIFE/Maven 资源构建共同验证。

## Migration Plan

1. 定稿状态栏、toolbar 排序和 `Plugin.attributes` 契约。
2. 迁移有 IIFE 的插件入口。
3. 新增四个最小前端插件并补齐后端加载元数据。
4. 删除宿主专属状态和行为。
5. 补齐所有现有静态插件 toolbar 的 `order` 并实现通用排序。
6. 运行各插件、宿主、Boot/Cloud/AI 构建与 OpenSpec 验证。

## Open Questions

无。关键方案已在 full Design 阶段确认。

```

## openspec/changes/move-status-bar-items-to-plugins/tasks.md

- Source: openspec/changes/move-status-bar-items-to-plugins/tasks.md
- Lines: 1-25
- SHA256: ad1e7c47e723170d32a87dad64d245b9227381ae6b761856c71748e963bded6d

```md
## 1. 通用扩展契约

- [ ] 1.1 实现 `statusBarItems` 与静态插件 `toolbars` 的稳定 `order` 语义、通用插件变更事件和边界测试。
- [ ] 1.2 为 `Plugin` 增加通用 `attributes`，并向 IIFE 工厂传入当前 `pluginData`。

## 2. 前端插件迁移

- [ ] 2.1 将 Config、Freemarker、DTP、WebSocket、MQTT、Debezium、Vue3、Sa-Token 的状态栏入口迁入各自 Boot 插件 console，并验证构建产物。
- [ ] 2.2 将 LangChain4j 状态栏入口迁入 AI 插件前端资源，恢复可维护的构建来源并验证产物。

## 3. 新增前端插件

- [ ] 3.1 为 Druid、Spring Boot Admin、Swagger、Eureka 创建独立最小前端插件、IIFE 资源和后端加载配置，覆盖 Admin/Eureka 自定义路径。

## 4. 宿主解耦

- [ ] 4.1 删除 `magic-status-bar.vue` 中全部 `hasXxxPlugin`、`checkPlugin`、专属图标项、点击函数、文案判断和轮询，只保留通用插件状态栏机制及边界测试。

## 5. Toolbar 排序

- [ ] 5.1 为所有现有静态插件 toolbar 补充明确 `order`，宿主稳定排序且不改变内置、动态 toolbar 和 `adminOnly` 行为。

## 6. 集成验证

- [ ] 6.1 运行受影响插件、宿主、Boot/Cloud/AI 构建，检查 IIFE 与 Maven 资源产物，完成浏览器交互、OpenSpec 严格验证和差异检查。

```

## openspec/changes/move-status-bar-items-to-plugins/specs/magic-plugin-status-bar-extension/spec.md

- Source: openspec/changes/move-status-bar-items-to-plugins/specs/magic-plugin-status-bar-extension/spec.md
- Lines: 1-106
- SHA256: 7e5ad666f6ce723f3b8ea5cc1139365c99b59a2c00f835da50bec8024e951c38

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 可选插件拥有自己的状态栏入口
每个可选插件 SHALL 通过自己的前端插件对象声明状态栏项。状态栏项 MUST 包含图标、标题、`displayKey`、`order` 和点击动作，且插件未加载时对应入口 MUST NOT 出现。后端插件元数据 MUST NOT 承载状态栏描述。

#### Scenario: 加载有前端 IIFE 的插件
- **WHEN** 控制台成功加载一个提供 `statusBarItems` 的插件 IIFE
- **THEN** 状态栏展示该插件声明的入口
- **AND** 点击行为由插件声明而不是宿主插件名分支提供

#### Scenario: 加载新增的最小前端插件
- **WHEN** Druid、Spring Boot Admin、Swagger 或 Eureka 集成启用
- **THEN** 控制台加载该集成自有的 IIFE
- **AND** 状态栏入口由 IIFE 返回的 `statusBarItems` 提供

#### Scenario: 插件没有加载前端 IIFE
- **WHEN** 后端插件元数据没有有效的 `globalName` 或 `javascriptFilename`
- **THEN** 宿主不为该插件生成状态栏入口

### Requirement: 宿主状态栏与具体插件解耦
`magic-status-bar.vue` MUST NOT 包含具体可选插件的存在性状态、资源类型判断、图标项或点击函数。宿主 SHALL 仅通用收集、校验、过滤、排序、刷新和渲染插件状态栏项。

#### Scenario: 构建不含任何可选插件的宿主
- **WHEN** 宿主控制台未加载可选插件
- **THEN** 宿主仍可独立构建和运行
- **AND** 状态栏不显示任何插件入口

#### Scenario: 同时加载多个可选插件
- **WHEN** 多个插件分别声明状态栏项
- **THEN** 宿主通过同一扩展点渲染所有允许显示的入口
- **AND** 不执行任何按插件名或资源类型编写的专属判断

#### Scenario: 插件加载列表发生变化
- **WHEN** 插件加载器成功加入一个插件对象
- **THEN** 加载器发出通用插件变更事件
- **AND** 状态栏重新收集插件项
- **AND** 刷新不依赖状态文案内容或定时轮询

#### Scenario: 插件声明非法状态栏项
- **WHEN** 状态栏项缺少图标、标题、`displayKey` 或点击函数
- **THEN** 宿主忽略该项目
- **AND** 其他插件入口继续正常显示
- **AND** 页面不显示错误内容

### Requirement: 状态栏兼容原有行为
迁移后的插件状态栏入口 MUST 保持原有图标、标题、`displayKey`、跳转目标和配置开关行为，并 SHALL 位于在线人数之后、宿主内置入口之前。

#### Scenario: 配置隐藏插件入口
- **WHEN** `config.header[displayKey]` 为 `false`
- **THEN** 对应插件状态栏项不显示

#### Scenario: 点击迁移后的插件入口
- **WHEN** 用户点击任一迁移后的插件状态栏项
- **THEN** 控制台执行迁移前相同的外链或相对管理地址跳转

#### Scenario: 按声明顺序渲染插件入口
- **WHEN** 多个有效状态栏项包含不同的 `order`
- **THEN** 宿主按 `order` 数值升序稳定排列

#### Scenario: 状态栏项缺少合法顺序
- **WHEN** 状态栏项没有 `order` 或 `order` 不是合法数值
- **THEN** 该项目排在具有合法 `order` 的项目之后
- **AND** 同序项目保持插件加载顺序

### Requirement: 插件构建产物携带状态栏声明
受影响插件的构建产物 MUST 包含 `statusBarItems`、对应 SVG symbol、`order` 和点击逻辑。Druid、Spring Boot Admin、Swagger、Eureka MUST 提供可由宿主加载的独立 IIFE，LangChain4j MUST 提供可维护的 console 构建来源。

#### Scenario: 构建插件前端
- **WHEN** 执行受影响插件的前端和 Maven 资源构建
- **THEN** 每个 IIFE 产物包含该插件的状态栏声明

#### Scenario: 获取插件元数据
- **WHEN** 宿主请求插件列表
- **THEN** Druid、Spring Boot Admin、Swagger、Eureka 返回有效的 `globalName` 和 `javascriptFilename`
- **AND** 宿主可从 classpath 提供对应 IIFE 资源

### Requirement: 插件运行时属性传入前端工厂
后端 `Plugin` SHALL 支持通用 `attributes`，插件加载器 SHALL 将当前插件元数据作为 `pluginData` 传入对应前端工厂。状态栏组件 MUST NOT 直接解析这些属性或插件名称。

#### Scenario: 打开自定义 Spring Boot Admin 路径

```

Full source: openspec/changes/move-status-bar-items-to-plugins/specs/magic-plugin-status-bar-extension/spec.md
