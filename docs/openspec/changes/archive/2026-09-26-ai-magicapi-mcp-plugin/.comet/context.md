# Comet Design Handoff

- Change: ai-magicapi-mcp-plugin
- Phase: design
- Mode: compact
- Context hash: ae490fa62a0d9991ef66995a16be538eaa66f0328c0431e3f098a92f4a136780

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/ai-magicapi-mcp-plugin/proposal.md

- Source: docs/openspec/changes/ai-magicapi-mcp-plugin/proposal.md
- Lines: 1-39
- SHA256: b70e335e32119656d092ea63bd74b741296d41989417b692224a058f15d842bf

```md
# Proposal

## Why

Boot 层 `fw-plus-boot-autoconfigure` 维护着一套**自研 JSON-RPC** 的 Magic-API MCP 插件（`magicapi/plugin/mcp/`，75 个 Java + 25 个测试），与 AI 层已基于 `aj-mcp` 内核实现的 `fw-plus-ai-mcp-spring-boot-starter` 功能重叠、双轨并存。用户希望收敛到 AI 层统一的 MCP 内核：删除 Boot 层自研实现，在 `fw-plus-ai-boot-autoconfigure` 内基于 mcp starter（aj-mcp）实现功能对齐的 Magic-API MCP 插件，能力对齐现有 README 描述的全功能。

## What Changes

- **BREAKING**: 删除 Boot 层自研 MCP 插件全部实现——`fw-plus-boot-autoconfigure` 中 `magicapi/plugin/mcp/` 包（75 个 Java 文件）及其测试（25 个）。
- **BREAKING**: 更新 Boot 层 `MagicApiPluginAutoConfiguration`，移除对 `MagicAPIMcpConfiguration.class` 的 import（第 16 行）与注册（第 60 行），不再装配 MCP 插件。
- **BREAKING**: 移除 Boot 层 `fw-plus-magicapi-plugin-mcp` 模块（含 `fw-plus-magicapi-plugin/pom.xml` 聚合条目）；其 console 前端资源与 README 按用户确认事项迁移/处置。
- 在 `fw-plus-ai-boot-autoconfigure` 中新增 `magicapi/plugin/mcp/` 实现，基于 aj-mcp 内核（`fw-plus-ai-mcp` / mcp starter）：
  - 将 Magic-API 函数 / API / 任务自动注册为 MCP 工具，支持 groups/paths/methods/opt-in 过滤与 `mcpInputSchema` 回退。
  - 新增 `mcp-resource`、`mcp-prompt` 两类代码类 MagicAPI 资源类型及其 storage 校验（URI/MIME/name/arguments/messages）。
  - Streamable HTTP 与 SSE 端点、协议版本协商（2025-11-25 / 2025-06-18）、`Mcp-Session-Id`/header 语义。
  - Sa-Token OAuth2 token 校验 + capability scope（tools/resources/prompts/…）接入 mcp starter 认证 SPI（`McpAuthService`），保留自定义 verifier 扩展点。
  - SSE 变更通知（tools/resources/prompts list_changed、resources/updated，经 aj-mcp `publishXxxChanged`），Redis-only 集群跨节点分发与回环抑制。
  - Server-to-client 请求（roots/list、sampling/createMessage、elicitation/create）与 pending request 生命周期，复用 aj-mcp 内核能力。
  - 控制台诊断入口（`diagnostics/execute`）与 mcp-resource/mcp-prompt 预览。
  - 可扩展 provider：`McpToolProvider` / `McpResourceProvider` / `McpPromptProvider` / `McpCompletionProvider`。
- 新增配置绑定：MCP + Magic-API 插件开关与过滤规则，配置前缀与 Boot 层既有 README 的兼容性由设计阶段确定（候选 `magic-api.plugin.mcp.*` 或 `fw-plus.ai.mcp.*`）。
- 更新/新增 OpenSpec capability：`mcp-server-starter` 之「与 Magic-API 集成」requirement 行为变更，新增 `magicapi-mcp-plugin` capability 覆盖工具暴露、资源/提示词类型、通知、认证、集群与诊断行为。

## Capabilities

### New Capabilities

- `magicapi-mcp-plugin`: 基于 aj-mcp 内核的 Magic-API MCP 插件——工具暴露（函数/API/任务与过滤规则）、mcp-resource/mcp-prompt 资源类型与 storage、SSE 变更通知、Redis 集群分发、Sa-Token OAuth2 认证与 scope、diagnostics、可扩展 provider 的全部行为。

### Modified Capabilities

- `mcp-server-starter`: 「与 Magic-API 集成」requirement 由「将接口/函数/任务注册为工具」的简单承诺扩展为「由 AI 层 magicapi mcp 插件提供完整工具暴露（含过滤/opt-in/inputSchema）、认证、通知与集群语义」，其交付主体从 Boot 层自研实现迁移至 AI 层 autoconfigure。

## Impact

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure`：删除 `magicapi/plugin/mcp/` 包（75 Java）、相关测试（25）、更新 `MagicApiPluginAutoConfiguration`。
- `fw-plus-boot/.../fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp`：模块移除，聚合 pom `fw-plus-magicapi-plugin/pom.xml` 更新；console 前端资源处置按迁移决策执行。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`：新增 `magicapi/plugin/mcp/` 实现、spring.factories / AutoConfiguration.imports 注册、`McpAuthService` 的 Sa-Token OAuth2 实现；pom 需引入 magic-api 插件与 sa-token-oauth2 相关 optional 依赖。
- `docs/openspec/specs/mcp-server-starter/spec.md`：delta spec 修改「与 Magic-API 集成」requirement。
- 依赖层面：`sa-token-oauth2`、Magic-API plugin 依赖的传递关系从 Boot 层模块迁移至 AI 层 autoconfigure（保持 optional，`org.ssssssss` 版本受约束不动）。
```

