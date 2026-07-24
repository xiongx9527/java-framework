# Brainstorm Summary

- Change: align-s3-browser-console-ui
- Date: 2026-07-24

## 确认的技术方案

已确认：在 `fw-plus-magicapi-plugin-s3/src/console` 内重构 `magic-s3-browser.vue`，沿用 Magic 控制台紧凑表单、左右分栏、表格/树列表式布局和主题变量；通过插件内部全局 setter 将 `opt.modal` 暴露给浏览页，所有接口失败统一走 `modal.alert` 弹出错误，同时保留页面内错误状态作为辅助信息。

## 关键取舍与风险

不修改宿主 UI，不调整后端接口；弹窗优先使用现有 Magic modal，避免自造通知系统。风险是插件 toolbar 目前不会透传任意 `data` 字段，因此不采用组件 props 透传 modal，而是沿用已有 `setS3BrowserRequest` 模式增加插件内 modal/notifier setter。

## 测试策略

更新 `src/console/test/magic-s3-browser-contract.test.cjs`，覆盖 Magic 控件/控制台变量、统一错误 helper、`modal.alert` 接入、关键失败场景调用提示、插件入口仍在 `name: 's3'` 与 `s3-browser` toolbar 内；之后运行前端 test/build、Maven package 和 `git diff --check`。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖 UI 一致性、插件边界、功能保持和错误弹窗行为。
