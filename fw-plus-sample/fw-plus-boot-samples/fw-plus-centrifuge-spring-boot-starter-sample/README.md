# fw-plus-centrifuge-spring-boot-starter-sample

Centrifuge Starter 最小示例，用于验证 Java 监听消息和发送消息。

## 前置条件

- 已启动 Centrifugo，并且服务地址可访问
- 当前示例默认使用：
  - 实例名：`node-1`
  - endpoint：`ws://localhost:9000/connection/websocket`
  - token：`token-1`

## 启动

```bash
cd fw-plus-core && ./mvnw clean install -DskipTests
cd fw-plus-sample && ../fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```

### 直接运行

```bash
cd fw-plus-sample/fw-plus-boot-samples/fw-plus-centrifuge-spring-boot-starter-sample
mvn spring-boot:run
```

### 打包为可执行 jar

```bash
cd fw-plus-sample/fw-plus-boot-samples/fw-plus-centrifuge-spring-boot-starter-sample
mvn clean package -DskipTests
java -jar target/fw-plus-centrifuge-spring-boot-starter-sample-0.1.1-SNAPSHOT.jar
```

## 验证

### 1. 打开前端页面

浏览器访问：

```
http://localhost:8080/
```

页面会提供：
- 实例名输入框
- channel 输入框
- message 输入框
- 发送按钮

点击“发送消息”后，页面会显示发送结果。

### 2. 订阅频道并查看 Java 控制台输出

如果还没订阅频道，先调用订阅接口：

```bash
curl -X POST "http://localhost:8080/sample/centrifuge/subscribe?instanceName=node-1&channel=chat"
```

然后使用发送接口：

```bash
curl -X POST "http://localhost:8080/sample/centrifuge/send-instance?instanceName=node-1&channel=chat&message=hello from curl"
```

启动日志中会先看到连接成功输出：

```
[centrifuge] connected: node-1:...
```

发送消息后，会看到：

```
[centrifuge] message: hello from curl
```

这说明：
- Java 已经连接到 Centrifugo
- 前端页面通过 Java 服务发送消息成功
- `DemoCentrifugoListener` 已收到频道消息

## 前端直连 Centrifugo

浏览器访问：

```
http://localhost:8080/ws.html
```

页面会提供：
- Centrifugo WebSocket 地址输入框
- token 输入框
- channel 输入框
- 连接 / 订阅 / 发送 / 断开按钮

说明：
- 这个页面不经过 Java 服务转发，直接连接 Centrifugo
- token 为空时，仅用于本地无 token 场景；生产环境请使用真实 token
- 发送后，如果 Java 侧的 `DemoCentrifugoListener` 收到消息，会打印在 Java 控制台

## 接口说明

示例控制器暴露了以下接口：

- `POST /sample/centrifuge/subscribe?instanceName=node-1&channel=chat`
- `POST /sample/centrifuge/unsubscribe?instanceName=node-1&channel=chat`
- `POST /sample/centrifuge/send-default?channel=chat&message=hello`
- `POST /sample/centrifuge/send-instance?instanceName=node-1&channel=chat&message=hello`
- `POST /sample/centrifuge/send-all?channel=chat&message=hello`
- `POST /sample/centrifuge/direct-send?instanceName=node-1&channel=chat&message=hello`
- `GET /sample/centrifuge/instances`

## 业务实现位置

- 监听消息：`DemoCentrifugoListener`
- 发送消息：`DemoCentrifugoSender`
- 前端页面：`src/main/resources/static/index.html`
- 前端直连页面：`src/main/resources/static/ws.html`

## 常见问题

- 如果连接失败，请确认 Centrifugo 地址和 token 是否正确
- 如果页面打不开，请确认 sample 是否运行在 `8080` 端口
- 如果收不到消息，请先确认已订阅对应 channel，再确认前后端使用的 channel 是否一致
- 如果直连页面报错，请确认浏览器可访问 Centrifugo 地址，并且 Centrifugo 已开启 WebSocket 连接