## docs/openspec/changes/ai-magicapi-mcp-plugin/design.md

- Source: docs/openspec/changes/ai-magicapi-mcp-plugin/design.md
- Lines: 1-98
- SHA256: 573fc0c41fe5ca5b88998c99111a00cd5d437c1d20cb4e1e783e3bba440dca71

[TRUNCATED]

```md
---
comet_change: ai-magicapi-mcp-plugin
change_ref: docs/openspec/changes/ai-magicapi-mcp-plugin
role: technical-design
canonical_spec: openspec
status: draft
---

# Design

## Context

现况与动机见 `proposal.md`。关键技术前提（已勘察验证）：

- `fw-plus-ai-mcp-spring-boot-starter` 为纯依赖 POM，实现全在 `fw-plus-ai-boot-autoconfigure`，经 `McpAutoConfiguration` 装配多 runtime（default + `servers[]`），`McpAuthService` 目前是放行占位（`PermitAllAuthService`），传输按 Web 栈分流（webmvc `McpRoutingRegistrar` / webflux `McpWebfluxWebBinding`），Redis 集群经 `McpRedisBroadcaster` + `McpRedisMessageConsumer`。
- aj-mcp 内核（`aj-mcp-server:1.6`）已原生提供：`publishToolsChanged()` / `publishPromptsChanged()` / `publishResourcesChanged()` / `publishResourceUpdated(uri)`（变更通知）、`listRoots()` / `createMessage()` / `elicit()`（server-to-client）、`sendProgress()` / `publishLog()`、协议版本协商 `onProtocolNegotiated` + `getNegotiatedProtocolVersion()`、`getClientCapabilities()`。
- `FeatureMgr`（aj-mcp）的 addTool/addResource/addPrompt 是私有方法，Core 层 `fw-plus-ai-mcp` 已通过 `McpFeatureScanner` 反射调用注册 `@McpService` 能力；同样的反射通道可复用于注册 Magic-API 动态能力。
- Boot 层自研实现的可迁移行为契约：工具过滤规则、mcp-resource/mcp-prompt 校验、Sa-Token OAuth2 认证 scope、SSE 通知、集群分发、diagnostics、provider SPI。
- AI autoconfigure 已有 `magicapi/plugin/` 结构（model/vector 插件经 `MagicApiPluginAutoConfiguration` 聚合，由 `MagicAPIBaseAutoConfiguration` 关联启动），新 mcp 插件复用该聚合入口。

## Goals / Non-Goals

**Goals:**
- 复用既有 mcp starter 的 runtime/传输/集群骨架，Magic-API 插件作为「能力源」接入，不重写 JSON-RPC。
- 能力对齐 `fw-plus-magicapi-plugin-mcp/README.md` 全功能，且既有需求/行为经 spec 固化。
- 删除 Boot 层自研实现且不留残余引用，AI 层编译装配通过。

**Non-Goals:**
- 不改 aj-mcp 内核 / Core `fw-plus-ai-mcp` / `fw-plus-ai-mcp-spring-boot-starter` POM 形态；`org.ssssssss` 组依赖版本保持不动。
- 不改既有 `@McpService` 扫描、多 runtime、会话归属校验、集群 routeKey 语义等 mcp starter 主干行为。
- 不实现 Authorization Server（仅消费 Sa-Token OAuth2 发牌结果，与 README 一致）。
- 不新增 MCP datasource 目录。

## Decisions

### D1: 插件形态——AI autoconfigure 内实现，不新增 starter 模块
Boot 层 `fw-plus-magicapi-plugin-mcp` 是「console 前端 + pom 依赖」的 thin 模块，实现全在 autoconfigure；AI 层 `fw-plus-ai-mcp-spring-boot-starter` 已是同样的「依赖 POM + autoconfigure 实现」模型。因此 mcp 插件与 model/vector 一致，直接沉淀在 `fw-plus-ai-boot-autoconfigure` 的 `magicapi/plugin/mcp/` 包内，由 `MagicApiPluginAutoConfiguration` 的 `@Import` 聚合。
- 备选 A：新建 `fw-plus-ai-magicapi-mcp-plugin-spring-boot-starter`。拒绝——与 mcp starter 现有模型重复，且无需独立发版。
- 备选 B：复用 Boot 层模块改造成依赖中转。拒绝——Boot 层模块整体移除是用户已确认的迁移方向。

### D2: 能力如何进入 aj-mcp FeatureMgr——复用 McpFeatureScanner 反射通道
Core `fw-plus-ai-mcp` 已有「反射调用私有 `FeatureMgr.addTool/addResource/addPrompt/addCompletion`」的能力注册通道（`McpFeatureScanner`）。Magic-API 动态能力照同通道注册：插件实现创建运行时动态的工具/资源/提示词 store（见 D4），按变更将 store 全量 diff 同步进 FeatureMgr。变更后调用 `McpServer.publishXxxChanged()` 触发通知。
- 备选 A：改 aj-mcp 开放注册 API。拒绝——改外部内核，超出本 change；且已有通道可复用。
- 备选 B：自维护一套独立的 JSON-RPC 分发（照抄 Boot 自研）。拒绝——正是要消除的重复。

### D3: 工具注册时机与动态性——启动注册 + 变更时 diff 刷新
Magic-API 的能力（api/function/task）运行时可热变更（`MagicAPIBaseAutoConfiguration` 已提供动态注册语境）。设计为：插件在 runtime 初始化后将当前快照注册，此后监听 Magic-API 资源变更事件（复用既有 `MagicNotifyService`/cluster 通知通道或 storage 监听），增量 diff 同步进 FeatureMgr 并 `publishXxxChanged()`。
- 备选 A：仅启动时注册一次。拒绝——Magic-API 运行期即改即生效的既有语义会被破坏。

### D4: 过滤与应用层 store 放置——在插件侧而非内核侧
工具过滤规则（include/exclude/methods/opt-in）、`mcpInputSchema` 回退、scope 校验属于 Magic-API → MCP 的映射语义，收在插件的 `MagicApiMcpToolRegistry`（纯应用层，直接吃 Magic-API APIInfo/FunctionInfo 元数据），产出已过滤后的 `iTool` 列表再经 D2 通道注册。registry 持有（tools/resources/prompts 三类）快照，`McpToolProvider` 返回即快照，避免每次 `tools/list` 重复过滤。
- 备选 A：把过滤逻辑沉入 FeatureMgr。拒绝——内核不应感知 Magic-API 元数据。

### D5: 认证——以 McpAuthService 实现 Sa-Token OAuth2 校验，不引入覆盖 controller
mcp starter 已有认证 SPI（`McpAuthService.authenticate(McpRequest)`）+ 占位 `PermitAllAuthService`，README 的「Sa-Token OAuth2 资源服务器校验 + scope 裁剪」正好落位成一个新实现 `SaTokenOAuth2McpAuthService`（条件装配：`@ConditionalOnBean` sa-token-oauth2 且 `auth.enabled=true`，缺省 fallback 放行），scope 映射从 token claims 抽 capability scope 列表。不覆盖 `McpTransportController`/SSE controller，避免重走 Boot 层「controller 内手工鉴权」模式。
- 备选 A：复用 Boot 层 `McpAccessTokenVerifier` 的 controller 拦截模式。拒绝——mcp starter 已统一收敛到 SPI，认证路径经一次 MCP 请求上下文抽象；自定义 verifier（README 承诺）以 SPI 的替换 bean 或 scope 映射策略提供。

### D6: 配置前缀——`magic-api.plugin.mcp.*` + `fw-plus.ai.mcp.*` 双轨
Boot 层用户已按 README 使用 `magic-api.plugin.mcp.*`；AI 层 mcp starter 用 `fw-plus.ai.mcp.*`。设计：Magic-API MCP 插件的专属配置（enabled/endpoint/tools/auth/resources/prompts 等）沿用 `magic-api.plugin.mcp.*` 前缀原键（兼容既有配置迁移），通过 `@ConfigurationProperties("magic-api.plugin.mcp")`；传输与 runtime 归属仍走 `fw-plus.ai.mcp.*`（既有 mcp starter 语义），两者在装配层缝合成目标插件。
- 备选 A：全部改成 `fw-plus.ai.mcp.magicapi.*`。拒绝——破坏既有 README 用户配置，迁移成本高，且与「对齐 README 全功能」的目标相悖。设计中默认保留 `magic-api.plugin.mcp.*`，并在文档中标明两轨映射关系。
- （本决策若在实际装配中遇到前缀冲突，按 Open Questions 处理。）

### D7: resources/prompts 配置来源——Magic-API storage 动态配置 + 自定义 provider 并存
`mcp-resource`/`mcp-prompt` 是 Magic-API 动态资源类型（控制台维护），插件侧实现对应 `MagicResourceStorage` 读取校验（复用 model/vector 同款 `MagicResourceStorage`/`MagicDynamicRegistry` 模式），校验通过才进 FeatureMgr。自定义 provider Bean（工具/资源/提示词/completion）与动态能力并存（各自注册，`tools/list` 聚合）。
- 备选 A：静态 resources/prompts 只读配置文件。拒绝——README 明确控制台维护，动态语义优先。

### D8: diagnostics 与 health 语义——独立 controller，复用统一认证/会话
`diagnostics/execute` 走独立 `MagicMcpDiagnosticController`，复用同一个认证 SPI + 会话归属校验 + provider 过滤，不绕过既有约束（spec「诊断受既有约束」照此实现）。
- 备选 A：并入 transport controller 方法分发。拒绝——路由语义与 JSON-RPC 主通道区分更清晰，且不污染 aj-mcp processMessage。

### D9: server-to-client 与 progress/logging——直接复用 aj-mcp 内核
roots/sampling/elicitation、progress、logging audit 已由 aj-mcp `McpServer` 原生提供，插件仅做参数装配与状态记录（pending request 表来自 `McpServer.getRunningRequests()`/`getPendingClientResponses()`），不加自定义 JSON-RPC。诊断/SSE 通知/版本协商同样走内核能力。
- 备选 A：自实现。拒绝——重复内核已实现能力。

### D10: Boot 层处置
- 删除 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/mcp/`（75 Java）与 `src/test/.../mcp/`（25 测试）。
- `MagicApiPluginAutoConfiguration` 移除 mcp import/注册。
- 移除 `fw-plus-magicapi-plugin-mcp` 模块 + 聚合 pom 条目。
- console 前端（`src/console/`，含 node_modules 构建产物）随模块删除；README 中可迁移的说明性内容并入 AI 层插件文档。node_modules/构建产物不归档。


