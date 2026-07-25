## Context

S3 插件已有数据源配置页 `magic-s3-datasource-info.vue`，它使用 Magic 控制台常见的紧凑表单结构、`magic-input` / `magic-select` 等组件、`var(--magic-input-height)` 和 `var(--main-border-color)` 等主题变量。新加入的 `magic-s3-browser.vue` 已能作为插件工具栏入口出现，但当前页面风格更像独立工具，和原 magic-s3 页面不一致；同时部分异常只转成页面状态或被捕获，不符合用户希望“错误信息需要弹出来”的反馈方式。

## Goals / Non-Goals

**Goals:**

- 让 `magic-s3-browser.vue` 的顶部操作区、列表区、详情区和确认区与原 magic-s3 数据源配置页视觉一致。
- 保留现有文件管理能力，不改变后端接口语义。
- 统一前端错误处理，让接口失败通过 Magic 控制台弹窗或消息机制可见提示。
- 更新或补充插件前端契约测试，覆盖入口和错误提示相关约束。

**Non-Goals:**

- 不新增 S3 上传功能。
- 不调整 S3 后端接口、存储配置模型或动态注册逻辑。
- 不把 S3 文件管理 UI 写回宿主控制台。
- 不重做整体 Magic-API 控制台设计系统。

## Decisions

### Decision 1: 复用原 S3 数据源配置页的视觉语言

实现时以 `magic-s3-datasource-info.vue` 和同类插件信息页作为视觉基准。顶部使用紧凑的 form-like 工具区，控件优先使用 Magic 控制台已有组件，样式优先引用现有 CSS 变量。列表和详情区域保留文件管理所需结构，但不使用独立应用式大间距卡片。

替代方案是保留当前浏览器页的独立样式，只局部调色。该方案改动更小，但无法满足“和原来的 magic-s3 页面一致”。

### Decision 2: 错误提示集中封装在浏览页内部

`magic-s3-browser.vue` 内部引入统一的错误提示 helper，将接口失败、异常对象和后端返回消息规整成可读文本，再调用 Magic 控制台可用的 modal/message 机制弹出。页面可以保留必要的内联状态，但失败必须有显式弹出提示。

替代方案是在每个 catch 中手写提示。该方式简单但容易遗漏，后续新增操作也容易出现不一致。

### Decision 3: 保持插件边界和构建产物路径

所有 UI 改动限定在 `fw-plus-magicapi-plugin-s3/src/console`。完成后通过插件前端构建生成 `dist/magic-s3.1.0.0.iife.js`，再由 Maven 资源复制进入插件 jar。宿主只负责加载插件，不承载 S3 浏览页面源码。

## Risks / Trade-offs

- [Risk] Magic 控制台现有弹窗 API 在插件上下文中能力有限 -> 先复用插件入口传入的 `modal` 或已有全局消息机制，必要时降级为一致的 modal 提示。
- [Risk] 样式调整可能影响小高度工具栏内可用空间 -> 使用紧凑布局、稳定高度和 overflow 处理，避免内容挤压。
- [Risk] 文件名未变导致浏览器缓存旧 IIFE -> 验证时强刷或检查 Network 中 `magic-s3.1.0.0.iife.js` 是否包含新内容。

## Migration Plan

- 修改插件 UI 源码和前端测试。
- 运行 S3 插件前端测试和 build。
- 运行 S3 插件 Maven package，确认新版 IIFE 被复制到 `target/classes/magic-editor/plugins`。
- 部署后重启后端服务，并在浏览器强刷验证插件资源。

## Open Questions

- “弹出来”的最终呈现优先使用现有 Magic modal；如果该 modal 只适合确认弹窗，则实现阶段可选择项目内已有消息提示组件，但必须保持用户可见且风格一致。
