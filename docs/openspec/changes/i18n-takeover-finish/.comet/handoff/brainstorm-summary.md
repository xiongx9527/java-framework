# Brainstorm Summary

- Change: i18n-takeover-finish
- Date: 2026-10-21

## 确认的技术方案

1. `I18nAutoConfiguration.initResolver` 由 `@PostConstruct` + `SpringUtils.getBean(MessageSource.class)` 改为 `@Autowired(required = false) MessageSource messageSource`，由容器注入，消除自动配置早于静态 context 就绪时的 NPE。
2. 收尾工作树既有未提交改动：boot-dependencies/pom.xml、magicapi-plugin/pom.xml、magicapi-sample 两个集成测试。
3. 不新增模块级 i18n 文案迁移，不改动解析逻辑。

## 关键取舍与风险

- `required = false` 容忍无 MessageSource 场景，回退默认行为，与现状一致。
- 注入时机变化需样例集成测试验证。
- 工作树另有 86 个 magicapi-plugin-word console 资源删除与 AGENTS.md/CLAUDE.md 改动，与本次无关，归档时排除。

## 测试策略

- `fw-plus-boot-autoconfigure` 的 `BootAutoconfigureI18nTest` 验证注入行为。
- `fw-plus-magicapi-spring-boot-starter-sample` 的 `MagicApiAllPluginsApplicationContextTest` 与 `MagicApiAllPluginsClasspathTest` 全绿。

## Spec Patch

无。