```

Full source: docs/openspec/changes/ai-magicapi-mcp-plugin/design.md

## docs/openspec/changes/ai-magicapi-mcp-plugin/tasks.md

- Source: docs/openspec/changes/ai-magicapi-mcp-plugin/tasks.md
- Lines: 1-54
- SHA256: a486a3bf18c6bcc35d801804629c1ded5e815d0060d2bbf940e81c4e2f5cb8e3

```md
# Tasks

## 1. 勘察与基座验证

- [ ] 1.1 用最小 spike 新建临时测试类验证 aj-mcp `McpServer.publishToolsChanged/publishResourcesUpdated`、`listRoots/createMessage/elicit`、`processMessage` 在多 runtime 下可用，验证方式：spike 代码编译通过且运行时日志可见通知/请求行为（完成后删除 spike）
- [ ] 1.2 确认 Core `fw-plus-ai-mcp` `McpFeatureScanner` 暴露的注册通道签名与调用方式，验证方式：阅读源码并记录可复用的 addTool/addResource/addPrompt/addCompletion 反射入口
- [ ] 1.3 确认搏层删除前全部引用点（grep 全局 `magicapi.plugin.mcp`、`MagicAPIMcpConfiguration`、`plugin/mcp`、`fw-plus-magicapi-plugin-mcp`），验证方式：产出引用清单，覆盖 main/test/resources/pom/docs

