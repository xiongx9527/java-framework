# 本地功能分支精选整合设计

## 背景

本地 `feature/*` 与 `worktree-*` 分支中存在多批尚未以相同补丁进入 `dev` 的代码和测试。这些分支普遍落后 `dev` 数百个提交，并包含过期的 OpenSpec、Superpowers/SDD 状态与任务报告；部分分支还存在功能重叠或名称与实际内容不一致的问题。因此，本次不直接合并完整分支历史，而是把仍有效的代码和测试按模块重新整合到最新 `dev`。

设计确认后，`dev` 已从审计时的 `988455f2` 前进到 `53199588`。实施开始前必须再次冻结 `dev` 提交，并重新计算候选补丁；若整合期间 `dev` 再次变化，只能在完成验证后通过 rebase 或重新整合适配最新提交，不能覆盖并发更新。

## 目标

- 将 MCP 配置通知、POI、Template、MQTT、EasyTrans 和 Nebula JDBC 分支中的有效实现与测试整合到 `dev`。
- 对功能重叠的 POI、Nebula 和 MCP 通知改动做语义级取舍，形成单一最终实现。
- 保留当前 `dev` 的新提交和现有行为，不因旧分支基线回流旧代码。
- 完成模块定向测试以及 Core、Boot 分层构建验证后，再让 `dev` 快进到整合结果。

## 非目标

- 不执行八条旧分支的整体 `merge --no-ff`。
- 不把旧分支中的 `.superpowers/`、历史 OpenSpec change 状态、任务报告或 `.comet/current-change.json` 带入 `dev`。
- 不因为整合而升级依赖、Magic-API 版本或 Java 版本。
- 不推送远端，不删除本地分支或 worktree。
- 不处理候选分支之外的其他本地分支。

## 整合策略

采用“隔离分支 + 按模块精选提交 + 冲突后语义复核”的方式：

1. 从实施时最新的 `dev` 创建隔离 worktree 和 `codex/integrate-local-feature-worktree-branches` 分支。
2. 对每一组候选分支重新运行祖先、`git cherry` 和路径差异检查，排除已经等价进入最新 `dev` 的补丁。
3. 优先 cherry-pick 只修改目标模块代码和测试的提交；提交同时包含流程文档时，使用无提交 cherry-pick 后仅保留代码与测试。
4. 对同一文件的多分支实现，不按提交时间机械覆盖；以测试表达的最终行为、空值边界、资源生命周期和当前 `dev` API 为准进行人工合并。
5. 每完成一个模块就运行该模块定向测试并形成独立整合提交，便于审查和回退。
6. 全部验证通过后，确认主工作区仍为干净且 `dev` 未变化，再以 `--ff-only` 将整合分支推进到 `dev`。

## 模块边界与取舍

### MCP 配置通知

来源为 `feature/20260708/notify-mcp-config-changes`。`worktree-notify-mcp-config-changes` 的唯一补丁已与 `dev` 补丁等价，`worktree-expand-mcp-config-notifications` 已是 `dev` 祖先，因此只评估 feature 分支中剩余的通知时机、前缀过滤和回归测试。应保留“注册成功后再发布事件”的语义，并适配当前 `dev` 中已有通知结构。

### POI

主要来源为 `feature/20260713/stabilize-fw-plus-poi`，补充来源为名称不符的 `worktree-optimize-fw-plus-dify`。两者都修改 `FwPlusExcelTransService` 和对应测试，不能顺序覆盖。最终实现统一覆盖导入、导出、批量导出、模板导出和翻译服务的空值/边界行为；重复测试合并为一套，不引入只记录任务状态的提交。

### Template

来源为 `worktree-feature-20260714-optimize-fw-plus-template`。整合 `TemplateParser`、`ExcelParser`、Boot 自动配置和 Magic-API `TemplateModule` 的输入校验与能力边界，并保留相应测试。旧 OpenSpec archive 和验证报告不进入代码整合提交。

### MQTT

来源为 `worktree-mqtt-plugin-optimization`。整合 MQTT Server 默认启用边界、MQTT Client 配置解析、数据源刷新、订阅注册/保留/关闭和异常信息改进。重点防止刷新或移除数据源时遗留消费者，并确保持久化成功后才注册订阅。

### EasyTrans

来源为 `worktree-optimize-easytrans`。整合批量空元素处理、递归身份跟踪和代理回退保护，保留 `MateEasyTransUtilsTest`。不得改变现有外部接口或 Java 8 兼容性。

### Nebula JDBC

实现来源为 `worktree-stabilize-fw-plus-nebula-jdbc`，基线测试来源为 `worktree-stabilize-nebula-jdbc`。统一地址解析、连接配置范围校验、连接生命周期和 unsupported method 行为；只保留与当前模块结构/API 兼容的测试。两个分支的测试模型不一致时，以当前 `dev` 生产代码和最终生命周期契约为准调整。

## 错误与冲突处理

- cherry-pick 发生冲突时，先比较基线、分支和当前 `dev` 三方实现，再手工保留当前 API 与候选行为；禁止使用整侧覆盖解决冲突。
- 候选测试若依赖旧类名、旧包结构或已删除 Maven 配置，应调整为当前结构，而不是恢复旧结构。
- 某个补丁若已被 `dev` 以不同实现覆盖，且当前测试能证明目标行为，则记录为语义等价并跳过。
- 某模块测试失败时暂停后续模块整合，先判断是基线环境、依赖不可用还是实际回归。
- 若主工作区 `dev` 在整合期间前进，不直接移动 `dev` 引用；先将隔离分支重新基于新 `dev` 验证。

## 验证策略

1. 分支审计：记录实施基准提交，确认每个候选补丁的最终状态为已整合、语义等价或明确跳过。
2. 模块测试：分别运行 MCP/Boot 自动配置、POI、Template、MQTT Client、EasyTrans 和 Nebula JDBC 的定向测试。
3. Core 分层验证：使用当前仓库可用的 Maven 入口构建受影响的 Core 模块及其依赖。
4. Boot 分层验证：在 Core 产物可用后构建 Boot autoconfigure 及受影响插件模块。
5. Java 8 检查：确认生产代码未引入 Java 9+ API，Core 编译目标保持 1.8。
6. Git 验证：确认整合 worktree 干净、`dev..integration` 仅包含预期提交、候选代码文件无遗漏，最终推进使用 `git merge --ff-only`。

## 完成条件

- 六个模块组的有效代码与测试已进入整合分支，重叠实现已统一。
- 过期流程产物和旧 change 状态未进入整合差异。
- 所有可执行的定向测试和分层构建通过；任何因外部仓库或环境导致的未执行项都有明确证据。
- `dev` 只通过快进接收经过验证的整合提交，且不覆盖并发提交。
- 不推送、不删除候选分支和 worktree。
