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