## 2. 插件配置与装配

- [ ] 2.1 在 `fw-plus-ai-boot-autoconfigure` 新增 `magicapi/plugin/mcp/config/MagicApiMcpProperties`（`@ConfigurationProperties("magic-api.plugin.mcp")`），字段对齐 Boot 层 README 的 enabled/endpoint/protocol/auth/tools/headers/cluster 等键，验证方式：单元测试断言默认值与嵌套绑定
- [ ] 2.2 新增 `magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration` 与条件装配（`@ConditionalOnProperty magic-api.plugin.mcp.enabled`、`@ConditionalOnClass` magic-api/aj-mcp），验证方式：配置类被 `MagicApiPluginAutoConfiguration` @Import 后 Spring 测试上下文可见
- [ ] 2.3 在 `MagicApiPluginAutoConfiguration`（AI 层）@Import 追加 mcp 配置，验证方式：AI autoconfigure 编译通过、装配测试断言 mcp bean 存在

## 3. 工具暴露

- [ ] 3.1 新增 `magicapi/plugin/mcp/tools/MagicApiMcpToolRegistry`：订阅 Magic-API api/function 元数据，应用 include-groups/paths、exclude-paths、methods、opt-in 过滤，产物为已过滤工具快照，验证方式：单元测试覆盖分组/路径/method/opt-in/排除优先各场景
- [ ] 3.2 实现 `mcpInputSchema` 优先、缺失回退 `{type:object}`，`tools/call` 返回 content + structuredContent，验证方式：单测断言 schema 选取与回退
- [ ] 3.3 经 Core `McpFeatureScanner`/反射通道将工具快照注册进 FeatureMgr，注册后调用 `publishToolsChanged`，验证方式：单测断言 FeatureMgr toolStore 含注册工具、通知触发
- [ ] 3.4 增量 diff 刷新：监听 Magic-API 能力变更事件，串行单线程 diff 同步 FeatureMgr 并 `publishToolsChanged`，验证方式：模拟变更事件断言 store 增量同步且无重复注册

