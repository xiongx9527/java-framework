# Comet Design Handoff

- Change: dynamic-s3-platform-options
- Phase: design
- Mode: compact
- Context hash: d7ea675a0d975d3fbf4849382febf93581743f2d52b28a5fabf82e8be0b3f591

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/dynamic-s3-platform-options/proposal.md

- Source: openspec/changes/dynamic-s3-platform-options/proposal.md
- Lines: 1-28
- SHA256: 7392d68afdf894e662ee9cb051d90acd07848b44edd1dafa8ea1e831c9b9bcff

```md
## Why

S3 数据源表单的平台下拉目前硬编码为少量平台，已经落后于后端 `MagicDynamicS3DataSource` 和 `x-file-storage` 实际可支持的平台范围。需要改为登录后动态加载，让控制台只展示当前运行时可用、后端可构建的平台候选，同时保留现有配置和使用方式。

## What Changes

- 新增 S3 平台候选能力，由后端根据当前 classpath 和已支持的平台配置类生成下拉选项。
- 前端 S3 datasource 表单登录后请求动态候选接口，并用返回结果更新平台下拉。
- 当前静态候选作为接口不可用或返回空时的兼容兜底，不改变已有 `platform` 字段保存格式。
- 候选扩展应允许后续通过配置追加平台候选，但展示前仍必须经过运行时可用性过滤。
- 不移除现有 MinIO、阿里云 OSS、腾讯云 COS、Amazon S3 等已可用能力。

## Capabilities

### New Capabilities

- `dynamic-s3-platform-options`: S3 datasource 平台下拉根据当前运行时可用能力动态展示。

### Modified Capabilities

- 无。

## Impact

- 影响 `fw-plus-boot-autoconfigure` 中 Magic-API S3 插件相关配置、Provider 和 Controller。
- 影响 `fw-plus-magicapi-plugin-s3` 控制台的 S3 datasource 表单选项加载逻辑。
- 新增或调整后端单元测试，覆盖平台候选过滤和接口认证约束。
- 前端需保留默认候选兜底，避免旧部署或接口异常时破坏现有编辑能力。

```

## openspec/changes/dynamic-s3-platform-options/design.md

- Source: openspec/changes/dynamic-s3-platform-options/design.md
- Lines: 1-89
- SHA256: bb1c28c465d7338531614f96864b5526633da257e665f6d6c9676a4fe6ced34c

[TRUNCATED]

```md
## Context

Magic-API S3 插件的控制台表单目前在 `magic-s3-datasource-info.vue` 中硬编码平台候选，仅展示 `minio`、`aliyun-oss`、`tencent-cos`、`aws-s3`。后端 `MagicDynamicS3DataSource` 已通过 `SpringFileStorageProperties` 和平台分支支持更多平台，例如 `amazon-s3`、`huawei-obs`、`qiniu-kodo`、`baidu-bos`、`upyun-uss` 等。

前面 JDBC datasource 已经采用“后端按运行时 classpath 过滤候选，前端登录后加载”的模式。本次 S3 平台候选应沿用同一思路：下拉展示由后端统一裁剪，前端只负责消费接口和保留兼容兜底。

## Goals / Non-Goals

**Goals:**

- S3 平台下拉由后端动态返回，不再只依赖前端硬编码数组。
- 后端只返回当前 classpath 存在配置类、且 `MagicDynamicS3DataSource` 当前可构建的平台。
- 接口必须登录后访问，并遵守 Magic-API 控制台权限规则。
- 前端接口失败或返回空时继续使用当前默认候选，不影响旧部署。
- 编辑已有数据源时，即使当前保存的 `platform` 不在动态候选里，也必须保留当前值可见、可提交。

**Non-Goals:**

- 不改变 S3 datasource JSON 结构，继续使用现有 `platform` 字段。
- 不实现依赖 jar 的运行时下载、安装或热加载。
- 不重写 S3 连接测试、上传、删除、预签名 URL、资源注册逻辑。
- 不把尚未在 `MagicDynamicS3DataSource` 中有明确构建分支的平台暴露为默认候选。

## Decisions

### Decision: 新增 S3 平台候选 Provider

在 `fw-plus-boot-autoconfigure` 的 S3 插件包下新增 Provider，职责类似 `MagicJdbcClassOptionProvider`：

- 维护默认平台候选基线，至少覆盖当前后端明确支持的平台分支。
- 每个平台候选绑定平台值、展示文本和对应的 `SpringFileStorageProperties` 配置类。
- 通过 `ClassUtils.isPresent` 或 `ClassUtils.forName` 检查配置类是否存在于当前 classpath。
- 只返回“配置类存在且后端可构建”的候选。
- 支持配置追加候选，但追加候选同样需要通过 classpath 和可构建能力过滤。

备选方案是前端通过构建时依赖决定静态列表，但它无法反映运行时 jar 差异，也会继续出现前后端候选不一致的问题。

### Decision: 新增登录后访问的候选接口

在 S3 Controller 中新增平台候选接口，例如：

```text
GET ${magic-api.web:/magic-api/web}/s3/datasource/platform/options
```

接口返回 `JsonBean<List<MagicS3PlatformOption>>` 或等价结构，字段保持前端容易消费：

```json
[
  { "text": "minio", "value": "minio" },
  { "text": "aliyun-oss", "value": "aliyun-oss" }
]
```

接口使用 `@Valid(requireLogin = true)`，并复用 `allowVisit(request, Authorization.VIEW)` 这样的控制台权限检查，保持与 JDBC 动态选项接口一致。

### Decision: 前端登录后加载，失败时兜底

`magic-s3-datasource-info.vue` 将当前硬编码列表改为默认兜底列表，组件初始化后请求后端动态接口：

- 接口返回非空数组时替换下拉候选。
- 接口失败、未登录、旧后端不存在该接口或返回空时保留默认候选。
- 如果正在编辑的数据源已有 `platform`，但不在候选中，前端临时追加当前值，避免表单显示空值或保存时丢失。

这种方式可以兼容旧部署，也符合“有对应类，下拉才显示”的动态要求。

### Decision: 平台值以现有后端可构建值为准

动态候选的平台值以 `MagicDynamicS3DataSource#createProperties` 当前明确支持的分支为准。当前前端静态候选中的 `aws-s3` 不对应后端分支；动态候选应优先返回 `amazon-s3`。为避免破坏旧数据源编辑，前端必须保留当前已保存的非候选值，但不应把不可构建的平台作为默认动态候选。

