# fw-plus-ws-spring-boot-starter-sample

`fw-plus-ws-spring-boot-starter` 的最小可用样例：一个 Spring Boot 应用 + 一个业务订阅实现 + 一个演示页面。

## 运行

```bash
cd fw-plus-core && ./mvnw -o -f fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample/pom.xml spring-boot:run
```

或直接在 IDE 中运行 `com.xiongx9527.fw.plus.boot.ws.sample.WsSampleApplication`，然后打开 <http://localhost:8080/>。

## 里面有什么

| 文件 | 作用 |
| --- | --- |
| `WsSampleApplication` | 应用入口，引入 starter 后 `/ws/api` 端点自动注册 |
| `DemoWsDispatcher` | 实现 `WsBusinessDispatcher`：`echo` 文本订阅与二进制订阅都原样回发 |
| `application.yml` | `fw.plus.ws.*` 配置样例（端点路径、命名实例、跨域、二进制、广播） |
| `static/index.html` | 演示页面：连接、发送文本/二进制帧并查看回显 |

握手地址为 `ws://localhost:8080/ws/api?clientId=<clientId>`，与 `index.html` 的拼接方式一致。

`application.yml` 用顶层 `fw.plus.ws.path` / `fw.plus.ws.paths` 把默认实例注册到 `/ws/api` 与 `/ws/api2`，再用 `fw.plus.ws.servers` 追加了一个 key 为 `report` 的实例（`/ws/api3` + `appId=report`）：一个实例 = 一个 key + 一组端点 + 一个 appId，握手时按请求路径匹配实例，未命中则回落到 `fw.plus.ws.appId`。

业务侧只需提供 `WsBusinessDispatcher` 实现：地基负责连接、会话、心跳与广播，
`findEnabledSubscribes` 决定哪些订阅生效，`execute` 执行业务并通过 `WsReplyHandle` 回发。
未提供实现时，地基退化为纯 WebSocket 通道。

## 说明

- 跨实例广播依赖 Redis；未配置 Redis 连接时本样例只走单实例本地广播。
- `fw.plus.ws.websocket.trustedDomain` 设为 `*` 只为本地联调，生产环境请改为具体域名。
