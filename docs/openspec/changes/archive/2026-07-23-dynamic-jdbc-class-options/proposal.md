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
