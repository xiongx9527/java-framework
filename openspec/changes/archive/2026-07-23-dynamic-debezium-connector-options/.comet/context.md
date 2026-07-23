# Comet Design Handoff

- Change: dynamic-debezium-connector-options
- Phase: design
- Mode: compact
- Context hash: 1290896d15366a1801040671d139a47ec6bd61f6389d0944559f1556db368bfa

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/dynamic-debezium-connector-options/proposal.md

- Source: openspec/changes/dynamic-debezium-connector-options/proposal.md
- Lines: 1-28
- SHA256: ae8c918110c34c20ad1bbbcf43d3bddb8f5ffc9a3ac87fd8979fa522f12c90d2

```md
## Why

Debezium 数据源编辑表单目前在前端组件中硬编码 `connectorClass` 下拉选项，支持范围与运行时实际可用的 Debezium connector 依赖容易漂移。需要把连接器选项改为动态来源，让控制台随插件后端能力或配置变化而更新。

## What Changes

- 新增 Debezium connector 选项的动态获取能力，由插件后端暴露当前可选的 connector class 列表。
- 修改 Debezium 数据源编辑组件，使 `connectorClass` 下拉项从动态接口加载，而不是维护前端硬编码数组。
- 保留现有 MySQL、PostgreSQL、SQL Server、Oracle、MongoDB connector class 作为默认可用范围，但默认范围由后端集中维护。
- 当已有数据源保存了当前列表之外的 `connectorClass` 时，表单仍应能展示并保留该值，避免编辑保存时丢失配置。
- 不新增 Debezium connector 依赖，不改变 Debezium 引擎创建、运行、故障恢复或数据源持久化格式。

## Capabilities

### New Capabilities
- `debezium-connector-options`: Debezium 数据源控制台能够从后端动态加载连接器 class 选项，并兼容已有自定义 connector 值。

### Modified Capabilities

无。

## Impact

- 影响前端组件：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/components/magic-debezium-datasource-info.vue`
- 影响前端服务封装：`fw-plus-boot/.../fw-plus-magicapi-plugin-debezium/src/console/src/service/magic-debezium-datasource.js`
- 影响后端控制器：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/web/MagicDebeziumController.java`
- 可能新增后端常量、配置属性或简单 DTO，用于集中定义 connector class 选项。
- 验证需覆盖前端构建/静态检查，以及后端接口在 JDK 1.8 与 Spring Boot 2.7.18 下编译通过。

```

## openspec/changes/dynamic-debezium-connector-options/design.md

- Source: openspec/changes/dynamic-debezium-connector-options/design.md
- Lines: 1-65
- SHA256: bb70bd93039ca427df4e062472ff2a3dcae93059499f52ed2753aa9e0900865a

```md
## Context

Debezium 插件控制台的 `magic-debezium-datasource-info.vue` 直接维护 `connectorOptions` 常量，并把它传给 `magic-select`。后端 `MagicDebeziumController` 当前主要提供数据源测试能力，实际 Debezium 引擎创建逻辑位于 `MagicDynamicDebeziumDataSource`，数据源动态注册由 `DebeziumDataSourceMagicDynamicRegistry` 负责。

本次需求来自数据源编辑体验：connector class 的可选项应由运行时或插件后端统一提供，而不是散落在前端构建产物中。项目约束包括 JDK 1.8、Spring Boot 2.7.18、现有 Magic-API 插件控制台请求封装，以及不引入新的 Debezium connector 依赖。

## Goals / Non-Goals

**Goals:**

- 前端 Debezium 数据源表单从动态接口加载 `connectorClass` 下拉选项。
- 后端集中定义并返回当前支持的 Debezium connector class 选项。
- 已保存但不在动态列表内的 connector class 在编辑时仍可展示和保留。
- 保持现有数据源 JSON 字段、测试接口和引擎启动行为兼容。

**Non-Goals:**

- 不新增 MySQL/PostgreSQL/SQL Server/Oracle/MongoDB 之外的 Debezium connector 依赖。
- 不实现运行时安装 connector jar 或插件热加载。
- 不修改 Debezium 引擎配置生成、Kafka topic 规则、故障恢复逻辑。
- 不调整 Magic-API 通用资源管理交互。

## Decisions

1. 后端提供 Debezium connector options 接口。

   方案：在 Debezium 插件后端新增只读接口，例如 `/debezium/datasource/connector-options`，返回 `[{ text, value }]` 或等价 DTO。前端服务封装新增方法调用该接口。

   理由：connector 支持范围属于运行时能力，后端比前端更接近 classpath、配置和版本事实。相比继续在 Vue 组件内维护常量，这能减少前后端漂移；相比让前端读取外部 JSON，后端接口更容易跟 Spring 配置、条件装配和权限模型集成。

2. 默认连接器列表由后端集中维护，并可按实际 classpath 做可用性过滤。

   方案：后端维护当前默认 connector class 候选集，并在返回前检查 class 是否可加载；不可加载的候选项不返回。若后续需要用户配置扩展，可在同一服务层扩展配置属性，不影响前端协议。

   理由：完全扫描 classpath 查找 connector 实现成本高、结果不稳定，也可能暴露非预期类；显式候选集加 classpath 过滤更可控。相比纯配置列表，该方案保留开箱即用体验。

3. 前端保留已有值兼容。

   方案：组件加载动态选项后，如果 `info.connectorClass` 存在且不在返回列表中，则临时追加一个 `{ text: value, value }` 选项供当前表单展示。保存时仍保存原字段值。

   理由：老配置或用户手工填过的自定义 connector 不应因为下拉列表变化而在编辑时不可见或被清空。

4. 接口失败时不阻断表单编辑。

   方案：前端请求失败时至少保留当前 `info.connectorClass` 作为可选项，并允许用户继续编辑其他字段；错误不应影响已有数据源查看。

   理由：动态选项是编辑辅助能力，不应成为读取/保存已有配置的单点故障。

## Risks / Trade-offs

- 动态接口返回空列表 → 表单下拉没有默认候选项。缓解：后端默认候选集覆盖现有五类 connector，并在测试中验证至少返回 classpath 中可用项。
- classpath 过滤导致某些历史硬编码选项不再展示 → 已有值兼容逻辑会把当前值补回，但新建数据源只展示实际可用项。
- 前端异步加载存在短暂空状态 → 使用响应式 `connectorOptions` 初始化为空，并在加载后更新；必要时保留当前值避免视觉跳动影响保存。
- 后端 DTO 与 Magic Select 结构耦合 → 可先返回通用 `text/value` 结构，保持与现有前端组件一致；若后续有更多元数据，再增量扩展字段。

## Migration Plan

1. 后端新增 connector options 查询能力，默认候选集与当前前端硬编码列表保持一致。
2. 前端服务新增获取 connector options 方法。
3. Vue 组件删除本地硬编码 `connectorOptions` 常量，改为加载服务数据并补齐当前值。
4. 验证已有 Debezium 数据源 JSON 不需要迁移；失败回滚时恢复前端常量即可。

## Open Questions

无阻塞问题。后续实现时可以根据现有 controller 风格确定精确返回包装类型和 URL 命名。

```

