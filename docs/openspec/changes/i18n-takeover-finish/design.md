---
comet_change: i18n-takeover-finish
role: technical-design
canonical_spec: openspec
---

# Design: i18n-takeover-finish

## 背景

本 change 接管 `dev` 工作树中已存在但未提交的国际化收尾改动，目标是消除 `I18nAutoConfiguration` 的 NPE 隐患并让样例集成测试通过，不引入新的 i18n 行为设计。

## 确认的技术方案

### 1. MessageSource 注入方式

原实现：

```java
@PostConstruct
public void initResolver() {
    I18nMessageResolver.setMessageSource(SpringUtils.getBean(MessageSource.class));
}
```

问题：`@PostConstruct` 执行时 `SpringUtils` 静态持有的 `ApplicationContext` 可能尚未设置（自动配置早于部分上下文回调），`getBean` 会 NPE。

改为容器注入：

```java
@Autowired(required = false)
public void initResolver(MessageSource messageSource) {
    I18nMessageResolver.setMessageSource(messageSource);
}
```

由 Spring 在填充 `I18nAutoConfiguration` Bean 属性阶段直接注入 `MessageSource`，不依赖静态 context。`required = false` 容忍缺失，缺失时 `I18nMessageResolver` 退回无 MessageSource 的默认行为（与现状一致）。

### 2. 范围边界（已确认 + 接管后复核修正）

本 change 实际只接管 **2 个 i18n 文件**：

- `I18nAutoConfiguration.java`（修改）：移除同配置类内 `@Autowired initResolver`，改为仅声明 `messageSource` `@Bean`。
- `I18nResolverConfiguration.java`（新增）：`@AutoConfigureAfter(I18nAutoConfiguration.class)` 的独立配置类，负责把容器 `MessageSource` 注入 `I18nMessageResolver`，以此打破「同配置类内声明 `messageSource` `@Bean` 又索取它」触发的循环依赖。

工作树中其余改动 **均不属于本 change**，已在接管复核中确认：

- `boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml` 以及样例 `pom.xml` 的未提交改动，全部是 **移除 `fw-plus-magicapi-plugin-word` 模块**（连同 86 个 word 插件 console 资源删除），与 i18n 无关。
- `AGENTS.md`/`CLAUDE.md` 改动与 i18n 无关。
- `MagicApiAllPluginsApplicationContextTest` / `MagicApiAllPluginsClasspathTest` 的未提交改动，内容是把 `fw-plus-magicapi-plugin-word` 从「28→27」对账清单与 classpath 断言里剔除——同样是 word 插件移除的连带修改，并非 i18n 适配。

> 结论：本 change **不存在** i18n 专属的 pom 同步需求；`boot-dependencies`/`magicapi-plugin` 的 pom 改动与样例测试改动一律按 dirty-worktree 协议排除，归入 word 插件移除这一独立工作树改动。

- 不新增任何模块级的 i18n 文案迁移（既有迁移已在先前提交完成）。
- 不改动 `I18nMessageResolver`/`ComposableMessageSource`/`BundleLoader` 的解析逻辑。
- **排除无关改动**：86 个 `magicapi-plugin-word` console 资源删除、`AGENTS.md`/`CLAUDE.md`、word 插件相关的 pom 与样例测试改动，统一不纳入本 change。

### 3. 验证策略

- `fw-plus-boot-autoconfigure` 的 `BootAutoconfigureI18nTest` 验证注入行为（已 4/4 通过）。
- **样例集成测试已移出本 change**：`MagicApiAllPluginsApplicationContextTest` / `MagicApiAllPluginsClasspathTest` 的启动依赖 magicapi swagger 插件的 `magicConfiguration ↔ magicSwaggerController` 循环依赖，该缺陷在提交 `ab98f52f2` 已存在、与 i18n 无关（i18n 只动了 `I18nAutoConfiguration`）。用户已确认「移出本 change，留待单独修复」。样例测试改动本身也是 word 插件移除的连带修改，故一并移出。

## 关键取舍与风险

- 取舍：`required = false` 容忍无 `MessageSource` 场景，回退默认行为，与现状一致；不引入新的静态解析路径。独立 `I18nResolverConfiguration` 配置类避免同配置类内循环依赖。
- 风险：无关工作树改动（word 插件移除、样例测试、AGENTS/CLAUDE）须在归档时精确排除，避免污染提交。
- **已知前置缺陷（移出本 change）**：magicapi swagger 插件 `MagicSwaggerConfiguration.magicSwaggerController(MagicConfiguration)` 与核心 `MagicConfiguration` 形成循环依赖，导致样例上下文无法启动，阻塞全部样例集成测试。本 change 不修复，需单独 change 处理。

## 数据流

```
Spring 容器
  └─> I18nAutoConfiguration Bean 属性填充
        └─> @Autowired MessageSource -> I18nMessageResolver.setMessageSource()
              └─> 异常/文案解析时由 MessageSource 解析多语言文案
```

## Spec Patch

无。现有 `specs/i18n/spec.md` 已覆盖「容器注入 MessageSource」与「按状态码解析文案」验收场景，无需回写。
