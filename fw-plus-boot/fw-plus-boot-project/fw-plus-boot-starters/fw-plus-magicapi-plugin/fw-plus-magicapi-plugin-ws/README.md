# fw-plus-magicapi-plugin-ws

MagicAPI WebSocket 插件，用于在 MagicAPI 中维护 WebSocket 数据源、连接事件脚本和业务消息脚本，并通过 `ws` 模块向指定客户端或全部客户端推送消息。

## 依赖与启用

在业务应用引入 `fw-plus-magicapi-plugin-ws` 后默认启用。该插件的 WS 依赖来自 `fw-plus-ws-spring-boot-starter`（提供 `spring-boot-starter-websocket` 与 `jjwt-*`），需要单独使用 WS 能力时也可直接依赖该 starter。

```yaml
fw:
  plus:
    ws:
      enabled: true
      websocket:
        path: /ws/api
        paths:                   # 追加连接路径，与 path 去重后逐条生效
        trustedDomain: https://example.com
      token:
        secret: 请替换为生产环境随机密钥
```

关键配置：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `fw.plus.ws.enabled` | `true` | 是否启用 WS 插件 |
| `fw.plus.ws.websocket.path` | `/ws/api` | WebSocket 连接路径，会叠加 `magic-api.web` 前缀 |
| `fw.plus.ws.websocket.paths` | 空 | 追加连接路径，逐条叠加 `magic-api.web` 前缀；与主路径重复的项会去重 |
| `fw.plus.ws.websocket.trustedDomain` | 空 | 允许跨域来源；未配置时不允许跨域，显式 `*` 才允许任意来源 |
| `fw.plus.ws.websocket.subProtocols` | 空 | 可选 WebSocket 子协议，逗号分隔 |
| `fw.plus.ws.token.secret` | 无 | JWT 签名密钥，必须显式配置 |
| `fw.plus.ws.token.expireHours` | `2` | 连接 token 有效期，单位小时 |
| `fw.plus.ws.param.token` | `token` | 连接 URL 中 token 参数名 |
| `fw.plus.ws.param.clientId` | `clientId` | 连接 URL 中客户端标识参数名 |
| `fw.plus.ws.init.type` | `init` | 建连时执行的业务标识 |
| `fw.plus.ws.close.type` | `close` | 断连时执行的业务标识 |
| `fw.plus.ws.redis.channel.broadcast` | `fw-plus:websocket:broadcast` | Redis 集群广播频道 |

> 配置前缀为 `fw.plus.ws`。旧前缀 `magic-api.plugin.ws` 已不再被读取，升级时需要同步修改配置。

> `token.secret` 不再提供内置默认值。生产环境建议使用 32 字节以上随机字符串，并通过配置中心或环境变量注入。

## 最快接入流程

1. 引入 WS 插件（`fw-plus-magicapi-plugin-ws`），并配置 `fw.plus.ws.token.secret` 和 `fw.plus.ws.websocket.trustedDomain`。
2. 在 MagicAPI 控制台新增 `ws-datasource`，例如 `key=default`、`secret=123`。
3. 新增一个 MagicAPI HTTP API，用于给前端签发连接 token。
4. 前端调用 token API 获取 token，再连接 `/ws/api?token=...&clientId=...`。
5. 根据需要新增 `ws-subscribe` 脚本处理 `init`、`close` 和业务消息 `type`。

最小配置示例：

```yaml
magic-api:
  web: /magic/web
fw:
  plus:
    ws:
      enabled: true
      websocket:
        path: /ws/api
        trustedDomain: http://localhost:5173
      token:
        secret: ${WS_TOKEN_SECRET}
        expireHours: 2
      param:
        token: token
        clientId: clientId
```

## 资源目录

插件新增两个 MagicAPI 资源类型：

- `ws-datasource`：WebSocket 应用数据源，JSON 文件。
- `ws-subscribe`：WebSocket 事件/业务脚本，`.ms` 文件，结构为 JSON 头部 + `================================` + magic-script 脚本。

### ws-datasource

示例：

```json
{
  "name": "默认数据源",
  "key": "default",
  "secret": "123",
  "description": "默认 WebSocket 应用"
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 是 | 数据源显示名称 |
| `key` | 是 | 应用标识，即脚本和连接 token 中的 `appId` |
| `secret` | 是 | 生成连接 token 时校验的应用密钥 |
| `description` | 否 | 描述 |

`key` 创建后不允许修改，同一个 `key` 必须唯一。

### ws-subscribe

建连脚本示例：

```javascript
{
  "name": "上线通知",
  "key": "default",
  "businessId": "init",
  "enabled": true
}
================================
import log;
import ws;

