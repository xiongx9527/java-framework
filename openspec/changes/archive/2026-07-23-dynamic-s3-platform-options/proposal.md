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
