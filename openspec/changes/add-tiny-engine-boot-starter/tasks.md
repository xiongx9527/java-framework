## 1. Core 依赖确认

- [x] 1.1 确认 `integrate-tiny-engine-core` 已提供 TinyEngine core 模块 artifactId、包名、Service、Mapper、Mapper XML 和 SQL 资源路径。
- [x] 1.2 核对 core TinyEngine 模块已兼容 Java 8、Spring Boot 2.7.18、MyBatis-Plus 版本体系，并确认 Boot change 不需要复制领域逻辑。
- [x] 1.3 明确需要从 Boot 层托管或传递的 TinyEngine core 依赖，以及是否需要在 Boot BOM 中增加依赖管理。
- [x] 1.4 核对最新 `abstract-ai-model-and-embedding` 结果，确认 TinyEngine Boot starter 不新增旧 LangChain4j 专用 model/store 坐标；后续 AI/RAG 只通过 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 中立抽象衔接。

## 2. 自动配置

- [x] 2.1 在 `fw-plus-boot-autoconfigure` 新增 TinyEngine 自动配置包和顶层 AutoConfiguration。
- [x] 2.2 新增 `fw.plus.tiny-engine` 配置属性类，覆盖 `enabled`、`web.enabled`、`service.enabled`、`mapper.enabled`、`resources.enabled`、`sql.enabled`、API base path、Mapper XML 和 SQL manifest 位置等必要配置。
- [x] 2.3 使用条件注解确保缺少 TinyEngine core classpath、非 Servlet Web 环境、缺少 `SqlSessionFactory` 或总开关关闭时不注册不安全的 TinyEngine Bean。
- [x] 2.4 在 Boot 层实现 TinyEngine REST Controller 薄适配并注册暴露配置，Controller 委托 core Service，且允许宿主应用通过自定义 Bean 覆盖默认装配。
- [x] 2.5 注册 TinyEngine Mapper 扫描与 Mapper XML 资源加载配置，确保宿主无需手写 `@MapperScan` 或复制 XML。
- [x] 2.6 注册 TinyEngine SQL 资源发现能力，默认不自动修改宿主数据库，自动初始化必须受显式属性控制。
- [x] 2.7 将 TinyEngine 自动配置加入 `META-INF/spring.factories` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 3. Starter 与依赖管理

- [x] 3.1 在 `fw-plus-boot-project/fw-plus-boot-starters` 新增 TinyEngine Spring Boot starter 模块。
- [x] 3.2 将 TinyEngine starter 接入 `fw-plus-boot-starters` 聚合 POM，并保持现有 `${revision}` 版本管理方式。
- [x] 3.3 为 TinyEngine starter 声明 `fw-plus-boot-starter`、`fw-plus-boot-autoconfigure` 间接能力、MyBatis-Plus/Web 相关依赖和 core TinyEngine 模块依赖。
- [x] 3.4 如仓库现有 BOM 需要，更新 `fw-plus-boot-dependencies` 以托管 TinyEngine starter 与 core TinyEngine 依赖。

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