## 4. resources / prompts 资源类型

- [ ] 4.1 新增 `mcp-resource`/`mcp-prompt` 两类 Magic-API 代码类资源类型的 storage 实现（参考 model/vector 的 `MagicResourceStorage`/`MagicDynamicRegistry` 模式），校验 URI/MIME/name/arguments/messages 字段，验证方式：单测覆盖合法/非法字段、storage 拒绝路径
- [ ] 4.2 校验通过的能力经反射通道注册为 aj-mcp resource/prompt/resource-template/completion，注册后 `resources/list`、`prompts/list`、`resources/read`、`prompts/get`、`completion/complete` 可访问，验证方式：单测断言 store 注册与 handler 行为
- [ ] 4.3 支持自定义 `McpToolProvider`/`McpResourceProvider`/`McpPromptProvider`/`McpCompletionProvider` Bean 并存聚合，验证方式：装配测试注入自定义 provider 后 list 聚合出动态 + 自定义能力

## 5. 认证与 scope

- [ ] 5.1 新增 `SaTokenOAuth2McpAuthService` 实现 `McpAuthService`：校验 Sa-Token OAuth2 token，从 claims 解析 scope 列表，按能力（tools/resources/prompts/…）裁剪，验证方式：单测覆盖有效/无效 token、scope 缺失拒绝、认证关闭放行
- [ ] 5.2 条件装配：`@ConditionalOnBean` sa-token-oauth2 且 `auth.enabled=true`，缺省 `PermitAllAuthService` 占位保留；支持替换 scope 映射策略（自定义 verifier 语义），验证方式：装配测试断言三种场景（启用/关闭/自定义）下的 bean 选择
- [ ] 5.3 将认证接入既有最新校验流（复用 `McpAuthService` SPI，不覆盖 controller），验证方式：集成式单测——带合法 token 请求通过、非法 token 返回客户端错误且不进入业务分发

