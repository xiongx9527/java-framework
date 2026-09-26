# Brainstorming 检查点 — ai-magicapi-mcp-plugin

- 日期: 2026-09-25
- 阶段: design
- 状态: **用户已确认设计方案**

## 交互决策
- 迁移策略: 删除 Boot 层自研实现，收敛到 AI 层统一 mcp starter (aj-mcp) 内核。
- 能力范围: 功能对齐 `fw-plus-magicapi-plugin-mcp/README.md` 全功能。
- 深层设计: 在 Open 阶段已产出的 `design.md` 基础上细化，10 项决策 (D1-D10) 正式向用户呈现。

## 关键技术方案 (D1-D10)
1. **D1 插件形态** — 沉淀在 `fw-plus-ai-boot-autoconfigure` 的 `magicapi/plugin/mcp/` 包，复用既有 runtime/传输/集群骨架，不新增 starter。排除: 独立 starter、Boot 层中转。
2. **D2 能力注册** — 复用 Core `McpFeatureScanner` 反射通道 (私有 `FeatureMgr.addTool/addResource/addPrompt`) 注册动态能力。排除: 改 aj-mcp 内核、自维护 JSON-RPC。
3. **D3 注册时机** — 启动注册快照 + 监听 Magic-API 变更事件，单线程串行 diff 刷新 + `publishXxxChanged`。排除: 仅启动注册一次。
4. **D4 过滤位置** — include/exclude/methods/opt-in 与 `mcpInputSchema` 回退收在应用层 `MagicApiMcpToolRegistry`，不污染内核。
5. **D5 认证** — 新 `SaTokenOAuth2McpAuthService` 实现既有 `McpAuthService` SPI，scope 从 claims 裁剪；`@ConditionalOnBean` + `auth.enabled`，缺省 `PermitAllAuthService` 占位。排除: Boot 层 controller 拦截模式。
6. **D6 配置前缀** — 插件专属配置沿用 `magic-api.plugin.mcp.*` (兼容 README)，传输/runtime 走 `fw-plus.ai.mcp.*`，装配层单点缝合。排除: 全改 `fw-plus.ai.mcp.magicapi.*`。
7. **D7 resources/prompts** — Magic-API storage 动态配置 (model/vector 同款 `MagicResourceStorage`/`MagicDynamicRegistry` 模式) + 自定义 provider Bean 并存。排除: 静态只读配置。
8. **D8 diagnostics** — 独立 `MagicMcpDiagnosticController`，复用统一认证/会话/过滤，不绕过约束。
9. **D9 server-to-client** — 直接复用 aj-mcp 内核 (`listRoots`/`createMessage`/`elicit`/`sendProgress`/`publishLog`)，插件只做参数装配与状态记录。
10. **D10 Boot 层处置** — 删 75 Java + 25 测试 + 模块 + 聚合 pom 条目；console 前端随模块删除；README 可迁移说明并入 AI 层文档。

## 风险与对策
- 内核能力缺口 → 已 javap 勘察确认 aj-mcp 1.6 原生支持必需能力；个别 README 细节以插件侧 header/协商适配层补齐，不改内核。
- FeatureMgr 反射依赖私有签名 → 收敛在 Core 单点，版本受 BOM 约束。
- 双轨前缀缝合力 → 映射集中一处 + 测试覆盖。
- 动态 diff 竞态 → 单线程 executor + 快照 diff + 同步后通知。
- Boot 删除影响面 → 删除前全局 grep 引用清单，双库编译回归验证。

## 测试策略
- 单元测试: 过滤规则/认证 scope/storage 校验/通知触发 (对齐既有 25 个测试语义)。
- Spring 上下文测试: 装配与条件取舍 (三种认证场景、provider 聚合)。
- 回归: `fw-plus-boot` 编译 + AI 编译测试 + `fw-plus-sample` validate。
- 设计阶段 spike (任务 1.1): 验证 aj-mcp 多 runtime 下通知与 processMessage。

## 下一动作
- `comet state complete-design <change> --design-doc docs/openspec/changes/ai-magicapi-mcp-plugin/design.md`
- 推进 transition → build，加载 comet-build skill，按 tasks.md 从 1.1 开始。