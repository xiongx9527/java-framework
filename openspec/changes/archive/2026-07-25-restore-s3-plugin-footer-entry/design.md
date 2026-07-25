## Context

附件中的代码片段展示了宿主 `magic-status-bar.vue` 曾经维护 `hasS3Plugin`、`icons` 中的 `s3` 项、`openS3()` 以及对 `s3-datasource` 的检测。当前宿主状态栏已经不再包含 S3 专属逻辑，这符合“插件 UI 不写到宿主 UI”边界，但原来的 X-file-storage 外链入口也随之消失。

## Approach

- 在 S3 插件 `src/console/src/index.js` 的返回对象中新增一个状态栏/头部外链入口元数据。
- 在宿主状态栏读取所有插件返回的通用 `statusBarItems` 并追加渲染。
- 入口信息复用原行为：
  - `icon`: S3 插件注入的 `#magic-s3-s3`
  - `title`: `X-file-storage`
  - `displayKey`: `s3`
  - `onClick`: 新标签打开 `https://x-file-storage.xuyanwu.cn/`
- 宿主 UI 不增加 `hasS3Plugin`、`openS3()` 或 `s3-datasource` 检测逻辑。
- 契约测试直接校验 S3 插件注册外链入口，并校验宿主状态栏仍不包含 S3 专属入口代码。

## Notes

该改动只恢复入口元数据，不改变 S3 文件管理 toolbar、数据源注册、后端接口或 `adminOnly` 门控。
