---
change: ai-magicapi-mcp-plugin
design-doc: docs/openspec/changes/ai-magicapi-mcp-plugin/design.md
base-ref: 97c085066d40c6a62e30cfbae730f82d482e57de
---

# 实施计划：ai-magicapi-mcp-plugin

<!-- comet-task-authority: docs/openspec/changes/ai-magicapi-mcp-plugin/tasks.md -->

> 任务清单唯一权威：`tasks.md`（revision `62d4efe9...`）。本计划只说明实施顺序、依赖、约束与验收方式。

## 执行方式（已确认）

- build_mode: `autonomous`（串行实施 + 关键里程碑独立审查）
- tdd_mode: `tdd`（RED→GREEN；恢复场景已存在的实现不回退，如实记录 RED 缺失）
- review_mode: `standard`（风险任务段独立审查；Verify 完成唯一一次最终集成审查）

## 总体架构（design.md D1-D10 摘要）

- 插件落在 AI 层 `fw-plus-ai-boot-autoconfigure` 的 `magicapi/plugin/mcp/` 包，经 `MagicApiPluginAutoConfiguration` @Import 聚合。
- 能力注册复用 Core `McpFeatureScanner` 反射通道进各 runtime 的 `FeatureMgr`；拒绝重复的 JSON-RPC。
- 配置前缀 `magic-api.plugin.mcp.*`；认证走 `McpAuthService` SPI；集群复用 `McpRedisBroadcaster`；server-to-client/progress/logging 直接复用 aj-mcp 内核。
- Boot 层清理：删 75 Java + 25 测试 + `fw-plus-magicapi-plugin-mcp` 模块 + 聚合 pom 条目 + console 前端。

## 阶段 0：勘察与基座验证（任务组 1）

### 1.1 aj-mcp 多 runtime 能力 spike

<!-- comet-task-ref:2c96912d-0800-4b9a-9a34-baa81e5ee23b -->

- 在 AI autoconfigure 的 test 源集新建临时 spike 测试类，验证 `McpServer.publishToolsChanged/publishResourcesUpdated`、`listRoots/createMessage/elicit`、`processMessage` 在多 runtime（default + servers[]，webmvc）下可用。
- 验收：spike 编译通过，运行时日志可见通知/请求行为。完成后删除 spike 类。
- 约束：不触碰 `fw-plus-ai` 库层；spike 只存在于 AI autoconfigure test 目录，不进入 main。

### 1.2 McpFeatureScanner 注册通道确认

<!-- comet-task-ref:cef7e8f0-48e3-48c7-b210-2097827bb0e0 -->

- 阅读 Core `fw-plus-ai-mcp` 的 `McpFeatureScanner`（`fw-plus-core/.../fw-plus-ai-mcp/src/main/java/com/xiongx9527/fw/plus/ai/mcp/feature/McpFeatureScanner.java`），记录 addTool/addResource/addPrompt/addCompletion 反射入口的可复用调用方式。
- **已核实（证据，供 3.3 / 4.2 复用）**：`McpFeatureScanner` 已提供完整的公开多 runtime 注册通道：
  - 公开方法 `public void register(List<String> basePackages, Map<String, FeatureMgr> featureMgrs)`：扫描各 basePackage 下标注 aj-mcp `@McpService` 的能力类，逐方法按其注解（`@Tool/@Prompt/@Resource/@ResourceTemplate/@CompletePrompt/@CompleteResourceTemplate`）走 `resolveMethod`+反射调用私有 `FeatureMgr.addTool/addPrompt/addResource/addResourceTemplate/addCompletion`；方法/类可加 `@McpServer` 指定归属 runtime key（缺省 `DEFAULT_KEY="default"`），key 未声明于 servers 时抛 `IllegalStateException`。
  - 对 Magic-API 动态能力（非 `@McpService` 静态类）不直接落在本扫描器内，3.3/4.2 应复用同一组 `resolveMethod` 私有方法解析出的 `Method` 通道（`com.xiongx9527.fw.plus.ai.mcp.feature` 可新增强化扫描器或参数化 FeatureMgr store），变更后调用 `publishToolsChanged/publishResourcesChanged/publishPromptsChanged` 通知。
  - 结论：3.3/4.2 无需新绕开内核；在 Core `McpFeatureScanner` 旁提供 Magic-API store 与 FeatureMgr 的同步桥接。
- 验收：记录签名与调用样例，供 3.3 / 4.2 复用。

### 1.3 Boot 层删除前引用清单

<!-- comet-task-ref:75eb8cec-23bd-4a62-abb0-da4bfb602564 -->

