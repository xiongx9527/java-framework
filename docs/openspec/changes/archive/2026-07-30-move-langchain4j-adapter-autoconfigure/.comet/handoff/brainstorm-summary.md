# Brainstorm Summary

- Change: move-langchain4j-adapter-autoconfigure
- Date: 2026-07-30

## 确认的技术方案

已确认删除独立 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` Maven 模块和坐标，将其 46 个生产类、4 个测试类及自动配置资源迁入 `fw-plus-ai-boot-autoconfigure`。Java 包继续保持 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*`，不做根包扁平化；`fw-plus-ai-langchain4j-adapter-spring-boot-starter` 继续作为业务应用安装默认 LangChain4j 实现的公开入口。

源码与测试目标路径没有重名冲突，唯一文件路径冲突是 `META-INF/spring.factories`，必须合并条目而不是覆盖。旧模块依赖并入统一 autoconfigure POM 时继续使用 optional，adapter Starter 仍显式依赖 Core adapter，具体 provider SDK 继续由业务按需安装。

用户确认自动配置同时保留 `spring.factories` 与 `AutoConfiguration.imports`。实现时合并统一模块已有 `spring.factories` 条目，并把独立模块仅包含 `LangChain4jAdapterAutoConfiguration` 的 imports 文件迁入统一模块；元数据测试同步改为验证两种注册资源都包含正确入口。

## 关键取舍与风险

完整合并会让统一 autoconfigure jar 包含 LangChain4j 类名，但具体实现继续由 `autoconfigure.langchain4j` 子包、optional 依赖和 `@ConditionalOnClass` 隔离；中立 Starter 不传递 adapter/provider SDK。删除旧坐标是明确的破坏性变化，不提供兼容空 POM。

双注册会改变统一模块之前只使用 `spring.factories` 的约束，但用户明确选择保留独立模块现有注册行为。测试必须确认两种资源只列出同一个 adapter 根配置，并通过应用上下文验证不会产生重复 Bean。

## 测试策略

先修改结构与边界测试形成 RED，再迁移源码/POM/资源并转为 GREEN。迁移后保留缺失 adapter、缺失 Milvus/Qdrant/Weaviate/Azure 等 SDK 的 `FilteredClassLoader` 启动测试，保留 Bean 方法精确 classpath 条件反射断言；同时验证统一模块注册、Starter Maven Enforcer、BOM/Reactor 旧坐标删除和文档引用清理。

## Spec Patch

无。现有 delta spec 已允许使用 Spring Boot 2.7 支持的元数据注册，没有要求必须保留 `AutoConfiguration.imports`。
