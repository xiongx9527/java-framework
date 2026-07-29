## Context

fw-plus 当前 Magic-API 基础集成集中在 Boot 自动配置层，并通过 `MagicAPIBaseAutoConfiguration` 导入 `MagicAPIController`、`AuthConfiguration`、基础模块、资源服务、数据源动态注册和插件自动配置。该配置使用 `@ConditionalOnWebApplication(type = SERVLET)`，其中部分 bean 明确依赖 Servlet/MVC 类型，例如 `FilterRegistrationBean`、Spring MVC controller mapping、`MagicHttpServletRequest` 等。

用户已明确边界：Servlet 环境进入目前已有路径；WebFlux 环境进入新的改造路径。因此本设计不覆盖完整动态 API runtime Reactive 化，也不承诺所有 Magic-API 插件 Web 入口都支持 WebFlux。本次重点是让 WebFlux 应用可以安全装配必要的 Magic-API 管理接口，同时把 Servlet-only 配置继续留在现有路径中，保证 MVC 应用不回归。

## Goals / Non-Goals

**Goals:**

- 保持 Servlet/MVC 环境继续使用目前已有 Magic-API 自动配置、controller、filter 和插件 controller registration 路径。
- 为 WebFlux 环境新增独立的 Magic-API controller/管理接口入口或适配，覆盖控制台 option、资源 reload、单资源 reload、数据源选项、数据源测试等现有管理接口。
- 隔离 `AuthConfiguration`、Servlet filter registration、MVC controller registration 等 Servlet-only 入口，避免 WebFlux 应用创建不适用 bean。
- 补充 Boot、Cloud、AI 层与 Magic-API 相关的 Servlet/WebFlux 条件装配测试，验证两套路径互不污染。

**Non-Goals:**

- 不实现完整 Magic-API 动态 API 执行链的 WebFlux runtime。
- 不将所有插件 Web controller 或前端插件入口 Reactive 化。
- 不修改 Magic-API 2.2.2 依赖版本、脚本执行核心或外部 `resources/` 内容。
- 不改变 Servlet/MVC 环境下已有管理接口的路径、权限、返回结构和 bean 装配语义。

## Decisions

1. **Servlet 继续走现有路径，WebFlux 走新增路径。**
   - 选择：保留当前 Servlet `MagicAPIBaseAutoConfiguration` 及其 MVC controller/filter 入口；新增 WebFlux 专用配置承载管理接口适配。
   - 理由：Servlet 路径已经可用，直接迁移或混改会增加回归风险；WebFlux 需要不同 Web 栈入口，应独立演进。
   - 替代方案：把现有 Servlet 配置改成通用配置。该方案会混合 Servlet 与 Reactive 条件，边界不够清晰。

2. **先做 controller/管理接口适配，不扩大到完整 runtime。**
   - 选择：本 change 只覆盖 Magic-API 管理控制器及其必要的请求/权限/资源服务适配。
   - 理由：完整动态 API runtime 涉及请求执行链、脚本阻塞调度、插件入口和响应写回，范围明显更大；先处理管理接口可以更快解决 WebFlux 装配与控制台基础可用问题。
   - 替代方案：一次性实现完整 WebFlux runtime。该方案风险高，且容易引入 Servlet 兼容性回归。

3. **Servlet-only 配置显式保留在 Servlet 条件下。**
   - 选择：`FilterRegistrationBean`、MVC controller mapping、Servlet request/response 相关 bean 继续由 Servlet 条件保护；WebFlux 路径新增独立配置或 handler。
   - 理由：Servlet 与 Reactive Web 栈生命周期不同，混用 bean 会造成启动失败或隐性行为差异。
   - 替代方案：在原配置类中用大量条件分支混合处理。该方案可读性和测试边界较差。

4. **WebFlux 管理接口复用现有服务和响应对象。**
   - 选择：WebFlux controller/handler 调用现有 `MagicConfiguration`、`MyDefaultMagicResourceService`、`MagicJdbcClassOptionProvider`、`JsonBean` 等服务，必要时只新增轻量 request adapter。
   - 理由：管理接口本身以配置、权限和资源服务为主，复用现有服务能保持返回结构一致并降低改动范围。

5. **Cloud/AI 只调整条件边界，不复制管理接口实现。**
   - 选择：Boot 层承载 controller/Servlet 隔离实现；Cloud/AI 层只在必要时补充 WebFlux 条件装配，继续导入自身 Magic-API 插件配置。
   - 理由：保持分层职责清晰，避免多个库层重复维护 controller 适配。

## Risks / Trade-offs

- [Risk] 管理接口中的部分方法仍直接依赖 `MagicHttpServletRequest` 授权上下文。 → Mitigation: 为这些方法提供 WebFlux request adapter 或把授权检查提取为可复用 helper，并用 WebFlux 测试覆盖。
- [Risk] WebFlux 下控制台管理接口可访问但动态 API runtime 仍不可用，用户可能误解支持范围。 → Mitigation: 在 spec、tasks 和必要文档中明确本 change 非目标，后续 runtime 支持另立 change。
- [Risk] 拆分 Servlet/WebFlux 条件时影响现有 MVC 装配。 → Mitigation: 保留现有 Servlet 测试并新增 Servlet 回归断言，确认 bean、路径和返回结构不变。
- [Risk] Cloud/AI 层依赖 Boot Magic-API 条件，WebFlux 环境下装配边界不清。 → Mitigation: 通过 ApplicationContextRunner 分别验证 Servlet、Reactive 和 disabled 场景。

## Migration Plan

1. 保留现有 Servlet Magic-API 配置入口，使 Servlet 环境继续进入目前路径。
2. 新增 WebFlux 管理接口配置入口，使 WebFlux 环境进入新的改造路径。
3. 为 Servlet-only filter、MVC controller 注册和相关自动配置补充明确条件。
4. 为 WebFlux 管理接口补充必要 request adapter 与授权/资源服务调用。
5. 补充测试并回归现有 Servlet Magic-API 测试。

回滚策略：移除新增 WebFlux controller/handler 和条件装配调整，保留原 Servlet 配置即可恢复到当前行为。

## Open Questions

- 无阻塞问题；完整动态 API runtime 是否支持 WebFlux 不在本 change 范围内。