- 全局 grep `magicapi.plugin.mcp`、`MagicAPIMcpConfiguration`、`plugin/mcp`、`fw-plus-magicapi-plugin-mcp`，覆盖 main/test/resources/pom/docs/样例。
- **已核实引用清单（`fc-game 1.3 基线`）**：
  - **代码（删除主体）**：`fw-plus-boot-autoconfigure` 的 `magicapi/plugin/mcp/` 全部（75 Java main + 25 测试）——上一条命令逐文件列出（auth/client/completion/core/logging/model/notification/prompts/protocol/provider/resources/service/session/starter/tools/web/webflux）。
  - **autoconfigure 编排（同包外，须一并改）**：`MagicApiPluginAutoConfiguration.java`（import 第 16 行、注册第 60 行）；`magicapi/webflux/MagicAPIWebFluxControllerRegistrar.java`（第 14-18 行 import mcp 类 + 第 129-136 行 `"mcp".equals(name)` 分支，**位于 webflux/ 包、不在删除主体内，删除后必须同步清理，否则编译失败**）。
  - **boot 测试（主体外）**：`.../webflux/MagicAPIWebFluxControllerTest.java`（引用 mcp bean）。
  - **pom/模块**：`fw-plus-magicapi-plugin-mcp/` 模块目录；`fw-plus-magicapi-plugin/pom.xml` 第 35 行 `<module>`；`fw-plus-boot-dependencies/pom.xml` 第 758 行 `dependencyManagement` 条目。
  - **样例**：`fw-plus-sample/.../fw-plus-magicapi-spring-boot-starter-sample/pom.xml` 第 212 行依赖；`MagicApiAllPluginsClasspathTest.java` 第 33 行；`MagicApiAllPluginsApplicationContextTest.java` 第 187 行 `configurations.put("fw-plus-magicapi-plugin-mcp", "mcp")`。
  - **文档**：`AGENTS.md`、`CLAUDE.md`、`docs/**`（本 change 文档自身，无需删除）。
- 验收：产出引用清单；作为 8.x 删除前的护栏基线。
- 命令参考：`rg -l "magicapi.plugin.mcp|MagicAPIMcpConfiguration|fw-plus-magicapi-plugin-mcp" --type-add 'mvn:pom.xml' --glob '**/pom.xml' --glob '!**/node_modules/**'`。

## 阶段 1：插件配置与装配（任务组 2，依赖阶段 0 的 runtime 认知）

### 2.1 MagicApiMcpProperties

<!-- comet-task-ref:ce9ea509-996b-4d4e-ab82-30b416ee5bdf -->

