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
