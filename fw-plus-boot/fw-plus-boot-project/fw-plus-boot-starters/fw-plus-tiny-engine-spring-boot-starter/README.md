# fw-plus TinyEngine Spring Boot Starter

`fw-plus-tiny-engine-spring-boot-starter` 提供 TinyEngine 后端能力，并内置 TinyEngine 前端工程源码作为可选构建输入。

这个 starter 的职责边界是：

- 提供 `fw-plus-tiny-engine-core` 的应用、页面、物料、模型、用户等后端接口能力。
- 提供 Spring MVC 与 Spring WebFlux 两套 Controller 自动配置入口。
- 提供中立用户体系 SPI，由宿主应用或 Magic-API、Sa-Token、Spring Security、IAM 等外部工具实现登录态与权限。
- 在源码目录 `src/console` 中按 Magic-API 插件的模式携带 TinyEngine 前端工程源码。
- 在 Maven `prepare-package` 阶段执行 `pnpm install` 与 `pnpm run build:prod`，并把 `designer-demo/dist` 复制到 jar 的 `META-INF/resources/tiny-engine/`，对应运行入口 `/tiny-engine/`。

内置前端源码保留 `designer-demo/`、`packages/`、`scripts/`、`patches/` 以及根目录必要构建配置。以下内容不会放入 starter：

- `mockServer/`：本地开发 mock 后端。
- `docs/`：上游文档体积较大，不属于运行或构建 TinyEngine 前端的必要内容。
- `node_modules/`、`dist/`、`coverage/`、`.env.local`、`.DS_Store` 等本机、构建或生成产物。

宿主应用必须按运行模式显式添加以下依赖之一：

- Spring MVC：`org.springframework.boot:spring-boot-starter-web`
- Spring WebFlux：`org.springframework.boot:spring-boot-starter-webflux`

Magic-API 的 TinyEngine 插件只负责在 Magic 控制台 `statusBarItems` 增加入口。默认会跳转到宿主同源的 `/tiny-engine/`；业务也可以通过插件配置覆盖为独立域名或网关地址。

Maven `package` 需要 Node 与 pnpm；`compile` 不触发前端构建。构建不会读取 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 或 `/Users/xiongxiao/Downloads/TinyEngine-develop` 等外部前端源码路径；这些源码已经作为 starter 内部资源保存。
