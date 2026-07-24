# Comet Design Handoff

- Change: add-magicapi-db-browser
- Phase: design
- Mode: compact
- Context hash: e7e788baee0b5b2520894379d493800111c6659ea5db4b090102d822df723cc4

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-magicapi-db-browser/proposal.md

- Source: openspec/changes/add-magicapi-db-browser/proposal.md
- Lines: 1-34
- SHA256: 336f72dc4141928cb9bd87aef16451998cdd1e418d05d062990974e578650935

```md
## Why

Magic-API 控制台当前可以配置 JDBC 数据源，但缺少面向已配置数据源的只读数据库浏览能力。开发和排查接口时，用户需要在控制台内快速查看表、视图、函数、存储过程以及表/视图数据预览，减少在外部数据库客户端和 Magic-API 控制台之间来回切换。

## What Changes

- 新增 Magic-API DB 只读浏览能力，参照 `magic-s3` 插件形态提供控制台入口和后端管理接口。
- 调整现有 `magic-s3` 文件管理 UI 的归属：宿主控制台中已有的 S3 管理页面和入口需要迁移到 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI 中。
- 支持选择已有 JDBC 数据源后浏览 catalog/schema、表、视图、函数、存储过程等元数据。
- 支持查看表/视图字段、主键、索引等详情。
- 支持表/视图数据分页预览，并提供基础排序、过滤和行数限制。
- 后端优先基于 JDBC `DatabaseMetaData` 实现通用能力，并对常见数据库行为做兼容处理。
- 所有 DB 浏览接口必须遵循 Magic-API 控制台登录与权限规则。
- 本次明确保持只读，不提供执行任意 SQL、建表改表、修改数据、删除数据等写操作。
- 本次不改变已有 datasource JSON 保存结构，不替换现有 Magic-API `db` 脚本模块。
- 不拆分为多个 change：本次是单一用户能力“只读 DB 浏览器”，后端 API、控制台入口和数据预览需要协同交付，拆分会导致前后端能力互相等待。

## Capabilities

### New Capabilities

- `magicapi-db-browser`: Magic-API 控制台提供已配置 JDBC 数据源的只读元数据浏览和数据预览能力。

### Modified Capabilities

- `magic-s3-console-ui`: S3 文件管理 UI 从宿主控制台迁移到 S3 插件控制台资源，宿主控制台不再维护 S3 专属页面和工具栏逻辑。

## Impact

- 影响 `fw-plus-boot-autoconfigure` 中 Magic-API JDBC 数据源相关 Controller、服务或插件自动配置。
- 新增 DB 前端 UI 必须放在独立 Magic-API 插件 UI 中，参照 `fw-plus-magicapi-plugin-s3` 组织资源；同时迁移现有 S3 文件管理 UI 到 `fw-plus-magicapi-plugin-s3/src/console`，清理宿主 `fw-plus-magicapi-spring-boot-starter/src/console` 中的 S3 专属页面、入口和文案。
- 复用现有 `MagicDynamicDataSource`、`DataSourceInfo`、数据源动态注册和连接测试能力。
- 新增后端单元测试，覆盖元数据查询、分页预览、只读限制、权限约束和异常处理。
- 前端需要处理不同数据库元数据不一致、空 schema、空表、无权限访问、连接失败等状态。

```

## openspec/changes/add-magicapi-db-browser/design.md

- Source: openspec/changes/add-magicapi-db-browser/design.md
- Lines: 1-69
- SHA256: 788a4b1533ad5d71f3b0f3460f344e00a65404fccf88dab894353b1f11b7ba85

