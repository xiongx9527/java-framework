## Context

S3、Kafka、Pulsar 插件注册 `Plugin` 时都会通过 `SpringUtils.getAppConfig("magic-api.plugin.<name>.admin.enabled", "false")` 读取管理员入口开关，并写入 `plugin.adminEnabled`。DB 插件目前只返回 `new Plugin("db")` 或 `new Plugin("db", "MagicDB", "magic-db.1.0.0.iife.js")`，没有设置 `adminEnabled`。

## Root Cause

DB 插件配置类缺少和其他管理插件一致的 `admin.enabled` 配置读取逻辑。前端通用 toolbar 已按 `pluginData.adminEnabled === true` 做管理员入口门控，因此后端插件元数据未设置会导致 DB 管理入口无法通过配置开启管理员模式。

## Fix

- 在 `MagicAPIDbBrowserConfiguration#plugin()` 中先创建 `Plugin` 实例。
- 读取 `magic-api.plugin.db.admin.enabled`，默认 `false`。
- 调用 `plugin.setAdminEnabled(Boolean.parseBoolean(adminEnabled))` 后返回。
- 在 DB 插件 `src/console/src/index.js` 的 toolbar 上增加 `adminOnly: true`，复用宿主控制台的通用 `pluginData.adminEnabled === true` 门控逻辑。
- 将 DB 管理插件配置装配绑定到 `magic-api.plugin.db.admin.enabled=true`，关闭 admin 时不注册 DB 管理后端 Controller。
- 测试通过 Spring `Environment` 注入配置，断言 `adminEnabled` 从默认 `false` 和配置 `true` 正确映射。

## Non-Goals

- 不新增 DB 管理接口。
- 不调整宿主通用 toolbar 渲染逻辑。
- 不改变插件名称、全局变量名或 JS 文件名。
