---
change: i18n-takeover-finish
design-doc: docs/openspec/changes/i18n-takeover-finish/design.md
base-ref: 3b29eeb74d9eca4926e91f168b6f009e78e0f93f
---

<!-- comet-task-authority: docs/openspec/changes/i18n-takeover-finish/tasks.md -->

# 实施计划：i18n-takeover-finish

本计划接管 `dev` 工作树中未提交的国际化收尾改动并收口。执行策略：autonomous + tdd + standard 审查。

## 任务与验收

### T1 — I18nAutoConfiguration 注入修复 <!-- comet-task-ref:f0c2426f-5a6f-452d-83f0-30ac649787e4 -->
- 范围：`I18nAutoConfiguration.initResolver` 改为 `@Autowired(required = false) MessageSource`；移除 `SpringUtils` 静态取 Bean 与 `@PostConstruct`。工作树已含该改动，需确认无遗漏并保留注释说明。
- 依赖：无
- 约束：JDK 1.8；不引入新依赖。
- 验收：模块编译通过；`BootAutoconfigureI18nTest` 覆盖注入行为（无 SpringUtils 依赖）。
- TDD：RED = 还原为 `@PostConstruct`+`SpringUtils.getBean` 时，自动配置早于静态 context 就绪会 NPE，相关测试失败；GREEN = 改为 `@Autowired` 注入后测试通过。

### T2 — 同步 pom 未提交调整 <!-- comet-task-ref:8ed0cd71-6da4-4390-a007-ce20d23e80db -->
- 范围：`boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml` 工作树中已调整但未提交的内容，仅保留与 i18n 收尾相关的依赖/版本项。
- 依赖：T1
- 约束：不扩大 pom 改动范围；与 i18n 收尾无关的改动不纳入。
- 验收：`fw-plus-boot` 模块编译通过。

### T3 — 样例集成测试适配 <!-- comet-task-ref:320c6743-d90b-48dc-8828-a680841df4c3 -->
- 范围：更新 `fw-plus-magicapi-spring-boot-starter-sample` 的 `MagicApiAllPluginsApplicationContextTest` / `MagicApiAllPluginsClasspathTest`，使其适配 i18n 收尾（如 classpath 资源、自动配置 bean 预期变化）。
- 依赖：T1、T2
- 约束：只改测试预期，不绕过失败。
- 验收：两个集成测试全绿。

### T4 — 运行测试确保全绿 <!-- comet-task-ref:3d1c65ca-1b15-4ea8-9eba-4661c73b2b70 -->
- 范围：运行 `fw-plus-boot-autoconfigure` 的 i18n 测试与样例集成测试。
- 依赖：T1-T3
- 验收：相关测试全绿。

### T5 — 改动范围校验与排除 <!-- comet-task-ref:9e01250d-9c7b-401e-8a32-3cffeeac75f5 -->
- 范围：核对 git diff，确保本 change 提交仅含 T1-T3 的 i18n 收尾；排除 86 个 `magicapi-plugin-word` console 资源删除、`AGENTS.md`/`CLAUDE.md` 等与本次无关的改动。
- 依赖：T1-T4
- 验收：`git status`/`git diff` 核对明确，提交路径精确。

## 实施顺序

T1 → T2 → T3 → T4 → T5。T1-T3 为代码/配置/测试改动，T4 为验证，T5 为提交前范围核对。

## 风险与回退

- NPE 修复改变注入时机：以 `BootAutoconfigureI18nTest` + 样例集成测试覆盖；若失败，回退为保留 `SpringUtils` 但加空校验。
- pom 调整范围：仅保留 i18n 相关，过大改动则暂停确认。
- 提交严格按 T5 排除无关文件，避免污染。
