# Mate 自动配置安全优化设计

## 背景

本次优化范围限定在 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate` 目录。该目录包含 Mate 总入口、EasyTrans 自动配置、WebMVC Validator 自动配置，以及请求/响应 Advice。

## 目标

- 保持现有配置前缀、Bean 方法名、包名和默认启用行为兼容。
- 降低自动配置在非 Servlet 环境、用户自定义 Bean、空请求体和异常响应路径下的污染风险。
- 修复 ResponseBodyAdvice 对 `@ResponseBody` / `@RestController` 的识别不足。
- 为关键边界行为补充自动化测试，防止后续回归。

## 非目标

- 不升级 EasyTrans、Spring Boot、Spring MVC 或 Maven 依赖版本。
- 不改动 `fw-plus-core` 的 `CommonsValidatorHolder` 实现。
- 不重构 EasyTrans 业务服务，不改变翻译缓存和 RPC 语义。
- 不新增第三方依赖。

## 设计

### 1. Validator 自动配置条件收紧

`MateWebMvcValidatorAutoConfiguration` 当前只使用属性条件，没有显式 `@Configuration`、Servlet Web 条件，也没有用户 Bean 让位逻辑。

优化后：
- 增加 `@Configuration(proxyBeanMethods = false)`。
- 增加 `@ConditionalOnWebApplication(type = SERVLET)`。
- 两个 Advice Bean 增加 `@ConditionalOnMissingBean`，用户自定义同类型 Bean 时不覆盖。

### 2. Validator Advice 上下文清理

`CommonsValidatorRequestBodyAdvice` 当前仅在 `body` 非空时写入 `CommonsValidatorHolder`，空 body 不清理旧值。`CommonsValidatorResponseBodyAdvice` 正常写响应时清理，但 supports 判定过窄。

优化后：
- `afterBodyRead` 在 body 为空时主动 `clear()`，避免复用线程残留数据。
- `handleEmptyBody` 主动 `clear()`。
- `ResponseBodyAdvice.supports` 同时识别方法级 `@ResponseBody` 和类级/元注解 `@ResponseBody`，覆盖 `@RestController`。
- 保持 `beforeBodyWrite` 清理行为不变。

### 3. EasyTrans 自动配置让位

`MateEasyTransAutoConfiguration` 当前未显式 `@Configuration`，核心 `@Bean` 没有 `@ConditionalOnMissingBean`，用户自定义 Bean 时可能被覆盖。

优化后：
- 增加 `@Configuration(proxyBeanMethods = false)`。
- 对核心 EasyTrans Bean、Redis 相关 Bean、`springContextUtil` 增加 `@ConditionalOnMissingBean`。
- 不改变 Redis 条件开关、`@Primary`、`@ConditionalOnBean` 和多数据源校验语义。

## 风险与兼容性

- Bean 让位只在用户已提供同类型 Bean 时生效，默认场景保持原有 Bean 创建。
- 空 body 清理可能改变此前残留上下文可被误读的副作用，但该副作用属于污染风险，不应依赖。
- ResponseAdvice 扩大识别范围符合 Spring MVC `@RestController` 常规用法。

## 验证

- 定向测试：`mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mate*Test,*CommonsValidator*Test' test -q`
- 静态检查：`git diff --check`
