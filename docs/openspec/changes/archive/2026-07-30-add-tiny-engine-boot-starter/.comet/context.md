# Comet Design Handoff

- Change: add-tiny-engine-boot-starter
- Phase: design
- Mode: compact
- Context hash: 8da6c53ef62af2a8b7700ffa6297b3e4eff62d7f926c54095a140788e7e75902

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-tiny-engine-boot-starter/proposal.md

- Source: openspec/changes/add-tiny-engine-boot-starter/proposal.md
- Lines: 1-29
- SHA256: fa79dc6a44dfae481307b809bdad404031825d499db1910822aa5c28219e231a

```md
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

```

## openspec/changes/add-tiny-engine-boot-starter/design.md

- Source: openspec/changes/add-tiny-engine-boot-starter/design.md
- Lines: 1-67
- SHA256: 90fbab19bb8f28c9c8b6ebddb37cdc70d281b655c8fc11e779a8ebdc21931719

```md
## Context

`fw-plus-boot` 当前以 `fw-plus-boot-autoconfigure` 承载自动配置，并通过 `META-INF/spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册顶层 AutoConfiguration。各 starter 位于 `fw-plus-boot-project/fw-plus-boot-starters`，通常依赖 `fw-plus-boot-starter` 与对应 core 模块，再由 starter 聚合 POM 统一纳入构建。

