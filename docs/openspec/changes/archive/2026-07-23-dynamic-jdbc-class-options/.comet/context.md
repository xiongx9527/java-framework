# Comet Design Handoff

- Change: dynamic-jdbc-class-options
- Phase: design
- Mode: compact
- Context hash: 5c8e36f49a6304801ae1bb964dd3cea3364a5efa0fb64ada55ce662a898defad

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/dynamic-jdbc-class-options/proposal.md

- Source: openspec/changes/dynamic-jdbc-class-options/proposal.md
- Lines: 1-28
- SHA256: f370678d6cfeaa0cbb0ffd2d279a8227d288485b3723c41e413ab9c0c1ab39a3

```md
## Why

Magic-API 数据源编辑表单当前内置 JDBC Driver 和 DataSource 类型候选值，容易显示当前运行环境并不存在的类。用户选择不存在的类后会在保存或运行时失败，体验上也无法体现框架实际可用能力。

## What Changes

- 将 JDBC Driver 下拉候选从纯前端固定列表调整为运行时可用类列表。
- 将 DataSource 类型下拉候选从纯前端固定列表调整为运行时可用类列表。
- 仅当对应类在当前应用 classpath 中存在时，才向前端配置下发并在下拉中展示。
- 保留输入框可手工输入能力，便于用户使用未被自动候选覆盖但实际可配置的类名。
- 保留现有默认候选作为扫描候选基线，但不存在的类不再显示。
- 不引入运行时上传 JDBC 驱动 jar、远程下载依赖或热加载外部 jar 的能力。

## Capabilities

### New Capabilities
- `dynamic-jdbc-class-options`: Magic-API 数据源配置页面根据当前运行环境动态展示可用 JDBC Driver 和 DataSource 类型候选。

### Modified Capabilities
- 无。

## Impact

- 影响前端 Magic-API 控制台数据源编辑表单候选项来源。
- 影响后端 Magic-API/Spring Boot 自动配置向控制台注入的编辑器配置。
- 可能涉及 `fw-plus-boot-autoconfigure` 中 Magic-API 数据源注册与 DataSourceBuilder 相关配置。
- 不改变已有 datasource JSON 文件格式，仍使用 `driverClassName` 和 `type` 字段。
- 不改变具体数据源创建和连接池初始化逻辑。

```

## openspec/changes/dynamic-jdbc-class-options/design.md

- Source: openspec/changes/dynamic-jdbc-class-options/design.md
- Lines: 1-66
- SHA256: c2a05c8468d0d79dde7ccb36f867f2f45cc5e258cd182361213e6442cf766360

