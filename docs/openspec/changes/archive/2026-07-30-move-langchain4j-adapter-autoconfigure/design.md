## Context

当前 AI Boot Reactor 同时发布 `fw-plus-ai-boot-autoconfigure` 和 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`。后者包含 46 个生产类、4 个测试类、`spring.factories` 与 `AutoConfiguration.imports`，但所有代码已经位于 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 命名空间。

业务应用通过 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 安装默认 LangChain4j 实现。该 Starter 同时依赖中立 model/store Starter、Core LangChain4j adapter 和独立 autoconfigure artifact。独立 artifact 还被 AI Reactor、Boot BOM、README 和验证命令单独维护。

本仓库保持 Java 8、Spring Boot 2.7.18 与 LangChain4j 0.35.0 基线。具体 provider SDK 继续由业务按需安装，Starter 不得批量传递 provider SDK。

## Goals / Non-Goals

**Goals:**

- 删除独立 LangChain4j adapter autoconfigure Maven 模块和发布坐标。
- 将其源码、资源与测试无行为变化地迁入 `fw-plus-ai-boot-autoconfigure`。
- 保留 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包级隔离和现有条件装配。
- 保留 LangChain4j adapter Starter 作为唯一推荐安装入口。
- 保持 provider SDK 缺失时可安全启动，并保持中立 Starter 不传递具体框架或 provider SDK。

**Non-Goals:**

- 不改变 Core `fw-plus-ai-langchain4j-adapter` 的 SPI 实现。
- 不修改 `fw-plus.ai.model`、`fw-plus.ai.embedding-store` 或 adapter 选择算法。
- 不增加、升级或替换任何 LangChain4j/provider SDK 版本。
- 不扁平化 `langchain4j` Java 子包，不迁移 AskData 业务实现。
- 不保留旧 autoconfigure 坐标的兼容空 POM。

## Decisions

### 1. 完整合并到现有 Boot autoconfigure artifact

将独立模块的 `src/main/java`、`src/test/java` 和 `src/main/resources` 合并到 `fw-plus-ai-boot-autoconfigure`，随后删除整个旧模块目录和 Reactor module 声明。保留 Java 包与类名，因此应用代码、条件注解和 Bean 名称不发生变化。

未采用兼容空 POM，因为它仍会保留一个没有独立职责的公开坐标；未采用只迁移入口配置类，因为 provider 配置、adapter 配置与测试仍会形成分裂所有权。

### 2. 以包级条件装配代替 artifact 级隔离

统一 artifact 的中立自动配置继续位于 `com.xiongx9527.fw.plus.ai.autoconfigure.model`、`store`、`magicapi` 等包；具体框架实现限定在 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*`。LangChain4j 根配置继续以 adapter/Core SDK 类作为 classpath 条件，直接引用可选 provider SDK 类型的 Bean 方法继续保留精确 `@ConditionalOnClass(name = ...)`。

原“整个公共 autoconfigure artifact 不得包含 LangChain4j”测试改为验证包级隔离和传递依赖边界。中立 Core、Starter、Magic-API 和公共 API 仍不得引用具体框架。

### 3. 合并 POM 依赖但保持 optional 边界

旧模块的编译依赖迁入 `fw-plus-ai-boot-autoconfigure`，其中 LangChain4j provider wrapper、provider SDK 和 Core adapter 依赖继续标记为 `optional`。这样统一模块自身可以编译全部配置类，但依赖该 jar 的中立 Starter 不会传递这些依赖。

`fw-plus-ai-langchain4j-adapter-spring-boot-starter` 删除对旧 autoconfigure 坐标的依赖，显式依赖 `fw-plus-ai-boot-autoconfigure`，并继续显式依赖 Core adapter 与中立 model/store Starter。其 Maven Enforcer 规则继续只允许 `langchain4j-core`，禁止批量 provider SDK。

### 4. 合并自动配置元数据并保持注册兼容

把 `LangChain4jAdapterAutoConfiguration` 追加到统一模块的 `spring.factories`。旧模块的 `AutoConfiguration.imports` 路径与内容迁入统一模块，保留 Spring Boot 2.7 当前的双注册行为。注册测试改为从统一模块资源读取并断言目标类可加载、只注册一次且条件装配不变。

### 5. 测试随所有权迁移，保留缺 SDK 回归

四个 adapter 测试类原样迁入统一模块，再调整模块目录定位和边界断言。重点保留：

- 缺少 Milvus、Qdrant、Weaviate 等可选 SDK 时上下文可启动且相关 Bean 不注册。
- 直接链接 provider 类型的 Bean 方法仍声明精确 classpath 条件。
- Starter 的解析依赖不包含被禁止的 provider SDK。
- Reactor、BOM、Starter POM、文档和源码中不再出现旧 artifact 坐标。

## Risks / Trade-offs

- [Risk] 统一 autoconfigure jar 体积增加，并包含具体框架类名。 → 通过 `langchain4j` 子包隔离、optional Maven 依赖和 classpath 条件避免中立应用加载或传递具体实现。
- [Risk] 合并 `spring.factories` 与 `AutoConfiguration.imports` 后重复注册同一配置。 → 元数据测试同时读取两种注册文件，并通过上下文测试确认配置只产生一组 Bean。
- [Risk] POM 依赖迁移时遗漏 provider 编译依赖，导致统一模块无法编译。 → 以旧模块 POM 为清单逐项迁移，并运行统一模块全量测试与 dependency tree/enforcer 检查。
- [Risk] 删除公开坐标导致直接依赖旧 autoconfigure artifact 的应用解析失败。 → README 和迁移文档要求改用保留的 adapter Starter；不承诺兼容旧坐标。
- [Risk] 原边界测试被放宽过度。 → 仅对 `autoconfigure.langchain4j` 子包建立例外，继续扫描其他中立包、Starter POM、公开签名和 Magic-API。

## Migration Plan

1. 先补充/调整结构测试，使其期望旧模块和 BOM 坐标删除、统一模块拥有 LangChain4j 自动配置。
2. 将旧模块源码、资源和测试移动到 `fw-plus-ai-boot-autoconfigure`，合并 POM optional 依赖与注册元数据。
3. 修改 adapter Starter、AI Reactor 和 Boot BOM，删除旧模块目录与坐标。
4. 更新 README、迁移文档和验证命令，统一只引用 Boot autoconfigure 与 adapter Starter。
5. 运行 JDK 8 下的统一模块测试、Starter Maven Enforcer、AI Reactor validate/test、文档检查和 `git diff --check`。

回滚时整体恢复旧模块目录、Reactor/BOM 坐标和 Starter 依赖；不得只恢复旧 POM 而不恢复其源码与资源。

## Open Questions

无。模块归属、Starter 保留、包结构和兼容策略均已由用户确认。
