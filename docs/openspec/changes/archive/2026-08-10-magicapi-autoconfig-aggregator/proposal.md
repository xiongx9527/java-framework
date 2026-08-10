## Why

`fw-plus-boot-autoconfigure` 中 MagicAPI 模块的 3 个自动配置类（`MagicAPIDynamicDataSourceAutoConfiguration`、`MagicAPIBaseAutoConfiguration`、`MagicAPIWebFluxAutoConfiguration`）各自独立注册在 `spring.factories` / `AutoConfiguration.imports` 中，缺乏统一的模块入口。这与 fw-plus 中其他模块的聚合模式（`MybatisAutoConfiguration` 聚合 5 个 mybatis 子配置、`MqttAutoConfiguration` 聚合 server/client、`MateAutoConfiguration` 聚合 easytrans/webmvc 等）不一致。

当前 `MagicAPIBaseAutoConfiguration` 自身带 `@ConditionalOnWebApplication(type = SERVLET)`，无法直接作为环境无关的聚合入口把 `MagicAPIWebFluxAutoConfiguration`（REACTIVE 模式）也 `@Import` 进来——否则在 REACTIVE 应用中 Base 不加载，WebFlux 配置也无法注册。

新建 `MagicAPIWebMvcAutoConfiguration` 承载当前 Base 的 SERVLET 特定内容，`MagicAPIBaseAutoConfiguration` 重构为环境无关的聚合入口，把 3 个子配置（WebMvc/WebFlux/DynamicDataSource）统一 `@Import`，使 `spring.factories` 中 MagicAPI 模块从 3 行收敛为 1 行，与项目主流聚合模式对齐。

## What Changes

- 新建 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIWebMvcAutoConfiguration`：
  - 内容 = 当前 `MagicAPIBaseAutoConfiguration`（`@ConditionalOnWebApplication(type = SERVLET)` + 所有字段/Bean/@Import/implements ApplicationRunner）
  - 即把当前 Base 的 SERVLET 特定实现整体迁移到 `MagicAPIWebMvcAutoConfiguration`
- 重构 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration` 为环境无关聚合入口：
  - 去掉 `@ConditionalOnWebApplication(type = SERVLET)`
  - 去掉 `@EnableConfigurationProperties`、所有字段、所有 `@Bean` 方法、原有 `@Import` 列表、`implements ApplicationRunner`
  - 保留 `@Configuration(proxyBeanMethods = false)` + `@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")` + `@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)`
  - 新增 `@AutoConfigureBefore(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")`：补偿 `MagicAPIDynamicDataSourceAutoConfiguration` 原有的 `@AutoConfigureBefore` 在 `@Import` 场景下失效的排序依赖
  - `@Import({MagicAPIWebMvcAutoConfiguration.class, MagicAPIWebFluxAutoConfiguration.class, MagicAPIDynamicDataSourceAutoConfiguration.class})`
- 修改 `META-INF/spring.factories`：将 magicapi 相关 3 行注册（L20-22）替换为 1 行 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration`
- 修改 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：同步上述 3 行 → 1 行替换
- **BREAKING**：`MagicAPIBaseAutoConfiguration` 的类签名从 `implements ApplicationRunner` 变为空类，其原 SERVLET 特定 Bean/字段迁移到 `MagicAPIWebMvcAutoConfiguration`。这些类属于 fw-plus 内部自动配置实现，不对外暴露 public API；通过 Spring Boot autoconfigure 机制使用的项目无感知。`spring.autoconfigure.exclude` 对 `@Import` 的类仍生效，下游排除能力不受影响。

## Capabilities

### New Capabilities

无。本次变更为自动配置注册方式重构，不引入新能力。

### Modified Capabilities

无。本次变更不改变任何 spec 级别的行为或验收场景，已通过 `.openspec.yaml` 设置 `skip_specs: true`。最终注册的 Bean 集合与条件求值结果与改动前等价（SERVLET 应用加载 WebMvc+DynamicDataSource，REACTIVE 应用加载 WebFlux）。

## Impact

- **受影响代码**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 单一 Maven 模块。
  - 1 个新建 Java 文件（`magicapi/MagicAPIWebMvcAutoConfiguration.java`，内容迁移自原 Base）
  - 1 个重构 Java 文件（`magicapi/MagicAPIBaseAutoConfiguration.java`，改为聚合入口）
  - 2 个资源文件修改（`spring.factories`、`AutoConfiguration.imports`）
- **运行时行为**：Bean 注册顺序、条件注解、Bean 名称保持等价。`MagicAPIDynamicDataSourceAutoConfiguration` 原有的 `@AutoConfigureBefore` 改由 `MagicAPIBaseAutoConfiguration` 的 `@AutoConfigureBefore` 统一补偿。
- **依赖与 API**：不涉及 Maven 依赖变化。
- **下游影响**：通过 autoconfigure 机制使用的项目无感知；显式引用 `MagicAPIBaseAutoConfiguration` 的下游项目需注意其不再 `implements ApplicationRunner`，Bean 定义迁移到 `MagicAPIWebMvcAutoConfiguration`。