- 包 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config`，`@ConfigurationProperties("magic-api.plugin.mcp")`。
- 字段对齐 Boot README：enabled/endpoint/protocol(default-version, supported-versions)/headers(accept, protocol-version, session-id, origin)/tools(functions.enabled, functions.include-groups, functions.include-paths, functions.exclude-paths, api.enabled, api.include-groups, api.include-paths, api.exclude-paths, api.methods, api.require-opt-in, api.opt-in-option-name)/auth(enabled, resource, authorization-server, tools-scope, resources-scope, prompts-scope, logging-scope, roots-scope, sampling-scope, elicitation-scope)/cluster.enabled。
- TDD：先写默认值/嵌套绑定断言，RED 后实现。
- 验收：AutoconfigureConfigurationMetadata 或单测断言默认值与嵌套绑定。

### 2.2 MagicAPIMcpConfiguration 与条件装配

<!-- comet-task-ref:ec41c81b-1ffc-432d-a4e7-2838c650156f -->

- 包 `magicapi/plugin/mcp/starter/`，`@ConditionalOnProperty(magic-api.plugin.mcp.enabled)` + `@ConditionalOnClass(magic-api + aj-mcp)`，`@EnableConfigurationProperties(MagicApiMcpProperties)`。
- 参照既有 `MagicAPIModelConfiguration` / `MagicAPIVectorConfiguration` 风格。
- TDD：RED（无配置类时上下文缺 bean）→ GREEN。
- 验收：被 `MagicApiPluginAutoConfiguration` @Import 后 Spring 测试上下文可见 mcp 配置相关 bean。

### 2.3 MagicApiPluginAutoConfiguration 聚合

<!-- comet-task-ref:b06c9274-1b88-4849-9313-9b98d702d805 -->

- 在 AI 层 `magicapi/plugin/MagicApiPluginAutoConfiguration.java` 的 `@Import` 追加 `MagicAPIMcpConfiguration.class`。
- 验收：AI autoconfigure 编译通过、装配测试断言 mcp bean 存在。

## 阶段 2：工具暴露（任务组 3，最核心，依赖 1.2 的通道签名）

### 3.1 MagicApiMcpToolRegistry

<!-- comet-task-ref:c7607ea1-4984-4e7d-9fbe-be9be8a9bdae -->

- 包 `magicapi/plugin/mcp/tools/`，订阅 Magic-API api/function 元数据，实现过滤语义：`exclude-paths` 最高优先、include-groups(Ant 匹配 group path)、include-paths、methods(仅 API)、require-opt-in（options/attributes 中显式 `mcpTool=true`）。产物为已过滤工具快照。
- 参考 Boot 层 `magicapi/plugin/mcp/tools/` 既有工具过滤实现迁移契约。
- TDD：单测覆盖分组/路径/method/opt-in/排除优先各场景。
- 验收：快照正确、被 `McpToolProvider` 引用。

### 3.2 inputSchema 优先与回退、tools/call 响应

<!-- comet-task-ref:5dc38ec7-967d-41e8-ac4a-a60caf585859 -->

- `mcpInputSchema` 优先；缺失/无效回退 `{"type":"object"}`；`tools/call` 返回 MCP content + structuredContent。
- TDD：单测断言 schema 选取与回退、content 结构。
- 验收：匹配 spec「inputSchema 优先与回退」。

### 3.3 FeatureMgr 注册 + publishToolsChanged

<!-- comet-task-ref:6c283604-e387-42c3-8655-291936a1c81b -->

- 经 Core `McpFeatureScanner`/反射通道将工具快照注册进各 runtime `FeatureMgr`；注册后 `McpServer.publishToolsChanged()`。
- TDD：单测断言 FeatureMgr toolStore 含注册工具、通知触发。
- 验收：匹配 spec「工具暴露」+ design D2/D3。

### 3.4 增量 diff 刷新

<!-- comet-task-ref:94ebe6b1-2203-4e80-9b6a-f994d724332e -->

- 监听 Magic-API 能力变更事件（复用既有 MagicNotifyService/存储监听），串行单线程 executor diff 同步 FeatureMgr，同步后 `publishToolsChanged`；无重复注册。
- TDD：模拟变更事件断言 store 增量同步、无重复，通知在 sync 后触发。
- 验收：匹配 design D3 竞态对策（串行化 + 快照 diff）。

## 阶段 3：resources / prompts 资源类型（任务组 4，依赖阶段 0 通道 + model/vector 模式）

### 4.1 mcp-resource / mcp-prompt storage

<!-- comet-task-ref:d64bcdfb-3392-40dd-8000-644e996e3b29 -->

- 包 `magicapi/plugin/mcp/resources/` 与 `magicapi/plugin/mcp/prompts/`，参照 `model/vector` 的 `MagicResourceStorage`/`MagicDynamicRegistry` 模式新增两类代码类资源类型存储。
- 校验字段：URI/MIME/name/description/template（resource）；name/arguments/messages/role/content.type/content.text + `{{arg}}` 占位符（prompt）。
- TDD：单测覆盖合法/非法字段、storage 拒绝路径。
- 验收：控制台可维护该两类资源类型且后端拒绝非法定义。

### 4.2 注册 resource/prompt/template/completion + 端点可访问

<!-- comet-task-ref:5d803961-637d-4bda-a3c9-987d3230b76c -->

- 校验通过的能力经反射通道注册为 aj-mcp resource/prompt/resource-template/completion。
- `resources/list`、`prompts/list`、`resources/read`、`prompts/get`、`completion/complete` 可访问。
- TDD：单测断言 store 注册与 handler 行为。
- 验收：匹配 spec「resources 与 prompts 资源类型」。

### 4.3 自定义 provider 并存聚合

<!-- comet-task-ref:192d27b8-deec-41ad-847e-85173a954a36 -->

- 支持 `McpToolProvider`/`McpResourceProvider`/`McpPromptProvider`/`McpCompletionProvider` Bean 并存聚合。
- 验收：装配测试注入自定义 provider 后 list 聚合出动态 + 自定义能力。

## 阶段 4：认证与 scope（任务组 5，依赖 2.x 装配 + mcp 认证 SPI）

### 5.1 SaTokenOAuth2McpAuthService

<!-- comet-task-ref:bc826bf8-ccaa-4900-87c0-034aed0d8efb -->

- 包 `magicapi/plugin/mcp/auth/`，实现 `McpAuthService`（`fw-plus-ai-boot-autoconfigure` 的 mcp/auth/McpAuthService.java）。
- 校验 Sa-Token OAuth2 token（资源服务器语义），从 claims 解析 scope，按能力（tools/resources/prompts/logging/roots/sampling/elicitation）裁剪，scope 键对齐 `auth.*-scope` 配置。
- TDD：单测覆盖有效/无效 token、scope 缺失拒绝、认证关闭放行。
- 验收：匹配 spec「认证与 scope」。

### 5.2 条件装配三场景

<!-- comet-task-ref:c4a38e71-63b4-4a37-bfc2-f1c6d1ad461f -->

- `@ConditionalOnBean(sa-token-oauth2)` 且 `auth.enabled=true` → 启用；否则 `PermitAllAuthService` 占位；支持自定义 scope 映射策略替换。
- TDD：装配测试断言三种场景（启用/关闭/自定义）下 bean 选择。
- 验收：匹配 design D5。

### 5.3 认证接入既有校验流

<!-- comet-task-ref:21f06a8c-6b2a-4921-977c-67289efd68b6 -->

- 复用 `McpAuthService` SPI，不覆盖 `McpTransportController`。
- TDD：集成式单测——合法 token 通过、非法 token 返回客户端错误且不进入业务分发。
- 验收：匹配 design D5「不引入覆盖 controller」。

## 阶段 5：变更通知与集群（任务组 6，依赖 3.x/4.x 变更点 + 既有集群通道）

### 6.1 各类通知方法触发

<!-- comet-task-ref:9df910e6-f7b8-4cfb-abd1-dbc38c140305 -->

- 资源/提示词/工具变更分别调用 `publishResourcesChanged`/`publishPromptsChanged`/`publishToolsChanged`；`resources/updated` 仅发送给订阅该 URI 的 session。
- TDD：单测断言各通知触发路径与按 URI 订阅语义。
- 验收：匹配 spec「变更通知」。

### 6.2 Redis 集群分发与回环抑制

<!-- comet-task-ref:6d44095b-e212-4416-87e2-03f3e7b642f1 -->

- 复用 `McpRedisBroadcaster`/`McpRedisMessageConsumer` 通道接收 MagicNotifyService 变更事件，event 携带 origin id 抑制回环；未启用 cluster 时保持单节点本地行为。
- TDD：单测断言跨节点投递与回环忽略、单节点降级。
- 验收：匹配 README「Redis-only 集群」语义与 design D3。

## 阶段 6：server-to-client 与诊断（任务组 7，依赖 runtime 能力）

### 7.1 roots/sampling/elicitation pending 生命周期

<!-- comet-task-ref:14f3efec-d7bb-4d40-b793-c6e102b99b10 -->

- 复用 aj-mcp 内核 `listRoots`/`createMessage`/`elicit`；发起前基于 `getClientCapabilities` 检查 capability；响应按 id 关联 resolved/failed；超时清理标记 timed out。
- TDD：单测断言未声明 capability 不发起、响应按 id 关联。
- 验收：匹配 README「Server-to-client 请求」。

### 7.2 MagicMcpDiagnosticController

<!-- comet-task-ref:a109da76-26af-45ad-aa06-453783b9063e -->

- `POST /mcp/diagnostics/execute`：调用 `resources/read`/`prompts/get`，复用统一认证/会话/过滤，不绕过约束。
- TDD：测试断言合法（走能力执行）/未授权（被拒）两条路径。
- 验收：匹配 README「控制台诊断」与 design D8。

## 阶段 7：AI 层前端插件模块（任务组 8）

### 8.1 新增 fw-plus-ai-magicapi-plugin-mcp 模块（仅前端 console）

<!-- comet-task-ref:b25d037b-09a2-4a28-94ba-8b45a41ac730 -->

- 在 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/` 下新增 `fw-plus-ai-magicapi-plugin-mcp`，参照 Boot 层同名模块：
  - `pom.xml`：父 POM 为 `fw-plus-ai-magicapi-plugin`，含 exec-maven-plugin（src/console npm install + npm run build）与 maven-resources-plugin（拷贝 dist 到 `magic-editor/plugins`）。
  - `src/console/`：package.json（magic-mcp、vite 构建 IIFE）、vite.config.js（`magic-mcp.${version}.iife.js`，external vue）、src/index.js（注册 mcp-resource/mcp-prompt 资源与 toolbars）、components/、service/、icons/、i18n/、dist/。
