---
comet_change: add-magicapi-db-browser
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-24-add-magicapi-db-browser
status: final
---

# Magic-API DB 只读浏览与插件 UI 归属设计

## 背景

Magic-API 当前已有 JDBC datasource 的保存、动态注册、连接测试和脚本侧 `db` 使用能力，但控制台缺少面向已配置数据源的只读浏览能力。用户在开发和排查接口时，需要快速查看表、视图、函数、存储过程、字段、索引以及少量数据预览。

本设计同时修正控制台 UI 归属边界：新增 DB 管理模块必须作为插件 UI 交付；现有 S3 文件管理 UI 也必须从宿主控制台迁回 `fw-plus-magicapi-plugin-s3/src/console`。宿主 `fw-plus-magicapi-spring-boot-starter/src/console` 只保留通用插件加载和挂载能力，不维护 S3/DB 这类插件业务页面。

## 目标

- 复用现有 JDBC datasource 注册和连接池生命周期，不新增第二套 datasource 持久化结构。
- 提供 catalog/schema、表、视图、函数、存储过程、字段、主键、索引等只读元数据浏览接口。
- 提供表/视图分页数据预览，支持基础排序、简单过滤和最大行数限制。
- 后端不开放任意 SQL，不提供 DDL/DML、函数调用、存储过程调用或写操作入口。
- DB 管理前端作为独立 Magic-API 插件 UI 交付，不写入宿主控制台业务页面。
- S3 文件管理 UI 迁移到 S3 插件 UI，清理宿主控制台内的 S3 专属页面、入口和文案。

## 非目标

- 不实现完整数据库客户端，不提供 SQL 编辑器。
- 不支持建表、改表、改数据、删数据或执行脚本。
- 不改变现有 datasource JSON 保存结构。
- 不替换 Magic-API 原生 `db` 脚本模块。
- 第一版不追求覆盖触发器、事件、序列、物化视图等厂商专属对象。

## 总体架构

后端新增 DB 浏览 Controller/Service，放在 Magic-API 自动配置相关模块中，复用现有 `MagicDynamicDataSource`、`DataSourceInfo` 和动态注册体系。服务层从已注册 datasource 获取连接，通过 JDBC `DatabaseMetaData` 读取元数据；数据预览只接收结构化请求参数，由受控 SQL Builder 生成只读 SELECT。

前端新增 DB 插件控制台资源，形态参照 `fw-plus-magicapi-plugin-s3/src/console`，通过插件 IIFE 注册入口、图标、i18n、服务封装和页面组件。S3 文件管理 UI 也归入 S3 插件 IIFE；宿主控制台移除 `magic-s3.vue` 这类业务页面和 S3 专属工具栏/状态栏逻辑。

数据流：

1. 插件 UI 从控制台插件运行时获取 request、modal、i18n、JavaClass 等宿主能力。
2. 用户选择已注册 JDBC datasource key。
3. DB 插件调用 DB 浏览 Controller 获取 catalog/schema/object 列表。
4. 用户选择表或视图后，插件调用详情接口获取字段、主键、索引。
5. 数据预览请求只传对象定位、分页、排序和简单过滤条件。
6. 后端校验 datasource、对象和列名均来自元数据白名单后，使用 PreparedStatement 执行只读 SELECT。

## 后端设计

### DTO 边界

定义独立 DTO，避免直接暴露 JDBC 或 datasource 内部对象：

- datasource 摘要：`key`、`name`、driver/product 信息和连接状态。
- namespace：catalog、schema，以及驱动缺失字段时的空值兼容。
- database object：名称、类型、catalog、schema、remarks。
- column：名称、JDBC type、typeName、size、decimalDigits、nullable、defaultValue、remarks、ordinal。
- primary key/index：名称、列、顺序、唯一性、索引类型中驱动可提供的字段。
- routine：函数/存储过程名称、类型、catalog、schema、remarks。
- preview：columns、rows、page、pageSize、hasMore、appliedSort、appliedFilters。

### 元数据读取

元数据主路径使用 `Connection.getMetaData()`：

- `getCatalogs()` 和 `getSchemas()` 读取命名空间。
- `getTables(catalog, schema, pattern, types)` 读取表和视图。
- `getProcedures()`、`getFunctions()` 读取存储过程和函数。
- `getColumns()`、`getPrimaryKeys()`、`getIndexInfo()` 读取结构详情。

不同驱动返回字段不一致时，响应结构保持稳定，缺失字段允许为空。后续如需增强 MySQL/PostgreSQL/Oracle/SQL Server 差异，可在 Service 后增加 dialect adapter，但第一版不把方言查询作为主路径。

### 数据预览

数据预览不接受 SQL 文本，只接受结构化参数：datasource key、catalog、schema、objectName、objectType、page、pageSize、sortColumn、sortDirection、filters。

