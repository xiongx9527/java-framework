## ADDED Requirements

### Requirement: Core BOM 依赖管理声明唯一
`fw-plus-dependencies` SHALL NOT 在 `dependencyManagement` 中重复声明同一个框架模块坐标。对于 `com.xiongx9527.fw.plus:fw-plus-auth-satoken`，BOM SHALL 只保留一个 dependencyManagement 声明，并 SHALL 使用 `${project.version}` 作为版本。

#### Scenario: 检查 auth-satoken 依赖管理声明
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- **THEN** `com.xiongx9527.fw.plus:fw-plus-auth-satoken` 只在 dependencyManagement 中声明一次
- **AND** 该声明使用 `${project.version}` 作为版本

#### Scenario: 构建 Core BOM
- **WHEN** 对 `fw-plus-dependencies` 执行 Maven 构建
- **THEN** 构建成功
- **AND** Maven 输出不包含 `fw-plus-auth-satoken` 的重复 dependencyManagement 声明警告
