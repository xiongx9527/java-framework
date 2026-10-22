# Tasks: mcp-websocket-custom-auth

- [x] 1. 在 `fw-plus-auth-core` 定义业务可实现的自定义授权扩展接口（如 `CustomAuthService`/复用核心 `AuthService`），并扩展 `InstanceAuthRegistry` 支持按自定义 key 注册/解析业务 `AuthService` <!-- comet-task:eb62f9fa-7c9f-42e4-bd52-8d5659f165d3 -->
- [x] 2. 在 mcp 自动配置中装配自定义 provider：配置 `auth.provider=<自定义key>` 时经 `McpAuthService.adapt(registry.resolve(...))` 使用业务实现 <!-- comet-task:6d533758-4b43-49e9-8a29-996cb58cf81f -->
- [x] 3. 在 websocket 自动配置中装配自定义 provider：`WsAuthSupport` 配置 `fw.plus.ws.auth.provider=<自定义key>` 时走业务实现 <!-- comet-task:e9308ec9-b7b2-414e-b65d-49b0944c61c1 -->
- [x] 4. 保证内置 SaToken provider 行为不变；自定义 provider 缺失/失败时 fail-closed（拒绝） <!-- comet-task:99f191bb-ade4-47dd-8dac-80cec72bf9b4 -->
- [x] 5. 提供使用示例或测试：验证业务自定义实现在 mcp server 与 websocket 两端均生效，同时内置 provider 不变 <!-- comet-task:a9f8e881-59d5-47af-9141-ab0cb3308c4e -->
