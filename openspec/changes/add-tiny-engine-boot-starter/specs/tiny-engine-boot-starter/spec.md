## ADDED Requirements

### Requirement: TinyEngine starter dependency
系统 SHALL 在 `fw-plus-boot` 中提供 TinyEngine Spring Boot starter，使宿主 Spring Boot 2.7.18 应用能够通过引入 starter 获得 TinyEngine 自动配置能力。

#### Scenario: Host imports starter
- **WHEN** 宿主应用引入 TinyEngine starter 并满足 `integrate-tiny-engine-core` 产物依赖
- **THEN** TinyEngine 自动配置类、属性绑定、Mapper 扫描配置、Boot 层 Controller 适配注册配置和资源加载配置 SHALL 出现在宿主应用 classpath 中

#### Scenario: Starter follows boot module style
- **WHEN** 维护者检查 `fw-plus-boot-project/fw-plus-boot-starters`
- **THEN** TinyEngine starter MUST 按现有 starter 模块风格接入聚合 POM，并继续使用 `${revision}` 版本体系

### Requirement: TinyEngine auto-configuration registration
系统 SHALL 在 `fw-plus-boot-autoconfigure` 中注册 TinyEngine 自动配置，并兼容当前 Spring Boot 2.7.18 使用的自动配置发现机制。

#### Scenario: Spring Boot loads auto-configuration
- **WHEN** 宿主应用启动并扫描自动配置
- **THEN** TinyEngine 自动配置 MUST 通过 `META-INF/spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册为可发现配置

#### Scenario: Required classes are absent
- **WHEN** TinyEngine core 依赖或 Servlet Web 环境不存在
- **THEN** TinyEngine 自动配置 MUST 不注册依赖这些类或环境的 Controller、Service、Mapper 相关 Bean

### Requirement: TinyEngine configuration properties
系统 SHALL 提供 `fw.plus.tiny-engine` 配置属性绑定，用于控制 TinyEngine 总开关和子能力开关。

#### Scenario: TinyEngine enabled
- **WHEN** 宿主应用设置 `fw.plus.tiny-engine.enabled=true`
- **THEN** 系统 SHALL 按子能力开关注册 TinyEngine Web、Mapper 和资源加载能力

#### Scenario: TinyEngine disabled
- **WHEN** 宿主应用设置 `fw.plus.tiny-engine.enabled=false`
- **THEN** 系统 MUST NOT 注册 TinyEngine Controller、TinyEngine Service、TinyEngine Mapper 扫描或 TinyEngine SQL/Mapper XML 加载 Bean

#### Scenario: Web capability disabled
- **WHEN** 宿主应用设置 `fw.plus.tiny-engine.enabled=true` 且 `fw.plus.tiny-engine.web.enabled=false`
- **THEN** 系统 MUST NOT 注册 TinyEngine REST Controller，但 Mapper、Service 或资源能力 MAY 按各自开关继续注册

### Requirement: TinyEngine mapper and XML loading
系统 SHALL 在启用 TinyEngine Mapper 能力时扫描 core change 提供的 TinyEngine Mapper，并加载随 TinyEngine 模块发布的 Mapper XML 资源。

#### Scenario: Mapper capability enabled
- **WHEN** `fw.plus.tiny-engine.enabled=true` 且 `fw.plus.tiny-engine.mapper.enabled=true`
- **THEN** 系统 SHALL 扫描 `integrate-tiny-engine-core` 提供的 TinyEngine Mapper 包，并将 TinyEngine Mapper XML 资源加入 MyBatis 可加载位置

#### Scenario: Mapper XML packaged in starter path
- **WHEN** 宿主应用只通过 starter 间接引入 TinyEngine core 模块
- **THEN** TinyEngine Mapper XML MUST 能从 classpath 被发现，不要求宿主手工复制 XML 文件或手工配置 mapper locations

### Requirement: TinyEngine SQL resource discovery
系统 SHALL 确保 TinyEngine SQL 初始化资源随 starter/core classpath 可发现，并支持按数据库类型定位。

#### Scenario: SQL resources available
- **WHEN** 宿主应用启用 TinyEngine 资源能力
- **THEN** 系统 SHALL 能发现 TinyEngine MySQL、H2 和 PostgreSQL SQL 资源，资源来源 MUST 以 `integrate-tiny-engine-core` 迁入后的 SQL 文件为准

#### Scenario: SQL auto-initialization guarded
- **WHEN** 宿主应用未显式启用 TinyEngine SQL 自动初始化
- **THEN** 系统 MUST NOT 自动修改宿主数据库结构或写入初始化数据

### Requirement: TinyEngine REST API exposure
系统 SHALL 在启用 TinyEngine Web 能力时注册 Boot 层 TinyEngine REST Controller 适配，并委托 core change 提供的 Service 处理领域逻辑。

#### Scenario: REST API enabled
- **WHEN** 宿主 Servlet Web 应用设置 `fw.plus.tiny-engine.enabled=true` 且 `fw.plus.tiny-engine.web.enabled=true`
- **THEN** TinyEngine 低代码 REST API SHALL 注册到 Spring MVC，并通过 Boot 层 Controller 适配委托 core change 提供的 Service 处理领域逻辑

#### Scenario: Host bean override
- **WHEN** 宿主应用提供同职责 TinyEngine Bean 或 Controller 扩展点
- **THEN** 自动配置 MUST 允许宿主 Bean 优先生效，避免强制覆盖宿主定制

### Requirement: TinyEngine sample verification
系统 SHALL 提供基础样例或测试验证 TinyEngine starter 在 `fw-plus-sample` 中的启用、禁用和资源加载行为。

#### Scenario: Enabled sample starts
- **WHEN** 运行 TinyEngine starter 样例的基础验证
- **THEN** 样例 SHALL 在 Spring Boot 2.7.18 下启动应用上下文，并验证 TinyEngine REST Controller、Mapper XML 和 SQL 资源可用

#### Scenario: Disabled sample starts
- **WHEN** 样例设置 `fw.plus.tiny-engine.enabled=false`
- **THEN** 样例 SHALL 启动应用上下文，并验证 TinyEngine Controller 与 Service 未注册
