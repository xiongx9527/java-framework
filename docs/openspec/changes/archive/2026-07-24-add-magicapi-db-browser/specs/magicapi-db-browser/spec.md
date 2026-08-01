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

#### Scenario: Backend request fails
- **WHEN** 后端元数据或数据预览请求失败
- **THEN** 前端 MUST 展示可理解的错误状态
- **AND** 前端 MUST 保持当前数据源选择和已加载的上下文，便于用户重试或切换数据源

#### Scenario: DB browser UI is packaged as plugin UI
- **WHEN** 构建 DB 浏览控制台前端
- **THEN** DB 浏览 UI MUST 位于独立 Magic-API 插件 `src/console` 中
- **AND** 宿主控制台 MUST NOT 新增 DB 浏览业务页面、DB 专属工具栏逻辑或 DB 专属 i18n 文案

### Requirement: S3 management UI belongs to the S3 plugin
系统 SHALL 将现有 S3 文件管理 UI 归属到 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI 中，宿主控制台只保留通用插件加载/挂载能力。

#### Scenario: S3 management UI is provided by plugin
- **WHEN** S3 插件启用且 `adminEnabled` 为 true
- **THEN** S3 插件 UI MUST 提供原有 S3 文件管理入口和交互能力
- **AND** 该入口和页面 MUST 由 `fw-plus-magicapi-plugin-s3/src/console` 构建产物注册

#### Scenario: Host console has no S3 business page
- **WHEN** 检查宿主控制台源码
- **THEN** `fw-plus-magicapi-spring-boot-starter/src/console` MUST NOT 包含 S3 文件管理业务页面
- **AND** 宿主控制台 MUST NOT 包含 S3 专属状态栏/工具栏逻辑或 S3 专属 i18n 文案

### Requirement: Existing datasource behavior remains compatible
系统 SHALL 保持现有 JDBC datasource 保存、注册、测试和脚本侧 `db` 使用方式不变。

#### Scenario: Existing datasource is edited outside DB browser
- **WHEN** 用户使用现有 datasource 表单保存 JDBC 数据源
- **THEN** 保存结构 MUST 与当前 datasource JSON 格式兼容
- **AND** DB 浏览能力 MUST NOT 引入新的必填 datasource 字段

#### Scenario: Magic script uses db module
- **WHEN** Magic-API 脚本继续通过 `db` 模块访问数据源
- **THEN** 脚本侧行为 MUST 保持兼容
- **AND** DB 浏览模块 MUST NOT 改变脚本侧 `db` API
