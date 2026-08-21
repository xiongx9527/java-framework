# 验证报告：restore-s3-plugin-footer-entry

## 摘要

| 维度 | 状态 |
|------|------|
| 完整性 | 3/3 个任务完成，1 个增量规格已验证 |
| 正确性 | S3 外链入口、宿主通用扩展点、响应式重渲染、契约测试均已覆盖 |
| 一致性 | S3 专属入口逻辑保留在 S3 插件 UI 内，宿主只保留通用扩展能力 |

## 验证证据

- `openspec status --change "restore-s3-plugin-footer-entry" --json`：规划产物完整。
- `openspec instructions apply --change "restore-s3-plugin-footer-entry" --json`：3/3 个任务完成。
- `(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console && npm test && npm run build) && (cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console && npm run build) && (cd fw-plus-boot && mvn -f fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml process-resources -DskipTests && mvn -f fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/pom.xml prepare-package -DskipTests) && openspec validate restore-s3-plugin-footer-entry --strict && git diff --check && rg -n "pluginStatusBarItems|X-file-storage|statusBarItems" ...`：退出码为 0。
- S3 插件前端测试：`node --test test/*.test.cjs`，共 15 个测试，0 个失败。
- S3 插件构建：`vite build`，生成 `dist/magic-s3.1.0.0.iife.js`。
- 宿主控制台构建：`vite build` 通过；仅有既有 `config-js` 非 module 警告和大 chunk 警告。
- Maven 资源同步：宿主 `process-resources` 复制 15 个 `magic-editor` 资源；S3 插件 `prepare-package` 复制 1 个插件资源。
- OpenSpec 严格校验：通过。
- 空白检查：通过。
- 运行资源检查：`target/classes/magic-editor/plugins/magic-s3.1.0.0.iife.js` 包含 `statusBarItems` 和 `X-file-storage`；`target/classes/magic-editor/assets/index.3cd35be9.js` 包含宿主状态栏扩展逻辑和 `splice(0,0)` 头部插入逻辑。
- hash 一致性检查：宿主 `src/console/dist/index.html` 与 `target/classes/magic-editor/index.html` 均指向 `index.3cd35be9.js` 和 `vendor.17332c13.js`。

## 失败闭环

- 用户反馈“没生效”后，从 `archive` 重新打开到 `verify`，再以 `verify-fail` 回到 `build` 修复。
- 根因：宿主 `navs` 直接读取非响应式的 `constants.PLUGINS`。当只加载 S3 或没有其他已知插件状态变更时，computed 可能不重新计算，导致插件注册的 `statusBarItems` 不进入状态栏。
- 新增失败测试 `S3 外链入口通过响应式插件状态栏列表触发重新渲染`，先验证失败，再通过实现修复后转绿。

## 完整性

- `openspec/changes/restore-s3-plugin-footer-entry/tasks.md` 中所有任务均已完成。
- `fw-plus-magicapi-plugin-s3/src/console/src/index.js` 注册 `statusBarItems`，包含原 X-file-storage 外链入口。
- `magic-status-bar.vue` 将插件返回的通用 `statusBarItems` 同步到响应式 `pluginStatusBarItems`，并插入到在线人数后面的状态图标首位。
- S3 插件契约测试覆盖入口来自插件、链接地址、icon、displayKey、响应式重渲染，以及宿主不包含 `hasS3Plugin`、`openS3()`、`s3-datasource` 专属逻辑。

## 正确性

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js:35` 在 S3 插件 UI 返回对象中声明 `statusBarItems`。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js:36` 使用 S3 插件 icon `#magic-s3-s3`。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js:37` 恢复标题 `X-file-storage`。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js:39` 点击后新标签打开 `https://x-file-storage.xuyanwu.cn/`。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue:36` 使用 `pluginStatusBarItems` 响应式承载插件状态栏项。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue:37` 在插件加载后刷新状态栏扩展项。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue:47` 使用 `array.splice(0, 0, ...items)` 将插件状态栏入口放在在线人数后的第一个图标位置。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue:202` 使用通用 `pluginStatusBarItems` 扩展点，不判断 S3 插件。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/test/magic-s3-browser-contract.test.cjs:61` 增加契约测试，覆盖插件注册、宿主边界和响应式刷新。

## 一致性

- S3 专属入口元数据、icon、标题、跳转行为都位于 S3 插件 UI。
- 宿主状态栏没有恢复 `hasS3Plugin`、`openS3()` 或 `s3-datasource` 检测。
- 改动不触碰 S3 后端接口、数据源配置、文件管理页面行为或 `adminOnly` toolbar 门控。
- 未新增硬编码密钥或不安全操作。
- 当前 `review_mode=off`，按 Comet 状态跳过自动代码审查；构建、测试、OpenSpec 校验和安全抽查均已执行。

## 问题

### 严重问题

无。

### 警告

无。

### 建议

无。

## 最终结论

所有检查通过，可以进入归档阶段。
