# fw-plus-ai-magicapi-plugin-mcp

Magic-API 的 MCP 插件，把 Magic-API 的接口/函数与三类动态资源（工具、资源、提示词）暴露为 MCP 能力，并支持在控制台维护多个独立的 MCP server 实例。

本模块目录只负责 starter 依赖与控制台前端资源打包；后端实现位于 `fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp` 包。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-mcp</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

依赖关系：

- `fw-plus-ai-boot-autoconfigure`：插件后端配置、控制器、资源存储与动态注册。
- `sa-token-oauth2`（optional）：classpath 存在时 `McpAuthService` 自动选用 Sa-Token OAuth2 校验，否则回落放行实现；也可直接提供自定义 `McpAuthService` bean 替换。

## 启用条件

配置类 `MagicAPIMcpConfiguration`，满足以下条件时生效：

- classpath 存在 aj-mcp 的 `McpServer`、`McpServerRuntime`。
- 容器中已有 Magic-API 的 `MagicResourceService`（即应用启用了 Magic-API）。
- `magic-api.plugin.mcp.enabled` 不为 `false`（缺省启用）。

## 能力总览

| 能力 | 来源 | 说明 |
| --- | --- | --- |
| 工具 tools/list、tools/call | Magic-API `function`/`api` 脚本 | 按配置过滤后注册；调用时由 `MagicApiMcpServerRegistry` 改写响应为脚本执行结果 |
| 资源 resources/list、resources/read | `mcp-resource` 资源类型 | 返回定义里的 `content` |
| 资源模板 resource template | `mcp-resource` 的 `template` 字段 | RFC 6570 模板，`resources/read` 可按变量命中 |
| 提示词 prompts/list、prompts/get | `mcp-prompt` 资源类型 | 消息里的 `${arg}` 按调用参数插值 |
| 补全 completion/complete | `mcp-prompt` / `mcp-resource` 的 `completions` | 按当前输入前缀过滤候选 |
| 自定义能力 | `MagicApiMcpCapabilityProvider` bean | 与动态资源并存聚合，同名时自定义优先 |

## 工具过滤

```yaml
magic-api:
  plugin:
    mcp:
      tools:
        functions:
          enabled: true
          include-groups: [ "/system/**" ]   # 分组名 AntPath
          include-paths: [ "/public/**" ]    # 路径 AntPath
          exclude-paths: [ "/public/secret/**" ]
        api:
          enabled: false                     # 接口默认不暴露
          methods: [ GET, POST ]
          require-opt-in: true                # 需接口 option 里显式声明
          opt-in-option-name: mcpTool
```

规则：分组/路径同时配置时取交集；`exclude-paths` 优先于 include；`mcpInputSchema` option 优先，缺失时回落 `{ "type": "object" }`。

## 多 server 实例

每个 MCP server 实例有独立的 endpoint、传输协议与工具过滤，互不影响。

1. **starter 命名实例**（传输层多实例）：`fw-plus.ai.mcp.servers[]`，见 mcp starter 文档。
2. **插件自有实例**（控制台维护）：新增 `mcp-server` 资源定义，或启动配置：

```yaml
magic-api:
  plugin:
    mcp:
      servers:
        - key: report
          name: 报表服务
          endpoint: /mcp/report
          transport: http-sse        # 或 streamable-http
          tools:                     # 该实例自己的过滤配置，缺省继承顶层
            api:
              enabled: true
```

`mcp-server` 资源字段：`key`（必填、字母数字下划线点中划线、唯一）、`name`、`transport`、`endpoint`（必填、以 `/` 开头）、`enabled`、`tools`。新增/修改/删除资源即热创建/更新/销毁实例，动态路由随 web 栈（Servlet/WebFlux）自动注册与注销。

## 认证

入口认证统一由 mcp starter 的 `McpAuthService` SPI 承担，插件不再自带实现：

- classpath 有 `sa-token-oauth2` → `SaTokenOAuth2McpAuthService`（`SaOAuth2Util.checkAccessToken`，scope 取 token scopes）。
- 否则 → `PermitAllAuthService` 放行。
- 提供自定义 `McpAuthService` bean → 用户实现优先。

Servlet 与 WebFlux 入口在认证失败（匿名主体）时直接返回 401，不进入内核分发。

## 变更通知

能力变更后向客户端广播对应通知：

