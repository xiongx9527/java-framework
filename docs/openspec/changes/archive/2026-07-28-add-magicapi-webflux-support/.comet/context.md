# Comet Design Handoff

- Change: add-magicapi-webflux-support
- Phase: design
- Mode: compact
- Context hash: 93487fd0742a3900e66a30f4eaa91967cce8e3d66a0884a9f0cad92debf46e09

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-magicapi-webflux-support/proposal.md

- Source: openspec/changes/add-magicapi-webflux-support/proposal.md
- Lines: 1-28
- SHA256: b93a9fa2ee4c38d2ceff94b30a1db807bc5ece1c65fc2837474e2f0094c287ab

```md
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

```

## openspec/changes/add-magicapi-webflux-support/design.md

- Source: openspec/changes/add-magicapi-webflux-support/design.md
- Lines: 1-67
- SHA256: e192e645cfd987122419ef3619dc82ae979837fd10bea975beab0915987cf1fa

```md
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

```

## openspec/changes/add-magicapi-webflux-support/tasks.md

- Source: openspec/changes/add-magicapi-webflux-support/tasks.md
- Lines: 1-33
- SHA256: 41e85ccf46dfa2ac67474593a19c5225ac77185103b2878733c24e721f6f0abe

```md
## 1. 现状梳理与范围收敛

- [ ] 1.1 盘点 `MagicAPIController` 中 option、resource reload、single resource reload、datasource options、datasource test 等管理接口的依赖和返回结构。
- [ ] 1.2 盘点 `AuthConfiguration`、`AuthFilter`、Servlet filter registration、MVC controller registration 等 Servlet-only 依赖点。
- [ ] 1.3 盘点 Boot、Cloud、AI 层 Magic-API 自动配置条件，确认 Servlet 环境继续走目前路径、WebFlux 环境进入新路径所需的边界。

## 2. Controller 与 WebFlux 管理接口

- [ ] 2.1 提取或复用 Magic-API 管理接口中的公共服务逻辑，避免 Servlet 与 WebFlux 两套入口复制业务处理。
- [ ] 2.2 新增 WebFlux 可用的 Magic-API 管理接口入口，覆盖 option list、resource reload、single resource reload、datasource options 和 datasource test。
- [ ] 2.3 为 WebFlux 管理接口补齐必要的请求/授权适配，使权限检查可以使用现有 Magic-API authorization interceptor。
- [ ] 2.4 确认 WebFlux 管理接口返回 `JsonBean`、`MagicJdbcClassOptions` 等 payload 形状与 Servlet controller 保持一致。

## 3. Servlet 相关隔离与兼容

- [ ] 3.1 保持 Servlet/MVC 环境继续进入目前已有 Magic-API 自动配置、controller、filter 和 plugin controller registration 路径。
- [ ] 3.2 为 Magic-API Servlet-only controller、filter、MVC mapping 和 plugin controller registration 补充明确 Servlet 条件。
- [ ] 3.3 确保 WebFlux-only 应用上下文不会创建 `FilterRegistrationBean`、Servlet request/response 或 Spring MVC mapping 依赖的 Magic-API bean。
- [ ] 3.4 仅在必要处调整 Cloud/AI Magic-API 自动配置条件，避免复制 Boot 层 controller 适配实现。

## 4. 测试与验证

- [ ] 4.1 新增 Servlet 环境测试，断言仍进入当前 Magic-API Servlet/MVC 路径，且 controller/filter/plugin registration 行为不变。
- [ ] 4.2 新增 WebFlux 应用上下文测试，覆盖 Magic-API enabled/disabled 下管理接口 handler 注册情况。
- [ ] 4.3 新增 WebFlux 管理接口测试，覆盖 option list、datasource options、datasource test 和资源 reload 授权边界。
- [ ] 4.4 新增 WebFlux-only 上下文测试，断言不创建 Servlet filter registration、MVC mapping 依赖和 Servlet-only controller registration bean。
- [ ] 4.5 运行受影响 Maven 模块定向测试，并记录完整动态 API runtime 不属于本 change 验收范围。

## 5. 收尾

- [ ] 5.1 检查新增代码保持 JDK 1.8 兼容，不使用 Java 9+ API。
- [ ] 5.2 检查 Magic-API 2.2.2 与内部 Nexus 依赖版本未被修改。
- [ ] 5.3 更新必要文档或注释，说明 Servlet 进入目前路径、WebFlux 进入新改造路径，且本 change 不包含完整 WebFlux 动态 API runtime。

```

