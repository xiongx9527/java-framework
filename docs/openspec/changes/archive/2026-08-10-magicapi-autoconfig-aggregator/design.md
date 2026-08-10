## Context

`fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi` 包下当前有 3 个自动配置类独立注册在 `spring.factories` / `AutoConfiguration.imports` 中：

- `MagicAPIBaseAutoConfiguration`：`@ConditionalOnWebApplication(type = SERVLET)`，承载 SERVLET 模式下 MagicAPI 的全部 Bean/字段/`@Import`（MagicAPIController、AuthConfiguration、UidModuleConfiguration、LockModuleConfiguration、MyBatisFlexConfiguration、MybatisPlusModuleConfiguration、TemplateModuleConfiguration、MagicApiPluginAutoConfiguration、DifyModuleConfiguration、SseModuleConfiguration、TraceModuleConfiguration、MagicAPIDbTxConfiguration、MagicAPISeataConfiguration），`implements ApplicationRunner`
- `MagicAPIDynamicDataSourceAutoConfiguration`：`@ConditionalOnWebApplication(type = SERVLET)`，仅注册 `MagicDynamicDataSource` Bean，带 `@AutoConfigureBefore(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")`
- `webflux.MagicAPIWebFluxAutoConfiguration`：`@ConditionalOnWebApplication(type = REACTIVE)`，承载 REACTIVE 模式下 MagicAPI 的全部 Bean，`implements WebFluxConfigurer`

项目主流聚合模式是用 `@Import` 在顶层 AutoConfiguration 聚合同模块子配置（`MybatisAutoConfiguration`、`MpAutoConfiguration`、`MqttAutoConfiguration`、`MateAutoConfiguration`、`WebAutoConfiguration`、`JdbcAutoConfiguration`、`ReportAutoConfiguration`、`TemplateAutoConfiguration`、`NoSqlAutoConfiguration`、`UIDAutoConfiguration` 均如此）。

核心矛盾：当前 `MagicAPIBaseAutoConfiguration` 带 `@ConditionalOnWebApplication(SERVLET)`，无法直接作为环境无关聚合入口——若把 `MagicAPIWebFluxAutoConfiguration`（REACTIVE）`@Import` 进来，REACTIVE 应用中 Base 因 SERVLET 条件不满足不加载，WebFlux 配置也无法注册。

## Goals / Non-Goals

**Goals:**

- 新建 `MagicAPIWebMvcAutoConfiguration` 承载当前 Base 的 SERVLET 特定内容。
- `MagicAPIBaseAutoConfiguration` 重构为环境无关聚合入口。
- `spring.factories` / `AutoConfiguration.imports` 中 magicapi 注册从 3 行收敛为 1 行。
- 保持最终注册的 Bean 集合、条件求值、运行时行为与改动前等价（SERVLET 应用加载 WebMvc+DynamicDataSource，REACTIVE 应用加载 WebFlux）。

**Non-Goals:**

- 不重命名、不移动 `MagicAPIWebFluxAutoConfiguration` 或 `MagicAPIDynamicDataSourceAutoConfiguration`。
- 不改变 `MagicAPIWebFluxAutoConfiguration` / `MagicAPIDynamicDataSourceAutoConfiguration` 的条件注解或 Bean 定义。
- 不调整 `MagicAPIWebMvcAutoConfiguration` 内部从 Base 迁移过来的 `@Import` 列表（保持原样）。
- 不引入 delta spec（纯注册方式重构，无 spec 级行为变化）。

## Decisions

### D1：新建 `MagicAPIWebMvcAutoConfiguration` 承载 SERVLET 内容，而非直接改 Base

**选择**：新建 `MagicAPIWebMvcAutoConfiguration`，把当前 `MagicAPIBaseAutoConfiguration` 的全部内容（`@ConditionalOnWebApplication(SERVLET)` + 字段 + Bean + `@Import` + `implements ApplicationRunner`）整体迁移过去；`MagicAPIBaseAutoConfiguration` 重构为空体聚合入口。

**理由**：

- 当前 Base 的所有内容都是 SERVLET 特定的（`RequestMappingHandlerMapping` 字段、`MagicAPIController` 等 `@Import`），没有环境无关的可复用部分。
- 直接去掉 Base 的 `@ConditionalOnWebApplication(SERVLET)` 会导致 REACTIVE 应用中 SERVLET 特定 Bean 初始化失败。
- 新建 `MagicAPIWebMvcAutoConfiguration` 与现有 `MagicAPIWebFluxAutoConfiguration` 命名对称（WebMvc=SERVLET，WebFlux=REACTIVE），结构清晰。
- Base 重构为环境无关聚合入口后，可安全 `@Import` 三个子配置（WebMvc/WebFlux/DynamicDataSource），子配置各自带 `@ConditionalOnWebApplication` 条件，按应用类型自动激活。

