# 验证报告：i18n-takeover-finish

- 验证时间：2026-10-21
- 阶段：verify（Classic 只读验收）
- 结论：**通过**

## 验收目标

1. `I18nAutoConfiguration` 注入修复消除 NPE 隐患，且不引入 `messageSource` 循环依赖。
2. 容器能正确把 `MessageSource` 注入 `I18nMessageResolver` 静态入口。
3. 改动范围仅含本 change（i18n 注入修复 + 回归测试），排除 word 插件移除、样例测试改动、`AGENTS.md`/`CLAUDE.md` 等无关工作树改动。

## 验证执行与结果

### Build 检查（scope=build）

- 命令：`fw-plus-boot/./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=BootAutoconfigureI18nTest,I18nAutoConfigurationContextTest -Dmaven.test.skip=false`
- 结果：2 个测试类、5 个用例全绿（exit=0）。
- `I18nAutoConfigurationContextTest` 锁定两项契约：
  - Servlet Web 上下文装载 `I18nAutoConfiguration` + `I18nResolverConfiguration` 不再出现 `BeanCurrentlyInCreationException` 循环依赖；
  - 装载完成后 `I18nMessageResolver.getMessageSource()` 非空，证明注入路径生效（而非仅回退 `BundleLoader`）。
- `BootAutoconfigureI18nTest` 4/4 验证中英文文案解析。

### Verify 检查（scope=verify，整模块回归）

- 命令：`fw-plus-boot/./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dmaven.test.skip=false`
- 结果：**658 个用例，0 失败，0 错误，1 跳过**（跳过为模块既有预期），BUILD SUCCESS。
- 证明 i18n 注入修复未对 `fw-plus-boot-autoconfigure` 既有用例产生回归。

## 范围核对（T5）

本 change 实际文件：

- `fw-plus-boot/.../web/i18n/I18nAutoConfiguration.java`（修改：移除同配置类内 `@Autowired initResolver`，仅保留 `messageSource` `@Bean`）
- `fw-plus-boot/.../web/i18n/I18nResolverConfiguration.java`（新增：独立配置类注入 `MessageSource`）
- `fw-plus-boot/.../i18n/I18nAutoConfigurationContextTest.java`（新增：上下文装载回归测试）

确认排除（不纳入本 change，归档按 dirty-worktree 协议剔除）：

- `fw-plus-magicapi-plugin-word/` 整模块删除（86 个 console 资源 + pom）—— word 插件移除，独立于 i18n。
- `boot-dependencies/pom.xml`、`magicapi-plugin/pom.xml`、样例 `pom.xml` 的 word 插件移除改动。
- `MagicApiAllPluginsApplicationContextTest` / `MagicApiAllPluginsClasspathTest` 的「28→27」对账改动。
- `AGENTS.md`、`CLAUDE.md` 改动。
- `fw-plus-sample/.../fw-plus-ai-magicapi-sample/logDir_IS_UNDEFINED/` 运行期产物。

## 已知前置缺陷（移出本 change，留待单独修复）

- 现象：`fw-plus-magicapi-spring-boot-starter-sample` 启动报 `magicConfiguration` 与 `magicSwaggerController` 循环依赖。
- 位置：`MagicSwaggerConfiguration.magicSwaggerController(MagicConfiguration)`，与核心 `MagicConfiguration` 形成循环依赖。
- 归因：该缺陷在提交 `ab98f52f2`（「全量迁移用户可见中文异常文案到国际化」）已存在，与本次 i18n 改动（仅动 `I18nAutoConfiguration`）无关。用户已确认「移出本 change，留待单独修复」。
- 影响：样例集成测试（含 i18n 适配改动）暂无法在本 change 内跑通，其适配改动本身亦属 word 插件移除连带，一并移出。
