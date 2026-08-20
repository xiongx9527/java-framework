# 修复 Boot 回归与 Tiny Engine 前端可复现构建

日期：2026-08-20

## 目标

处理上一轮验收保留的两项说明：
1. Boot autoconfigure 全量测试中的 3 个既有失败；
2. Tiny Engine 前端构建不能依赖本机手工修复的 npm registry/node_modules 状态，建立可重复的依赖安装与构建流程。

## 当前基线

- 分支：dev
- 当前主工作区：/Users/xiongxiao/Workspace/Idea/codes/java-framework
- 已有 Tiny Engine 验收修复已合并到 dev。
- Orca worktree 创建本轮因 runtime unavailable 失败，因此本轮在干净 dev 工作区继续，所有改动范围严格限制在本任务文件。

## 实施任务

### 任务 1：复现并修复 Boot 三个失败

1. 分别运行三个失败测试，保存完整日志和退出码：
   - MagicAPIServletIsolationAutoConfigurationTest
   - MagicAPIMongoConfigurationTest
   - MybatisDataChangeAutoConfigurationTest
2. 对每个失败执行根因追踪：测试上下文、自动配置条件、依赖 bean、日志 appender/敏感信息处理。
3. 先补/调整最小失败测试，再修改生产实现；不通过 skipTests 掩盖。
4. 每个测试独立验证，再执行 Boot autoconfigure 全量回归。
5. 记录与 Tiny Engine 无关的剩余失败（若仍存在），不可扩大本任务范围。

### 任务 2：建立 Tiny Engine 前端可重复构建

1. 检查 Tiny Engine src/console 的 package、workspace、ignore、Maven exec 配置和历史版本，确认是否缺少 lockfile/packageManager/registry 约束。
2. 在不污染仓库 node_modules 的临时环境执行干净安装，验证当前失败是否可复现。
3. 选择最小可维护方案：优先提交 lockfile 和明确的 pnpm 版本/安装命令；仅在确有必要时补 registry 配置或 Maven 参数。
4. 增加一个能在干净环境阻止“未安装 workspace 依赖就直接构建”的检查，避免成功依赖本机残留 dist/node_modules。
5. 从干净依赖环境执行：安装、Tiny Engine 前端 build、Starter Maven package；确认不提交 node_modules 和生成 dist。

## 验证命令

- Boot 定向三测试；Boot autoconfigure 全量 test。
- Tiny Engine 前端全量 JS 测试（含 frontend-build）。
- Tiny Engine Starter mvn clean package。
- git diff --check、git status、检查未跟踪生成物。

## 收尾

- 仅提交本任务源码、测试、构建配置和必要 lockfile/文档。
- 不提交 node_modules、dist、target 或临时日志。
- 完成后给出测试统计、剩余失败和构建可复现性证据。
