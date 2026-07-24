---
comet_change: align-s3-browser-console-ui
role: technical-design
canonical_spec: openspec
---

# S3 文件管理控制台 UI 对齐设计

## 背景

S3 插件的文件管理入口已经位于 `fw-plus-magicapi-plugin-s3/src/console`，当前 `magic-s3-browser.vue` 能完成存储选择、路径浏览、搜索、加载更多、预览、下载、删除和 basePath 保存，但视觉仍偏独立工具页：使用原生 `select/input/button`、大块 toolbar、独立错误文本和悬浮确认框。原 `magic-s3-datasource-info.vue` 以及同类插件信息页采用 Magic 控制台的紧凑表单、统一边框、主题变量和低密度装饰。此次设计目标是保持文件管理能力不变，同时把浏览页收敛到同一控制台语言，并让接口失败明确弹出给用户。

## 确认方案

采用插件内重构方案：所有源码仍限定在 `fw-plus-magicapi-plugin-s3/src/console`，不修改宿主 UI，不改后端接口。`magic-s3-browser.vue` 的结构调整为 Magic 控制台风格的文件管理面板：顶部是紧凑 form-like 操作区，左侧是存储、路径与对象列表，右侧是选中文件详情和操作区。表单控件优先使用 `magic-select`、`magic-input` 等已有 Magic 组件；不可替换的操作按钮保持原有按钮，但统一高度、边框、背景和 hover 状态，全部引用 `var(--magic-input-height)`、`var(--main-border-color)`、`var(--main-background-color)` 等控制台变量。

错误提示通过插件内统一 helper 处理。沿用现有 `setS3BrowserRequest(opt.request)` 模式，在 S3 浏览服务模块增加 modal/notifier setter，例如 `setS3BrowserModal(opt.modal)`，由 `src/index.js` 在安装插件时注入 `opt.modal`。`magic-s3-browser.vue` 内部统一提取错误文本，并在加载存储、列文件、保存 basePath、预览、下载、删除失败时调用 Magic modal 的 `alert`。页面可以继续保留内联错误状态作为辅助提示，但失败必须弹出。若 modal 不存在，helper 只更新内联错误，保证测试和非宿主上下文不崩溃。

## 组件结构

`magic-s3-browser.vue` 保持单文件实现，避免为一次视觉对齐拆出过多新组件。模板分为四个区域：

- 顶部操作区：存储选择、basePath 输入/保存、搜索输入/搜索按钮、刷新按钮。控件高度和间距与数据源信息页一致。
- 路径导航区：展示根路径和分段按钮，支持回到 basePath 或上级分段，使用轻量文本按钮，不做独立卡片。
- 文件列表区：目录和文件以行列表呈现，固定图标/类型列、名称列、大小/时间等可用信息列；空状态和加载状态使用控制台普通状态文本。
- 文件详情区：展示选中文件名、预览链接、JSON 明细和预览/下载/删除操作。删除确认使用 Magic modal.confirm；如果插件上下文没有 confirm，再保留现有内置确认层作为降级。

## 数据流

插件安装时，`src/index.js` 同时调用 `setS3BrowserRequest(opt.request)` 和 `setS3BrowserModal(opt.modal)`。浏览页挂载后调用 `listStorages` 加载可用存储，默认选择第一项并按其 `basePath` 列文件。路径、搜索和加载更多沿用当前 requestSeq 上下文保护，避免旧请求覆盖新状态。预览和下载继续调用现有预签名接口，成功后更新 `previewUrl` 并打开新窗口或触发下载；失败只弹错，不改成功状态。删除成功后清空选中项并刷新列表；删除失败时保留当前列表和选中项。

## 错误处理

新增 `extractErrorMessage(cause, fallback)` 与 `showError(cause, fallbackKey)` 两层 helper。前者从 `Error.message`、字符串、后端响应对象中的 `message/msg/error` 等字段提取可读信息，兜底使用 i18n 文案；后者设置页面 `error`，并调用 `modal.alert(message, $i('s3.browser.errorTitle'))`。各失败场景的 fallback 文案保持具体：加载存储失败、列文件失败、保存基础路径失败、预览失败、下载失败、删除失败。

删除确认优先使用 `modal.confirm`，文案沿用 `s3.browser.confirmDelete`。确认回调只负责调用实际删除函数；失败时仍由统一错误 helper 弹出，不关闭详情状态为成功。

## 样式约束

样式不引入新的大卡片、渐变、营销式区块或宿主外视觉。根容器保持 `height: 100%`、`min-height: 420px`、`background: var(--main-background-color, #fff)`。顶部操作区使用 `display: flex`、`align-items: center`、`padding: 5px`、`border-bottom: 1px solid var(--main-border-color)`。列表和详情之间只用边框分隔；内容区使用 `min-height: 0` 和 `overflow: auto`，避免工具栏或长文件名挤压布局。长路径、文件名、URL 和 JSON 明细必须可换行或省略，不允许横向撑破面板。

## 测试策略

前端契约测试更新 `src/console/test/magic-s3-browser-contract.test.cjs`：检查 S3 插件入口仍返回 `name: 's3'`，`s3-browser` toolbar 仍在插件 UI 内并受 `adminOnly` 门控；检查浏览页使用 Magic 控件和控制台主题变量；检查存在统一错误 helper、modal setter、`modal.alert` 调用，以及加载存储、列文件、保存 basePath、预览、下载、删除失败路径均接入弹错逻辑。

实现完成后运行：

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm test
npm run build
```

随后在 S3 插件 Maven 模块运行 package，确认 `dist/magic-s3.1.0.0.iife.js` 被复制到 `target/classes/magic-editor/plugins/magic-s3.1.0.0.iife.js`。最后运行 `git diff --check`。

## 风险与边界

宿主 toolbar 当前只把 `toolbar.data` 透传给组件，而 S3 插件现有 toolbar 未设置 data。为了不碰宿主 UI，modal 注入采用插件模块级 setter，这是和 request 注入一致的低风险路径。浏览器缓存可能继续加载旧 `magic-s3.1.0.0.iife.js`，验证时需要检查 dist 和 target/classes 产物内容，必要时强刷页面。此次不新增上传能力，不修改后端 S3 接口和数据源配置格式，也不迁移任何 UI 到宿主控制台。
