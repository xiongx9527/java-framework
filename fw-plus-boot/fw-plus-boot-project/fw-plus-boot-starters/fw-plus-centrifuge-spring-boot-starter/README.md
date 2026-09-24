# fw-plus-centrifuge-spring-boot-starter

基于 `centrifuge-java` 的 Spring Boot Starter，提供多实例连接、频道订阅、消息监听和消息发送能力。

## 引入依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-centrifuge-spring-boot-starter</artifactId>
</dependency>
```

## 配置

```yaml
fw:
  plus:
    centrifugo:
      enabled: true
      token-getter-bean-name: myTokenGetter
      instances:
        - name: node-1
          endpoint: ws://localhost:9000/connection/websocket
          client-name: app-1
          token: token-1
          enabled: true
        - name: node-2
          endpoint: ws://localhost:9001/connection/websocket
          client-name: app-2
          token: token-2
          enabled: true
```

## 监听消息

实现 `CentrifugoMessageListener`：

```java
@Component
public class MyMessageListener implements CentrifugoMessageListener {

    @Override
    public void onConnected(String clientId) {
        // 业务实现
    }

    @Override
    public void onDisconnected(int code, String reason) {
        // 业务实现
    }

    @Override
    public void onError(Throwable error) {
        // 业务实现
    }

    @Override
    public void onMessage(Object subscription, byte[] data) {
        // 业务实现
    }

    @Override
    public void onJoin(String channel, Object info) {
        // 业务实现
    }

    @Override
    public void onLeave(String channel, Object info) {
        // 业务实现
    }
}
```

自定义 token getter：

```java
@Component("myTokenGetter")
public class MyTokenGetter implements ConnectionTokenGetter {

    @Override
    public void getConnectionToken(ConnectionTokenEvent event, TokenCallback callback) {
        String token = "...";
        callback.Done(null, token);
    }
}
```

## 订阅频道

收到频道消息前需要先订阅频道：

```java
@Autowired
private CentrifugoPublishService publishService;

publishService.subscribe("node-1", "chat");
```

订阅并监听频道事件：

```java
publishService.subscribe("node-1", "chat", new SubscriptionEventListener() {

    @Override
    public void onPublication(Subscription subscription, PublicationEvent event) {
        System.out.println(new String(event.getData()));
    }

    @Override
    public void onSubscribed(Subscription subscription, SubscribedEvent event) {
        System.out.println("subscribed");
    }

    @Override
    public void onUnsubscribed(Subscription subscription, UnsubscribedEvent event) {
        System.out.println("unsubscribed");
    }

    @Override
    public void onJoin(Subscription subscription, JoinEvent event) {
    }

    @Override
    public void onLeave(Subscription subscription, LeaveEvent event) {
    }

    @Override
    public void onSubscribing(Subscription subscription, SubscribingEvent event) {
    }

    @Override
    public void onError(Subscription subscription, SubscriptionErrorEvent event) {
        event.getError().printStackTrace();
    }
});
```

取消订阅：

```java
publishService.unsubscribe("node-1", "chat");
```

## 发送消息

```java
@Autowired
private CentrifugoPublishService publishService;

publishService.publish("node-1", "chat", "hello".getBytes());
```

```java
@Autowired
private CentrifugoConnectionService connectionService;

connectionService.send("node-1", "chat", "hello".getBytes());
```

## 注意事项

- 当前基于 `centrifuge-java` `0.7.0`，Java 8 兼容
- 收到频道消息前，需要先调用 `subscribe`；业务可按需订阅
- 多实例配置为空时，自动 fallback 到 `single` 配置
- 生产环境建议配置 token 认证和错误处理
- `token-getter-bean-name` 需要在 Spring 上下文中提供对应 `ConnectionTokenGetter` Bean