TinyEngine 参考项目当前包含 `base` 与 `app` 两层，其中 `base` 提供低代码领域对象、Service、Controller、Mapper XML，`app` 提供启动应用与 MySQL/H2/PostgreSQL SQL 资源。参考项目基线为 Spring Boot 3.1.9 与 Java 17，且包含 AI Chat/RAG 相关类；本 change 不直接迁移参考项目原始运行方式，而是依赖 `integrate-tiny-engine-core` 先把可复用领域能力整理为 fw-plus core 侧的 Java 8/Spring Boot 2.7.18 兼容模块，再由 Boot 层提供 Controller 适配与自动配置。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-boot-autoconfigure` 中新增 TinyEngine 自动配置，并使用 `fw.plus.tiny-engine` 属性绑定控制启用行为。
- 新增 TinyEngine starter，封装宿主应用需要的 autoconfigure、web、MyBatis-Plus 与 core TinyEngine 依赖。
- 通过自动配置接入 core change 提供的 TinyEngine Service、Mapper、Mapper XML 与 SQL 初始化资源，并在 Boot 层提供 REST Controller 适配。
- 提供启用与禁用两类基础样例验证，覆盖 REST API 暴露、Mapper/XML 加载、SQL 资源可发现，以及总开关关闭时不注册 TinyEngine 控制器与服务。

**Non-Goals:**

- 不重复实现 TinyEngine core 领域模型、Service、Mapper、动态 SQL 或 SQL 脚本内容。
- 不实现 TinyEngine AI Chat/RAG、向量检索或 LangChain4j 扩展。
- 不新增 LangChain4j 专用 TinyEngine 依赖或接口；后续 TinyEngine AI/RAG 集成必须面向已归档的 `abstract-ai-model-and-embedding` 中立抽象（`fw-plus-ai-model`、`fw-plus-ai-store-embedding`）和当前 LangChain4j adapter starter。
- 不迁移 `fw-plus-boot` 到 Spring Boot 3，不提升 JDK 到 17，不引入 Java 9+ API。
- 不修改 Magic-API 2.2.2 依赖版本或 Magic-API 插件行为。

## Decisions

1. **自动配置沿用 Boot 层现有注册方式。**
   TinyEngine 顶层配置类放在 `com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine` 包下，并同时注册到 `spring.factories` 和 `AutoConfiguration.imports`。这样兼容当前 Spring Boot 2.7.18 的加载方式，也与仓库现有自动配置入口保持一致。备选方案是在 starter 中显式 `@Import`，但会要求宿主额外配置，starter 体验较弱。

2. **以 `fw.plus.tiny-engine` 作为统一配置前缀。**
   设计一个总开关 `enabled`，并拆分 `web.enabled`、`mapper.enabled`、`resources.enabled`、`sql.enabled` 等子能力开关。总开关关闭时，所有 TinyEngine Controller、Service、Mapper 扫描和资源加载 Bean 都不得注册；子开关只控制对应能力。备选方案复用参考项目原属性，但其语义偏独立应用，不适合 fw-plus starter。

3. **Boot change 只装配 core change 产物并承载 Web 适配。**
   Service、Mapper 接口、Mapper XML 与 SQL 资源的权威来源是 `integrate-tiny-engine-core`。Controller 属于 Boot 层 Web 适配，本 change 可参考 `tiny-engine-backend-java/base` 中的 Controller 路由语义实现薄适配，但不得复制或分叉 core 领域逻辑。这样可以避免 core 与 boot 两套实现漂移。

4. **Mapper 与 XML/SQL 资源采用 starter 可发现路径。**
   自动配置需要明确扫描 core 中 TinyEngine Mapper 包，并将 XML 资源位置追加到 MyBatis 配置可识别的 mapper locations 中。SQL 初始化资源按数据库类型保留在 starter/core classpath 的稳定目录下，由属性选择或暴露为 Resource 列表供宿主初始化流程使用。备选方案要求宿主手写 `@MapperScan` 与 `mybatis-plus.mapper-locations`，但不满足“引入 starter 后即可配置启用”的目标。

5. **Controller 适配受属性和 Classpath 条件约束。**
   Boot 层 TinyEngine REST Controller 适配只在 Servlet Web 应用、TinyEngine core classpath 存在且 `fw.plus.tiny-engine.enabled=true`、`fw.plus.tiny-engine.web.enabled=true` 时注册。若宿主已有同名或同职责 Bean，自动配置应通过条件注解允许宿主覆盖。这样可以降低对已有应用 Web 层的侵入。

6. **样例验证放在 `fw-plus-sample/fw-plus-boot-samples`。**
   新增 TinyEngine starter sample 或扩展 boot samples 聚合，使用 Spring Boot 2.7.18、Java 8 兼容写法验证 starter 接入。样例只验证 starter 装配和基础 REST/资源发现，不承担完整 TinyEngine 业务回归。

7. **AI 边界跟随最新中立抽象。**
   `abstract-ai-model-and-embedding` 已把公共模型与向量存储能力整理为 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`，并将 LangChain4j 移到默认 adapter/starter。TinyEngine Boot starter 本 change 不实现 AI，但任何可选依赖、扩展点、文档或样例都不得重新引入旧 LangChain4j 专用 model/store 坐标；后续 AI starter 只能依赖中立 registry/factory，并由 LangChain4j adapter 或未来 Java 17 adapter 提供实现。

## Risks / Trade-offs