```md
## Context

Magic-API 当前已有 JDBC datasource 的保存、动态注册、连接测试和脚本侧 `db` 使用能力。已有后端基础包括 `MagicDynamicDataSource`、`DataSourceInfo`、`MyDataSourceMagicDynamicRegistry` 以及控制台接口 `/magic-api/web/datasource/options` 和 `/magic-api/web/datasource/myjdbc/test`。

本变更新增的是控制台侧只读 DB 浏览能力：用户选择一个已注册 JDBC 数据源后，在 Magic-API 控制台查看数据库对象和少量数据预览。它不是 SQL 客户端，也不是数据库管理/迁移工具。

## Goals / Non-Goals

**Goals:**

- 复用已有 JDBC 数据源注册结果，避免新增第二套数据源配置和连接池生命周期。
- 提供 catalog/schema、表、视图、函数、存储过程、字段、主键、索引等元数据浏览接口。
- 提供表/视图数据分页预览，支持基础排序、过滤和行数上限。
- 后端保持只读边界，所有接口遵循 Magic-API 控制台认证和权限规则。
- 前端参照 `magic-s3` 的控制台插件组织方式，为 DB 浏览能力提供入口和交互页面；UI 只放在插件 UI 中，不写入宿主控制台主 UI。
- 迁移现有 `magic-s3` 文件管理 UI：宿主控制台中已有的 S3 文件管理页面、状态栏/工具栏入口和 S3 专属 i18n 文案，需要归入 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI；宿主只保留通用插件加载/挂载能力。

**Non-Goals:**

- 不支持执行任意 SQL。
- 不支持 INSERT、UPDATE、DELETE、DDL、存储过程调用或函数调用。
- 不修改 datasource JSON 持久化格式。
- 不替换 Magic-API 原生 `db` 脚本模块。
- 不在第一版实现数据库厂商专属对象的完整覆盖，例如触发器、事件、序列、物化视图等。
- 不把 DB 浏览 UI 塞入宿主 `fw-plus-magicapi-spring-boot-starter/src/console` 主控制台。

## Decisions

### Decision: 使用 JDBC DatabaseMetaData 作为元数据主路径

后端 DB 浏览服务优先通过 `Connection.getMetaData()` 读取 catalog、schema、tables、views、procedures、functions、columns、primary keys、indexes 等信息。这样可复用现有 JDBC driver 和数据源配置，第一版能覆盖 MySQL、PostgreSQL、SQL Server、Oracle 等常见驱动的基础元数据。

替代方案是为每种数据库写 `information_schema` 或系统表查询。该方案信息更完整，但会快速扩大数据库方言范围，不适合作为第一版默认路径。后续可以在通用接口后增加按 driver/product name 分派的方言增强。

### Decision: 数据预览使用受控 SQL Builder，不开放任意 SQL

表/视图数据预览只接收结构化参数：数据源 key、catalog、schema、对象名、对象类型、页码、页大小、排序字段、排序方向和简单过滤条件。后端必须使用 identifier quote/白名单校验构造 SELECT，并使用 PreparedStatement 绑定过滤值。

替代方案是直接提供 SQL 输入框。该方案功能强，但会把模块变成 SQL 客户端，带来写操作、权限和注入风险，违背只读范围。

### Decision: 控制台插件参照 magic-s3，并修正 S3 UI 归属

前端必须按 `fw-plus-magicapi-plugin-s3/src/console` 的方式组织为独立插件资源，例如新增 `fw-plus-magicapi-plugin-db` 或同等插件 UI。后端不新建第二套 datasource 存储，而是在 `fw-plus-boot-autoconfigure` 中新增 DB 浏览 Controller/Service，复用 `MagicDynamicDataSource` 获取已注册连接。

现有 S3 文件管理 UI 不能继续留在宿主 `fw-plus-magicapi-spring-boot-starter/src/console`。实现阶段需要把宿主中的 `magic-s3.vue` 相关页面、工具栏/状态栏入口、S3 专属图标与文案迁到 `fw-plus-magicapi-plugin-s3/src/console`，通过 S3 插件 IIFE 注册自己的管理入口；宿主控制台只负责发现和加载插件，不包含 S3 业务界面。

替代方案是把 DB 浏览完全塞入主 starter 控制台。该方案文件更少，但会污染宿主 UI，且不符合“UI 写到插件 UI 里面”的边界，因此排除。

### Decision: 第一版保持只读和有限数据量

数据预览必须限制最大页大小和默认行数，避免控制台误拉大表。异常信息返回时应适合排查但不泄露密码、完整连接串等敏感配置。

## Risks / Trade-offs

- [Risk] 不同 JDBC driver 对 `DatabaseMetaData` 支持不一致 → Mitigation: 返回统一 DTO，允许字段为空；测试覆盖 H2/MySQL 风格关键路径，并为后续方言扩展保留接口边界。
- [Risk] 表名、schema、排序字段参与 SQL 构造有注入风险 → Mitigation: 对 identifier 做元数据白名单校验和 quote 处理；过滤值全部使用 PreparedStatement 参数绑定。
- [Risk] 大表预览造成慢查询或内存压力 → Mitigation: 限制最大 pageSize，默认分页，只返回当前页字段和值。
- [Risk] 控制台用户可能把该模块理解为完整数据库客户端 → Mitigation: UI 和后端都不提供任意 SQL 输入和写操作入口。
- [Risk] S3 文件管理 UI 迁移可能影响原有状态栏/工具栏打开方式 → Mitigation: 在 S3 插件内保留等价入口和交互能力，宿主侧仅移除 S3 专属耦合，不移除通用插件加载机制。

## Migration Plan

- 新增能力默认不改变已有 datasource 配置和脚本运行行为。
- 若插件前端资源未加载，已有 Magic-API 控制台仍可照常使用。
- 回滚时移除新增 DB 浏览 Controller/Service 和前端插件资源即可，不需要迁移用户数据。

## Open Questions

- 第一版需要重点验证的实际数据库驱动列表仍待实现阶段结合项目依赖确认；默认按 JDBC 通用能力实现，优先兼容 MySQL/PostgreSQL 常见行为。

```

