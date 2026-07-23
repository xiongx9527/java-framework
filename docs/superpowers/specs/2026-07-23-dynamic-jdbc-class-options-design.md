---
comet_change: dynamic-jdbc-class-options
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-23-dynamic-jdbc-class-options
status: final
---

# 动态 JDBC 类候选设计

## 背景

Magic-API 控制台 datasource 表单当前内置 JDBC Driver 和 DataSource 类型候选。固定候选会展示当前应用 classpath 中不存在的类，用户选择后会在保存、测试连接或运行时失败。

本次变更目标是让下拉候选反映当前运行环境真实可用能力：有对应类才显示；DataSource 类型还必须是系统能构建的数据源类型；手动输入能力保留。

用户已选择方案 B：后端提供候选接口，前端启动后请求接口并覆盖现有候选常量。

## 范围

本次涉及：

- `fw-plus-boot-autoconfigure` 中 Magic-API 控制台相关 Controller 或服务。
- Magic-API datasource 动态注册中已有的 `DataSourceBuilder` 映射和 DataSource 类型基线。
- `fw-plus-magicapi-spring-boot-starter` 控制台启动逻辑和 datasource 表单候选使用逻辑。

本次不涉及：

- 运行时上传 jar、远程下载依赖或自定义 ClassLoader 热加载。
- 修改 datasource JSON 保存格式。
- 修改具体 JDBC 连接、数据源创建或连接测试主流程。
- 把 Nebula、Neo4j、MongoDB SQL 当作 DataSource 类型；它们属于 JDBC Driver 候选。

## 推荐架构

新增后端候选服务，例如 `MagicJdbcClassOptionProvider`，负责生成：

- `jdbcDrivers`
- `datasourceTypes`

新增后端接口，例如：

```text
GET ${magic-api.web:/magic-api/web}/datasource/options
```

该接口必须复用 Magic-API 控制台的登录认证和权限规则。它不是公开配置接口，未登录请求必须被拒绝。

返回结构：

```json
{
  "jdbcDrivers": [
    "com.mysql.cj.jdbc.Driver",
    "com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver",
    "com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver",
    "com.mongodb.jdbc.MongoDriver"
  ],
  "datasourceTypes": [
    "com.zaxxer.hikari.HikariDataSource",
    "com.alibaba.druid.pool.DruidDataSource"
  ]
}
```

接口返回的是已经过滤后的结果。不存在的类不返回。

前端在 Magic-API 编辑器初始化后请求该接口，成功后覆盖：

```js
constants.JDBC_DRIVERS = result.jdbcDrivers || constants.JDBC_DRIVERS
constants.DATASOURCE_TYPES = result.datasourceTypes || constants.DATASOURCE_TYPES
```

datasource 表单当前使用 `magic-select inputable`，应保持不变。

## JDBC Driver 候选算法

候选来源按顺序合并并去重：

1. 传统数据库默认候选：
   - `com.mysql.jdbc.Driver`
   - `com.mysql.cj.jdbc.Driver`
   - `oracle.jdbc.driver.OracleDriver`
   - `org.postgresql.Driver`
   - `com.microsoft.sqlserver.jdbc.SQLServerDriver`
   - `com.ibm.db2.jcc.DB2Driver`
2. fw-plus 图数据库 JDBC 候选：
   - `com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver`
   - `com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver`
3. MongoDB SQL JDBC 候选：
   - `com.mongodb.jdbc.MongoDriver`
4. 用户配置追加候选。

过滤规则：

```text
类名非空
-> 去重
-> 使用当前应用 ClassLoader 判断类存在
-> 存在才返回
```

实现上优先使用 Spring `ClassUtils.isPresent(className, classLoader)`。不要全量扫描 classpath。

MongoDB BI Connector 的传统 MySQL 协议场景复用 MySQL Connector/J 候选，不新增虚假的 MongoDB BI 专用 Driver 类。

## DataSource 类型候选算法

候选来源按顺序合并并去重：

1. 现有默认 DataSource 类型基线：
   - `com.zaxxer.hikari.HikariDataSource`
   - `com.alibaba.druid.pool.DruidDataSource`
   - `org.apache.tomcat.jdbc.pool.DataSource`
   - `org.apache.commons.dbcp2.BasicDataSource`
2. Spring 容器中已注册的 `DataSourceBuilder` Bean 名称。
3. 用户配置追加候选。

过滤规则：

```text
类名非空
-> 类存在
-> 有对应 DataSourceBuilder，或现有默认构建器明确可安全构建
-> 返回
```

DataSource 类型不能只看 classpath。类存在但系统无法构建时，默认不展示，避免用户选择后保存成功但注册失败。

## 配置扩展

建议新增配置属性：

```yaml
fw:
  plus:
    magicapi:
      jdbc:
        driver-candidates:
          - com.example.CustomDriver
        datasource-type-candidates:
          - com.example.CustomDataSource
```

这些配置只是追加候选基线，不绕过过滤规则。配置了不存在的类，接口仍不返回。

如果项目已有 Magic-API 配置属性类，应优先复用现有 prefix 和属性绑定方式；不要引入新的配置框架。

## 前端流程

编辑器启动时：

1. 保持现有 `MAGIC_EDITOR_CONFIG` 合并逻辑。
2. 等 Magic-API 控制台登录认证完成后，再调用候选接口。
3. 请求成功且返回数组时覆盖常量。
4. 请求失败、未登录或会话失效时保留当前默认常量，避免控制台不可用；登录态恢复后可再次加载。
5. 表单继续保持 `inputable`，允许用户手动填写下拉中没有的类名。

接口失败只影响下拉过滤，不应阻塞整个 Magic-API 控制台启动。

## 错误处理

- 后端过滤某个候选类失败时，跳过该候选并记录 debug 日志即可。
- 候选接口本身不应因为单个候选类异常而整体失败。
- 未认证请求访问候选接口时，后端按 Magic-API 控制台权限规则拒绝。
- 前端未登录、会话失效或请求失败时不弹阻塞错误，使用默认常量兜底；登录后再加载动态候选。
- 手动输入的类名仍由现有保存、测试连接、数据源注册流程处理。

## 测试策略

后端单元测试：

- JDBC Driver 候选中存在的类会返回。
- JDBC Driver 候选中不存在的类不会返回。
- Nebula、Neo4j、MongoDB SQL Driver 作为候选基线参与过滤。
- 配置追加 Driver 候选同样经过过滤。
- DataSource 类型类存在且有 builder 时返回。
- DataSource 类型类不存在时不返回。
- DataSource 类型类存在但没有 builder 或安全构建支持时默认不返回。

前端测试或 smoke 验证：

- 候选接口成功时，`constants.JDBC_DRIVERS` 和 `constants.DATASOURCE_TYPES` 被接口结果覆盖。
- 未登录访问候选接口会被拒绝，登录完成后才发起候选加载。
- 候选接口失败时，前端保留默认常量。
- datasource 表单 Driver 和 DataSource 类型字段仍可手动输入。

兼容性验证：

- 现有 datasource JSON 的 `driverClassName` 和 `type` 字段不变。
- 已保存的数据源加载不依赖下拉候选是否包含当前值。
- 现有连接测试接口行为不变。

## 取舍

方案 B 多一次前端请求，但边界清晰、便于调试，也便于后续扩展更多动态候选。相比直接注入 `MAGIC_EDITOR_CONFIG`，它更容易通过接口 smoke 测试确认后端计算结果。

默认常量仍作为兜底存在，因此在接口不可用时仍可能显示不存在的类。这是可接受的降级行为；正常服务端运行时应优先使用接口返回结果。
