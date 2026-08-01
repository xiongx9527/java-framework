# Brainstorm Summary

- Change: fix-magicapi-plugin-sample-integration
- Date: 2026-07-27

## 已确认事实

- 整体测试宿主固定为 `fw-plus-magicapi-spring-boot-starter-sample`。
- 当前清单覆盖 Boot 25 个和 AI 2 个 Magic-API 插件；classpath 契约测试已转绿。
- 真实 sample 启动已越过 AI 自动配置元数据问题，当前阻断是 `magicResourceService` 与 MQTT Client resource storage 的循环依赖。
- sample 启动类直接使用 `@EnableApolloConfig`，默认配置还包含 Apollo、Redis、Mongo 等外部地址，无法形成离线可重复的启动证据。
- Magic-API 2.2.2 的 `DefaultMagicResourceService` 构造器会为全部 storage 同步执行 `setMagicResourceService(this)`；MQTT Client 是同类插件中唯一在 storage Bean 创建时提前反向注入服务的实现。
- `apollo.bootstrap.enabled=false` 不能关闭 `@EnableApolloConfig` 的直接 import 链；框架的 Apollo 变更适配器还需通过 `fw.plus.config.event.apollo-enabled=false` 独立关闭。
- 当前 sample 自动化测试只验证 27 个 artifact 的 `pom.properties` 在 classpath，尚未覆盖完整 Spring 上下文、真实 sample 进程或 HTTP 入口。
- 不升级 Magic-API 2.2.2，不修改内部 Nexus 坐标，不使用 Java 9+ API。

## 确认的技术方案

用户选择方案 A：保留单一真实 sample 宿主；MQTT Client storage 改为无参创建并依赖 Magic-API 核心统一回填；移除 sample 主类上冗余的 `@EnableApolloConfig`，由现有 `apollo.bootstrap.*` 自动配置控制；增加独立 `all-plugins` profile 和 classpath、Spring 上下文、真实进程、HTTP 入口四层验证。

### 已确认的架构与数据流

- `fw-plus-magicapi-spring-boot-starter-sample` 是唯一整体宿主，运行时显式包含 Boot 25 个和 AI 2 个插件。
- sample 主类移除冗余 `@EnableApolloConfig`；默认配置仍由 Apollo bootstrap 控制，`all-plugins` profile 同时关闭 Apollo bootstrap 与框架 Apollo 事件适配器。
- `all-plugins` 使用空的临时 Magic-API 资源目录，不加载现有外部数据源演示资源。
- MQTT Client storage 无参创建；`MagicResourceService` 收集全部 storage 后由 Magic-API 2.2.2 构造器统一回填，动态注册器在后续应用事件中读取资源。
- 不启用循环依赖兼容开关，不增加 `@Lazy`、代理或新的公共 API。

## 关键取舍与风险

- “全部插件通过”需区分离线装配通过与依赖真实中间件的业务读写通过；后者需要额外基础设施和凭证，不适合作为默认可重复测试。
- 仅检查 artifact 存在不足以证明联合装配；必须保留真实 ApplicationContext 和 sample 进程启动验证。
- 测试配置不能掩盖插件的默认装配冲突，也不能改变生产插件公开 API 或运行时语义。
- 不采用 `spring.main.allow-circular-references=true`、`@Lazy` 或额外 BeanPostProcessor 掩盖/重复 Magic-API 已有统一回填机制。
- `all-plugins` profile 使用空的临时资源目录，避免加载仓库内指向真实服务的 datasource 演示资源。
- 用户已确认采用离线严格整体验证：覆盖全部 27 个插件的 classpath、自动配置、完整 Spring 上下文、真实 sample 进程和 HTTP 入口；不执行真实外部中间件的数据读写。

## 测试策略（候选）

- 插件模块与自动配置定向单测。
- 27 个插件 artifact/classpath 与自动配置元数据契约测试。
- MQTT Client 最小循环依赖 RED/GREEN 测试，并断言 storage 最终由核心回填同一 `MagicResourceService`。
- 使用真实 `MagicapiSampleApplication` 的完整 Spring ApplicationContext 装配测试，断言 Apollo 两条访问链均关闭。
- 按 Core -> Boot -> Cloud/AI -> sample 的分层 Maven 构建。
- 启动真实 sample 进程，检查健康状态、Magic-API Web/宿主入口及关键插件装配证据。

上述架构、失败处理与测试矩阵已由用户分段确认。

## Spec Patch

候选为无；当前变更不新增 capability，也不改变公开验收契约。待并行调查完成后复核。
