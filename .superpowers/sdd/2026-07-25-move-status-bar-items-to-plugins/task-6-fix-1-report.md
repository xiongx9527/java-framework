# Task 6 第 1/2 轮审查修复报告

## 修复范围

- 将 `plugin-toolbar-order.contract.test.mjs` 从文本扫描和正则匹配改为 JavaScript AST 解析。
- 仅定位 `export default` 插件工厂顶层返回对象中的 `toolbars` 数组。
- 仅收集该数组的顶层静态对象元素，并只读取对象顶层的静态数值 `order`。
- 显式识别并跳过 Pulsar 的动态 `pulsarAdminToolbar`；其他非静态对象元素会令契约失败。
- 未修改 14 个生产 `index.js` 或任何 `order` 值，未改变生产行为。

## AST parser 依赖

宿主 Console 原 `package.json` 未直接声明可维护的 ECMAScript AST parser。为避免依赖 Vite 等工具的未声明传递依赖，在宿主 `devDependencies` 中显式加入 `acorn ^8.17.0`。

仓库当前没有已跟踪的 `package-lock.json`、`npm-shrinkwrap.json`、`yarn.lock` 或 `pnpm-lock.yaml`，因此本轮没有可同步修改的已跟踪锁文件，也未新增锁文件。

## RED

先增加内联对抗 fixture：toolbar 对象本身不含顶层 `order`，但注释、字符串和嵌套对象均包含伪 `order: 100`。

命令：

```bash
node --test test/plugin-toolbar-order.contract.test.mjs
```

结果：失败，1/2 通过。新增用例得到旧 helper 返回的整段对象文本，而不是 `{ order: undefined }`；原有 14 插件契约仍通过，证明旧实现会接受这类假阳性来源。

## GREEN

- 聚焦契约：`node --test test/plugin-toolbar-order.contract.test.mjs`，2/2 通过。
- 宿主全量：`npm test`，15/15 通过。
- 空白检查：`git diff --check`，无输出。

## 风险信号和顾虑

- `npm test` 仍输出既存的 `MODULE_TYPELESS_PACKAGE_JSON` 警告；本轮未修改模块类型配置。
- `npm install --package-lock=false` 报告现有依赖树有 18 个漏洞（4 moderate、13 high、1 critical），并提示 3 个依赖安装脚本尚未获 allow-scripts 批准；本轮未执行自动修复，以免扩大范围。
- 仓库没有已跟踪锁文件；`acorn` 已作为直接开发依赖声明，但依赖解析仍遵循现有项目使用 semver 范围且不锁版本的策略。
- 协调者未提交的 `openspec/changes/move-status-bar-items-to-plugins/.comet/subagent-progress.md` 未被修改、暂存或提交。