| 变更 | 通知 |
| --- | --- |
| api/function 增删改 | `notifications/tools/list_changed` |
| mcp-resource 增删 | `notifications/resources/list_changed` |
| mcp-resource 同名内容更新 | `notifications/resources/updated`（仅订阅该 uri 的客户端） |
| mcp-prompt 增删改 | `notifications/prompts/list_changed` |

集群部署时，能力变更会经 MCP 广播频道（`fw-plus:mcp:broadcast`）携带来源节点 id 转发到其它节点落地，节点收到自己的消息会忽略；无 Redis 时仅本节点生效。

## 诊断接口

控制台可访问：

- `/mcp/server/list`、`/mcp/server/defined`：全部实例与定义。
- `/mcp/server/test`、`/mcp/server/doDelete`：保存/删除前校验。
- `/mcp/server/changeConnected`、`/mcp/server/refresh`：启停实例、刷新工具快照。
- `/mcp/diagnostics/capabilities`：各实例能力清单。
- `/mcp/diagnostics/execute`、`/mcp/diagnostics/executePrompt`：试运行 `resources/read` / `prompts/get`。

## 客户端请求（roots / sampling / elicitation）

`MagicMcpClientGateway` 在调用内核 `listRoots`/`createMessage`/`elicit` 前检查客户端是否声明了对应 capability；未声明则不发请求（roots 返回空列表，sampling/elicitation 抛 `IllegalStateException`）。pending 状态通过 `runningRequests()`、`pendingClientResponses()` 只读查询。

## 环境变量与端口

插件自身不监听端口；端点由 `mcp-server` 实例的 `endpoint` 决定（`http-sse` 为 `{endpoint}/sse` + `{endpoint}/messages/{uuid}`，`streamable-http` 为 `{endpoint}`）。

## 从 Boot 层旧实现迁移

本插件取代了 `com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mcp`（Boot 层同名模块，含其自研 transport / SSE / 诊断 / Sa-Token 认证，已删除）。迁移要点：

| 旧（Boot 层） | 新（AI 层） |
| --- | --- |
| `fw-plus-magicapi-plugin-mcp` | `fw-plus-ai-magicapi-plugin-mcp`（console 前端） |
| `…boot.autoconfigure.magicapi.plugin.mcp.*` | `…ai.autoconfigure.magicapi.plugin.mcp.*` + `…ai.autoconfigure.mcp.*` |
| `McpAccessTokenVerifier` / `SaTokenMcpAuthService` | starter 的 `McpAuthService` SPI；classpath 有 `sa-token-oauth2` 时自动用 `SaTokenOAuth2McpAuthService`，或提供自定义 bean |
| `McpToolsHandler` / `McpResourcesHandler` 等 | 经内核 `FeatureMgr` 的 store 注册 + `MagicApiMcpServerRegistry` 改写响应 |

配置键变化（旧键已无消费者）：

- `magic-api.plugin.mcp.transport` / `session-timeout` / `sse-stream` / `max-request-bytes` / `max-response-chars` / `trusted-origins` / `protocol.*` / `headers.*` → 改用 `fw-plus.ai.mcp.*`（mcp starter）
- `magic-api.plugin.mcp.auth.*`（原 Boot 层插件自带）→ 不再需要；scope 取 token scopes，能力 scope 名沿用 `mcp:tools`/`mcp:resources`/`mcp:prompts` 等约定
- 保留：`enabled`、`tools.*`、`servers[]`

## 已知限制

1. **快照缓存**：`MagicApiMcpToolRegistry` 的工具快照只在收到 Magic-API 资源变更事件后失效。自定义 `MagicApiMcpCapabilityProvider` 若内部状态运行期变化，需自行触发重建（例如注入该 registry 调 `invalidateSnapshot()`）。
2. **completion 的 resource 侧**：内核按模板名反查拼 key，控制台维护 `template` 时建议让 `name` 与 `template` 保持易对应的关系；两种 ref 形态（模板名 / uriTemplate）均已注册。
3. **WebFlux 动态路由**：验证到装配级（`ApplicationContextRunner` + 路由探测），未覆盖真实 Netty 端到端。
4. **认证缺省语义**：未引入 `sa-token-oauth2` 且未提供自定义 `McpAuthService` 时为放行占位，生产环境必须二选一。
5. **console dist 不入库**：`src/console/dist/` 被 `.gitignore` 排除，jar 中的 `magic-mcp.*.iife.js` 由 `npm run build` 在 `prepare-package` 阶段生成（与 vector/model 插件一致）。
