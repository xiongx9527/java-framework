# TinyEngine Starter 使用说明

`fw-plus-tiny-engine-spring-boot-starter` 是 TinyEngine 后端 starter，只提供应用、页面、物料、模型、用户体系等后端 API 和 Core 能力。

它不内置 TinyEngine 前端源码、`designer-demo`、`mockServer` 或构建后的 `dist`。TinyEngine 前端需要外部部署，可以放在同源 `/tiny-engine/`、网关路径或独立域名。

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

## 外部前端部署

本 starter 不发布 `/tiny-engine/` 静态资源。你可以：

- 独立部署 TinyEngine 前端到 Nginx/CDN；
- 通过网关把 `/tiny-engine/` 转发到前端服务；
- 让前端通过同源或网关访问本 starter 暴露的 TinyEngine 后端 API。

`designer-demo` 是上游演示宿主壳，`mockServer` 是上游本地开发 mock 后端；二者都不进入 starter JAR。

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
