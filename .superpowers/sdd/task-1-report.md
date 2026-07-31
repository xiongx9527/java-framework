# Task 1 Report: TinyEngine Source Synchronization

## 实现内容

- 新增 `scripts/sync-tiny-engine-console.sh`，接收 TinyEngine 来源目录和可选同步目标目录。
- 脚本校验 `package.json`、`pnpm-workspace.yaml`、`designer-demo`、`packages`、`LICENSE`，再使用 `rsync --delete` 生成自包含的 console 源码。
- 同步排除 `.git`、`.codegraph`、`node_modules`、`dist`、`coverage`、`.idea`、`.DS_Store` 与 `package-lock.json`。
- 生成 `src/console/.tinyengine-source.properties`，记录来源仓库、提交和 UTC 同步时间。
- 新增 Node 契约测试，使用临时 target 验证同步输入、`node_modules` 与 `package-lock.json` 排除，以及 `designer-demo` 和 commit 元数据保留。
- 模块级 `.gitattributes` 将 `src/console/**` 标为 `-whitespace`，避免原样 vendored 上游文件的既有尾随空白导致 `git diff --check` 失败。

## TDD 证据

- RED：在同步脚本不存在时运行 Node 测试，失败原因为 `scripts/sync-tiny-engine-console.sh: No such file or directory`。
- RED：上游确认不提供 `pnpm-lock.yaml` 后，更新 fixture 使用 `package-lock.json`，运行测试失败原因为旧脚本仍报告 `missing TinyEngine input: pnpm-lock.yaml`。
- GREEN：移除不成立的 `pnpm-lock.yaml` 输入要求并排除 `package-lock.json` 后，Node 测试通过（`pass 1`、`fail 0`）。

## 测试与验证

```text
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-source-sync.test.mjs
# 结果：pass 1, fail 0

sh fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/scripts/sync-tiny-engine-console.sh /Users/xiongxiao/Workspace/Idea/codes/TinyEngine
# 结果：完成

rg -n '^source\\.(repository|commit|syncedAt)=' .../src/console/.tinyengine-source.properties
# 结果：source.commit=484898fd0af3b0561c47ced9e159f3a9a0d1bee0

find .../src/console -name <each excluded entry>
# 结果：未发现 .git、.codegraph、node_modules、dist、coverage、.idea、.DS_Store、package-lock.json

git diff --check -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter
# 结果：无输出
```

## 变更文件

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/scripts/sync-tiny-engine-console.sh`
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-source-sync.test.mjs`
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/.gitattributes`
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/**`
- `.superpowers/sdd/task-1-report.md`

## 自评

同步脚本不依赖 Maven 读取外部 TinyEngine 绝对路径；该路径只在手动同步命令中作为输入。测试的可选 target 参数确保不会删除真实 `src/console`。

## 疑虑

指定 TinyEngine 提交没有 `pnpm-lock.yaml`，且含 `package-lock.json`。本任务已按确认后的边界排除 `package-lock.json`，不生成 pnpm lockfile；Task 2 需要使用 pnpm 生成并提交该 lockfile 后再执行 frozen 构建。

为保持 TinyEngine 源码同步保真，未清理上游已有的尾随空白；模块级 `.gitattributes` 已将这些 vendored 文件排除在 Git whitespace 检查之外。

## 评审修复（2026-07-31）

- 同步脚本现在先执行 `git status --porcelain --untracked-files=no`；来源存在已跟踪但未提交的改动时，以退出码 `3` 拒绝同步，保证 `source.commit` 与已复制内容一致。
- rsync 继续排除既有的本地/构建状态，并新增 `.env.local`、`*.timestamp-*.mjs` 与 `logs/`；使用 `--delete-excluded` 清理目标目录中之前同步遗留的排除项。
- starter 级 `.gitignore` 显式反忽略 `src/console/**/build/` 及其内容，以保留 TinyEngine workspace 的 `packages/build/vite-config`、`packages/build/vite-plugin-meta-comments` 和 `mockServer/build`。
- Node 契约测试覆盖构建输入保留、`package-lock.json`/`.env.local`/日志/timestamp 排除（含目标端遗留项清理）以及已跟踪来源变脏时的拒绝同步。
