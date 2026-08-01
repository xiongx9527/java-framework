# Brainstorm Summary

- Change: dynamic-jdbc-class-options
- Date: 2026-07-23

## 已确认事实

- 用户要求 JDBC Driver 和 DataSource 类型下拉必须动态化：只有当前应用有对应类时才显示。
- 手动输入能力必须保留，不能因为下拉过滤而阻止高级用户填写自定义类名。
- 前端 `magic-editor.vue` 已通过 `config.jdbcDrivers` 和 `config.datasourceTypes` 覆盖常量。
- datasource 表单里的 Driver 和 DataSource 类型字段当前均使用 `magic-select inputable`。
- 后端 `MyDataSourceMagicDynamicRegistry` 已维护默认 DataSource 类型数组，并在注册数据源时按 `type` 查找 `DataSourceBuilder`。
- 当前图数据库 JDBC Driver 类名为 `com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver` 和 `com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver`，应进入 JDBC Driver 候选基线，并按 classpath 是否存在动态展示。
- MongoDB SQL Interface / Atlas SQL JDBC Driver 的官方 Driver 类名为 `com.mongodb.jdbc.MongoDriver`，应进入 JDBC Driver 候选基线，并按 classpath 是否存在动态展示。
- MongoDB BI Connector 的传统 JDBC 认证插件场景集成 MySQL Connector/J；因此该场景继续复用 `com.mysql.cj.jdbc.Driver` 或 `com.mysql.jdbc.Driver` 候选。

## 候选技术方案

- 候选 A：仅在后端生成 `MAGIC_EDITOR_CONFIG` 时注入过滤后的 `jdbcDrivers` 和 `datasourceTypes`，前端保持现状。
- 候选 B：增加独立接口供前端启动后拉取候选，再覆盖常量。
- 候选 C：前端仍保留固定列表，仅在保存或测试时后端校验类是否存在。

## 确认的技术方案

用户选择候选 B：新增后端候选接口，前端启动后请求可用 JDBC Driver 和 DataSource 类型候选，再覆盖现有常量。该方案比直接注入配置多一次请求，但接口边界更清晰，也便于后续排查当前运行环境实际识别到哪些候选类。

## 关键风险

- 需要准确找到或扩展 `config-js` / `MAGIC_EDITOR_CONFIG` 的后端输出位置。
- DataSource 类型不能只按类存在展示，还要结合 `DataSourceBuilder` 或安全通用构建能力。
- 需要避免没有后端配置时前端回退固定列表带来的误导；正常服务端页面应尽量总是注入过滤后数组。

## 测试策略候选

- 单元测试覆盖 JDBC Driver 候选类存在和不存在。
- 单元测试覆盖 DataSource 类型类存在、类不存在、类存在但没有构建支持。
- smoke 或 MVC 层测试验证编辑器配置里实际包含过滤后的数组。

## Spec Patch

- 已在 delta spec 中补充验收场景：默认 JDBC Driver 候选基线必须包含 fw-plus Nebula JDBC Driver、fw-plus Neo4j JDBC Driver 和 MongoDB SQL JDBC Driver，并且这些候选仍必须经过 classpath 存在性过滤后才展示。
- 已在 delta spec 中补充验收场景：动态候选接口必须在 Magic-API 控制台登录认证通过后加载，未认证请求必须按控制台权限规则拒绝。