## openspec/changes/dynamic-debezium-connector-options/tasks.md

- Source: openspec/changes/dynamic-debezium-connector-options/tasks.md
- Lines: 1-23
- SHA256: 0b1488f926c41cc7fd3628906708c37256fa0756b284863a30c49918612f84e3

```md
## 1. 后端连接器选项

- [ ] 1.1 新增 Debezium 连接器下拉选项的数据结构，使用 `text` 和 `value` 字段承载前端选择项。
- [ ] 1.2 在后端集中维护默认 Debezium connector class 候选列表，内容与当前控制台已有选项保持一致。
- [ ] 1.3 返回默认候选项前按运行时 classpath 过滤不可加载的 connector class，并保持兼容 JDK 1.8。
- [ ] 1.4 在 Debezium 控制器中新增只读接口，返回当前可用的连接器选项。

## 2. 控制台服务接入

- [ ] 2.1 在 Debezium 控制台服务封装中新增加载连接器选项的方法。
- [ ] 2.2 保持服务返回结构与现有 `magic-select` 的选项对象格式兼容。

## 3. Vue 表单接入

- [ ] 3.1 将 `magic-debezium-datasource-info.vue` 中硬编码的 `connectorOptions` 数组替换为从服务加载的响应式选项。
- [ ] 3.2 当后端返回列表不包含当前 `info.connectorClass` 时，将当前值补充到本次表单的本地选项中。
- [ ] 3.3 当连接器选项加载失败时，不清空、不阻断已有数据源表单中的当前连接器值。

## 4. 验证

- [ ] 4.1 使用对应模块的 Maven Wrapper 验证受影响的 Debezium 后端模块可以编译通过。
- [ ] 4.2 执行 Debezium 控制台插件可用的前端构建或静态检查命令。
- [ ] 4.3 确认 `dynamic-debezium-connector-options` 的 OpenSpec 校验通过。

```

## openspec/changes/dynamic-debezium-connector-options/specs/debezium-connector-options/spec.md

- Source: openspec/changes/dynamic-debezium-connector-options/specs/debezium-connector-options/spec.md
- Lines: 1-34
- SHA256: 18f13486b9ddb7d3f502689634fade1ae16d9398fac32c2dc5af4edadf981615

```md
## ADDED Requirements

### Requirement: Dynamic connector option loading
Debezium datasource editing SHALL load connector class options from the Debezium plugin backend instead of relying on a connector option list hardcoded in the Vue datasource info component.

#### Scenario: Opening the datasource form
- **WHEN** a user opens the Debezium datasource create or edit form
- **THEN** the connector class select SHALL request connector options from the backend and render the returned options

#### Scenario: Backend support changes
- **WHEN** the backend connector option response changes because available connector support or configuration changes
- **THEN** the connector class select SHALL reflect the backend response without requiring a source edit to the Vue component connector list

### Requirement: Default connector compatibility
The backend SHALL provide default Debezium connector options for the connector classes currently offered by the console when those classes are available at runtime.

#### Scenario: Existing default connectors are available
- **WHEN** the runtime contains the existing MySQL, PostgreSQL, SQL Server, Oracle, or MongoDB Debezium connector classes
- **THEN** the backend connector option response SHALL include the corresponding connector class values

#### Scenario: Connector class is unavailable
- **WHEN** a default connector class cannot be loaded from the runtime classpath
- **THEN** the backend connector option response SHALL omit that connector class from the selectable defaults

### Requirement: Preserve existing custom connector values
The datasource form SHALL preserve and display an existing `connectorClass` value even when that value is not present in the dynamically loaded option list.

#### Scenario: Editing a datasource with a custom connector
- **WHEN** a user edits an existing Debezium datasource whose `connectorClass` is not returned by the backend connector option response
- **THEN** the connector class select SHALL still display the existing value as a selectable option for that form instance

#### Scenario: Dynamic option request fails
- **WHEN** the datasource form cannot load connector options from the backend
- **THEN** the form SHALL continue to display and preserve the current `connectorClass` value if one exists

```
