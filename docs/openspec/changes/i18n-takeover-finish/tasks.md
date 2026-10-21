# Tasks: i18n-takeover-finish

- [x] 1. 将 `I18nAutoConfiguration.initResolver` 由同配置类内 `@Autowired` 注入改为独立配置类 `I18nResolverConfiguration`（`@AutoConfigureAfter(I18nAutoConfiguration.class)`），打破 `messageSource` `@Bean` 引发的循环依赖；`I18nAutoConfiguration` 仅保留 `messageSource` `@Bean` 声明，移除 `SpringUtils`/`@PostConstruct` 用法。新增 `I18nAutoConfigurationContextTest` 锁定「上下文装载无循环依赖 + MessageSource 被注入 `I18nMessageResolver`」契约 <!-- comet-task:f0c2426f-5a6f-452d-83f0-30ac649787e4 -->
- [x] 2. pom 同步复核：工作树 `boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml` 的未提交改动全部是移除 `fw-plus-magicapi-plugin-word`（与 i18n 无关），本 change **无 i18n 专属 pom 改动**，故此项为无操作并据实记录 <!-- comet-task:8ed0cd71-6da4-4390-a007-ce20d23e80db -->
- [x] 3. 样例集成测试 `MagicApiAllPluginsApplicationContextTest` / `MagicApiAllPluginsClasspathTest` 适配：其未提交改动实为剔除 `fw-plus-magicapi-plugin-word`（word 插件移除连带），且启动受 magicapi swagger 插件 `magicConfiguration ↔ magicSwaggerController` 循环依赖阻塞（缺陷在 `ab98f52f2` 已存在，与 i18n 无关）。**本 change 已确认「移出」，留待单独 change 修复，故本项在此 change 内关闭** <!-- comet-task:320c6743-d90b-48dc-8828-a680841df4c3 -->
- [x] 4. 运行 `fw-plus-boot-autoconfigure` 的 `BootAutoconfigureI18nTest`（4/4）与新增 `I18nAutoConfigurationContextTest`（1/1），验证容器注入 `MessageSource` 行为，全绿 <!-- comet-task:3d1c65ca-1b15-4ea8-9eba-4661c73b2b70 -->
- [x] 5. 校验改动仅含本 change 范围：本 change 实际文件 = `I18nAutoConfiguration.java`（修改）、`I18nResolverConfiguration.java`（新增 main）、`I18nAutoConfigurationContextTest.java`（新增 test），均为 i18n 注入修复与回归测试。已确认排除 word 插件 console 删除、word 插件相关 pom/样例测试改动、`AGENTS.md`/`CLAUDE.md` 等无关改动（见下方范围外清单）<!-- comet-task:9e01250d-9c7b-401e-8a32-3cffeeac75f5 -->

## 范围外清单（归档须排除的工作树改动）

- `fw-plus-magicapi-plugin-word/` 整模块删除（86 个 console 资源 + pom）—— word 插件移除，独立于 i18n。
- `fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`、`fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`、`fw-plus-sample/.../magicapi-spring-boot-starter-sample/pom.xml` 的 word 插件移除改动。
- `MagicApiAllPluginsApplicationContextTest.java` / `MagicApiAllPluginsClasspathTest.java` 的「28→27」对账改动。
- `AGENTS.md`、`CLAUDE.md` 改动。
- `fw-plus-sample/.../fw-plus-ai-magicapi-sample/logDir_IS_UNDEFINED/` 运行期产物（非源码）。
- 已知前置缺陷（移出本 change、留待单独修复）：magicapi swagger 插件 `MagicSwaggerConfiguration.magicSwaggerController(MagicConfiguration)` 与核心 `MagicConfiguration` 循环依赖，阻塞全部样例集成测试启动。
