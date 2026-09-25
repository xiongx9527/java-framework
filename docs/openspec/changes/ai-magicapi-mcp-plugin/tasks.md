# Tasks

## 1. 勘察与基座验证

- [x] 1.1 用最小 spike 新建临时测试类验证 aj-mcp `McpServer.publishToolsChanged/publishResourcesUpdated`、`listRoots/createMessage/elicit`、`processMessage` 在多 runtime 下可用，验证方式：spike 代码编译通过且运行时日志可见通知/请求行为（完成后删除 spike） <!-- comet-task:2c96912d-0800-4b9a-9a34-baa81e5ee23b -->
- [x] 1.2 确认 Core `fw-plus-ai-mcp` `McpFeatureScanner` 暴露的注册通道签名与调用方式，验证方式：阅读源码并记录可复用的 addTool/addResource/addPrompt/addCompletion 反射入口 <!-- comet-task:cef7e8f0-48e3-48c7-b210-2097827bb0e0 -->
- [x] 1.3 确认搏层删除前全部引用点（grep 全局 `magicapi.plugin.mcp`、`MagicAPIMcpConfiguration`、`plugin/mcp`、`fw-plus-magicapi-plugin-mcp`），验证方式：产出引用清单，覆盖 main/test/resources/pom/docs <!-- comet-task:75eb8cec-23bd-4a62-abb0-da4bfb602564 -->

## 2. 插件配置与装配

- [x] 2.1 在 `fw-plus-ai-boot-autoconfigure` 新增 `magicapi/plugin/mcp/config/MagicApiMcpProperties`（`@ConfigurationProperties("magic-api.plugin.mcp")`），字段对齐 Boot 层 README 的 enabled/endpoint/protocol/auth/tools/headers/cluster 等键，验证方式：单元测试断言默认值与嵌套绑定 <!-- comet-task:ce9ea509-996b-4d4e-ab82-30b416ee5bdf -->
- [x] 2.2 新增 `magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration` 与条件装配（`@ConditionalOnProperty magic-api.plugin.mcp.enabled`、`@ConditionalOnClass` magic-api/aj-mcp），验证方式：配置类被 `MagicApiPluginAutoConfiguration` @Import 后 Spring 测试上下文可见 <!-- comet-task:ec41c81b-1ffc-432d-a4e7-2838c650156f -->
- [x] 2.3 在 `MagicApiPluginAutoConfiguration`（AI 层）@Import 追加 mcp 配置，验证方式：AI autoconfigure 编译通过、装配测试断言 mcp bean 存在 <!-- comet-task:b06c9274-1b88-4849-9313-9b98d702d805 -->

## 3. 工具暴露

- [x] 3.1 新增 `magicapi/plugin/mcp/tools/MagicApiMcpToolRegistry`：订阅 Magic-API api/function 元数据，应用 include-groups/paths、exclude-paths、methods、opt-in 过滤，产物为已过滤工具快照，验证方式：单元测试覆盖分组/路径/method/opt-in/排除优先各场景 <!-- comet-task:c7607ea1-4984-4e7d-9fbe-be9be8a9bdae -->
- [x] 3.2 实现 `mcpInputSchema` 优先、缺失回退 `{type:object}`，`tools/call` 返回 content + structuredContent，验证方式：单测断言 schema 选取与回退 <!-- comet-task:5dc38ec7-967d-41e8-ac4a-a60caf585859 -->
- [x] 3.3 经 Core `McpFeatureScanner`/反射通道将工具快照注册进 FeatureMgr，注册后调用 `publishToolsChanged`，验证方式：单测断言 FeatureMgr toolStore 含注册工具、通知触发 <!-- comet-task:6c283604-e387-42c3-8655-291936a1c81b -->
- [x] 3.4 增量 diff 刷新：监听 Magic-API 能力变更事件，串行单线程 diff 同步 FeatureMgr 并 `publishToolsChanged`，验证方式：模拟变更事件断言 store 增量同步且无重复注册 <!-- comet-task:94ebe6b1-2203-4e80-9b6a-f994d724332e -->

## 4. resources / prompts 资源类型

