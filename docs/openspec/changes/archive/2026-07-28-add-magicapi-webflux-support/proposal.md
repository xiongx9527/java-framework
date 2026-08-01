## Why

当前 fw-plus 的 Magic-API 扩展控制器、权限过滤器和基础自动配置主要绑定在 Servlet Web 环境。Servlet 环境应继续进入目前已有的 MVC/Servlet 实现路径；WebFlux 环境则需要进入新的适配改造路径，避免 Reactive 应用加载 `MagicAPIController`、`AuthConfiguration`、`FilterRegistrationBean`、MVC 映射等 Servlet-only 入口后启动失败或管理接口不可用。

## What Changes

- 保持 Servlet 环境继续进入目前已有的 Magic-API Servlet/MVC 自动配置、controller、filter 和插件 controller 注册路径。
- 新增 WebFlux 环境专用的 Magic-API controller/管理接口适配路径，使 WebFlux 应用可以访问控制台 option、资源 reload、数据源测试、数据源选项等管理接口。
- 梳理并隔离 Magic-API 的 Servlet-only 自动配置、控制器注册和过滤器注册，确保 WebFlux 环境不会创建 Servlet-only bean。
- 必要时为 WebFlux controller/handler 提供轻量请求适配，使管理接口能复用现有 Magic-API 配置、权限和资源服务。
- 增加覆盖 Servlet 继续走现有路径、WebFlux 进入新路径、以及两套路径互不污染的测试。

## Capabilities

### New Capabilities

- `magicapi-webflux-controller-servlet-isolation`: Magic-API controller/管理接口在 WebFlux 应用中的新增适配路径，以及 Servlet 环境继续走现有路径的隔离契约。

### Modified Capabilities

- 无。

## Impact

- 主要影响 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 中 Magic-API controller、auth/filter、自动配置条件和相关测试。
- 可能影响 `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure` 与 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure` 中 Magic-API 自动配置的 Servlet/WebFlux 条件边界。
- 本变更不承诺完整动态 API runtime、所有插件 Web 入口或脚本执行链的 Reactive 化；这些如需支持，应作为后续独立 change 处理。
- 不得引入 Java 9+ API，不得修改 Magic-API 2.2.2 及内部 Nexus 依赖版本。
