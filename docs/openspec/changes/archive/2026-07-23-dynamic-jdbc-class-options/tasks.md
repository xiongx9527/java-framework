## 1. 后端候选接口

- [x] 1.1 新增 Magic-API 控制台候选接口，返回 `jdbcDrivers` 和 `datasourceTypes`。
- [x] 1.2 增加后端候选提供器，合并默认 JDBC Driver 候选、图数据库 JDBC Driver 候选、MongoDB SQL JDBC Driver 候选和配置追加候选。
- [x] 1.3 使用运行时应用 ClassLoader 过滤 JDBC Driver 候选，只返回当前 classpath 中存在的类。
- [x] 1.4 增加 JDBC Driver 追加候选配置项，且不改变 datasource JSON 格式。

## 2. DataSource 类型过滤

- [x] 2.1 复用现有 DataSourceBuilder 注册表和已支持 DataSource 类型基线作为候选输入。
- [x] 2.2 按运行时类存在性过滤 DataSource 类型候选。
- [x] 2.3 按可构建能力过滤 DataSource 类型候选，没有 builder 或安全通用构建支持的类默认不展示。
- [x] 2.4 增加 DataSource 类型追加候选配置项，并应用同样的类存在性和可构建能力过滤。

## 3. 控制台集成

- [x] 3.1 确保动态候选接口复用 Magic-API 控制台登录认证和权限规则，未登录请求必须被拒绝。
- [x] 3.2 在 Magic-API 控制台登录完成后请求后端候选接口，并用返回结果覆盖 `constants.JDBC_DRIVERS` 和 `constants.DATASOURCE_TYPES`。
- [x] 3.3 保留前端现有默认常量，作为候选接口不可用时的兜底。
- [x] 3.4 保留 datasource 表单中 Driver 和 DataSource 类型字段的手动输入能力。

## 4. 验证

- [x] 4.1 增加 JDBC Driver 候选过滤单元测试，覆盖类存在和类缺失两种情况。
- [x] 4.2 增加 DataSource 类型过滤单元测试，覆盖类存在、类缺失、类存在但不支持构建三种情况。
- [x] 4.3 增加测试或 smoke 验证，确认配置追加候选在候选接口返回前已完成过滤。
- [x] 4.4 增加测试或 smoke 验证，确认未登录访问候选接口会被拒绝，登录后才会加载候选。
- [x] 4.5 验证现有 datasource 保存和加载行为仍兼容当前 datasource JSON 文件。
