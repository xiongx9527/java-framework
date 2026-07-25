# Comet Design Handoff

- Change: align-s3-browser-console-ui
- Phase: design
- Mode: compact
- Context hash: 73c3f553cca132f9f945b03fe6256c00a29f9f092e9a71fb6e960b7ccdeffda2

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/align-s3-browser-console-ui/proposal.md

- Source: openspec/changes/align-s3-browser-console-ui/proposal.md
- Lines: 1-27
- SHA256: 9c39c849190a9d2b0142b02a269cd4cf8e04dcef250ea0ebe12b6e86cd54774a

```md
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

```

## openspec/changes/align-s3-browser-console-ui/design.md

- Source: openspec/changes/align-s3-browser-console-ui/design.md
- Lines: 1-54
- SHA256: 897f1bf70257fd768e28ea78fd1c3abdc5cdbbc84252413c0f64d4ead544bd86

```md
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

```

## openspec/changes/align-s3-browser-console-ui/tasks.md

- Source: openspec/changes/align-s3-browser-console-ui/tasks.md
- Lines: 1-23
- SHA256: 59b36174875578d469d2aad1e8f454366ffc4e4076f3b656b7fa322e8175c2dd

```md
## 1. 基线与契约

- [ ] 1.1 对比 `magic-s3-datasource-info.vue`、同类插件信息页和当前 `magic-s3-browser.vue`，确认需要复用的 Magic 控制台布局、控件和主题变量。
- [ ] 1.2 补充或更新 S3 插件前端契约测试，覆盖工具栏入口、`name: "s3"`、`s3-browser` 工具栏和错误提示能力约束。

## 2. 页面风格一致性

- [ ] 2.1 重构 `magic-s3-browser.vue` 顶部操作区，使用与原 magic-s3 页面一致的紧凑表单布局和 Magic 控件风格。
- [ ] 2.2 调整文件列表、路径导航、详情区、空状态、加载状态和删除确认区，使其使用 Magic 控制台一致的边框、间距、颜色变量和稳定尺寸。
- [ ] 2.3 保留选择存储、路径导航、搜索、加载更多、basePath 保存、预览、下载和删除的现有交互语义。

## 3. 错误提示

- [ ] 3.1 在 `magic-s3-browser.vue` 中封装统一错误消息提取与弹出 helper，优先使用插件上下文可用的 Magic modal/message 机制。
- [ ] 3.2 将加载存储、列文件、保存 basePath、预览、下载和删除失败场景接入弹窗/消息提示，并保留后端返回的可读错误信息。
- [ ] 3.3 确保删除、预览和下载失败不会误显示成功状态，页面列表和当前选择保持可恢复。

## 4. 验证与构建

- [ ] 4.1 运行 S3 插件前端测试，确认契约测试通过。
- [ ] 4.2 运行 S3 插件前端构建，确认新版 `dist/magic-s3.1.0.0.iife.js` 包含更新内容。
- [ ] 4.3 运行 S3 插件 Maven package 或资源复制链路，确认新版 IIFE 进入 `target/classes/magic-editor/plugins`。
- [ ] 4.4 检查目标文件 diff 和 `git diff --check`，确认未引入无关格式问题。

```

## openspec/changes/align-s3-browser-console-ui/specs/magic-s3-browser-console-ui/spec.md

- Source: openspec/changes/align-s3-browser-console-ui/specs/magic-s3-browser-console-ui/spec.md
- Lines: 1-38
- SHA256: 2584b19483fba47d383f38988e39e95a11590d11d3ebea4bbb953c2418d8cc90

```md
## ADDED Requirements

### Requirement: S3 文件管理页面遵循 Magic 控制台风格
S3 文件管理页面 SHALL 使用与原 magic-s3 数据源配置页面一致的控制台布局、控件、间距、边框和主题变量。页面 SHALL 避免独立应用式卡片、营销式布局或与宿主控制台不一致的视觉风格。

#### Scenario: 页面打开后呈现控制台风格
- **WHEN** 管理员通过 S3 插件工具栏打开文件管理页面
- **THEN** 页面顶部操作区、表单控件、列表区域和详情区域使用 Magic 控制台一致的紧凑布局与主题变量

#### Scenario: 保持插件 UI 边界
- **WHEN** S3 文件管理页面被实现或构建
- **THEN** 页面源码和入口保持在 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI 内，不迁移到宿主 UI

### Requirement: S3 文件管理功能保持可用
S3 文件管理页面 SHALL 保留现有文件浏览能力，包括选择存储、路径导航、搜索、加载更多、basePath 保存、预览、下载和删除。视觉调整 MUST NOT 移除或破坏这些已有功能。

#### Scenario: 管理员浏览对象列表
- **WHEN** 管理员选择一个可用 S3 存储并进入路径
- **THEN** 页面展示该路径下的对象和目录，并允许继续切换路径或加载更多

#### Scenario: 管理员执行文件操作
- **WHEN** 管理员选择文件并点击预览、下载或删除操作
- **THEN** 页面调用对应 S3 浏览接口并保持原有操作语义

### Requirement: 操作错误通过弹窗或消息提示
S3 文件管理页面 SHALL 在加载存储、列文件、预览、下载、删除和保存 basePath 失败时，通过 Magic 控制台可见的弹窗或消息机制提示错误。错误提示 MUST 包含后端返回的错误信息或可读的失败原因。

#### Scenario: 列文件失败时弹出错误
- **WHEN** 管理员选择的 S3 存储未注册、配置错误或接口返回失败
- **THEN** 页面弹出错误提示，并显示后端返回的失败原因

#### Scenario: 删除失败时弹出错误
- **WHEN** 管理员删除文件失败
- **THEN** 页面弹出错误提示，并且不把失败误显示为删除成功

#### Scenario: 预览或下载失败时弹出错误
- **WHEN** 管理员预览或下载文件失败
- **THEN** 页面弹出错误提示，并保留当前文件列表和选择状态

```