- [x] 4.1 新增 `mcp-resource`/`mcp-prompt` 两类 Magic-API 代码类资源类型的 storage 实现（参考 model/vector 的 `MagicResourceStorage`/`MagicDynamicRegistry` 模式），校验 URI/MIME/name/arguments/messages 字段，验证方式：单测覆盖合法/非法字段、storage 拒绝路径 <!-- comet-task:d64bcdfb-3392-40dd-8000-644e996e3b29 -->
- [x] 3.5 插件侧 MCP server 动态实例：新增 `mcp-server` 资源类型（`MagicMcpServerInfoMagicResourceStorage` + `MagicMcpServerMagicDynamicRegistry`）与 `MagicDynamicMcpServer`（按资源定义创建/销毁独立 server：独立 transport registry、端点路由与工具过滤），`McpRoutingRegistrar` 提供运行期 `registerRuntime/unregisterRuntime`，`McpRuntimeFactory` 提供单实例构建与 server 工厂重载，`MagicMcpServerController` 提供 list/test/doDelete/changeConnected/refresh/defined 接口；启动期实例来自 `magic-api.plugin.mcp.servers[]`，验证方式：`MagicMcpServerStorageTest` 9 例、`McpRuntimeFactoryTest` 16 例全绿，模块全量 294 例仅剩基线 langchain4j 失败 <!-- comet-task:4d1c8a23-9f6e-4f77-b0b6-2ab4e1d63c51 -->
- [ ] 4.2 校验通过的能力经反射通道注册为 aj-mcp resource/prompt/resource-template/completion，注册后 `resources/list`、`prompts/list`、`resources/read`、`prompts/get`、`completion/complete` 可访问，验证方式：单测断言 store 注册与 handler 行为 <!-- comet-task:5d803961-637d-4bda-a3c9-987d3230b76c -->
- [ ] 4.3 支持自定义 `McpToolProvider`/`McpResourceProvider`/`McpPromptProvider`/`McpCompletionProvider` Bean 并存聚合，验证方式：装配测试注入自定义 provider 后 list 聚合出动态 + 自定义能力 <!-- comet-task:192d27b8-deec-41ad-847e-85173a954a36 -->

## 5. 认证与 scope

- [x] 5.1 新增 `SaTokenOAuth2McpAuthService` 实现 `McpAuthService`：校验 Sa-Token OAuth2 token，从 claims 解析 scope 列表，按能力（tools/resources/prompts/…）裁剪，验证方式：单测覆盖有效/无效 token、scope 缺失拒绝、认证关闭放行 <!-- comet-task:bc826bf8-ccaa-4900-87c0-034aed0d8efb -->
- [ ] 5.2 条件装配：`@ConditionalOnBean` sa-token-oauth2 且 `auth.enabled=true`，缺省 `PermitAllAuthService` 占位保留；支持替换 scope 映射策略（自定义 verifier 语义），验证方式：装配测试断言三种场景（启用/关闭/自定义）下的 bean 选择 <!-- comet-task:c4a38e71-63b4-4a37-bfc2-f1c6d1ad461f -->
- [ ] 5.3 将认证接入既有最新校验流（复用 `McpAuthService` SPI，不覆盖 controller），验证方式：集成式单测——带合法 token 请求通过、非法 token 返回客户端错误且不进入业务分发 <!-- comet-task:21f06a8c-6b2a-4921-977c-67289efd68b6 -->

## 6. 变更通知与集群

- [x] 6.1 资源/提示词/工具注册变更后分别调用 aj-mcp 通知方法，验证方式：单测断言 `publishResourcesChanged`/`publishPromptsChanged`/`resources/updated`（按 URI 订阅）触发路径 <!-- comet-task:9df910e6-f7b8-4cfb-abd1-dbc38c140305 -->
- [ ] 6.2 接入既有 Redis 集群分发（复用 `McpRedisBroadcaster`/`McpRedisMessageConsumer` 通道）与 `MagicNotifyService` 变更事件，携带 origin id 抑制回环，验证方式：单测断言跨节点投递与回环忽略、单节点降级行为 <!-- comet-task:6d44095b-e212-4416-87e2-03f3e7b642f1 -->

