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
- 每个平台候选绑定平台值、展示文本、对应的 `SpringFileStorageProperties` 配置类，以及实际平台 SDK 所需类。
- 通过 `ClassUtils.isPresent` 或 `ClassUtils.forName` 检查配置类和实际 SDK 类是否存在于当前 classpath。
- 只返回“配置类存在、实际 SDK 类存在且后端可构建”的候选。
- 支持配置追加候选，但追加候选同样需要通过 classpath 和可构建能力过滤。

备选方案是前端通过构建时依赖决定静态列表，但它无法反映运行时 jar 差异，也会继续出现前后端候选不一致的问题。

### Decision: 新增登录后访问的候选接口

在 S3 Controller 中新增平台候选接口。Controller 注解使用插件相对路径：

```text
GET /s3/datasource/platform/options
```

Magic-API 控制台请求对象会拼接 `${magic-api.web:/magic-api/web}` 基础路径后访问该接口。

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
- [Risk] 平台候选过宽，展示了后端不能构建的平台 → Provider 同时校验配置类存在、实际 SDK 类存在和后端支持表。
- [Risk] 已保存的历史 `platform` 不在新候选中，编辑表单出现空值 → 前端把当前值临时追加到候选。
- [Risk] `aws-s3` 与 `amazon-s3` 命名不一致 → 动态候选使用后端真实支持值，历史值只做编辑保留，不自动迁移。

## Migration Plan

1. 新增后端 Provider、选项模型和登录后接口。
2. 前端改为动态加载平台候选并保留默认兜底。
3. 增加后端单元测试覆盖 classpath 过滤、配置追加过滤和接口权限。
4. 执行 S3 插件前端构建或最小 smoke，确认候选加载失败时仍可编辑。

回滚时删除新增接口和前端加载逻辑即可恢复硬编码候选；已保存数据源 JSON 不需要迁移。

## Open Questions

- 是否需要在后续单独修复 `aws-s3` 到 `amazon-s3` 的兼容别名或迁移策略。本次只保证旧值编辑不丢失，不把不可构建别名作为默认动态候选。