SQL Builder 规则：

- objectName、schema、catalog、sortColumn、filter column 必须先通过元数据白名单校验。
- identifier 使用当前数据库的 quote string 处理；驱动未返回 quote string 时使用保守策略并拒绝异常 identifier。
- pageSize 使用默认值和最大值限制。
- 排序方向只允许 `ASC`/`DESC`。
- 过滤只支持明确列出的简单操作符，例如 `eq`、`ne`、`like`、`gt`、`gte`、`lt`、`lte`、`isNull`、`isNotNull`。
- 过滤值全部通过 PreparedStatement 绑定。
- 只构造 SELECT，不构造 DDL/DML，不提供调用 routine 的入口。

`hasMore` 可通过请求 `pageSize + 1` 条记录实现，返回前截断为实际 pageSize。

### 权限与错误

所有接口遵循 Magic-API 控制台认证和 VIEW 权限规则。错误响应统一处理：datasource 不存在、连接失败、元数据读取失败、对象不存在、列名非法、过滤条件非法、权限失败。错误信息需要便于排查，但不得返回密码、完整连接串或敏感配置。

## 前端设计

### DB 插件 UI

新增 DB 管理插件控制台资源，结构参照 S3 插件：

- `src/index.js` 注册插件入口、图标、i18n 和页面组件。
- `src/service/*` 封装 DB 浏览接口。
- `src/components/*` 实现数据源选择、对象浏览、详情和数据预览。
- `src/i18n/zh-cn.js`、`src/i18n/en.js` 放插件自己的文案。

交互布局保持工作台风格：左侧数据源和对象树，中间/右侧展示对象详情；表和视图详情使用字段、主键、索引、数据预览分区。函数和存储过程只展示元数据，不提供执行按钮。

必须覆盖状态：加载中、空 schema、空表、无权限、连接失败、请求失败、过滤非法、数据预览为空。请求失败时保留当前 datasource 和已加载上下文，方便重试或切换。

### S3 插件 UI 迁移

现有宿主控制台中的 S3 文件管理页面和入口需要迁移到 `fw-plus-magicapi-plugin-s3/src/console`：

- 将 `magic-s3.vue` 等文件管理页面能力迁入 S3 插件组件。
- 将 S3 专属工具栏/状态栏入口迁为 S3 插件 IIFE 注册的入口。
- 将 S3 专属图标和 i18n 文案迁到 S3 插件资源。
- 宿主控制台删除 S3 业务页面、S3 专属入口判断和 S3 专属文案，只保留通用插件加载/挂载机制。

迁移后，S3 插件启用且 `adminEnabled=true` 时仍应提供原有文件管理入口和交互能力。

## 安全边界

- 不接受任意 SQL 文本。
- 不提供 DDL/DML、函数调用、存储过程调用入口。
- identifier 必须来自已读取的元数据白名单。
- 过滤值必须使用 PreparedStatement 参数绑定。
- pageSize 必须有最大值限制。
- 错误响应不得泄露密码、完整连接串等敏感信息。
- 插件 UI 不能绕过 Magic-API 控制台认证和权限规则。

## 测试策略

后端测试：

- 使用 H2 或可控 DataSource 覆盖元数据列表、表/视图详情、可选字段为空、分页预览、pageSize 限制。
- 覆盖排序字段白名单、未知列拒绝、安全过滤和 SQL 注入防护。
- 覆盖 datasource 不存在、连接失败、权限失败和统一错误响应。

前端测试/验证：

- DB 插件执行构建或静态检查，确认 IIFE 可打包。
- S3 插件执行构建或静态检查，确认迁移后的文件管理 UI 可打包。
- 通过源码检查确认宿主控制台不再包含 S3/DB 业务页面、专属入口和专属 i18n。

集成验证：

- 运行相关 Maven 模块测试或编译，确认 JDK 1.8 与 Spring Boot 2.7.18 兼容。
- 运行 `openspec validate add-magicapi-db-browser` 确认规格合法。
- 最终运行前端插件构建和必要的后端测试命令。

## 回滚策略

后端新增 DB 浏览 Controller/Service 和 DTO 可直接移除，不需要迁移用户数据。前端回滚时移除 DB 插件资源，并恢复或调整 S3 插件 UI 变更。由于不修改 datasource JSON 和脚本侧 `db` API，回滚不会影响现有 Magic-API 脚本运行方式。

## 实施顺序

1. 实现后端 DTO、元数据服务和只读预览 SQL Builder。
2. 接入 Controller、权限和异常响应。
3. 创建 DB 插件 UI 并完成数据源/对象/详情/预览工作流。
4. 将 S3 文件管理 UI 从宿主迁入 S3 插件 UI。
5. 清理宿主控制台内 S3 专属页面、入口和文案。
6. 补测试、构建和说明文档。