## 6. 变更通知与集群

- [ ] 6.1 资源/提示词/工具注册变更后分别调用 aj-mcp 通知方法，验证方式：单测断言 `publishResourcesChanged`/`publishPromptsChanged`/`resources/updated`（按 URI 订阅）触发路径
- [ ] 6.2 接入既有 Redis 集群分发（复用 `McpRedisBroadcaster`/`McpRedisMessageConsumer` 通道）与 `MagicNotifyService` 变更事件，携带 origin id 抑制回环，验证方式：单测断言跨节点投递与回环忽略、单节点降级行为

## 7. server-to-client 与诊断

- [ ] 7.1 复用 aj-mcp 内核 `listRoots/createMessage/elicit`，仅做初始化请求前 capability 检查与 pending request 状态记录（基于 `getClientCapabilities`/`getRunningRequests`），验证方式：单测断言未声明 capability 不发起请求、响应按 id 关联 resolved/failed
- [ ] 7.2 新增 `MagicMcpDiagnosticController`：`diagnostics/execute` 调用 `resources/read`/`prompts/get`，复用统一认证/会话/过滤，不绕过约束，验证方式：测试断言合法（走能力执行）/未授权（被拒）两条路径

## 8. Boot 层清理

- [ ] 8.1 删除 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/mcp/`（75 Java）与 `src/test/.../mcp/`（25 测试），验证方式：目录移除且无编译引用
- [ ] 8.2 更新 Boot `MagicApiPluginAutoConfiguration`：移除 mcp import（第 16 行）与注册（第 60 行），验证方式：编译通过
- [ ] 8.3 移除 `fw-plus-magicapi-plugin-mcp` 模块及 `fw-plus-magicapi-plugin/pom.xml` 聚合条目，console 前端（含 node_modules 构建产物）随之移除，验证方式：pom 聚合列表无该模块、模块目录删除
- [ ] 8.4 将 README 可迁移的说明性内容并入 AI 层 mcp 插件文档，验证方式：AI 插件侧文档覆盖工具过滤/资源类型/认证/通知/集群/诊断各节

## 9. 回归与联调

- [ ] 9.1 `fw-plus-boot` 全量编译 + 既有测试通过（验证删除无残留），验证方式：`./fw-plus-core/mvnw -f fw-plus-boot/pom.xml clean install -DskipTests` 及 autoconfigure 测试
- [ ] 9.2 `fw-plus-ai` 编译 + 新增 mcp 插件测试全绿，验证方式：`./fw-plus-core/mvnw -f fw-plus-ai/pom.xml test`（或对应 scope）
- [ ] 9.3 样例聚合入口 validate 通过、无 `MagicApiPluginAutoConfiguration`/插件类解析错误，验证方式：`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`
```

## docs/openspec/changes/ai-magicapi-mcp-plugin/.openspec.yaml

- Source: docs/openspec/changes/ai-magicapi-mcp-plugin/.openspec.yaml
- Lines: 1-2
- SHA256: 8ce533eab9c047a4566f79d81b21eb252f3437f591dee942f9dbb2669f23cdfb

