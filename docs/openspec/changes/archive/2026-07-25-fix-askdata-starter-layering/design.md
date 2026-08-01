## 修复方案

将 AskData 实现整体迁移到 `fw-plus-ai-boot-autoconfigure`，保持原有 Java 包名 `com.xiongx9527.fw.plus.ai.askdata` 不变。Spring Boot 自动配置资源也迁入 autoconfigure，使该模块成为 AskData 自动配置和默认实现的承载模块。

`fw-plus-ai-askdata-spring-boot-starter` 保留为纯 starter：只声明对 `fw-plus-ai-boot-autoconfigure` 和 `fw-plus-ai-langchain4j-spring-boot-starter` 的依赖，不再包含业务源码、自动配置资源或实现测试。

测试随实现迁移到 autoconfigure，并继续运行原 AskData 测试集，确保迁移只改变模块位置，不改变行为。

## 根因

前序实现把 starter 当成实现模块使用，导致业务代码落在 starter 内。该仓库的 starter 模块应作为依赖聚合入口，实际自动配置和实现应由 autoconfigure 模块承载。

## 验证

- 结构验证：starter `src/main/java` 下不再存在 Java 业务源码。
- 行为验证：在 `fw-plus-ai-boot-autoconfigure` 中运行 AskData 相关测试。
- 入口验证：编译 `fw-plus-ai-askdata-spring-boot-starter`，确认 starter POM 仍能引入所需依赖。
