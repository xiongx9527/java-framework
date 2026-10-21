# Spec: i18n

国际化消息解析能力：合并多 Bundle 的 `MessageSource`、异常按状态码解析用户可见文案，自动配置以容器方式注入 `MessageSource`。

## Requirement: 自动配置以容器方式注入 MessageSource

`I18nAutoConfiguration` 必须把容器中的 `MessageSource` 注入到静态解析器 `I18nMessageResolver`，且注入过程不得依赖尚未就绪的静态 `SpringUtils` context。

### Scenario: 自动配置初始化时注入
- **WHEN** Spring 容器完成 `I18nAutoConfiguration` 的 Bean 初始化且容器中存在 `MessageSource`
- **THEN** `I18nMessageResolver` 持有的 `MessageSource` 为容器注入的实例，且初始化过程不抛出 NPE

### Scenario: 容器中不存在 MessageSource
- **WHEN** 容器中没有 `MessageSource` Bean
- **THEN** 自动配置以 `required = false` 容忍缺失，不阻断应用启动

## Requirement: 用户可见文案经 i18n 解析

业务与框架抛出的异常、插件返回文案、校验提示、WebSocket 原因、展示名等用户可见中文文案，应走 `I18nMessageResolver`/`ComposableMessageSource` 按状态码或 key 解析，支持 `messages.properties` 与 `messages_en.properties` 双语言。

### Scenario: 按状态码解析异常文案
- **WHEN** 框架内置异常携带 8 位状态码被抛出
- **THEN** 其用户可见消息由 i18n 文案资源解析，而非写死中文

### Scenario: 默认语言回退
- **WHEN** 请求语言无对应翻译
- **THEN** 回退到 `messages.properties` 默认文案
