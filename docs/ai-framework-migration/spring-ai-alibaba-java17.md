# Spring AI Alibaba adapter 后续实施蓝图（Java 17 / Boot 3）

## 当前支持状态

本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。Spring AI Alibaba adapter 本次没有实现，也不存在可解析的 `fw-plus-ai-spring-ai-alibaba-j17-starter`。当前 AI Boot BOM 中出现的版本属性不等于 Java 17 / Boot 3 adapter 已完成兼容验证；后续 change 必须重新以目标 release 的 BOM 做 SDK 版本探针。

该 adapter 属于独立 Java 17 reactor，不能加入 Java 8 Core BOM、当前 `fw-plus-ai` reactor 或 LangChain4j starter 的传递依赖。中立 Core 不暴露 Spring AI Alibaba、Spring AI、DashScope 或任何 provider 原生类型。

## 前置条件

Spring Boot 2.7 可以运行在 Java 17，但本路线将 Spring AI Alibaba 放到独立 Java 17 / Boot 3 生态，避免 `jakarta.*` 与高版本 Spring 进入当前 Java 8 Core。SDK 版本探针先固定可编译的 SDK/BOM 组合。

## Maven Parent 与 Starter

在 `fw-plus-ai-j17` 中设置 Java 17 parent、独立 dependency-management BOM 与下列模块：

```text
fw-plus-ai-spring-ai-alibaba-j17-adapter/
fw-plus-ai-spring-ai-alibaba-j17-autoconfigure/
fw-plus-ai-spring-ai-alibaba-j17-starter/
```

parent 锁定经验证的 Spring Boot 3、Spring AI、Spring AI Alibaba 和 provider SDK 版本。自动配置使用 Boot 3 `@AutoConfiguration`，在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，并全程使用 `jakarta.*` 命名空间；不要在此 reactor 引入 Boot 2 `spring.factories` 约定。

## 同 key 配置迁移

实现 `AiModelAdapter` 和/或 `AiEmbeddingStoreAdapter` 时，framework 恒为 `spring-ai-alibaba`，每个 adapter 显式声明支持的 provider/type。允许的中立配置形式如下，provider/type 名称由 SDK 探针后的能力矩阵决定：

```yaml
fw-plus:
  ai:
    model:
      models:
        knowledge-chat:
          framework: spring-ai-alibaba
          provider: dashscope
          type: CHAT
          properties: {}
```

显式 framework 仅匹配同名 adapter；缺省时只允许唯一候选。不能因 DashScope/provider 名称相同而跨 adapter 推断，也不能回退到 LangChain4j。原生 Chat、Embedding、VectorStore、Document 与 provider client 仅存在于 Java 17 adapter 内部。

## Java API 迁移

应用侧持续使用 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 和中立 facade。Spring AI Alibaba、Spring AI、DashScope/provider client 仅存在于 Java 17 adapter 内部，不得成为 Java 8 公共 API 的参数或返回值。

## Magic-API 影响

`model[...]` 与 `vector[...]` 的业务 key 保持稳定，MagicScript 不能获得 SDK 原生对象。缺 class、缺配置、零/多候选或能力未验证时必须脱敏失败，不注册半初始化 Bean，且不得回退 LangChain4j。

## 验证

1. 版本探针：验证 BOM、JDK 17、Boot 3、Spring Framework 6、`jakarta.*`、SDK 字节码及传递依赖，固定有效版本。
2. SPI 契约：model/store 分别验证唯一候选、零候选、多候选、显式 framework、未支持 provider/type 与脱敏错误。
3. Boot 3 条件装配：缺 SDK class、缺 provider SDK、缺必要配置或 adapter 不唯一时不产生 registry 条目或半初始化 Bean。
4. provider 能力：为每个声明的 chat、stream、embedding、vector 能力提供 SDK mock/fixture 测试；未实现能力受控失败。
5. 集成验证：在受控密钥下验证至少一个实际模型端点和一个实际向量后端，并审查日志和异常脱敏。
6. 隔离验证：当前 Java 8 Core 的 dependency tree/effective POM 中不得出现目标 SDK、Boot 3 或 Jakarta API。

实际 reactor 出现后才执行：

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml \
  -pl fw-plus-ai-spring-ai-alibaba-j17-adapter,fw-plus-ai-spring-ai-alibaba-j17-autoconfigure -am test
```

## 回滚

在这些测试通过前，`framework: spring-ai-alibaba` 只能作为未来规划值；生产切换仍使用已验证的 LangChain4j adapter。回滚按完整发布版本进行，禁止将未来 Java 17 starter 与当前 Java 8 / Boot 2 发布物或旧 artifact 混装。
