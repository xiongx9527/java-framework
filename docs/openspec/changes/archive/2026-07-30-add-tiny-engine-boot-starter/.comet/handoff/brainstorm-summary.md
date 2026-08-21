# Brainstorm Summary

- Change: add-tiny-engine-boot-starter
- Date: 2026-07-30

## 确认的技术方案

按当前 OpenSpec 设计继续实现 TinyEngine Boot starter。Boot 层只负责自动配置、starter 包装、Controller 薄适配、Mapper/XML/SQL 资源发现和样例验证；TinyEngine 领域模型、Service、Mapper、动态 SQL 与 SQL 脚本继续以 `integrate-tiny-engine-core` 迁入后的 `fw-plus-tiny-engine-core` 为事实源。当前 core artifactId 为 `fw-plus-tiny-engine-core`，包前缀为 `com.xiongx9527.fw.plus.tinyengine`，Mapper 包为 `com.xiongx9527.fw.plus.tinyengine.mapper`，Mapper XML 位于 `classpath*:mapper/tinyengine/**/*.xml`，SQL manifest 位于 `META-INF/tiny-engine/sql/manifest.properties`，SQL 资源位于 `META-INF/tiny-engine/sql/{h2,mysql,postgresql}/`。

自动配置放在 `fw-plus-boot-autoconfigure` 的 TinyEngine 包下，并同时注册到 `spring.factories` 与 `AutoConfiguration.imports`。属性前缀统一为 `fw.plus.tiny-engine`，总开关控制所有 TinyEngine 自动装配，子开关控制 Web、Mapper、资源与 SQL 能力。Controller 只在 Servlet Web 环境、core classpath 存在、总开关与 `web.enabled` 同时启用时注册，并委托 core Service，不复制领域逻辑。

用户新增确认：必须使用最新 `abstract-ai-model-and-embedding` 结果。TinyEngine Boot starter 本 change 不实现 AI Chat/RAG，但不得重新引入旧 LangChain4j 专用 model/store artifact 或直接 provider SDK；后续 TinyEngine AI/RAG 必须通过 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 中立 registry/factory 衔接，由当前 LangChain4j adapter starter 或未来 Java 17 adapter 提供具体实现。

## 关键取舍与风险

采用 starter 自动装配而不是要求宿主手写 `@MapperScan`、mapper locations 或 SQL 路径，换取更好的开箱体验；风险是 core 包名或资源路径变化会导致 Boot 配置失配，因此 build 第一项必须重新核对 core POM、包与资源路径。

SQL 默认只做资源发现，不自动修改宿主数据库；显式初始化必须受属性保护，并优先用 H2/隔离数据源验证。Controller 暴露支持宿主 Bean 覆盖，降低路径、安全与异常处理冲突。AI 边界通过 spec/task 检查保护，避免 TinyEngine Boot starter 与旧 LangChain4j 坐标重新耦合。

## 测试策略

使用 `ApplicationContextRunner` 或等价 Spring Boot 测试覆盖启用、禁用、缺 core classpath、非 Servlet Web、宿主 Bean 覆盖、Mapper/XML 资源和 SQL manifest 发现。新增样例或样例测试验证 Boot 2.7.18 下 starter 可解析，启用场景能发现 Controller/Mapper/XML/SQL，禁用场景不注册 TinyEngine Controller/Service。构建检查覆盖 Boot 聚合 validate、TinyEngine starter 相关 compile/test、样例聚合 validate、Magic-API 版本不变、无 Spring Boot 3/JDK 17 API、无旧 LangChain4j 专用 model/store 坐标和无直接 AI provider SDK。

## Spec Patch

已补充 `Starter follows neutral AI boundary` 验收场景，并在 tasks/design 中记录最新 `abstract-ai-model-and-embedding` 边界：TinyEngine Boot starter 不新增旧 LangChain4j 专用 model/store artifact；后续 AI/RAG 通过 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 中立抽象。
