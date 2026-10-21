# Proposal

## Why

`dev` 分支上已提交多轮国际化基础设施与模块文案迁移（搭建 MessageSource 合并、异常按状态码解析文案、各模块 messages 资源），但工作树仍残留一批未提交改动：`I18nAutoConfiguration` 以 `@PostConstruct` + `SpringUtils.getBean` 注入 `MessageSource`，在自动配置初始化时静态 context 尚未就绪会 NPE；另有 `boot-dependencies`/`magicapi-plugin` 的 pom 调整与 `magicapi-spring-boot-starter-sample` 的集成测试改动尚未收尾。本 change 接管这批半成品，消除 NPE 隐患并让样例集成测试通过。

## What Changes

- 将 `I18nAutoConfiguration.initResolver` 由 `@PostConstruct` + `SpringUtils.getBean(MessageSource.class)` 改为 `@Autowired(required = false) MessageSource`，由容器注入，避免静态 context 未就绪导致的 NPE。
- 同步 `boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml` 中已在工作树调整但未提交的依赖/版本关系。
- 修正 `fw-plus-magicapi-spring-boot-starter-sample` 中因 i18n 迁移而需更新的集成测试（`MagicApiAllPluginsApplicationContextTest`、`MagicApiAllPluginsClasspathTest`），使其通过。
- 不新增尚未迁移模块的 i18n 文案迁移；不改动与 i18n 无关的代码。

## Capabilities

### New Capabilities
- `i18n`: 框架级国际化消息解析能力——合并多 Bundle 的 `MessageSource`、异常按状态码解析用户可见文案、自动配置以容器方式注入 `MessageSource`。本 change 落定自动配置注入策略这一行为契约。

### Modified Capabilities
<!-- 无既有 capability 的需求级别变更 -->

## Impact

- 受影响代码：`fw-plus-boot-autoconfigure` 的 `I18nAutoConfiguration`；`fw-plus-boot-dependencies`、`fw-plus-magicapi-plugin` 的 pom；`fw-plus-magicapi-spring-boot-starter-sample` 测试。
- 受影响 API：无对外 API 签名变更；`I18nMessageResolver.setMessageSource` 调用方由自动配置内部使用，行为不变。
- 依赖：无新增依赖。
- 风险：自动配置注入时机变化，需样例集成测试验证容器能正确注入 `MessageSource`。
