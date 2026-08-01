## Why

DB 管理功能的前端已经位于 `fw-plus-magicapi-plugin-db`，但后端配置、Controller、Service 和模型仍放在 `fw-plus-boot-autoconfigure` 的 `magicapi/dbbrowser` 包下，导致插件边界不清晰。

## What Changes

- 将 DB 管理后端代码和对应测试迁移到 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/db` 插件包目录。
- 将 Java 包名调整为 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db`。
- 由 `MagicApiPluginAutoConfiguration` 导入 DB 管理配置，`fw-plus-magicapi-plugin-db` 只保留控制台插件 UI。
- 将控制台入口展示名从“数据库浏览器”改为“db管理”。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `magicapi-db-browser`: 明确 DB 管理前端 UI 归属于 `fw-plus-magicapi-plugin-db`，后端归属于 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/db` 插件包，并调整控制台入口展示名。

## Impact

- 影响 `fw-plus-boot-autoconfigure` 的 DB 管理后端源码、测试和 Magic-API 插件汇总导入。
- 影响 `fw-plus-magicapi-plugin-db` 插件模块前端 i18n。
- 影响 `fw-plus-boot-autoconfigure` 的 Magic-API 插件汇总导入。
- 不改变 `/db/browser/**` 后端接口路径和请求/响应契约。