log.info("clientId:{} 上线了", clientId);
ws.broadcastMessageToClient(clientId, "hello，暂时无数据更新");

return null;
```

断连脚本示例：

```javascript
{
  "name": "下线通知",
  "key": "default",
  "businessId": "close",
  "enabled": true
}
================================
import log;

log.info("clientId:{} 下线了", clientId);

return null;
```

业务消息脚本示例：

```javascript
{
  "name": "订单刷新",
  "key": "default",
  "businessId": "order-refresh",
  "enabled": true
}
================================
import log;
import ws;

log.info("收到客户端消息: {}", params);
ws.broadcastMessageToClient(clientId, JSON.stringify({ type: "order-refresh", data: "ok" }));

return null;
```

脚本上下文变量：

| 变量 | 说明 |
| --- | --- |
| `appId` | 当前数据源 `key` |
| `clientId` | 连接客户端标识 |
| `sessionId` | 当前 WebSocket session id |
| `type` | 当前业务消息类型，建连和断连脚本中为对应生命周期业务标识 |
| `businessType` | `type` 的兼容别名，建议新脚本优先使用 `type` |
| `params` | 客户端发送的 JSON 消息对象，仅业务消息脚本存在 |

同一个数据源 `key` 下，`businessId` 必须唯一。

### 控制台测试说明

- `ws-datasource` 测试会校验必填字段、`other` JSON 格式和 token 生成前置条件。
- `ws-subscribe` 运行按钮使用模拟 WebSocket 上下文执行当前脚本，不会建立真实 WebSocket 连接，也不会注册真实 session。
- 模拟上下文包含 `appId`、`clientId`、`sessionId`、`type`、`businessType`、`params`。
- 推荐新脚本使用 `type`，`businessType` 作为兼容别名保留。

## 生成连接 Token

在 MagicAPI 脚本中使用 `ws` 模块生成连接 token：

```javascript
import ws;

// default 为 ws-datasource.key，123 为该数据源 secret
return ws['default'].generateToken('123');
```

生成的 token subject 为 `appId`，服务端连接时会校验 token 并提取 `appId`。

### Token 签发 API 示例

建议由后端根据当前登录用户生成 `clientId`，不要完全信任前端传入的用户标识。下面示例演示最小流程，实际项目中应接入登录态和权限校验。

```javascript
{
  "path": "/ws/token",
  "method": "GET",
  "description": "签发 WebSocket 连接 token"
}
================================
import ws;

var appId = "default";
var appSecret = "123";
var currentClientId = clientId;

if (!currentClientId) {
  return {
    code: 400,
    message: "clientId 不能为空"
  };
}

return {
  appId: appId,
  clientId: currentClientId,
  token: ws[appId].generateToken(appSecret)
};
```

上例为了演示流程直接读取请求参数 `clientId`。生产环境建议改为从当前登录态获取业务用户 ID，例如 `userId`、`loginId` 或 Sa-Token 当前账号 ID。

## 客户端对接

连接地址格式：

```text
ws://<host>:<port><magic-api.web 前缀>/ws/api?token=<token>&clientId=<clientId>
```

多个 `ws-datasource` 或多入口场景下，可在同一份配置上挂多条连接路径，`magic-api.web` 前缀对每条路径同样生效：

```yaml
fw:
  plus:
    ws:
      websocket:
        path: /ws/api
        paths:
          - /ws/api2
          - /ws/api3
  # 连接地址：ws://<host>:<port>/<magic-api.web>/ws/api2?token=<token>&clientId=<clientId>
