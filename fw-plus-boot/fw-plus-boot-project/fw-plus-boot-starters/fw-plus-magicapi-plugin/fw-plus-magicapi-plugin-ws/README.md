# fw-plus-magicapi-plugin-ws

MagicAPI WebSocket 插件，用于在 MagicAPI 中维护 WebSocket 数据源、连接事件脚本和业务消息脚本，并通过 `ws` 模块向指定客户端或全部客户端推送消息。

## 依赖与启用

在业务应用引入 `fw-plus-magicapi-plugin-ws` 后默认启用。该插件的 WS 依赖来自 `fw-plus-ws-spring-boot-starter`（提供 `spring-boot-starter-websocket` 与 `jjwt-*`），需要单独使用 WS 能力时也可直接依赖该 starter。

```yaml
fw:
  plus:
    ws:
      enabled: true
      path: /ws/api              # WebSocket 连接路径，会叠加 magic-api.web 前缀
      paths:                     # 追加连接路径，与 path 去重后逐条生效
      websocket:
        trustedDomain: https://example.com
```

关键配置：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `fw.plus.ws.enabled` | `true` | 是否启用 WS 插件 |
| `fw.plus.ws.appId` | `default` | 默认 appId，握手未命中命名实例时回落 |
| `fw.plus.ws.path` | `/ws/api` | WebSocket 连接路径，会叠加 `magic-api.web` 前缀 |
| `fw.plus.ws.paths` | 空 | 追加连接路径，逐条叠加 `magic-api.web` 前缀；与主路径重复的项会去重 |
| `fw.plus.ws.servers` | 空 | 命名实例列表，每项一个 `key` + 一组端点 + 一个 `appId`；`key` 必填唯一、端点不得与默认实例或其它实例重复 |
| `fw.plus.ws.websocket.trustedDomain` | 空 | 允许跨域来源；未配置时不允许跨域，显式 `*` 才允许任意来源 |
| `fw.plus.ws.websocket.subProtocols` | 空 | 可选 WebSocket 子协议，逗号分隔 |
| `fw.plus.ws.param.clientId` | `clientId` | 连接 URL 中客户端标识参数名 |
| `fw.plus.ws.init.type` | `init` | 建连时执行的业务标识 |
| `fw.plus.ws.close.type` | `close` | 断连时执行的业务标识 |
| `fw.plus.ws.redis.channel.broadcast` | `fw-plus:websocket:broadcast` | Redis 集群广播频道 |

> 配置前缀为 `fw.plus.ws`。旧前缀 `magic-api.plugin.ws` 已不再被读取，升级时需要同步修改配置。

## 最快接入流程

1. 引入 WS 插件（`fw-plus-magicapi-plugin-ws`），并配置 `fw.plus.ws.websocket.trustedDomain`。
2. 在 MagicAPI 控制台新增 `ws-datasource`，例如 `key=default`、`secret=123`。
3. 前端连接 `<magic-api.web 前缀>/ws/api?clientId=<clientId>`，`clientId` 由业务侧分配。
4. 根据需要新增 `ws-subscribe` 脚本处理 `init`、`close` 和业务消息 `type`。

最小配置示例：

```yaml
magic-api:
  web: /magic/web
fw:
  plus:
    ws:
      enabled: true
      path: /ws/api               # WebSocket 连接路径，会叠加 magic-api.web 前缀
      websocket:
        trustedDomain: http://localhost:5173
      param:
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
  "path": "/ws/api",
  "description": "默认 WebSocket 应用"
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 是 | 数据源显示名称 |
| `key` | 是 | 应用标识，握手命中的实例 `appId`，即脚本里的 `appId` 与 `ws['key']` 的数据源名 |
| `secret` | 是 | 数据源密钥，仅作为元信息保留，不参与握手鉴权 |
| `path` | 否 | 数据源绑定的 WS 端点路径，对应 `fw.plus.ws.servers` 中一个实例的端点 |
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

- `ws-datasource` 测试会校验必填字段（`key`、`name`、`secret`）与 `other` JSON 格式。
- `ws-subscribe` 运行按钮使用模拟 WebSocket 上下文执行当前脚本，不会建立真实 WebSocket 连接，也不会注册真实 session。
- 模拟上下文包含 `appId`、`clientId`、`sessionId`、`type`、`businessType`、`params`。
- 推荐新脚本使用 `type`，`businessType` 作为兼容别名保留。

握手不再签发或校验 token：`clientId` 由业务侧分配，应使用已鉴权、不可猜测的用户标识。

## 客户端对接

连接地址格式：

```text
ws://<host>:<port><magic-api.web 前缀>/ws/api?clientId=<clientId>
```

多个 `ws-datasource` 或多入口场景下，可在同一份配置上挂多条连接路径，`magic-api.web` 前缀对每条路径同样生效；端点键在顶层，与命名实例同形：

```yaml
fw:
  plus:
    ws:
      path: /ws/api
      paths:
        - /ws/api2
        - /ws/api3
    # 连接地址：ws://<host>:<port>/<magic-api.web>/ws/api2?clientId=<clientId>
```

多个应用共用一套 MagicAPI 时，按「一个实例一条路径 + 一个 appId」拆分：把数据源的 `path` 配到 `fw.plus.ws.servers` 的一个实例上，握手命中该端点后 appId 取实例 `appId`，`ws['key']` 与订阅派发都按这个 appId 走，实例之间互不串用。

```yaml
fw:
  plus:
    ws:
      appId: default                      # 未命中任何实例时的兜底 appId
      servers:
        - key: order                      # 对应 path 为 /ws/order 的数据源
          path: /ws/order
          appId: order
        - key: pay
          path: /ws/pay
          appId: pay
```

实例按握手请求的完整路径匹配：精确命中优先，其次按尾段后缀命中；未命中任何实例或实例 `appId` 留空时，回落全局 `fw.plus.ws.appId`。

端点注册来源不限于插件自身：宿主自行注册的 `WebSocketConfigurer`（webmvc 栈需在应用类上声明 `@EnableWebSocket`）或自带的 `SimpleUrlHandlerMapping`（webflux 栈）都与地基端点并存，互不覆盖。握手参数与订阅派发对所有路径一致生效。

浏览器示例：

```javascript
async function createWebSocket() {
  const clientId = 'user-10001';
  const socket = new WebSocket(`ws://localhost:8080/magic/web/ws/api?clientId=${encodeURIComponent(clientId)}`);

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
| `1008` | 策略违规，例如缺少 `clientId`、`clientId` 超长、appId 未在业务侧登记 |
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

- 生产环境不要配置 `websocket.trustedDomain: '*'`，应配置明确的业务域名。
- `clientId` 应使用业务侧不可猜测或已鉴权的用户标识，避免被客户端伪造导致消息串扰。
- 不要在日志中输出数据源 `secret` 等敏感信息。

## 排查

- 连接后立即关闭，提示缺少参数：检查 URL 是否包含 `clientId`。
- 提示 AppId 不存在：确认握手命中的端点已配到 `fw.plus.ws.path` / `paths` 或 `servers` 实例上，且该实例 `appId` 已创建 `ws-datasource`。
- 业务消息无响应：确认客户端 JSON 中存在 `type`，且存在同 `key`、同 `businessId`、`enabled=true` 的 `ws-subscribe`。
- 跨节点无法推送：确认 Redis、`StringRedisTemplate` 和广播频道配置正常。
- 浏览器跨域握手失败：确认 `websocket.trustedDomain` 包含当前页面 Origin。
