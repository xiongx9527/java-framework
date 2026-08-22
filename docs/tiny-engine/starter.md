# TinyEngine Starter 使用说明

`fw-plus-tiny-engine-spring-boot-starter` 提供 TinyEngine 后端 API、Core 能力，并在打包时发布 TinyEngine 前端静态资源到同源 `/tiny-engine/`。

starter 不包含上游 `mockServer`，也不内置 AI 模型、Base URL 或 API Key。Robot 中的模型服务由用户在页面里添加或由宿主应用自行配置。

## Maven 引入

宿主应用先引入 TinyEngine starter：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-tiny-engine-spring-boot-starter</artifactId>
	<version>${fw-plus.version}</version>
</dependency>
```

然后显式选择一种 Web 技术栈。

Spring MVC：

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

Spring WebFlux：

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

通用 starter 不强制传递 `spring-boot-starter-web` 或 `spring-boot-starter-webflux`。如果应用同时引入两者，Spring Boot 会按自身规则选择 WebApplicationType，生产环境建议只选择一种。

## 基础配置

```yaml
fw:
  plus:
    tiny-engine:
      enabled: true
      sql:
        database: mysql
        auto-initialize: false
      security:
        enabled: true
```

- `enabled`：TinyEngine 总开关，默认关闭。
- `sql.database`：选择 SQL 资源类型，当前覆盖矩阵包含 H2、MySQL、PostgreSQL。
- `sql.auto-initialize`：是否自动初始化 SQL。生产环境建议交给 Flyway、Liquibase 或运维脚本管理。
- `security.enabled`：用户体系强制开关。生产环境建议保持开启。

## 数据库初始化

SQL 资源在 `fw-plus-tiny-engine-core` 的 `META-INF/tiny-engine/sql/manifest.properties` 下登记。宿主可以：

1. 读取 manifest 中对应数据库的 SQL 文件；
2. 通过运维流程初始化数据库；
3. 在受信测试环境中显式启用 `fw.plus.tiny-engine.sql.auto-initialize=true`。

不要直接复制 `tiny-engine-backend-java` 的 Docker Compose 拓扑到 starter 运行时；原仓库部署文件只作为部署参考。

## 用户体系 SPI

Core 提供中立用户体系接口，具体登录态和权限由宿主实现：

- `TinyEngineAuthenticationProvider`
- `TinyEngineAccountProvider`
- `TinyEngineTenantProvider`
- `TinyEnginePermissionEvaluator`
- `TinyEngineUserSystemService`

Magic-API、Sa-Token、Spring Security、Keycloak、MaxKey 或业务 IAM 都可以通过这些 SPI 对接。TinyEngine Core 不绑定具体 token、SSO、密码散列或第三方 SDK。

## WebFlux 说明

TinyEngine Core 当前是 MyBatis-Plus/JDBC 阻塞实现。WebFlux Controller 会通过 TinyEngine 专用 reactive executor 把阻塞调用调度到专用线程池，避免在 Reactor Netty 事件循环中直接执行数据库或文件系统访问。

可配置项：

```yaml
fw:
  plus:
    tiny-engine:
      webflux:
        scheduler:
          thread-count: 8
          queue-capacity: 1024
          timeout: 30s
```

## 前端资源

默认打包产物包含：

- `/tiny-engine/index.html`
- `/tiny-engine/preview.html`
- `/tiny-engine/img/defaultAvator.png`

你也可以选择外部部署 TinyEngine 前端：

- 独立部署 TinyEngine 前端到 Nginx/CDN；
- 通过网关把 `/tiny-engine/` 转发到前端服务；
- 让前端通过同源或网关访问本 starter 暴露的 TinyEngine 后端 API。

`designer-demo` 是打包使用的上游演示宿主壳；`mockServer` 是上游本地开发 mock 后端，不进入 starter JAR。

## AI 配置

TinyEngine 官方 Robot 调用同源官方接口：

- `POST /app-center/api/chat/completions`
- `POST /app-center/api/ai/chat`
- `POST /app-center/api/encrypt-key`
- `POST /app-center/api/ai/search`

示例工程不会写入模型地址、模型名或 API Key。使用时在 Robot 设置里添加 OpenAI-compatible 服务，例如：

```text
Base URL: https://your-openai-compatible.example/v1
Model: your-model-name
API Key: sk-your-placeholder
```

如果需要兼容官方 `EKEY_` 加密 Key，可通过环境变量或配置提供 SM4 Key：

```yaml
fw:
  plus:
    tiny-engine:
      ai:
        official:
          sm4-key: ${TINY_ENGINE_SM4_KEY:}
```

也可以设置环境变量 `SM4KEY`。示例值必须替换为实际 16 字节 Key 的 Base64 编码；不要把真实 API Key 写入配置文件。

## Magic-API 入口

如果需要在 Magic-API 控制台状态栏打开 TinyEngine，额外引入：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-magicapi-plugin-tiny-engine</artifactId>
	<version>${fw-plus.version}</version>
</dependency>
```

插件只注册一个 `statusBarItems` 入口，不注册 Magic-Script 模块、资源类型或数据源。

默认打开同源 `/tiny-engine/`。如果前端部署在自定义路径，可以配置 Magic 插件属性：

- `pluginData.attributes.path`：同源路径，例如 `/lowcode/tiny-engine/`。
- `pluginData.attributes.url`：外部完整地址，例如 `https://tiny.example.com/designer/`。

`pluginData.attributes.url` 优先级高于 `pluginData.attributes.path`。