```md
## Context

Magic-API 控制台的数据源表单当前使用前端 `constants.JDBC_DRIVERS` 和 `constants.DATASOURCE_TYPES` 作为下拉候选。`magic-editor.vue` 已允许通过 `config.jdbcDrivers` 和 `config.datasourceTypes` 覆盖默认值，说明前端已经具备接收后端动态配置的入口。

后端现有 Magic-API 数据源注册逻辑包含 DataSourceBuilder 映射和默认 DataSource 类型数组，但前端候选列表仍可能展示当前应用 classpath 中不存在的 Driver 或 DataSource 类。用户要求“有对应的类，下拉才显示”，因此候选列表必须由运行时类存在性决定。

## Goals / Non-Goals

**Goals:**

- JDBC Driver 下拉只展示当前 classpath 中存在的驱动类。
- DataSource 类型下拉只展示当前 classpath 中存在且可被当前系统识别/构建的数据源类型。
- 后端提供动态候选接口，前端启动后请求接口并使用返回的 `jdbcDrivers` 和 `datasourceTypes` 覆盖现有候选常量。
- 保留表单手动输入能力，避免阻塞高级用户输入自定义类名。
- 保持现有 datasource JSON 结构兼容。

**Non-Goals:**

- 不实现运行时上传 JDBC driver jar、远程下载依赖或自定义 ClassLoader 热加载。
- 不改变数据源保存格式、连接测试协议或动态数据源注册主流程。
- 不强制自动识别所有第三方连接池；没有对应类或 builder 的类型不作为候选展示。

## Decisions

### 运行时 classpath 过滤

候选列表以系统维护的候选基线为输入，后端使用当前应用 ClassLoader 检测类是否存在。只有 `ClassUtils.isPresent(...)` 或等价机制确认存在的类，才进入下发配置。

选择该方案是因为它直接满足“有对应的类才显示”，同时不引入外部 jar 管理和安全风险。相比纯前端固定列表，它不会误导用户选择不存在的类；相比完全扫描 classpath，它更可控，也不会把无关类暴露给控制台。

默认 JDBC Driver 候选基线除传统关系型数据库驱动外，还应包含项目内图数据库 JDBC Driver 和 MongoDB SQL JDBC Driver，例如 `com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver`、`com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver`、`com.mongodb.jdbc.MongoDriver`。MongoDB BI Connector 传统 MySQL 协议场景继续由 MySQL Connector/J 候选覆盖。

### DataSource 类型以可构建能力为边界

DataSource 类型候选不仅要类存在，还应优先来自已注册 `DataSourceBuilder` 或框架明确支持的类型。若某个 DataSource 类存在但系统没有对应 builder，默认不展示为候选，除非现有通用构建逻辑已经能够安全创建它。

这样可以避免“类存在但保存后无法创建数据源”的半支持状态。

### 前端启动后拉取候选接口

后端新增一个 Magic-API 控制台可访问的候选接口，返回已经过滤后的 `jdbcDrivers` 和 `datasourceTypes` 数组。前端在编辑器初始化时请求该接口，成功后覆盖 `constants.JDBC_DRIVERS` 和 `constants.DATASOURCE_TYPES`；请求失败时保留现有默认常量作为兜底。

该方案比直接注入 `MAGIC_EDITOR_CONFIG` 多一次请求，但接口边界清晰，便于排查当前运行环境实际识别到哪些候选类，也便于后续扩展更多动态候选。

### 配置扩展策略

实现应允许通过 Spring/Magic-API 配置追加候选基线，例如新增 JDBC Driver 或 DataSource 类型的类名。追加项同样必须经过 classpath 存在性检测；不存在时不下发。

这让业务项目可在引入新依赖后通过配置扩展下拉，而无需改前端源码。

## Risks / Trade-offs

- [Risk] 某些 Driver 通过 `ServiceLoader` 注册但不在默认候选基线中 → 允许通过配置追加候选类名，追加后仍做 classpath 检测。
- [Risk] DataSource 类存在但没有 builder，展示后创建失败 → DataSource 候选应以已注册 builder 或可构建类型为边界。
- [Risk] 候选接口请求失败时回退默认固定列表，仍可能显示不存在的类 → 前端应在接口可用时优先使用接口结果；默认值只作为服务异常或离线开发兜底。
- [Risk] 旧项目依赖前端固定候选提示类名 → 表单保留手动输入能力，用户仍可输入自定义类名。

## Migration Plan

1. 在后端 Magic-API 控制台接口中加入动态 JDBC Driver 和 DataSource 类型候选查询能力。
2. 使用当前 ClassLoader 过滤默认候选和配置追加候选。
3. DataSource 类型合并已有 `DataSourceBuilder` bean 名称或明确支持的类型，并过滤不可用类。
4. 前端启动后请求候选接口，并在成功后覆盖 `constants.JDBC_DRIVERS` 和 `constants.DATASOURCE_TYPES`，无需改变 datasource JSON。
5. 增加单元测试覆盖 classpath 存在/不存在时的候选过滤行为。

回滚方式：移除后端动态配置下发后，前端会回退原固定常量列表。

```

## openspec/changes/dynamic-jdbc-class-options/tasks.md

- Source: openspec/changes/dynamic-jdbc-class-options/tasks.md
- Lines: 1-28
- SHA256: 5f6cf05f51b05a50190f132bb5a9820440601ec7acd22950443262c6c9058285

```md
## 1. 后端候选接口

- [ ] 1.1 新增 Magic-API 控制台候选接口，返回 `jdbcDrivers` 和 `datasourceTypes`。
- [ ] 1.2 增加后端候选提供器，合并默认 JDBC Driver 候选、图数据库 JDBC Driver 候选、MongoDB SQL JDBC Driver 候选和配置追加候选。
- [ ] 1.3 使用运行时应用 ClassLoader 过滤 JDBC Driver 候选，只返回当前 classpath 中存在的类。
- [ ] 1.4 增加 JDBC Driver 追加候选配置项，且不改变 datasource JSON 格式。

## 2. DataSource 类型过滤

- [ ] 2.1 复用现有 DataSourceBuilder 注册表和已支持 DataSource 类型基线作为候选输入。
- [ ] 2.2 按运行时类存在性过滤 DataSource 类型候选。
- [ ] 2.3 按可构建能力过滤 DataSource 类型候选，没有 builder 或安全通用构建支持的类默认不展示。
- [ ] 2.4 增加 DataSource 类型追加候选配置项，并应用同样的类存在性和可构建能力过滤。

## 3. 控制台集成

- [ ] 3.1 确保动态候选接口复用 Magic-API 控制台登录认证和权限规则，未登录请求必须被拒绝。
- [ ] 3.2 在 Magic-API 控制台登录完成后请求后端候选接口，并用返回结果覆盖 `constants.JDBC_DRIVERS` 和 `constants.DATASOURCE_TYPES`。
- [ ] 3.3 保留前端现有默认常量，作为候选接口不可用时的兜底。
- [ ] 3.4 保留 datasource 表单中 Driver 和 DataSource 类型字段的手动输入能力。

## 4. 验证

- [ ] 4.1 增加 JDBC Driver 候选过滤单元测试，覆盖类存在和类缺失两种情况。
- [ ] 4.2 增加 DataSource 类型过滤单元测试，覆盖类存在、类缺失、类存在但不支持构建三种情况。
- [ ] 4.3 增加测试或 smoke 验证，确认配置追加候选在候选接口返回前已完成过滤。
- [ ] 4.4 增加测试或 smoke 验证，确认未登录访问候选接口会被拒绝，登录后才会加载候选。
- [ ] 4.5 验证现有 datasource 保存和加载行为仍兼容当前 datasource JSON 文件。

```