```md
schema: spec-driven
created: 2026-09-25

```

## docs/openspec/changes/ai-magicapi-mcp-plugin/specs/magicapi-mcp-plugin/spec.md

- Source: docs/openspec/changes/ai-magicapi-mcp-plugin/specs/magicapi-mcp-plugin/spec.md
- Lines: 1-157
- SHA256: f06a3e636c15ee5bf2875a521b3faa1e1b7d6fd1ff8cd25582ae86324f6460c0

[TRUNCATED]

```md
# Spec Delta

## Purpose

在 AI 层基于 mcp starter（aj-mcp 内核）提供 Magic-API MCP 插件：将 Magic-API 的函数、API、任务自动暴露为 MCP 工具，新增控制台维护的 mcp-resource 与 mcp-prompt 资源类型，并提供 Sa-Token OAuth2 认证、SSE 变更通知、Redis 集群分发与诊断能力。

## ADDED Requirements

### Requirement: 工具暴露
插件 SHALL 支持将 Magic-API 的函数与 API 自动注册为 MCP 工具，且函数工具默认启用、API 工具默认关闭。用户 SHALL 能通过分组（group）、请求路径、HTTP method 与 opt-in 开关配置工具暴露范围，规则以「排除优先、白名单命中外放」为语义。

#### Scenario: 函数工具默认暴露
- **WHEN** 启用 Magic-API MCP 插件且未额外配置工具范围
- **THEN** 全部 Magic-API 函数注册为 MCP 工具，`tools/list` 列出、`tools/call` 可调用

#### Scenario: API 工具需显式启用
- **WHEN** 插件启用且未设置 `tools.api.enabled=true`
- **THEN** Magic-API 的 HTTP API 不注册为 MCP 工具，`tools/list` 不包含任何 API 对应工具

#### Scenario: 路径与分组过滤
- **WHEN** 配置包含 include-groups / include-paths / exclude-paths
- **THEN** 以 Ant 风格匹配 Magic-API group path 与 API/function path，`exclude-paths` 优先级最高，仅同时命中 include 且未命中 exclude 的能力注册为工具

#### Scenario: API 方法过滤
- **WHEN** 配置指定 `methods`（如 GET、POST）且启用 API 工具
- **THEN** 仅 HTTP method 命中的 API 注册为工具，其余不暴露

#### Scenario: opt-in 显式放行
- **WHEN** `require-opt-in=true` 且 API 的 options/attributes 未显式设置 opt-in 标记
- **THEN** 该 API 不注册为工具；设置了 opt-in 标记后才外放

#### Scenario: inputSchema 优先与回退
- **WHEN** Magic-API 函数/API 元数据包含 `mcpInputSchema`
- **THEN** 该 JSON object 作为 `tools/list` 返回的 `inputSchema`
- **WHEN** `mcpInputSchema` 缺失或无效
- **THEN** 安全回退为 `{ "type": "object" }`，工具仍可列出与调用，`tools/call` 返回 MCP content 与 `structuredContent`

### Requirement: resources 与 prompts 资源类型
插件 SHALL 提供 `mcp-resource` 与 `mcp-prompt` 两类 Magic-API 代码类资源类型，由控制台维护文件内容，插件负责存储校验（URI、MIME、prompt name、arguments、messages 等字段）并按 MCP 语义对外暴露。

#### Scenario: mcp-resource 校验与读取
- **WHEN** 控制台维护一个 `mcp-resource`，含合法 URI、name、mimeType、description 与 template 标记
- **THEN** 插件将其注册为 MCP resource，`resources/list` 列出、`resources/read` 按 URI 返回内容
- **WHEN** 资源 URI 非法或字段缺失
- **THEN** 后端 storage 拒绝并返回校验错误，不注册该资源

#### Scenario: mcp-prompt 校验与获取
- **WHEN** 控制台维护一个 `mcp-prompt`，含合法 name、arguments 与 messages（message 含 role 与结构化 content，文本支持 `{{argumentName}}` 占位符）
- **THEN** 插件将其注册为 MCP prompt，`prompts/list` 列出、`prompts/get` 按 name 展开占位符后返回
- **WHEN** prompt 字段缺失或与 MCP 语义不符
- **THEN** storage 拒绝并返回校验错误

#### Scenario: 资源模板与 completion
- **WHEN** 控制台维护带 template 标记的 resource，且配置了 completion 定义
- **THEN** 插件按 MCP 语义暴露 resource template 与 completion，`completion/complete` 可返回建议参数

### Requirement: 认证与 scope
插件 SHALL 作为 OAuth2 Protected Resource 校验访问凭据，支持按 MCP 能力（tools、resources、prompts 及厂商扩展能力）的 scope 裁剪，且允许用户替换 token 解析与 scope 映射实现；关闭认证仅限本地调试。

#### Scenario: 有效 token 放行
- **WHEN** MCP 请求携带合法的 Sa-Token OAuth2 token 且具备请求能力的对应 scope
- **THEN** 请求进入业务分发（tools/resources/prompts），结果按配置返回

#### Scenario: 无效或缺失 token 拒绝
- **WHEN** token 缺失、过期、签名无效或已吊销
- **THEN** 请求被拒绝并返回客户端错误（401/403 语义），不进入业务分发

#### Scenario: scope 裁剪
- **WHEN** token 合法的但缺少请求能力（tools/resources/prompts 等）对应的 scope
- **THEN** 对目标能力的操作被拒绝，其他具备 scope 的能力不受影响

#### Scenario: 认证关闭
- **WHEN** 明确配置关闭认证
- **THEN** MCP 端点放行所有请求，不校验 token 与 scope（仅限本地调试）

#### Scenario: 自定义 verifier
- **WHEN** 用户提供自定义的 token 解析与 scope 映射实现
- **THEN** 其解析结果代替默认逻辑，scope 映射按自定义实现生效

### Requirement: 传输与协议

```

