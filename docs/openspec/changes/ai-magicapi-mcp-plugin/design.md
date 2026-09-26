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

### D1: 插件形态——后端 Java 在 AI autoconfigure 内实现，新增 AI 层前端模块
 Boot 层 `fw-plus-magicapi-plugin-mcp` 是「console 前端 + pom 依赖」的 thin 模块，实现全在 autoconfigure（后端 Java 亦在 Boot autoconfigure 包内）；AI 层 `fw-plus-ai-mcp-spring-boot-starter` 已是同样的「依赖 POM + autoconfigure 实现」模型。因此 mcp 插件的后端 Java 直接沉淀在 `fw-plus-ai-boot-autoconfigure` 的 `magicapi/plugin/mcp/` 包内，由 `MagicApiPluginAutoConfiguration` 的 `@Import` 聚合。前端 console 参照 Boot 层同名模块形态，新增 `fw-plus-ai-magicapi-plugin-mcp`（仅承载 `src/console` 构建产物）并由 `fw-plus-ai-magicapi-plugin/pom.xml` 聚合；前后端解耦、依赖无环。
- 备选 A：新建 `fw-plus-ai-magicapi-mcp-spring-boot-starter`。拒绝——与 mcp starter 现有模型重复，且无需独立发版。
- 备选 B：复用 Boot 层模块改造成依赖中转。拒绝——Boot 层模块整体移除是用户已确认的迁移方向。
- 备选 C：前端 console 直接放在 autoconfigure resources。拒绝——与 Boot 层「插件模块承载 console」的既有分发形态不一致，也占用 autoconfigure 产物。

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

### D9
- 备选 A：全部改成 `fw-plus.ai.mcp.magicapi.*`。拒绝——破坏既有 README 用户配置，迁移成本高，且与「对齐 README 全功能」的目标相悖。设计中默认保留 `magic-api.plugin.mcp.*`，并在文档中标明两轨映射关系。
- （本决策若在实际装配中遇到前缀冲突，按 Open Questions 处理。）

### D11: 配置面与认证接线——只声明插件真正消费的键
`magic-api.plugin.mcp.*` 只保留 enabled/auth/tools/servers。传输与会话参数（endpoint、sessionTimeout、SSE 开关、请求/响应上限、信任源、协议版本、自定义头部）是 Boot 层插件自己 transport 实现才有的语义；AI 层的传输由 mcp starter（aj-mcp 内核 + `fw-plus.ai.mcp.*`）承担，继续在插件侧声明只会变成"配了不生效"的死配置，因此删除而非保留空壳。
认证按 starter 既有 SPI 接线：`MagicApiMcpAuthService` 读取 `Authorization: Bearer <subject>.<scope,...>`，产出带 scope 的 `McpPrincipal`；未开启认证时让位给 `PermitAllAuthService`。为了让"关闭认证"与"开启但 token 无效"可区分，放行主体改用 `McpPrincipal.PERMIT_ALL`（不再是 ANONYMOUS），两个 webmvc controller 在认证后拒绝匿名主体（401）。Sa-Token OAuth2/JWKS 校验随后续任务替换本实现，controller 签名不变。
- 备选 A：保留字段做"配置对齐"。拒绝——无消费者的配置项等于对用户撒谎，删除后由 `fw-plus.ai.mcp.*` 承担更诚实。
- 修正（auth 归属）：认证同样属传输入口层，最初在插件里放的 `MagicApiMcpAuthService` 与 `magic-api.plugin.mcp.auth.*` 会与 starter 的 `McpAuthService` SPI 形成两套语义（动态实例用插件版、starter runtime 用占位版，行为不一致），已删除。入口认证一律由 starter `McpAuthService` 承担：用户提供 bean 即生效，未提供时 `PermitAllAuthService` 放行；scope 裁剪若要支持也应在 starter 层基于 `McpPrincipal.getScopes()` 实现，插件侧只做能力聚合。

### D10: 变更通知按能力分类——单一出口 MagicMcpNotifier
三类能力各自的列表变化对应不同的 MCP 通知：api/function → `notifications/tools/list_changed`；mcp-resource 新增/删除 → `notifications/resources/list_changed`、同名 uri 内容更新 → `notifications/resources/updated`（内核只投递给订阅该 uri 的客户端）；mcp-prompt → `notifications/prompts/list_changed`。通知统一收口到 `MagicMcpNotifier`（遍历 starter runtime 与 `mcp-server` 动态实例，跳过 transport 未绑定的实例避免内核 NPE），各注册器只负责 store diff（`MagicApiMcpToolRegistrar.register` 回传 AtomicBoolean 变更标记、`MagicApiMcpCapabilityRegistrar` 做 resources/prompts diff），由事件监听层决定是否广播。capabilities 快照经 `MagicMcpCapabilitySource`（持 ObjectProvider 惰性取）读取，避免「注册表 ↔ 能力注册器」构造期环。
- 备选 A：各注册器自行 publish。拒绝——多实例下同一事件会重复广播，且 transport 未就绪时的 NPE 防护会散落多处。