### D2：`MagicAPIBaseAutoConfiguration` 的条件注解

**选择**：Base 保留 `@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")` + `@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)`，去掉 `@ConditionalOnWebApplication(SERVLET)` 和 `@EnableConfigurationProperties`。

**理由**：

- `@ConditionalOnClass` 保证仅当 magicapi 原生 starter 在 classpath 时整个模块启用，是 3 个子配置共同的前置条件。
- `@ConditionalOnProperty` 是 3 个子配置共同的开关（`fw.plus.magicapi.enabled`）。
- `@ConditionalOnWebApplication(SERVLET)` 移到 `MagicAPIWebMvcAutoConfiguration` 和 `MagicAPIDynamicDataSourceAutoConfiguration`（后者已有），保证 SERVLET 特定内容只在 SERVLET 应用加载。
- `@EnableConfigurationProperties({DataSourceProperties.class, MagicJdbcClassOptionProperties.class})` 移到 `MagicAPIWebMvcAutoConfiguration`（`MagicAPIWebFluxAutoConfiguration` 已有自己的 `@EnableConfigurationProperties`）。

### D3：`@AutoConfigureBefore` 补偿 `MagicAPIDynamicDataSourceAutoConfiguration` 排序依赖

**选择**：`MagicAPIBaseAutoConfiguration` 加 `@AutoConfigureBefore(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")`。

**理由**：

- `MagicAPIDynamicDataSourceAutoConfiguration` 原有 `@AutoConfigureBefore(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")`，确保 `MagicDynamicDataSource` Bean 在原生 `MagicAPIAutoConfiguration` 之前注册。
- 改用 `@Import` 后，子配置的 `@AutoConfigureBefore` 失效（不走 `AutoConfigurationImportSelector`）。
- 在 `MagicAPIBaseAutoConfiguration` 上统一加 `@AutoConfigureBefore`，保证整体（含 WebMvc/WebFlux/DynamicDataSource）在原生 `MagicAPIAutoConfiguration` 之前处理。
- `MagicAPIBaseAutoConfiguration`（原 SERVLET 版）原本无此注解，是通过 `spring.factories` 注册顺序（L20 DynamicDataSource 在 L21 Base 之前）隐式保证；收敛为 1 行后需要显式 `@AutoConfigureBefore` 补偿。
- 用 `name` 字符串形式，避免对 magicapi 原生 starter 的编译期硬依赖。

### D4：`@Import` 列表顺序

**选择**：`@Import` 列表顺序为 `MagicAPIWebMvcAutoConfiguration` → `MagicAPIWebFluxAutoConfiguration` → `MagicAPIDynamicDataSourceAutoConfiguration`。

**理由**：

- `MagicAPIWebMvcAutoConfiguration`（SERVLET）和 `MagicAPIWebFluxAutoConfiguration`（REACTIVE）互斥，同一应用只会激活一个，顺序无功能影响；按 WebMvc → WebFlux 命名顺序排列，便于阅读。
- `MagicAPIDynamicDataSourceAutoConfiguration`（SERVLET）与 `MagicAPIWebMvcAutoConfiguration`（SERVLET）同在 SERVLET 应用激活，放最后；`MagicAPIDynamicDataSourceAutoConfiguration` 仅注册 `MagicDynamicDataSource` Bean，`MagicAPIWebMvcAutoConfiguration` 通过 `@ConditionalOnMissingBean(MagicDynamicDataSource.class)` 等条件兜底，顺序差异由条件注解保护。

## Risks / Trade-offs

- **[风险] `MagicAPIDynamicDataSourceAutoConfiguration` 的 `@AutoConfigureBefore` 失效导致 `MagicDynamicDataSource` 注册太晚** → 通过 D3 在 `MagicAPIBaseAutoConfiguration` 上加 `@AutoConfigureBefore` 统一补偿；`MagicAPIWebMvcAutoConfiguration` 的 `magicDynamicRegistry` Bean 用 `@ConditionalOnMissingBean` 等条件兜底。verify 阶段通过编译 + 现有测试验证。
- **[风险] `MagicAPIBaseAutoConfiguration` 不再 `implements ApplicationRunner`，下游显式引用 `run` 方法** → 属于 BREAKING 变更，已在 proposal 标注；内部自动配置实现不对外暴露 public API，通过 autoconfigure 机制使用的项目无感知。
- **[取舍] 3 个子配置不再单独出现在 `spring.factories`** → 收敛为 1 行是本次目标；`spring.autoconfigure.exclude` 仍对 `@Import` 的类生效，下游排除能力不受影响。