Full source: docs/openspec/changes/ai-magicapi-mcp-plugin/specs/magicapi-mcp-plugin/spec.md

## docs/openspec/changes/ai-magicapi-mcp-plugin/specs/mcp-server-starter/spec.md

- Source: docs/openspec/changes/ai-magicapi-mcp-plugin/specs/mcp-server-starter/spec.md
- Lines: 1-26
- SHA256: e8aaf78d73aceea8c851f5e34274d9368f915ab8475e6e6680d897818c95d524

```md
# Spec Delta

## MODIFIED Requirements

### Requirement: 与 Magic-API 集成

MCP Server SHALL 通过 AI 层 `magicapi-mcp-plugin` capability（基于 mcp starter / aj-mcp 内核）将 Magic-API 能力暴露给 MCP 客户端：函数工具默认注册、API 工具可配置放行、任务能力可注册；工具暴露范围受分组/路径/method/opt-in 规则约束；插件同时提供 mcp-resource / mcp-prompt 资源类型、Sa-Token OAuth2 认证与 scope、SSE 变更通知与集群分发。

#### Scenario: Magic-API 工具自动注册
- **WHEN** 用户启用 Magic-API MCP 插件
- **THEN** Magic-API 中的函数接口自动注册为 MCP 工具，AI 客户端可通过 MCP 调用

#### Scenario: 函数工具自动注册
- **WHEN** 用户启用 Magic-API MCP 插件
- **THEN** Magic-API 函数自动注册为 MCP 工具，AI 客户端可通过 `tools/list` 列出并 `tools/call` 调用

#### Scenario: API 工具按配置放行
- **WHEN** 用户启用 API 工具并配置分组/路径/method/opt-in 规则
- **THEN** 仅命中匹配规则的 Magic-API HTTP API 注册为 MCP 工具，其余不暴露

#### Scenario: 认证与 scope
- **WHEN** 插件启用认证且请求携带合法 token 与目标能力的 scope
- **THEN** MCP 请求可访问对应工具/资源/提示词；缺失 token、scope 不符时被拒绝

#### Scenario: 变更通知与集群
- **WHEN** 工具、资源或提示词变更且客户端已建立 SSE 连接（或集群已启用）
- **THEN** 本节点 SSE 客户端收到对应 list_changed / updated 通知，跨节点经 Redis 分发
```
