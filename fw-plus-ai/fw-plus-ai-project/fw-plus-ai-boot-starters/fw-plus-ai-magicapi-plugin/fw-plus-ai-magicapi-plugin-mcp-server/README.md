# fw-plus-ai-magicapi-plugin-mcp-server

Magic-API 的 MCP 插件，把 Magic-API 的接口/函数与三类动态资源（工具、资源、提示词）暴露为 MCP 能力，并支持在控制台维护多个独立的 MCP server 实例。

本模块目录只负责 starter 依赖与控制台前端资源打包；后端实现位于 `fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp` 包。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-magicapi-plugin-mcp-server</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

依赖关系：

- `fw-plus-ai-boot-autoconfigure`：插件后端配置、控制器、资源存储与动态注册。
- `sa-token-core` / `sa-token-oauth2` / `sa-token-jwt` / `sa-token-apikey` / `sa-token-sso`（均 optional）：按实例所选 provider 提供对应的凭据校验实现；classpath 缺失的 provider 控制台不可选，实例配置了缺失模块的 provider 时 fail-closed 拒绝（401）。

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

`mcp-server` 资源字段：`key`（必填、字母数字下划线点中划线、唯一）、`name`、`transport`、`endpoint`（必填、以 `/` 开头）、`enabled`、`auth`（`provider` 认证方式选择）、`tools`。新增/修改/删除资源即热创建/更新/销毁实例，动态路由随 web 栈（Servlet/WebFlux）自动注册与注销。

## 认证

插件与 mcp starter 使用同一份内置 provider 清单：`permit-all`、`sa-token-login`、`sa-token-oauth2`、`sa-token-jwt`、`sa-token-apikey`、`sa-token-sso`。静态实例（`magic-api.plugin.mcp.servers[].auth.provider`）与控制台维护的动态 `mcp-server` 资源（表单「认证方式」下拉）逐实例选择一种 provider；未选择的实例保持未认证，Servlet MVC 与 WebFlux 均返回 **401**，不进入 MCP 内核，也不会回退到其它实例或放行服务。

- 控制台下拉选项来自 `GET /mcp/server/authProviders`，与后端 `McpAuthServiceRegistry.availableProviders()` 同一份清单：classpath 缺失的 Sa-Token 模块不可选，也没有历史 `custom` 选项；保存时后端再次校验（`MagicMcpServerInfoMagicResourceStorage.validate`），非法 provider 拒绝保存。
- **MCP 只校验请求携带的凭据，不实现认证服务**：不登录、不签发令牌、不保存任何 secret；认证经请求头/参数无界面完成，任何凭据都不写入 `mcp-server` 资源、控制台表单或配置。
- 实例配置的 provider 对应模块缺失时 fail-closed：请求一律 **401**，不回退、不导致启动失败；未知 provider（含历史 `custom`）拒绝保存，静态配置则启动失败。
- 认证主体携带 scopes（可多值或空集）：OAuth2/API Key 取凭据原生 scopes，JWT 取 `scopes`/`scope` claim，登录 token 与 SSO ticket 为空集；scope 覆盖不足返回 **403**，`permit-all` 显式绕过 scope 裁剪。

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
- `/mcp/server/authProviders`：当前 classpath 可用的认证 provider 清单（供表单渲染可选项）。
- `/mcp/server/test`、`/mcp/server/doDelete`：保存/删除前校验。
- `/mcp/server/changeConnected`、`/mcp/server/refresh`：启停实例、刷新工具快照。
- `/mcp/diagnostics/capabilities?key=<server-key>`：经该实例认证后的能力清单。
- `/mcp/diagnostics/execute`、`/mcp/diagnostics/executePrompt`：试运行 `resources/read` / `prompts/get`。

所有诊断接口均须提供有效实例 key，并按该实例配置的 `auth.provider` 通过认证；缺少或无效认证返回 **401**，未知实例返回 **404**。能力清单与试运行不会绕过 runtime 授权。

## 客户端请求（roots / sampling / elicitation）

`MagicMcpClientGateway` 在调用内核 `listRoots`/`createMessage`/`elicit` 前检查客户端是否声明了对应 capability；未声明则不发请求（roots 返回空列表，sampling/elicitation 抛 `IllegalStateException`）。pending 状态通过 `runningRequests()`、`pendingClientResponses()` 只读查询。

## 环境变量与端口

插件自身不监听端口；端点由 `mcp-server` 实例的 `endpoint` 决定（`http-sse` 为 `{endpoint}/sse` + `{endpoint}/messages/{uuid}`，`streamable-http` 为 `{endpoint}`）。

## 从 Boot 层旧实现迁移

本插件取代了 `com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mcp`（Boot 层同名模块，含其自研 transport / SSE / 诊断 / Sa-Token 认证，已删除）。迁移要点：

| 旧（Boot 层） | 新（AI 层） |
| --- | --- |
| `fw-plus-magicapi-plugin-mcp` | `fw-plus-ai-magicapi-plugin-mcp-server`（console 前端） |
| `…boot.autoconfigure.magicapi.plugin.mcp.*` | `…ai.autoconfigure.magicapi.plugin.mcp.*` + `…ai.autoconfigure.mcp.*` |
| `McpAccessTokenVerifier` / `SaTokenMcpAuthService` | starter 的 `McpAuthService` registry；按实例 `auth.provider` 选择内置认证 |
| `McpToolsHandler` / `McpResourcesHandler` 等 | 经内核 `FeatureMgr` 的 store 注册 + `MagicApiMcpServerRegistry` 改写响应 |

配置键变化（旧键已无消费者）：

- `magic-api.plugin.mcp.transport` / `session-timeout` / `sse-stream` / `max-request-bytes` / `max-response-chars` / `trusted-origins` / `protocol.*` / `headers.*` → 改用 `fw-plus.ai.mcp.*`（mcp starter）
- `magic-api.plugin.mcp.auth.*`（原 Boot 层插件自带）→ 不再使用；认证按实例 `auth.provider` 从内置 provider 中选择
- 保留：`enabled`、`tools.*`、`servers[]`

## 已知限制

1. **快照缓存**：`MagicApiMcpToolRegistry` 的工具快照只在收到 Magic-API 资源变更事件后失效。自定义 `MagicApiMcpCapabilityProvider` 若内部状态运行期变化，需自行触发重建（例如注入该 registry 调 `invalidateSnapshot()`）。
2. **completion 的 resource 侧**：内核按模板名反查拼 key，控制台维护 `template` 时建议让 `name` 与 `template` 保持易对应的关系；两种 ref 形态（模板名 / uriTemplate）均已注册。
3. **WebFlux 动态路由**：验证到装配级（`ApplicationContextRunner` + 路由探测），未覆盖真实 Netty 端到端。
4. **认证缺省语义**：实例未配置 `auth.provider` 或配置的 provider 对应模块缺失时一律返回 401（fail-closed）；生产环境需为每个允许访问的实例显式选择 provider。
5. **console dist 不入库**：`src/console/dist/` 被 `.gitignore` 排除，jar 中的 `magic-mcp.*.iife.js` 由 `npm run build` 在 `prepare-package` 阶段生成（与 vector/model 插件一致）。