## openspec/changes/dynamic-jdbc-class-options/specs/dynamic-jdbc-class-options/spec.md

- Source: openspec/changes/dynamic-jdbc-class-options/specs/dynamic-jdbc-class-options/spec.md
- Lines: 1-61
- SHA256: 307acf924986571d2c408786f0aacc821b52337bffad2a3ff3e754da85cd8164

```md
## ADDED Requirements

### Requirement: JDBC driver options reflect available classes
系统 SHALL 根据当前应用运行时 classpath 生成 JDBC Driver 下拉候选，只下发存在的 Driver 类名。

#### Scenario: Built-in graph and document SQL drivers are candidate inputs
- **WHEN** 系统生成默认 JDBC Driver 候选基线
- **THEN** 候选基线 MUST 包含 fw-plus Nebula JDBC Driver、fw-plus Neo4j JDBC Driver 和 MongoDB SQL JDBC Driver 类名
- **AND** 这些类名 MUST 仍经过 classpath 存在性过滤后才展示

#### Scenario: Existing driver class is shown
- **WHEN** 默认或配置候选中包含当前 classpath 存在的 JDBC Driver 类
- **THEN** 数据源表单 Driver 下拉候选 MUST 包含该类名

#### Scenario: Missing driver class is hidden
- **WHEN** 默认或配置候选中包含当前 classpath 不存在的 JDBC Driver 类
- **THEN** 数据源表单 Driver 下拉候选 MUST NOT 包含该类名

### Requirement: DataSource type options reflect constructible available classes
系统 SHALL 根据当前应用运行时 classpath 和数据源构建能力生成 DataSource 类型下拉候选，只下发存在且当前系统可识别/构建的 DataSource 类型。

#### Scenario: Existing supported DataSource type is shown
- **WHEN** 某个 DataSource 类型类存在于当前 classpath 且系统存在对应 DataSourceBuilder 或通用构建支持
- **THEN** 数据源表单类型下拉候选 MUST 包含该类名

#### Scenario: Missing DataSource type is hidden
- **WHEN** 默认或配置候选中包含当前 classpath 不存在的 DataSource 类型类
- **THEN** 数据源表单类型下拉候选 MUST NOT 包含该类名

#### Scenario: Unsupported DataSource type is hidden
- **WHEN** 某个 DataSource 类型类存在但当前系统没有对应 DataSourceBuilder 或通用构建支持
- **THEN** 数据源表单类型下拉候选 MUST NOT 默认包含该类名

### Requirement: Configured candidate classes are filtered before display
系统 SHALL 允许通过配置追加 JDBC Driver 和 DataSource 类型候选，但所有配置项在展示前 MUST 经过运行时可用性过滤。

#### Scenario: Configured available class is shown
- **WHEN** 用户通过配置追加一个当前 classpath 存在且符合候选类型要求的类名
- **THEN** 对应下拉候选 MUST 包含该类名

#### Scenario: Configured unavailable class is hidden
- **WHEN** 用户通过配置追加一个当前 classpath 不存在的类名
- **THEN** 对应下拉候选 MUST NOT 包含该类名

### Requirement: Manual input remains available
系统 SHALL 保留数据源表单 Driver 和 DataSource 类型字段的手动输入能力。

#### Scenario: User enters custom class name
- **WHEN** 用户需要输入未出现在下拉候选中的自定义 Driver 或 DataSource 类型类名
- **THEN** 表单 MUST 允许用户手动输入该类名

### Requirement: Dynamic class options are loaded after console authentication
系统 SHALL 在 Magic-API 控制台登录认证通过后加载动态 JDBC Driver 和 DataSource 类型候选接口。

#### Scenario: Authenticated console user loads options
- **WHEN** 用户已登录并进入 Magic-API 控制台
- **THEN** 前端 MUST 请求动态候选接口并用返回结果更新下拉候选

#### Scenario: Unauthenticated request is rejected
- **WHEN** 未认证请求访问动态候选接口
- **THEN** 后端 MUST 按 Magic-API 控制台权限规则拒绝该请求

```
