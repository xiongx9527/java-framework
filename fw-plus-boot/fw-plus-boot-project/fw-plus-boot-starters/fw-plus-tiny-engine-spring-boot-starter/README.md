# fw-plus TinyEngine Spring Boot Starter

`fw-plus-tiny-engine-spring-boot-starter` 只提供 TinyEngine 后端能力，不内置 TinyEngine 前端源码、`designer-demo`、`mockServer` 或构建后的 `dist` 静态资源。

这个 starter 的职责边界是：

- 提供 `fw-plus-tiny-engine-core` 的应用、页面、物料、模型、用户等后端接口能力。
- 提供 Spring MVC 与 Spring WebFlux 两套 Controller 自动配置入口。
- 提供中立用户体系 SPI，由宿主应用或 Magic-API、Sa-Token、Spring Security、IAM 等外部工具实现登录态与权限。
- 不负责构建或发布 TinyEngine 前端。前端应由独立工程或外部静态站点部署，并请求本 starter 暴露的后端 API。

上游 `designer-demo` 是 TinyEngine 的演示宿主壳，`mockServer` 是本地开发 mock 后端；二者都不属于 fw-plus TinyEngine starter 的后端能力边界。

宿主应用必须按运行模式显式添加以下依赖之一：

- Spring MVC：`org.springframework.boot:spring-boot-starter-web`
- Spring WebFlux：`org.springframework.boot:spring-boot-starter-webflux`

Magic-API 的 TinyEngine 插件只负责在 Magic 控制台 `statusBarItems` 增加入口。没有内置前端时，这个入口应跳转到宿主配置或网关暴露的 TinyEngine 前端地址，例如 `/tiny-engine/` 或独立域名。

Maven 构建不需要 Node、pnpm，也不会读取 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 等外部前端源码路径。