- [Risk] `integrate-tiny-engine-core` 的包名、模块名或资源路径最终变化，导致 Boot 自动配置路径失配。 → Mitigation：实现前以 core change 的最终 POM、Service/Mapper 包和资源目录为准，并在 Boot 层单独确认 Controller 适配包与路由，任务中保留对这些路径的核对步骤。
- [Risk] MyBatis-Plus 版本与参考项目不同，Mapper XML 或 TypeHandler 需要兼容调整。 → Mitigation：兼容调整应优先落在 core change；Boot change 只验证 XML 被加载并能在样例中通过基础查询/上下文启动。
- [Risk] 自动注册 Controller 可能与宿主应用安全、路径或异常处理冲突。 → Mitigation：提供 `web.enabled` 子开关，并让关键 Bean 支持宿主覆盖；必要时提供 API base path 属性。
- [Risk] SQL 初始化策略如果自动执行，可能对宿主数据库造成副作用。 → Mitigation：默认只保证 SQL 资源随 starter 可发现；自动初始化必须受显式属性控制，并在样例中使用 H2 或隔离库验证。
- [Risk] TinyEngine 后续 AI 能力误接旧 LangChain4j 专用 artifact，重新制造框架耦合。 → Mitigation：本 change 明确禁止新增旧专用 AI 依赖；构建检查扫描 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding`、旧 starter 坐标和直接 provider SDK 依赖，后续 AI 能力转交 `add-tiny-engine-ai-starter` 并使用中立抽象。

## Migration Plan

1. 确认 `integrate-tiny-engine-core` 已提供 fw-plus core TinyEngine 模块、Java 8 兼容源码、Mapper XML 和 SQL 资源。
2. 在 Boot BOM 与 starter 聚合中加入 TinyEngine starter 与 core TinyEngine 依赖声明。
3. 加入 TinyEngine 自动配置、属性类和自动配置元数据，验证默认禁用/启用语义。
4. 增加 Boot sample 并通过 Maven validate/test 验证启用和禁用场景。
5. 如需回滚，移除 starter 依赖或关闭 `fw.plus.tiny-engine.enabled`；已发布模块可通过不引入 starter 避免影响宿主应用。

## Open Questions

- `integrate-tiny-engine-core` 最终采用的模块 artifactId、Service/Mapper 包名和资源路径是什么？
- TinyEngine REST API 是否需要统一 base path，例如 `/tiny-engine` 或保留参考项目原路径？
- SQL 资源默认只暴露还是支持显式自动初始化，初始化顺序是否由 core 提供清单？

```

## openspec/changes/add-tiny-engine-boot-starter/tasks.md

- Source: openspec/changes/add-tiny-engine-boot-starter/tasks.md
- Lines: 1-38
- SHA256: bb7a26d4174aa4c68947094b04c99d766c855aa6e790fd6af05e42331b0d658e

```md
## 1. Core 依赖确认

- [ ] 1.1 确认 `integrate-tiny-engine-core` 已提供 TinyEngine core 模块 artifactId、包名、Service、Mapper、Mapper XML 和 SQL 资源路径。
- [ ] 1.2 核对 core TinyEngine 模块已兼容 Java 8、Spring Boot 2.7.18、MyBatis-Plus 版本体系，并确认 Boot change 不需要复制领域逻辑。
- [ ] 1.3 明确需要从 Boot 层托管或传递的 TinyEngine core 依赖，以及是否需要在 Boot BOM 中增加依赖管理。
- [ ] 1.4 核对最新 `abstract-ai-model-and-embedding` 结果，确认 TinyEngine Boot starter 不新增旧 LangChain4j 专用 model/store 坐标；后续 AI/RAG 只通过 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 中立抽象衔接。

## 2. 自动配置

- [ ] 2.1 在 `fw-plus-boot-autoconfigure` 新增 TinyEngine 自动配置包和顶层 AutoConfiguration。
- [ ] 2.2 新增 `fw.plus.tiny-engine` 配置属性类，覆盖 `enabled`、`web.enabled`、`mapper.enabled`、`resources.enabled`、`sql.enabled`、API base path 或资源位置等必要配置。
- [ ] 2.3 使用条件注解确保缺少 TinyEngine core classpath、非 Servlet Web 环境或总开关关闭时不注册 TinyEngine 相关 Bean。
- [ ] 2.4 在 Boot 层实现 TinyEngine REST Controller 薄适配并注册暴露配置，Controller 委托 core Service，且允许宿主应用通过自定义 Bean 覆盖默认装配。
- [ ] 2.5 注册 TinyEngine Mapper 扫描与 Mapper XML 资源加载配置，确保宿主无需手写 `@MapperScan` 或复制 XML。
- [ ] 2.6 注册 TinyEngine SQL 资源发现能力，默认不自动修改宿主数据库，自动初始化必须受显式属性控制。
- [ ] 2.7 将 TinyEngine 自动配置加入 `META-INF/spring.factories` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 3. Starter 与依赖管理

- [ ] 3.1 在 `fw-plus-boot-project/fw-plus-boot-starters` 新增 TinyEngine Spring Boot starter 模块。
- [ ] 3.2 将 TinyEngine starter 接入 `fw-plus-boot-starters` 聚合 POM，并保持现有 `${revision}` 版本管理方式。
- [ ] 3.3 为 TinyEngine starter 声明 `fw-plus-boot-starter`、`fw-plus-boot-autoconfigure` 间接能力、MyBatis-Plus/Web 相关依赖和 core TinyEngine 模块依赖。
- [ ] 3.4 如仓库现有 BOM 需要，更新 `fw-plus-boot-dependencies` 以托管 TinyEngine starter 与 core TinyEngine 依赖。

## 4. 样例与验证

- [ ] 4.1 在 `fw-plus-sample/fw-plus-boot-samples` 新增 TinyEngine starter 样例或测试模块，并接入样例聚合 POM。
- [ ] 4.2 编写启用场景样例配置，验证 Spring Boot 2.7.18 应用上下文可启动，TinyEngine REST Controller、Mapper XML 和 SQL 资源可发现。
- [ ] 4.3 编写禁用场景样例或测试，验证 `fw.plus.tiny-engine.enabled=false` 时 TinyEngine Controller 与 Service 未注册。
- [ ] 4.4 使用 H2 或隔离测试数据源验证资源加载，避免默认自动修改宿主真实数据库。

## 5. 构建检查

- [ ] 5.1 运行 `./fw-plus-core/mvnw -f fw-plus-boot/pom.xml validate` 或等价 Boot 聚合校验，确认新增模块 POM 可解析。
- [ ] 5.2 运行 TinyEngine starter 相关模块的编译/测试命令，确认自动配置与样例在 Java 8 约束下通过。
- [ ] 5.3 运行 `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`，确认样例聚合器能解析 TinyEngine sample。
- [ ] 5.4 检查未修改 Magic-API 2.2.2 版本，且未引入 Spring Boot 3 或 JDK 17 专属 API。
- [ ] 5.5 检查 TinyEngine Boot starter、样例和文档未依赖旧 LangChain4j 专用 model/store artifact，且没有直接引入 AI provider SDK。

```

