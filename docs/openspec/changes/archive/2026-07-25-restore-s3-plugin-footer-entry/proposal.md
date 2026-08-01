## Why

之前从宿主状态栏删除的 S3 外链入口仍需要保留，但入口逻辑不应继续写在宿主 UI 中。该入口应归属于 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI，由 S3 插件随自身加载注册。

## What Changes

- 在 S3 插件 UI 中恢复原状态栏的 X-file-storage 外链入口。
- 入口使用 S3 插件自己的 icon、title、displayKey 和点击行为，打开 `https://x-file-storage.xuyanwu.cn/`。
- 宿主 `magic-status-bar.vue` 不重新加入 S3 专属状态、过滤条件或 `openS3()` 逻辑。
- 增加插件契约测试，保证 S3 外链入口由插件注册。
- 宿主状态栏只增加通用插件状态栏入口扩展点，不维护 S3 专属逻辑。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `magic-s3-browser-console-ui`: S3 插件 UI 需要提供原状态栏的 X-file-storage 外链入口，并保持宿主 UI 不维护 S3 专属入口逻辑。

## Impact

- 影响 `fw-plus-magicapi-plugin-s3/src/console/src/index.js` 的插件注册元数据。
- 影响宿主 `magic-status-bar.vue` 的通用插件扩展点。
- 影响 S3 插件控制台契约测试。
- 不改变 S3 后端接口、数据源配置、文件管理页面行为或宿主通用工具栏渲染逻辑。
