# dynamic-s3-platform-options Specification

## Purpose
TBD - created by archiving change dynamic-s3-platform-options. Update Purpose after archive.
## Requirements
### Requirement: S3 platform options reflect runtime supported platforms
系统 SHALL 根据当前应用运行时 classpath 和 `MagicDynamicS3DataSource` 的平台构建能力生成 S3 平台下拉候选，只下发当前存在配置类、实际 SDK 类且后端明确可构建的平台。

#### Scenario: Supported platform class is shown
- **WHEN** 默认候选中的平台配置类存在于当前 classpath
- **AND** 默认候选中的平台实际 SDK 类存在于当前 classpath
- **AND** `MagicDynamicS3DataSource` 存在该平台的构建分支
- **THEN** S3 datasource 平台下拉候选 MUST 包含该平台

#### Scenario: Missing platform class is hidden
- **WHEN** 默认候选中的平台配置类或实际 SDK 类不存在于当前 classpath
- **THEN** S3 datasource 平台下拉候选 MUST NOT 包含该平台

#### Scenario: Platform without backend builder is hidden
- **WHEN** 某个平台配置类存在但 `MagicDynamicS3DataSource` 没有明确构建分支
- **THEN** S3 datasource 平台下拉候选 MUST NOT 默认包含该平台

### Requirement: S3 platform options can be extended by configuration
系统 SHALL 允许通过配置追加 S3 平台候选，但所有配置项在展示前 MUST 经过运行时可用性和后端可构建能力过滤。

#### Scenario: Configured available platform is shown
- **WHEN** 用户通过配置追加一个当前 classpath 存在配置类、实际 SDK 类且后端可构建的平台候选
- **THEN** S3 datasource 平台下拉候选 MUST 包含该平台

#### Scenario: Configured unavailable platform is hidden
- **WHEN** 用户通过配置追加一个当前 classpath 缺少配置类、缺少实际 SDK 类或后端不可构建的平台候选
- **THEN** S3 datasource 平台下拉候选 MUST NOT 包含该平台

### Requirement: Dynamic S3 platform options are loaded after console authentication
系统 SHALL 在 Magic-API 控制台登录认证通过后加载动态 S3 平台候选接口。

#### Scenario: Authenticated console user loads S3 platform options
- **WHEN** 用户已登录并进入 Magic-API 控制台的 S3 datasource 表单
- **THEN** 前端 MUST 请求动态 S3 平台候选接口
- **AND** 前端 MUST 用接口返回的非空候选更新平台下拉

#### Scenario: Unauthenticated request is rejected
- **WHEN** 未认证请求访问动态 S3 平台候选接口
- **THEN** 后端 MUST 按 Magic-API 控制台权限规则拒绝该请求

### Requirement: Existing S3 datasource editing remains compatible
系统 SHALL 保留已有 S3 datasource 的编辑和提交能力，动态候选加载不得导致历史 `platform` 值丢失。

#### Scenario: Dynamic endpoint is unavailable
- **WHEN** 前端请求动态 S3 平台候选接口失败、接口不存在或接口返回空候选
- **THEN** 前端 MUST 保留当前默认平台候选作为兜底

#### Scenario: Existing platform value is not in options
- **WHEN** 用户编辑已有 S3 datasource
- **AND** 当前 datasource 的 `platform` 值不在动态候选中
- **THEN** 前端 MUST 将当前 `platform` 值临时展示为可选项
- **AND** 表单提交 MUST 保留该 `platform` 值，除非用户主动修改

### Requirement: S3 datasource storage format remains unchanged
系统 SHALL 继续使用现有 S3 datasource JSON 保存结构，不因动态平台候选能力改变字段名称或字段含义。

#### Scenario: Datasource is saved after platform options load
- **WHEN** 用户保存 S3 datasource
- **THEN** 保存内容 MUST 继续包含现有 `platform` 字段
- **AND** 系统 MUST NOT 引入新的必填平台字段替代 `platform`