```

端点注册来源不限于插件自身：宿主自行注册的 `WebSocketConfigurer`（webmvc 栈需在应用类上声明 `@EnableWebSocket`）或自带的 `SimpleUrlHandlerMapping`（webflux 栈）都与地基端点并存，互不覆盖。magic-api 插件的握手鉴权与订阅派发对所有路径一致生效。

浏览器示例：

```javascript
async function createWebSocket() {
  const clientId = 'user-10001';
  const tokenResp = await fetch(`/magic/web/ws/token?clientId=${encodeURIComponent(clientId)}`);
  const tokenBody = await tokenResp.json();

  const socket = new WebSocket(`ws://localhost:8080/magic/web/ws/api?token=${encodeURIComponent(tokenBody.token)}&clientId=${encodeURIComponent(tokenBody.clientId)}`);

  socket.onopen = () => {
    console.log('connected');
  };

  socket.onmessage = (event) => {
    console.log('message:', event.data);
  };

  socket.onclose = (event) => {
    console.log('closed:', event.code, event.reason);
  };

  return socket;
}
```

心跳消息：

```javascript
socket.send('ping');
// 服务端响应: pong
```

标准 WebSocket 客户端（非浏览器）也可以发送 Ping 控制帧，服务端会按协议返回 Pong 控制帧。浏览器 `WebSocket` API 不开放主动发送 Ping 控制帧的能力，因此浏览器侧如需应用层心跳，可继续使用上面的文本 `ping` 兼容方式。

不支持的消息类型：

- `TextMessage`：支持，业务消息必须是 JSON 对象并包含非空 `type`。
- `PingMessage`：支持，返回标准 `PongMessage`，不触发业务脚本。
- `PongMessage`：支持，仅作为连接级信号，不触发业务脚本。
- `BinaryMessage`：不支持，服务端会使用 `1003` 关闭连接。

业务消息：

```javascript
socket.send(JSON.stringify({
  type: 'order-refresh',
  orderId: 'O202607050001'
}));
```

服务端会根据 `type` 匹配同一 `appId` 下启用的 `ws-subscribe.businessId` 脚本。

可恢复业务错误会返回统一文本 JSON，连接保持可用：

```json
{
  "type": "error",
  "code": "MISSING_TYPE",
  "message": "Message type is required"
}
```

常见错误码：

| 错误码 | 说明 |
| --- | --- |
| `INVALID_JSON` | 文本消息不是合法 JSON |
| `MISSING_TYPE` | JSON 消息缺少非空 `type` |
| `UNKNOWN_TYPE` | 当前 `appId` 下没有启用的同名 `ws-subscribe.businessId` |
| `PROCESSING_ERROR` | 消息处理过程出现异常 |

不可恢复的协议或策略错误会关闭连接：

| Close Code | 场景 |
| --- | --- |
| `1003` | 不支持的数据类型，例如 Binary 消息 |
| `1008` | 策略违规，例如缺少 token/clientId、token 无效、appId 不存在 |
| `1011` | 服务端内部错误 |
| `1012` | 服务端重启或应用关闭 |
| `1013` | 服务过载或容量限制 |

## 服务端推送 API

MagicAPI 脚本中导入 `ws` 模块后可推送消息：

```javascript
import ws;

// 使用默认 appId
ws.broadcastMessageToClient('user-10001', 'hello');
ws.broadcastDistributedMessageToAllClient('system notice');

// 指定 appId / 数据源 key
ws['default'].broadcastMessageToClient('user-10001', 'hello');
ws['default'].broadcastDistributedMessageToAllClient('system notice');
```

说明：

- `broadcastMessageToClient(clientId, message)`：向指定 `clientId` 的所有本地/集群会话推送。
- `broadcastDistributedMessageToAllClient(message)`：向指定 `appId` 下全部客户端广播。
- 启用 Redis `StringRedisTemplate` 时自动使用 Redis channel 做跨节点广播；未启用 Redis 时仅本节点生效。

## 集群模式

当 Spring 容器中存在 `StringRedisTemplate` 时，插件会创建 Redis 版 WebSocket handler，并通过 `redis.channel.broadcast` 配置的频道分发跨节点消息。

```yaml
fw:
  plus:
    ws:
      redis:
        channel:
          broadcast: fw-plus:websocket:broadcast
```

集群消息会携带当前节点 server id，接收端会跳过来源节点，避免消息回环放大。

## 安全建议

- 必须配置 `fw.plus.ws.token.secret`，不要使用短弱密钥。
- 生产环境不要配置 `websocket.trustedDomain: '*'`，应配置明确的业务域名。
- `clientId` 应使用业务侧不可猜测或已鉴权的用户标识，避免被客户端伪造导致消息串扰。
- 不要在日志中输出完整 token 或数据源 secret。

## 排查

- 连接后立即关闭，提示缺少参数：检查 URL 是否包含 `token` 和 `clientId`。
- 提示 token 验证失败：检查 `token.secret` 是否一致，token 是否过期。
- 提示 AppId 不存在：确认 token 对应的 `appId` 已创建 `ws-datasource`。
- 业务消息无响应：确认客户端 JSON 中存在 `type`，且存在同 `key`、同 `businessId`、`enabled=true` 的 `ws-subscribe`。
- 跨节点无法推送：确认 Redis、`StringRedisTemplate` 和广播频道配置正常。
- 浏览器跨域握手失败：确认 `websocket.trustedDomain` 包含当前页面 Origin。