## openspec/changes/add-magicapi-db-browser/tasks.md

- Source: openspec/changes/add-magicapi-db-browser/tasks.md
- Lines: 1-41
- SHA256: 52144cc025e3e7b001d1c56edef6b8829f61edd1949aee50fd464ffad1acc70d

```md
## 1. 后端模型与元数据服务

- [ ] 1.1 定义 DB 浏览响应/请求 DTO，覆盖数据源、catalog/schema、对象、字段、主键、索引、例程和分页数据预览。
- [ ] 1.2 实现基于 `MagicDynamicDataSource` 获取已注册 JDBC 数据源连接的只读服务入口。
- [ ] 1.3 实现基于 `DatabaseMetaData` 的 catalog/schema、表、视图、函数、存储过程列表查询。
- [ ] 1.4 实现表/视图字段、主键、索引详情查询，并兼容驱动缺失可选元数据字段。

## 2. 数据预览与只读安全

- [ ] 2.1 实现表/视图数据分页预览，返回列信息、当前页数据、分页参数和 hasMore 标记。
- [ ] 2.2 实现 identifier 白名单校验和 quote 处理，确保表名、schema、排序字段只能来自元数据结果。
- [ ] 2.3 实现安全过滤条件解析，仅允许后端支持的简单操作符，并使用 PreparedStatement 绑定过滤值。
- [ ] 2.4 实现 pageSize 默认值和最大值限制，避免单次预览返回过多数据。
- [ ] 2.5 拒绝任意 SQL、写操作、DDL/DML、函数调用和存储过程调用入口。

## 3. 后端控制台接口与自动配置

- [ ] 3.1 新增 DB 浏览 Controller，暴露数据源列表/对象列表/对象详情/数据预览接口。
- [ ] 3.2 将 DB 浏览 Controller 和服务接入 Magic-API 自动配置，遵循现有插件或控制台 Controller 注册方式。
- [ ] 3.3 为所有 DB 浏览接口接入 Magic-API 控制台认证和 VIEW 权限校验。
- [ ] 3.4 统一处理不存在数据源、连接失败、元数据读取失败和权限失败的响应。

## 4. 控制台插件与宿主 UI 边界

- [ ] 4.1 参照 `magic-s3` 创建 DB 管理控制台插件结构、服务封装、图标和国际化文案。
- [ ] 4.2 实现数据源选择和对象树/对象列表浏览界面。
- [ ] 4.3 实现表/视图详情页，展示字段、主键、索引和分页数据预览。
- [ ] 4.4 实现基础排序、过滤、分页、加载态、空状态和错误状态。
- [ ] 4.5 确保前端插件构建产物按现有 Magic-API 插件资源方式打包和加载。
- [ ] 4.6 将宿主控制台中已有的 S3 文件管理 UI、S3 专属工具栏/状态栏入口和 S3 i18n 文案迁移到 `fw-plus-magicapi-plugin-s3/src/console`。
- [ ] 4.7 清理 `fw-plus-magicapi-spring-boot-starter/src/console` 中的 S3 业务页面和专属入口，仅保留通用插件加载/挂载能力。

## 5. 测试与验证

- [ ] 5.1 为元数据服务添加单元测试，覆盖对象列表、详情查询和驱动可选字段为空场景。
- [ ] 5.2 为数据预览添加单元测试，覆盖分页限制、排序字段白名单、安全过滤和 SQL 注入防护。
- [ ] 5.3 为 Controller 添加测试，覆盖认证/权限、数据源不存在和异常响应。
- [ ] 5.4 运行相关 Maven 测试或模块编译，确认 JDK 1.8 与 Spring Boot 2.7.18 兼容。
- [ ] 5.5 运行前端插件构建或静态检查，确认控制台资源可打包。
- [ ] 5.6 运行 S3 插件前端构建或静态检查，确认迁移后的 S3 插件 UI 可打包。
- [ ] 5.7 更新必要说明文档，记录只读范围、支持对象类型、插件 UI 边界和使用方式。

```

## openspec/changes/add-magicapi-db-browser/specs/magicapi-db-browser/spec.md

- Source: openspec/changes/add-magicapi-db-browser/specs/magicapi-db-browser/spec.md
- Lines: 1-116
- SHA256: 0053bfe3fa66ddb37db3345ca2004c2d01af7b53f0b5867854653ba411018321

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Authenticated users can browse registered datasource metadata
系统 SHALL 允许已通过 Magic-API 控制台认证且具备查看权限的用户选择已注册 JDBC 数据源，并浏览该数据源暴露的 catalog、schema、表、视图、函数和存储过程元数据。

