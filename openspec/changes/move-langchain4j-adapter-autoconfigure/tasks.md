## 1. 结构回归与迁移准备

- [x] 1.1 调整结构测试，先断言 AI Reactor、Boot BOM 和 Starter 不再引用 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`，且统一 Boot autoconfigure 拥有 LangChain4j 注册入口。
- [x] 1.2 调整中立边界测试，仅允许 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包承载具体框架代码，并继续禁止中立 Starter 传递 adapter/provider SDK。
- [x] 1.3 运行目标结构测试并记录预期 RED，确认失败原因是旧模块与旧边界仍存在。

## 2. 自动配置模块合并

- [ ] 2.1 将独立模块的生产源码迁入 `fw-plus-ai-boot-autoconfigure`，保留现有 Java 包、类名、Bean 名称和条件注解。
- [ ] 2.2 将旧模块 POM 的编译依赖合并进统一 autoconfigure POM，并保持 adapter、LangChain4j wrapper 和 provider SDK 依赖为 optional。
- [ ] 2.3 合并 `spring.factories` 与 `AutoConfiguration.imports`，确保 `LangChain4jAdapterAutoConfiguration` 在 Spring Boot 2.7 下保持注册。
- [ ] 2.4 将四个 adapter 测试类迁入统一模块并修正模块目录定位，不削弱缺失 provider SDK 和精确 `@ConditionalOnClass` 回归。

## 3. 坐标与文档清理

- [ ] 3.1 修改 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`，保留其公开坐标并将旧 autoconfigure 依赖替换为 `fw-plus-ai-boot-autoconfigure`。
- [ ] 3.2 从 AI Reactor 和 Boot BOM 删除旧 autoconfigure module/坐标，并删除独立模块目录。
- [ ] 3.3 更新 Starter README、AI 迁移文档和验证命令，删除旧坐标并说明自动配置已归入统一 Boot autoconfigure。
- [ ] 3.4 扩展旧 artifact 扫描，确保源码、POM、BOM、README 和当前验证命令不再把旧坐标当作可发布依赖。

## 4. 验证与交付证据

- [ ] 4.1 在 JDK 8 下运行 `fw-plus-ai-boot-autoconfigure` 全量测试，确认迁移后的注册、模型/store adapter 和缺 SDK 场景全部通过。
- [ ] 4.2 运行 LangChain4j adapter Starter 的 Maven Enforcer 与依赖树检查，确认只允许 `langchain4j-core` 且不传递 provider SDK。
- [ ] 4.3 运行 AI 目标 Reactor validate/test、OpenSpec 严格校验、文档引用检查和 `git diff --check`。
- [ ] 4.4 记录验证报告，列出删除坐标的破坏性影响、通过的测试数量和未触及的 Core/provider 行为。
