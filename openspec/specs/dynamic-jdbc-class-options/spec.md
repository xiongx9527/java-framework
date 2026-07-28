# dynamic-jdbc-class-options Specification

## Purpose
TBD - created by archiving change dynamic-jdbc-class-options. Update Purpose after archive.
## Requirements
### Requirement: JDBC driver options reflect available classes
系统 SHALL 根据当前应用运行时 classpath 生成 JDBC Driver 下拉候选，只下发存在的 Driver 类名。

#### Scenario: Built-in graph and document SQL drivers are candidate inputs
- **WHEN** 系统生成默认 JDBC Driver 候选基线
- **THEN** 候选基线 MUST 包含 fw-plus Nebula JDBC Driver、fw-plus Neo4j JDBC Driver 和 MongoDB SQL JDBC Driver 类名
- **AND** 这些类名 MUST 仍经过 classpath 存在性过滤后才展示

#### Scenario: Existing driver class is shown
- **WHEN** 默认或配置候选中包含当前 classpath 存在的 JDBC Driver 类
- **THEN** 数据源表单 Driver 下拉候选 MUST 包含该类名

#### Scenario: Missing driver class is hidden
- **WHEN** 默认或配置候选中包含当前 classpath 不存在的 JDBC Driver 类
- **THEN** 数据源表单 Driver 下拉候选 MUST NOT 包含该类名

### Requirement: DataSource type options reflect constructible available classes
系统 SHALL 根据当前应用运行时 classpath 和数据源构建能力生成 DataSource 类型下拉候选，只下发存在且当前系统可识别/构建的 DataSource 类型。

#### Scenario: Existing supported DataSource type is shown
- **WHEN** 某个 DataSource 类型类存在于当前 classpath 且系统存在对应 DataSourceBuilder 或通用构建支持
- **THEN** 数据源表单类型下拉候选 MUST 包含该类名

#### Scenario: Missing DataSource type is hidden
- **WHEN** 默认或配置候选中包含当前 classpath 不存在的 DataSource 类型类
- **THEN** 数据源表单类型下拉候选 MUST NOT 包含该类名

#### Scenario: Unsupported DataSource type is hidden
- **WHEN** 某个 DataSource 类型类存在但当前系统没有对应 DataSourceBuilder 或通用构建支持
- **THEN** 数据源表单类型下拉候选 MUST NOT 默认包含该类名

### Requirement: Configured candidate classes are filtered before display
系统 SHALL 允许通过配置追加 JDBC Driver 和 DataSource 类型候选，但所有配置项在展示前 MUST 经过运行时可用性过滤。

#### Scenario: Configured available class is shown
- **WHEN** 用户通过配置追加一个当前 classpath 存在且符合候选类型要求的类名
- **THEN** 对应下拉候选 MUST 包含该类名

#### Scenario: Configured unavailable class is hidden
- **WHEN** 用户通过配置追加一个当前 classpath 不存在的类名
- **THEN** 对应下拉候选 MUST NOT 包含该类名

### Requirement: Manual input remains available
系统 SHALL 保留数据源表单 Driver 和 DataSource 类型字段的手动输入能力。

#### Scenario: User enters custom class name
- **WHEN** 用户需要输入未出现在下拉候选中的自定义 Driver 或 DataSource 类型类名
- **THEN** 表单 MUST 允许用户手动输入该类名

### Requirement: Dynamic class options are loaded after console authentication
系统 SHALL 在 Magic-API 控制台登录认证通过后加载动态 JDBC Driver 和 DataSource 类型候选接口。

#### Scenario: Authenticated console user loads options
- **WHEN** 用户已登录并进入 Magic-API 控制台
- **THEN** 前端 MUST 请求动态候选接口并用返回结果更新下拉候选

#### Scenario: Unauthenticated request is rejected
- **WHEN** 未认证请求访问动态候选接口
- **THEN** 后端 MUST 按 Magic-API 控制台权限规则拒绝该请求