## 7. server-to-client 与诊断

- [ ] 7.1 复用 aj-mcp 内核 `listRoots/createMessage/elicit`，仅做初始化请求前 capability 检查与 pending request 状态记录（基于 `getClientCapabilities`/`getRunningRequests`），验证方式：单测断言未声明 capability 不发起请求、响应按 id 关联 resolved/failed <!-- comet-task:14f3efec-d7bb-4d40-b793-c6e102b99b10 -->
- [ ] 7.2 新增 `MagicMcpDiagnosticController`：`diagnostics/execute` 调用 `resources/read`/`prompts/get`，复用统一认证/会话/过滤，不绕过约束，验证方式：测试断言合法（走能力执行）/未授权（被拒）两条路径 <!-- comet-task:a109da76-26af-45ad-aa06-453783b9063e -->

## 8. AI 层新增前端插件模块

- [ ] 8.1 新增 `fw-plus-ai-magicapi-plugin-mcp` 模块（仅承载前端 console，参照 Boot 层同名模块）：pom.xml（exec-maven-plugin npm 构建 + maven-resources-plugin 拷贝 dist 到 `magic-editor/plugins`）+ `src/console`（package.json、vite.config.js、src/index.js、components、service、icons、i18n、dist），验证方式：`fw-plus-ai-magicapi-plugin/pom.xml` 聚合含新模块且 `./mvnw -pl fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-mcp -am install` 产出 `magic-mcp.*.iife.js` 到 classpath <!-- comet-task:b25d037b-09a2-4a28-94ba-8b45a41ac730 -->
- [ ] 8.2 后端 Java 实现在 `fw-plus-ai-boot-autoconfigure` `magicapi/plugin/mcp/` 包内完成（保持 D1），前端模块与后端解耦、依赖关系无环，验证方式：AI 层编译链无环、前端模块仅含 console 构建资源 <!-- comet-task:568257b2-fafa-4616-b09a-3c78f0e503de -->

## 9. Boot 层清理

- [ ] 9.1 删除 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/mcp/`（75 Java）与 `src/test/.../mcp/`（25 测试），验证方式：目录移除且无编译引用 <!-- comet-task:80c54f04-a921-4db0-a5e7-c6dcd219ecd6 -->
- [ ] 9.2 更新 Boot `MagicApiPluginAutoConfiguration`：移除 mcp import（第 16 行）与注册（第 60 行），验证方式：编译通过 <!-- comet-task:1b901b46-be55-4464-8289-e564e740054f -->
- [ ] 9.3 删除 Boot 层 `fw-plus-magicapi-plugin-mcp` 模块及 `fw-plus-magicapi-plugin/pom.xml` 聚合条目（console 前端已由 AI 层新模块 8.1 承接），验证方式：pom 聚合列表无该模块、模块目录删除 <!-- comet-task:a34aeb4d-e707-49a9-a633-4157578c7051 -->
- [ ] 9.4 将 README 可迁移的说明性内容并入 AI 层 mcp 插件文档，验证方式：AI 插件侧文档覆盖工具过滤/资源类型/认证/通知/集群/诊断各节 <!-- comet-task:9169b7b1-e782-43e2-8620-22748896e225 -->

## 10. 回归与联调

- [ ] 10.1 `fw-plus-boot` 全量编译 + 既有测试通过（验证删除无残留），验证方式：`./fw-plus-core/mvnw -f fw-plus-boot/pom.xml clean install -DskipTests` 及 autoconfigure 测试 <!-- comet-task:ead0cbff-b527-4329-9c47-b5f339ee2500 -->
- [ ] 10.2 `fw-plus-ai` 编译 + 新增 mcp 插件测试全绿，验证方式：`./fw-plus-core/mvnw -f fw-plus-ai/pom.xml test`（或对应 scope） <!-- comet-task:b6db74ed-589b-4e2f-aaa1-6b84167813a6 -->
- [ ] 10.3 样例聚合入口 validate 通过、无 `MagicApiPluginAutoConfiguration`/插件类解析错误，验证方式：`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate` <!-- comet-task:a85e5598-d4ad-480f-b9c9-5e33e511be06 -->