## openspec/changes/add-magicapi-webflux-support/specs/magicapi-webflux-controller-servlet-isolation/spec.md

- Source: openspec/changes/add-magicapi-webflux-support/specs/magicapi-webflux-controller-servlet-isolation/spec.md
- Lines: 1-56
- SHA256: 25710ac6d79542930f7a8813e8c8db9b7fde4cc3f525c7a5db40581b12f06893

```md
## ADDED Requirements

### Requirement: Servlet environment uses existing Magic-API path
The system MUST keep Servlet web applications on the existing Magic-API Servlet/MVC auto-configuration path.

#### Scenario: Servlet application enters current implementation
- **WHEN** a Spring Boot Servlet application starts with Magic-API enabled
- **THEN** it uses the current Magic-API Servlet/MVC auto-configuration, controller mappings, auth filter registration, and plugin controller registration path

#### Scenario: Servlet behavior remains compatible
- **WHEN** a Servlet application calls existing Magic-API management endpoints
- **THEN** paths, permission checks, response structures, and plugin controller registration behavior remain compatible with the current implementation

### Requirement: WebFlux environment uses new management endpoint path
The system SHALL route Spring WebFlux applications into a new Magic-API management endpoint adaptation path when Magic-API is enabled and required Magic-API classes are present.

#### Scenario: WebFlux management endpoints are registered through new path
- **WHEN** a Spring Boot WebFlux application starts with Magic-API enabled
- **THEN** the Magic-API management endpoints for console options, resource reload, single resource reload, datasource options, and datasource test are registered through WebFlux-compatible handling rather than the Servlet/MVC controller path

#### Scenario: WebFlux management endpoints are disabled
- **WHEN** a Spring Boot WebFlux application starts with `fw.plus.magicapi.enabled=false`
- **THEN** Magic-API WebFlux management endpoint handlers are not registered

### Requirement: Controller response compatibility
The system MUST keep WebFlux Magic-API management endpoint responses compatible with the equivalent Servlet controller responses.

#### Scenario: Option list response keeps existing shape
- **WHEN** a WebFlux client requests the Magic-API option list endpoint
- **THEN** the response uses the same `JsonBean` result shape and option values as the existing Servlet controller contract

#### Scenario: Datasource option response keeps existing shape
- **WHEN** a WebFlux client requests datasource class options with valid authorization
- **THEN** the response returns the same `MagicJdbcClassOptions` payload shape as the existing Servlet controller contract

#### Scenario: Datasource test response keeps existing shape
- **WHEN** a WebFlux client submits datasource test input
- **THEN** the response returns the same success or error `JsonBean<String>` shape as the existing Servlet controller contract

### Requirement: Servlet-only bean isolation
The system MUST prevent Servlet-only Magic-API beans from being created in a WebFlux-only application context.

#### Scenario: WebFlux context excludes Servlet filter registration
- **WHEN** a Spring Boot WebFlux application starts without a Servlet web application context
- **THEN** Magic-API auto-configuration does not create Servlet `FilterRegistrationBean` instances for Magic-API auth filtering

#### Scenario: WebFlux context excludes MVC mapping dependencies
- **WHEN** a Spring Boot WebFlux application starts without Spring MVC infrastructure
- **THEN** Magic-API auto-configuration does not require Spring MVC `RequestMappingHandlerMapping` or Servlet-only controller registration beans

### Requirement: Scope boundary for dynamic API runtime
The system MUST NOT claim full WebFlux dynamic API runtime support as part of this change.

#### Scenario: Dynamic API runtime remains out of scope
- **WHEN** this change is implemented and verified
- **THEN** acceptance is based on Magic-API management/controller endpoints and Servlet/WebFlux path isolation, not on complete WebFlux support for arbitrary dynamic script API execution

```
