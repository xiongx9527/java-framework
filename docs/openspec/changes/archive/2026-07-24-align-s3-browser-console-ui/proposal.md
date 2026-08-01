## Why

S3 文件管理入口已经迁移到插件 UI，但 `magic-s3-browser.vue` 当前呈现为独立工具页风格，和原有 magic-s3 数据源配置页面的 Magic 控制台视觉体系不一致。用户在文件浏览、预览、下载、删除等操作失败时，也需要明确的弹窗或消息提示，避免错误只停留在页面状态或控制台日志中。

## What Changes

- 调整 S3 文件管理页面，使其与原 magic-s3 数据源配置页保持一致的控制台布局、控件密度、边框、间距和颜色变量。
- 保留现有 S3 文件管理能力，包括选择存储、路径导航、搜索、加载更多、预览、下载、删除和 basePath 保存。
- 将加载数据源、列文件、预览、下载、删除、保存 basePath 等失败场景通过 Magic 控制台弹窗或消息机制提示给用户。
- 保持 S3 文件管理入口仍位于 `fw-plus-magicapi-plugin-s3` 插件 UI 中，不迁回宿主 UI。
- 不引入新的 S3 后端接口、不新增上传能力、不改变 S3 数据源配置格式。

## Capabilities

### New Capabilities

- `magic-s3-browser-console-ui`: 规范 S3 文件管理插件页面的 Magic 控制台一致性、交互状态和错误提示行为。

### Modified Capabilities

- 无。

## Impact

- 主要影响 `fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-browser.vue`。
- 可能影响 S3 插件前端服务调用封装、i18n 文案和前端契约测试。
- 需要重新构建 S3 插件前端产物，确保 `magic-s3.1.0.0.iife.js` 中包含更新后的页面。