## openspec/changes/add-tiny-engine-boot-starter/specs/tiny-engine-boot-starter/spec.md

- Source: openspec/changes/add-tiny-engine-boot-starter/specs/tiny-engine-boot-starter/spec.md
- Lines: 1-86
- SHA256: b37ac132e0c4f70362f0d114798a48b0240ef33d08ec961e6653e483bd6e806b

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: TinyEngine starter dependency
系统 SHALL 在 `fw-plus-boot` 中提供 TinyEngine Spring Boot starter，使宿主 Spring Boot 2.7.18 应用能够通过引入 starter 获得 TinyEngine 自动配置能力。

#### Scenario: Host imports starter
- **WHEN** 宿主应用引入 TinyEngine starter 并满足 `integrate-tiny-engine-core` 产物依赖
- **THEN** TinyEngine 自动配置类、属性绑定、Mapper 扫描配置、Boot 层 Controller 适配注册配置和资源加载配置 SHALL 出现在宿主应用 classpath 中

#### Scenario: Starter follows boot module style
- **WHEN** 维护者检查 `fw-plus-boot-project/fw-plus-boot-starters`
- **THEN** TinyEngine starter MUST 按现有 starter 模块风格接入聚合 POM，并继续使用 `${revision}` 版本体系

#### Scenario: Starter follows neutral AI boundary
- **WHEN** 维护者检查 TinyEngine Boot starter、样例和文档的 AI 相关依赖或扩展说明
- **THEN** 系统 MUST NOT 新增旧 LangChain4j 专用 model/store artifact、旧 starter 坐标或直接 AI provider SDK 依赖，后续 TinyEngine AI/RAG 能力 MUST 通过 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 中立抽象衔接

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

```

Full source: openspec/changes/add-tiny-engine-boot-starter/specs/tiny-engine-boot-starter/spec.md
