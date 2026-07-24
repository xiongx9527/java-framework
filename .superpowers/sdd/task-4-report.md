# Task 4 Report: Magic-API DB 浏览器插件 UI

## 实现内容

- 新增独立 Maven 模块 `fw-plus-magicapi-plugin-db`，并加入 Magic-API 插件聚合 POM。
- 新增 Vite IIFE 插件，产物名为 `magic-db.1.0.0.iife.js`，全局名为 `MagicDB`，注册全屏、可关闭的数据库浏览器工具栏入口、图标和中英文 i18n。
- 新增 `magic-db-browser.js` service，统一通过插件运行时注入的 `request` 调用数据源、命名空间、对象、例程、详情和预览接口。
- 浏览页面包含数据源/命名空间/对象树、表和视图的字段/主键/索引/分页预览、函数和存储过程元数据、对象名称过滤、预览排序/简单 LIKE 过滤、加载/空/错误状态。
- 数据源、命名空间、对象和详情仅在请求成功时更新；失败时保留已加载上下文。

## TDD RED/GREEN 证据

- RED：在仅创建 Node 内置测试且 service 文件不存在时执行 `npm test`，得到 `ERR_MODULE_NOT_FOUND`，明确指向缺失的 `src/service/magic-db-browser.js`。
- GREEN：实现 service 后执行 `npm test`，1 个测试通过，验证六个端点均通过注入的 request 发起且参数完整。

## 测试结果

- `npm test`：通过，1/1。
- `npm install && npm run build`：通过，生成 `dist/magic-db.1.0.0.iife.js`（29.20 KiB）。
- `git diff --check`：通过。

## 变更文件

- `fw-plus-boot/.../fw-plus-magicapi-plugin/pom.xml`
- `fw-plus-boot/.../fw-plus-magicapi-plugin-db/pom.xml`
- `fw-plus-boot/.../fw-plus-magicapi-plugin-db/src/console/**`

## 自审发现

- 未修改宿主 console UI，未提前迁移 S3 UI。
- console 局部忽略构建产物、node_modules 和 package-lock，与现有插件不提交这些文件的约定一致。

## 问题/顾虑

- `npm install` 报告 4 个开发依赖漏洞，并提示 esbuild install script 未被 allow-scripts 覆盖；本次 Vite 构建仍成功。未引入额外生产依赖。

## Reviewer Fix: 失败时保留浏览上下文

- `selectSource` 与 `loadNamespace` 现在先将远端结果保存在局部变量，两个请求均成功后才同时切换选中状态并清空下级数据；失败时保留原数据源、命名空间和对象上下文。
- `selectObject` 现在先加载目标对象详情，并为表/视图加载首屏预览；两者均成功后才提交对象、详情、预览及第一页状态。失败时保留原对象的详情和预览。
- 当前 `npm test` 使用 Node 内置 runner，只覆盖 service 请求映射。该 console 未安装 Vue SFC 编译器、DOM 环境或组件测试工具，无法在不扩大依赖范围的情况下直接挂载 `<script setup>` 组件；本次运行既有 service 聚焦测试和 Vite 构建验证。