### D9: 插件自有 MCP server 实例——`mcp-server` 资源类型驱动动态增删
除 starter 的 `fw-plus.ai.mcp.servers[]` 多实例外，插件自身通过新增的 `mcp-server` 资源类型（参照 Boot 层 kafka/vector 的 `MagicResourceStorage` + `AbstractMagicDynamicRegistry` + 动态数据源三段式）提供独立的 server 实例：控制台新增/修改/删除资源即触发 `MagicMcpServerMagicDynamicRegistry` 事件回调，由 `MagicDynamicMcpServer` 创建/销毁实例。每实例拥有自己的 transport registry、端点路由与 `tools` 过滤配置，互不影响；server 主体替换为插件的 `MagicApiMcpServerRegistry`（保证 tools/call 响应由 Magic-API 脚本改写），构建复用 starter `McpRuntimeFactory`（新增 server 工厂重载），端点路由复用 `McpRoutingRegistrar`（新增运行期 `registerRuntime/unregisterRuntime`）。启动期实例来源为 `magic-api.plugin.mcp.servers[]`（动态注册器的 `defaultMappings()`）。
- 备选 A：由 starter 的 runtime 列表统一承载（插件只注入工具）。拒绝——插件的实例参数（工具过滤、端点语义）与 starter 的 MCP 服务配置不同生命周期，混在一个列表里会让工具过滤与认证边界互相污染。
- 备选 B：插件自己维护 endpoint 路由表，绕过 `McpRoutingRegistrar`。拒绝——重复实现传输与 SSE session 管理，且与 starter 路由注册的 endpoint 唯一性保障冲突。

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
- 移除 Boot 层 `fw-plus-magicapi-plugin-mcp` 模块 + 聚合 pom 条目。
- console 前端（`src/console/`）迁往 AI 层新模块 `fw-plus-ai-magicapi-plugin-mcp`（D1），Boot 层遗留目录随模块删除；README 中可迁移的说明性内容并入 AI 层插件文档。node_modules/构建产物不归档。

## Risks / Trade-offs

- **[内核能力缺口 → 无法 100% 对齐 README]** aj-mcp 支持范围已按 javap 勘察（通知/client-request/progress/logging/协商均在）；若个别 README 细节（如 `MCP-Protocol-Version` 写回时序、header 自定义）内核不直接开放，以插件侧 header/协商适配层补齐，不改内核。→ 实现前以最小 spike 验证过程消息与通知。
- **[FeatureMgr 反射注册的脆弱性]** 依赖私有方法（addTool/addResource/addPrompt）签名，aj-mcp 升版可能破坏。→ 复用 Core 层已封装的 `McpFeatureScanner` 通道，签名变动集中在单点；版本受 Core BOM 约束。
- **[配置双轨前缀的缝合力]** `magic-api.plugin.mcp.*` 与 `fw-plus.ai.mcp.*` 在装配层需一一对应，易产生漏配。→ 插件配置类集中定义映射，仅一处置映射逻辑；测试覆盖「Boot 风格配置 → 插件生效」。
- **[动态 diff 刷新竞态]** Magic-API 变更与 FeatureMgr 同步若并发，可能出现工具列表瞬时不一致。→ 变更处理串行化在单线程 executor，diff 基于快照；通知在 sync 完成后发出。
- **[删除 Boot 层的影响面]** 其他模块（含样例、boot starter 聚合 pom、docs）可能引用已删类。→ 删除前全局 grep 清理引用，编译（fw-plus-boot / fw-plus-ai）验证零残留。

## Migration Plan

1. 先在 `fw-plus-ai-boot-autoconfigure` 实现 `magicapi/plugin/mcp/`（能力注册 + resources/prompts storage + 认证 SPI + 通知 + 集群 + diagnostics + 配置），与 Boot 层并存期间以测试验证行为对齐。
2. 更新 `mcp-server-starter`/`magicapi-mcp-plugin` spec 后的实施任务依次落地，AI 层编译 + 新增测试通过。
3. 全局 grep 清理 Boot 层引用，删除 75 Java + 25 测试 + `MagicApiPluginAutoConfiguration` 中的注册，移除模块 + 聚合 pom 条目。
4. 回归 `fw-plus-boot` 全量编译与既有测试；rollback：保留 git 历史，Boot 层删除独立 commit，可 revert。

## Open Questions

- aj-mcp `ServerStreamableHttp`/`ServerSse` 对多个 runtime 的端点确权与 `/mcp` 单端点等 README 形态的兼容细节（`/mcp` 已含伪诊断子路径，是否需要独立 controller 前缀）——不改 spec/approach 的装配细节，实现阶段以最小 spike 定夺。
- `mcpInputSchema` 的 JSON schema 归一化（Magic-API 元数据格式 → iTool inputSchema）具体字段映射，实现阶段定。