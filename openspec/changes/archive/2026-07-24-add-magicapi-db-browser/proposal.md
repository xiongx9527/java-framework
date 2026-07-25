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
