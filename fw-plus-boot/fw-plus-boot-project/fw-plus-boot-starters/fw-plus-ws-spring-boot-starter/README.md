# fw-plus-ws-spring-boot-starter

WebSocket 长连接地基：端点注册、握手鉴权、会话管理、心跳、定向发送与跨实例广播。
业务语义（哪些订阅生效、脚本如何执行）由宿主通过 SPI 注入；未注入时退化为纯 WebSocket 通道。

同时支持 **webmvc（Servlet 栈）** 与 **webflux（响应式栈）**，两者共用同一套配置与订阅 SPI，行为一致。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-ws-spring-boot-starter</artifactId>
</dependency>
```

webflux 应用额外引入 `spring-boot-starter-webflux`，地基会自动切换到响应式实现。

## 配置

```yaml
fw:
  plus:
    ws:
      enabled: true
      token:
        secret: 请替换为至少32位的密钥        # 必填，缺失时连接鉴权直接失败
        expireHours: 2
      websocket:
        path: /ws/api
        paths:                     # 追加端点路径，与主路径共用同一套处理器
        trustedDomain: https://example.com    # 留空表示不允许跨域
        subProtocols:                          # 可选，逗号分隔
      binary:
        enabled: true                          # 是否接受二进制帧
        type: binary                           # 二进制帧的业务类型
      broadcast:
        async: false                           # 本地广播是否异步投递
```

### 完整配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `token.secret` | 必填 | JWT 密钥 |
| `token.expireHours` | `2` | Token 有效期（小时） |
| `websocket.path` | `/ws/api` | 端点路径 |
| `websocket.paths` | 空 | 追加端点路径，逗号分隔或列表；与主路径去重后逐条注册 |
| `websocket.trustedDomain` | 空 | 受信任来源，逗号分隔；留空表示不允许跨域 |
| `websocket.subProtocols` | 空 | 子协议，逗号分隔 |
| `websocket.supportsPartialMessages` | `false` | 是否支持部分消息 |
| `param.token` / `param.clientId` | `token` / `clientId` | 握手 query 参数名 |
| `param.maxClientIdLength` | `50` | clientId 最大长度 |
| `init.type` / `close.type` | `init` / `close` | 建连/断连订阅业务类型 |
| `appId` | `default` | 默认应用 ID |
| `session.cache.maxClients` | `10000` | 会话缓存最大 clientId 数 |
| `session.cache.expireMinutes` | `30` | 会话缓存与空闲判定阈值（分钟） |
| `session.cache.concurrencyLevel` | `16` | 缓存并发级别 |
| `session.delayMillis` | `1000` | 延迟关闭毫秒数 |
| `session.maxTextMessageSize` | `65536` | 单条文本帧上限（字节） |
| `session.maxBinaryMessageSize` | `65536` | 单条二进制帧上限（字节） |
| `session.maxTotalSessions` | `10000` | 全局会话数上限 |
| `session.maxSessionsPerClient` | `100` | 单个 clientId 会话数上限 |
| `session.idleScanEnabled` | `false` | 空闲会话定时扫描 |
| `session.idleScanIntervalSeconds` | `60` | 扫描周期（秒） |
| `session.closeOnCacheEviction` | `false` | 缓存驱逐时是否主动关闭仍打开的会话 |
| `scheduler.delayedClosePoolSize` | `2` | 延迟关闭线程池大小 |
| `redis.channel.broadcast` | `fw-plus:websocket:broadcast` | 跨实例广播频道 |
| `binary.enabled` | `true` | 是否接受二进制帧 |
| `binary.type` | `binary` | 二进制帧业务类型 |
| `broadcast.async` | `false` | 本地广播是否异步投递 |
| `webflux.scriptThreadPool.coreSize` | `8` | webflux 脚本线程池核心大小 |
| `webflux.scriptThreadPool.maxSize` | `32` | 最大大小 |
| `webflux.scriptThreadPool.queueCapacity` | `1000` | 队列容量 |

## 连接

客户端在握手 query 中携带 token 与 clientId：

```
ws://localhost:8080/ws/api?token=<jwt>&clientId=<clientId>
```

Token 为 JWT，`subject` 即 appId。缺失参数、clientId 超长、token 校验失败时以策略违规关闭；会话数超限时以 `SERVICE_OVERLOAD` 关闭。

## 收发消息

文本帧按 `type` 字段路由到订阅脚本：

```json
{"type": "order-refresh", "keyword": "订单"}
```

- 非 JSON → `INVALID_JSON`
- 缺 `type` → `MISSING_TYPE`
- 无匹配订阅 → `UNKNOWN_TYPE`
- 脚本异常 → `SCRIPT_ERROR`
- `ping` 文本或原生 Ping 帧 → 回 `pong`/`Pong`

二进制帧在 `binary.enabled=true` 时按 `binary.type` 进入同一条派发链路，脚本侧通过 `params.data`（Base64）与 `params.length` 取到原始内容；置 `false` 时以 `NOT_ACCEPTABLE` 关闭。

## 广播

应用内引入 Redis 后自动启用跨实例广播：本地会话投递 + Redis 频道发布，接收方按 serverId 排除自身，只做本地投递，不产生回环。

`broadcast.async=true` 时本地投递改为有界线程池异步执行，队列满则丢弃并告警，避免个别慢客户端阻塞广播链路；默认 `false` 保持同步语义。

## 多端点与多基础路径

一份配置可以把同一套 WS 能力（握手鉴权、会话管理、广播）挂到多条端点路径上：

```yaml
fw:
  plus:
    ws:
      websocket:
        path: /ws/api
        paths:
          - /ws/api2
          - /ws/api3
```

基础路径来自容器内的 `WsBasePathProvider` Bean，可声明多个：每个基础路径与每条相对路径都会注册，同一完整路径只注册一次。多个候选按 `@Primary` 优先、其余按 `@Order`/`Ordered` 定序；一个都没有时按无基础路径处理。

webflux 栈的展开结果与 webmvc 完全一致，两端共用同一套拼接规则；`websocket.path` 显式置空属配置错误，两端表现一致，只在基础路径后追加一个连接斜杠。

## 与宿主自注册的端点并存

地基不独占端点注册来源：

- **webmvc**：Spring 的 `DelegatingWebSocketConfiguration` 收集容器内全部 `WebSocketConfigurer`，宿主的注册器与地基注册器并存生效，互不覆盖。注意 webmvc 栈需要宿主在自己的应用类上声明 `@EnableWebSocket`（或 `@EnableWebSocketMessageBroker`），容器内的 `WebSocketConfigurer` 才会被收集；地基不代开，以免与消息代理场景冲突。
- **webflux**：地基注册一个 order 为 `LOWEST_PRECEDENCE - 100` 的 `SimpleUrlHandlerMapping`，宿主自带的 mapping 与它互不覆盖，两者由 order 决定先后。

单条路径注册失败只记录日志，不影响其余路径。

## 注意事项

- `fw.plus.ws.token.secret` 必填；更换后无需重启，密钥缓存按密钥内容索引，配置刷新即生效。
- 任意入站消息（文本、二进制、Ping、Pong）都会刷新活跃时间，只发业务消息从不发心跳的长连接不会被判超时。
- 缓存驱逐默认不会主动关闭仍打开的会话；需要回收不发 FIN 就掉线的连接时开启 `session.idleScanEnabled`。
- 生产环境请把 `websocket.trustedDomain` 配成具体来源，不要留空也不要用 `*`。
- webflux 栈下订阅脚本在独立有界线程池执行，不会占用 Reactor event loop。
