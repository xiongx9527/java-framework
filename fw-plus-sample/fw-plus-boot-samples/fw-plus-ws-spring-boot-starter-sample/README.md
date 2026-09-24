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
| `DemoTokenController` | `GET /demo/token?subject=default`（可加 `&path=/ws/api2`）按端点路径解析出的密钥签发握手用 JWT |
| `DemoWsDispatcher` | 实现 `WsBusinessDispatcher`：`echo` 文本订阅与二进制订阅都原样回发 |
| `application.yml` | `fw.plus.ws.*` 配置样例（密钥、端点路径、跨域、二进制、广播、按路径绑定的 token） |
| `static/index.html` | 演示页面：连接、发送文本/二进制帧并查看回显 |

握手地址为 `ws://localhost:8080/ws/api?token=<token>&clientId=<clientId>`，与 `index.html` 的拼接方式一致。

`application.yml` 另注册了 `/ws/api2`，并在 `fw.plus.ws.tokens` 里给它配了专用密钥：两条端点各自校验自己的 token，未命中规则的路径回落全局 `fw.plus.ws.token.secret`。

业务侧只需提供 `WsBusinessDispatcher` 实现：地基负责连接、鉴权、会话、心跳与广播，
`findEnabledSubscribes` 决定哪些订阅生效，`execute` 执行业务并通过 `WsReplyHandle` 回发。
未提供实现时，地基退化为纯 WebSocket 通道。

## 说明

- 跨实例广播依赖 Redis；未配置 Redis 连接时本样例只走单实例本地广播。
- `fw.plus.ws.websocket.trustedDomain` 设为 `*` 只为本地联调，生产环境请改为具体域名。
