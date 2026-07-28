# Brainstorm Summary

- Change: add-magicapi-webflux-support
- Date: 2026-07-27

## 确认的技术方案

采用“抽公共管理服务 + 双 Web 入口”方案：Servlet 环境继续进入目前已有 Magic-API Servlet/MVC 自动配置、controller、filter 和 plugin controller registration 路径；WebFlux 环境进入新的 controller/管理接口改造路径。WebFlux 新路径只覆盖 Magic-API 管理接口，不承诺完整动态 API runtime。

## 关键取舍与风险

- 保持 Servlet 零语义迁移，降低已有 MVC 应用回归风险。
- WebFlux 通过新增入口复用公共管理服务，避免复制 option、reload、datasource test 等业务逻辑。
- 授权逻辑当前依赖 `MagicHttpServletRequest` 和 Magic-API 用户上下文，需要抽取权限 helper 或提供轻量 WebFlux request adapter。
- 完整动态 API runtime 不在本 change 中实现，需要在设计、任务和验证中持续标明边界。

## 测试策略

- Servlet 环境测试：确认仍进入当前 Magic-API Servlet/MVC path，controller/filter/plugin registration 行为不变。
- WebFlux 环境测试：确认 enabled 时注册新管理接口路径，disabled 时不注册。
- WebFlux-only 隔离测试：确认不会创建 `FilterRegistrationBean`、`RequestMappingHandlerMapping` 依赖和 Servlet-only controller registration bean。
- 管理接口响应测试：确认 option list、datasource options、datasource test、resource reload 响应形状与 Servlet 保持一致。

## Spec Patch

无。当前 OpenSpec 已按用户确认边界更新，无需再回写 delta spec。

## Build Isolation Preference

用户已确认 build 阶段需要创建 worktree。进入 build 阶段后应在联合工作方式决策中采用 worktree 隔离，并确保当前 OpenSpec/Design/Plan 产物在 worktree 中可访问。