#### Scenario: Authenticated user lists database objects
- **WHEN** 已认证控制台用户选择一个存在且已注册的 JDBC 数据源
- **THEN** 系统 MUST 返回该数据源可读取的 catalog/schema 列表
- **AND** 系统 MUST 返回表、视图、函数和存储过程的列表或按类型筛选后的列表

#### Scenario: Missing datasource is rejected
- **WHEN** 用户请求浏览不存在或未注册的数据源 key
- **THEN** 系统 MUST 返回失败响应
- **AND** 系统 MUST NOT 创建新的数据源配置或连接配置

#### Scenario: Unauthenticated user is rejected
- **WHEN** 未认证请求访问 DB 浏览接口
- **THEN** 后端 MUST 按 Magic-API 控制台权限规则拒绝该请求

### Requirement: Users can inspect table and view structure
系统 SHALL 允许用户查看表和视图的结构详情，包括字段、主键和索引信息。

#### Scenario: User opens table details
- **WHEN** 用户选择一个表对象查看详情
- **THEN** 系统 MUST 返回字段名、JDBC 类型、类型名称、长度、精度、是否可空、默认值和备注信息中当前驱动可提供的字段
- **AND** 系统 MUST 返回主键信息和索引信息中当前驱动可提供的字段

#### Scenario: Driver omits optional metadata
- **WHEN** JDBC 驱动未返回某些可选元数据字段
- **THEN** 系统 MUST 保留响应结构
- **AND** 系统 MUST 允许对应字段为空

### Requirement: Users can preview table and view data safely
系统 SHALL 允许用户分页预览表和视图数据，但 MUST 保持只读并限制单次返回数据量。

#### Scenario: User previews first page
- **WHEN** 用户请求预览某个表或视图的第一页数据
- **THEN** 系统 MUST 返回列信息、当前页行数据、页码、页大小和是否还有更多数据
- **AND** 系统 MUST 使用默认页大小或用户提供的合法页大小

#### Scenario: Page size exceeds limit
- **WHEN** 用户请求的 pageSize 超过系统允许的最大值
- **THEN** 系统 MUST 将实际 pageSize 限制在最大值以内
- **AND** 系统 MUST NOT 返回超过最大值的行数

#### Scenario: User sorts by a known column
- **WHEN** 用户指定的排序字段存在于目标表或视图字段列表中
- **THEN** 系统 MUST 按指定方向返回分页数据

#### Scenario: User sorts by an unknown column
- **WHEN** 用户指定的排序字段不存在于目标表或视图字段列表中
- **THEN** 系统 MUST 拒绝该请求
- **AND** 系统 MUST NOT 拼接执行包含该未知字段的 SQL

#### Scenario: User filters by simple column value
- **WHEN** 用户指定字段、操作符和值进行简单过滤
- **THEN** 系统 MUST 只允许后端支持的安全过滤操作符
- **AND** 系统 MUST 使用 PreparedStatement 参数绑定过滤值

### Requirement: DB browser remains read-only
系统 SHALL 明确禁止 DB 浏览模块执行写操作、任意 SQL、DDL、DML、函数调用和存储过程调用。

#### Scenario: Arbitrary SQL is not accepted
- **WHEN** 请求包含任意 SQL 文本或写操作意图
- **THEN** DB 浏览接口 MUST 拒绝该请求
- **AND** 系统 MUST NOT 执行该 SQL

#### Scenario: Stored routines are listed but not invoked
- **WHEN** 用户浏览函数或存储过程
- **THEN** 系统 MUST 只返回函数或存储过程的元数据信息
- **AND** 系统 MUST NOT 提供调用函数或存储过程的接口

### Requirement: Console plugin provides a DB browsing workflow
系统 SHALL 通过 Magic-API 插件控制台资源提供 DB 浏览入口，使用户可以选择数据源、浏览对象树、查看对象详情并预览数据；DB 浏览 UI MUST NOT 写入宿主 `fw-plus-magicapi-spring-boot-starter/src/console` 主控制台业务页面。

#### Scenario: User browses from datasource to data preview
- **WHEN** 用户进入 DB 管理模块
- **THEN** 前端 MUST 展示可选择的数据源入口
- **AND** 用户选择数据源后 MUST 能浏览对象列表
- **AND** 用户选择表或视图后 MUST 能查看结构详情和分页数据预览

```

Full source: openspec/changes/add-magicapi-db-browser/specs/magicapi-db-browser/spec.md