## Risks / Trade-offs

- [Risk] 动态接口在旧后端或未登录状态不可用，导致前端加载失败 → 保留当前默认候选作为兜底。
- [Risk] 平台候选过宽，展示了后端不能构建的平台 → Provider 同时校验配置类存在和后端支持表。
- [Risk] 已保存的历史 `platform` 不在新候选中，编辑表单出现空值 → 前端把当前值临时追加到候选。
- [Risk] `aws-s3` 与 `amazon-s3` 命名不一致 → 动态候选使用后端真实支持值，历史值只做编辑保留，不自动迁移。

## Migration Plan

1. 新增后端 Provider、选项模型和登录后接口。

```

Full source: openspec/changes/dynamic-s3-platform-options/design.md

## openspec/changes/dynamic-s3-platform-options/tasks.md

- Source: openspec/changes/dynamic-s3-platform-options/tasks.md
- Lines: 1-26
- SHA256: a91495846915f4ab5a83514bc6b14f4760ad018a4c09eb718b6b044f21ad8285

```md
## 1. 后端测试先行

- [ ] 1.1 新增 S3 平台候选 Provider 单元测试，覆盖默认支持平台、缺失配置类过滤、后端不可构建平台过滤。
- [ ] 1.2 新增配置追加候选测试，覆盖可用候选展示和不可用候选隐藏。
- [ ] 1.3 新增 S3 平台候选接口测试，覆盖登录后可访问和未登录/无权限拒绝。

## 2. 后端实现

- [ ] 2.1 新增 S3 平台候选模型，返回字段包含 `text` 和 `value`。
- [ ] 2.2 新增 S3 平台候选 Provider，根据 classpath 和后端支持表生成动态候选。
- [ ] 2.3 增加配置属性，允许追加 S3 平台候选并在展示前过滤。
- [ ] 2.4 在 Magic-API S3 自动配置中注册 Provider Bean。
- [ ] 2.5 在 S3 Controller 中新增登录后访问的平台候选接口，并复用 Magic-API 控制台权限校验。

## 3. 前端实现

- [ ] 3.1 将 `magic-s3-datasource-info.vue` 的硬编码平台列表改为默认兜底候选。
- [ ] 3.2 组件登录后加载动态 S3 平台候选，返回非空时替换下拉选项。
- [ ] 3.3 接口失败、旧后端无接口或返回空候选时保留默认候选。
- [ ] 3.4 编辑已有 datasource 时，当前 `platform` 不在候选里也临时追加展示并保持可提交。

## 4. 验证与文档

- [ ] 4.1 运行后端相关单元测试，确认 Provider 和 Controller 行为符合 spec。
- [ ] 4.2 运行 S3 插件前端构建或最小 smoke，确认表单下拉可加载、失败可兜底。
- [ ] 4.3 更新使用说明，说明 S3 平台下拉来源、登录后加载、默认兜底和扩展配置方式。

```

## openspec/changes/dynamic-s3-platform-options/specs/dynamic-s3-platform-options/spec.md

- Source: openspec/changes/dynamic-s3-platform-options/specs/dynamic-s3-platform-options/spec.md
- Lines: 1-61
- SHA256: f1a693ec2b590c55406fc23b8a24550caea4af00e64a66523c4d631d7337ca42

```md
## ADDED Requirements

### Requirement: S3 platform options reflect runtime supported platforms
系统 SHALL 根据当前应用运行时 classpath 和 `MagicDynamicS3DataSource` 的平台构建能力生成 S3 平台下拉候选，只下发当前存在配置类且后端明确可构建的平台。

#### Scenario: Supported platform class is shown
- **WHEN** 默认候选中的平台配置类存在于当前 classpath
- **AND** `MagicDynamicS3DataSource` 存在该平台的构建分支
- **THEN** S3 datasource 平台下拉候选 MUST 包含该平台

#### Scenario: Missing platform class is hidden
- **WHEN** 默认候选中的平台配置类不存在于当前 classpath
- **THEN** S3 datasource 平台下拉候选 MUST NOT 包含该平台

#### Scenario: Platform without backend builder is hidden
- **WHEN** 某个平台配置类存在但 `MagicDynamicS3DataSource` 没有明确构建分支
- **THEN** S3 datasource 平台下拉候选 MUST NOT 默认包含该平台

### Requirement: S3 platform options can be extended by configuration
系统 SHALL 允许通过配置追加 S3 平台候选，但所有配置项在展示前 MUST 经过运行时可用性和后端可构建能力过滤。

#### Scenario: Configured available platform is shown
- **WHEN** 用户通过配置追加一个当前 classpath 存在且后端可构建的平台候选
- **THEN** S3 datasource 平台下拉候选 MUST 包含该平台

#### Scenario: Configured unavailable platform is hidden
- **WHEN** 用户通过配置追加一个当前 classpath 不存在或后端不可构建的平台候选
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

```
