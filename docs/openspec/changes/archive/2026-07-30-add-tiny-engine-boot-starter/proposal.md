## Why

TinyEngine 低代码后端能力需要从独立参考项目进入 fw-plus 体系后，被宿主 Spring Boot 2.7.18 应用以 starter 方式引入和启用。当前拆分依赖 `integrate-tiny-engine-core` 提供领域模型、服务、Mapper 与 SQL/XML 资源，本 change 负责把这些 core 能力包装成符合 `fw-plus-boot` 风格的自动配置能力。

## What Changes

- 在 `fw-plus-boot` 增加 TinyEngine 自动配置入口，沿用现有 `fw-plus-boot-autoconfigure` 的 `spring.factories` 与 `AutoConfiguration.imports` 注册方式。
- 增加 TinyEngine Spring Boot starter，使宿主应用只需引入 starter 并配置开关即可暴露 TinyEngine REST API。
- 增加 `fw.plus.tiny-engine` 配置属性绑定，覆盖总开关、Controller 暴露、Mapper 扫描、Mapper XML/SQL 资源加载、基础上下文或可替换 Bean 的启用策略。
- 基于 core change 迁入的 Service、Mapper、XML 和 SQL 资源，在 Boot 层提供 TinyEngine REST Controller 适配、Web 装配和资源加载能力，不在本 change 重复实现领域逻辑。
- 增加基础样例验证，证明 Spring Boot 2.7.18 宿主引入 starter 后能启用 REST API，且禁用总开关时不注册 TinyEngine 控制器与服务。

## Capabilities

### New Capabilities

- `tiny-engine-boot-starter`: 覆盖 TinyEngine 在 `fw-plus-boot` 中的自动配置、starter 包装、属性开关、Mapper/XML/SQL 资源加载、Controller 暴露和样例验证。

### Modified Capabilities

- 无。

## Impact

- 影响 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure`：新增 TinyEngine 自动配置、属性类、条件注册、Mapper 扫描/资源加载配置和自动配置元数据。
- 影响 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters`：新增 TinyEngine starter 模块，并接入 starter 聚合 POM。
- 可能影响 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies` 与相关 BOM：需要托管新增 starter 与 core TinyEngine 模块依赖，版本继续使用 `${revision}`。
- 影响 `fw-plus-sample/fw-plus-boot-samples`：新增或扩展 TinyEngine starter 样例，用于验证启用/禁用、Mapper/XML 与 SQL 资源加载。
- 依赖 `integrate-tiny-engine-core` 已完成并提供兼容 Java 8、Spring Boot 2.7.18 的 TinyEngine core 模块；本 change 不迁移 Spring Boot 3/JDK 17，不调整 Magic-API 2.2.2 版本，不实现 AI Chat/RAG 扩展。
