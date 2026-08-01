## Why

DB 管理插件注册 `Plugin` 时没有读取 `magic-api.plugin.db.admin.enabled`，导致控制台无法像 S3、Kafka、Pulsar 插件一样通过配置控制管理员入口可见性。

## What Changes

- 在 DB 管理插件配置中读取 `magic-api.plugin.db.admin.enabled`，默认值为 `false`。
- 将读取结果写入 `Plugin#setAdminEnabled`。
- 将 DB 管理后端配置装配绑定到 `magic-api.plugin.db.admin.enabled=true`，关闭时不注册 DB 管理 Controller。
- 将 DB 插件 toolbar 标记为 `adminOnly`，让通用控制台按 `plugin.adminEnabled` 门控入口。
- 增加回归测试，覆盖 DB 插件 adminEnabled 配置映射和 toolbar 门控契约。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `magicapi-db-browser`: DB 管理入口必须通过 `magic-api.plugin.db.admin.enabled` 写入插件 `adminEnabled` 元数据，并由插件 toolbar 的 `adminOnly` 标记接入宿主通用门控逻辑。

## Impact

- 影响 `MagicAPIDbBrowserConfiguration#plugin()` 的插件元数据。
- 影响 DB 管理后端自动配置装配条件。
- 影响 DB 管理后端配置测试。
- 影响 DB 插件控制台入口元数据。
- 不改变 `/db/browser/**` 接口路径、请求参数或响应结构；当 `magic-api.plugin.db.admin.enabled` 未开启时，DB 管理后端不装配。