- 将新模块加入 `fw-plus-ai-magicapi-plugin/pom.xml` `<modules>`。
- 后端 Java 仍在 `fw-plus-ai-boot-autoconfigure` `magicapi/plugin/mcp/` 包内实现（D1 保持），前端模块不依赖后端结构。
- 验收：聚合含新模块、`./mvnw -pl fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-mcp -am install` 产出 `magic-mcp.*.iife.js` 到 classpath。

### 8.2 前后端解耦确认

<!-- comet-task-ref:568257b2-fafa-4616-b09a-3c78f0e503de -->

- 前端模块与后端实现解耦：前端模块仅含 console 构建资源，后端 Java 在 autoconfigure 包内。
- 验收：AI 层编译链无环。

## 阶段 8：Boot 层清理（任务组 9，依赖 1.3 引用清单 + 新实现就位）

### 9.1 删除 Boot 层实现与测试

<!-- comet-task-ref:80c54f04-a921-4db0-a5e7-c6dcd219ecd6 -->

- 按 1.3 引用清单确认后删除 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/mcp/`（75 Java）与 `src/test/.../magicapi/plugin/mcp/`（25 测试）。
- **同步清理（删除后必改，否则编译失败）**：`magicapi/webflux/MagicAPIWebFluxControllerRegistrar.java` 的 mcp import（14-18 行）与 `"mcp".equals(name)` 分支（129-136 行）；`src/test/.../magicapi/webflux/MagicAPIWebFluxControllerTest.java` 的 mcp 引用。
- 验收：目录移除且无编译引用。

### 9.2 更新 Boot MagicApiPluginAutoConfiguration

<!-- comet-task-ref:1b901b46-be55-4464-8289-e564e740054f -->

- 移除 mcp import（第 16 行）与注册（第 60 行）。
- 验收：fw-plus-boot 编译通过。

### 9.3 移除 Boot 模块与聚合条目（console 已由 8.1 承接）

<!-- comet-task-ref:a34aeb4d-e707-49a9-a633-4157578c7051 -->

- 移除 Boot 层 `fw-plus-magicapi-plugin-mcp` 模块目录 + `fw-plus-magicapi-plugin/pom.xml` 聚合条目（console 前端已由 AI 层新模块 8.1 承接，此处删除 Boot 残留）。
- 验收：pom 聚合列表无该模块、模块目录删除。

### 9.4 README 说明并入 AI 层文档

<!-- comet-task-ref:9169b7b1-e782-43e2-8620-22748896e225 -->

- 将 README 可迁移的说明性内容并入 AI 层 mcp 插件文档（覆盖工具过滤/资源类型/认证/通知/集群/诊断各节）。
- 验收：AI 插件侧文档覆盖各节。

## 阶段 9：回归与联调（任务组 10，依赖全部前序）

### 10.1 fw-plus-boot 回归

<!-- comet-task-ref:ead0cbff-b527-4329-9c47-b5f339ee2500 -->

- `./fw-plus-core/mvnw -f fw-plus-boot/pom.xml clean install -DskipTests` + autoconfigure 既有测试通过（删除无残留）。
- 验收：编译与测试全绿。

### 10.2 fw-plus-ai 编译 + 新增测试

<!-- comet-task-ref:b6db74ed-589b-4e2f-aaa1-6b84167813a6 -->

- `./fw-plus-core/mvnw -f fw-plus-ai/pom.xml test`（或对应 scope），新增 mcp 插件测试全绿。
- 验收：AI 层编译 + 测试全绿。

### 10.3 样例聚合 validate

<!-- comet-task-ref:a85e5598-d4ad-480f-b9c9-5e33e511be06 -->

- `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`，无 `MagicApiPluginAutoConfiguration`/插件类解析错误。
- 验收：validate 通过。

## 审查节奏（review_mode: standard）

- 里程碑审查点：任务组 3 完成后（工具暴露核心）、任务组 4 完成后（资源类型）、任务组 5 完成后（认证）、任务组 8 完成后（前端模块）、任务组 9 完成后（删除影响面）。由独立审查者审查 diff。
- Verify 阶段执行唯一一次最终集成审查，Build 不在本计划之外追加最终审查。

## 关键护栏

- Java 1.8：不引入 `List.of()`/`Map.copyOf()` 等 Java 9+ API。
- 不修改 aj-mcp 内核、Core `fw-plus-ai-mcp`、mcp starter 主干行为；`org.ssssssss` 依赖版本不动。
- 删除 Boot 层前用 1.3 引用清单复核全局引用（含样例/docs/pom）。
- node_modules/构建产物不归档。
- 源码 UTF-8；缩进用 Tab。