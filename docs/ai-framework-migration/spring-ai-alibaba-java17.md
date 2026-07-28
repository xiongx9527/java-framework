# 从 LangChain4j 迁移到 Spring AI Alibaba（Java 17）

## 前置条件

Spring Boot 2.7 可以运行在 Java 17；本方案使用 Spring Boot 3.x，是为了匹配 Spring AI Alibaba 及其 Spring AI 基线，不是 Java 17 对 Boot 版本的强制要求。现有 Java 8 / Boot 2.7 / LangChain4j 线路继续作为可回滚基线。

后续 change 必须先做 SDK 版本探针：验证 Spring AI Alibaba BOM、其对应 Spring AI BOM、Boot 3.x、DashScope 等 provider starter、`jakarta.*` API 和 Maven 插件的兼容组合。Spring AI Alibaba 与基础 Spring AI 的版本必须成组锁定，不能分别采用“最新版本”拼装。

## Maven Parent 与 Starter

未来在独立 `fw-plus-ai-j17` reactor 中创建 Java 17 / Boot 3.x parent。计划 starter 坐标为 `com.xiongx9527.fw.plus.ai:fw-plus-ai-spring-ai-alibaba-j17-starter:${revision}`；当前没有该 artifact，应用 POM 不得引用它。

独立 parent 负责 Spring AI Alibaba 与 Spring AI 的 dependency management，starter 只消费 Java 8 中立 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` artifact 和探针确认的目标 SDK。它不进入 Core BOM、Java 8 reactor 或默认 LangChain4j starter 的传递依赖图。

后续自动配置必须在显式 `framework=spring-ai-alibaba`、SDK class 存在、provider 能力已声明且配置完整时才创建 adapter。还要避开基础 Spring AI starter 的重复 adapter/自动配置；存在零个或多个候选 adapter 时都应启动失败，不注册 registry 条目或半初始化 Bean，不静默改用基础 Spring AI 或 LangChain4j。

## 同 key 配置迁移

以下旧配置继续作为回滚入口，模型和 store key 分别固定为 `knowledge-chat`、`knowledge`：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: knowledge-chat
        models:
          knowledge-chat:
            provider: dashscope
            model-type: chat
            model-name: qwen-plus
      embedding-store:
        default-store: knowledge
        stores:
          knowledge:
            provider: redis
```

真实 adapter 交付后，未来中立绑定约定如下；该片段当前不可启用：

```yaml
fw-plus:
  ai:
    model:
      default-model: knowledge-chat
      models:
        knowledge-chat:
          framework: spring-ai-alibaba
          provider: dashscope
          type: chat
          properties:
            model-name: qwen-plus
    embedding-store:
      default-store: knowledge
      stores:
        knowledge:
          framework: spring-ai-alibaba
          provider: redis
          type: vector
```

同一 key 同时出现在中立和旧 LangChain4j 配置时，中立定义优先。迁移日志只记录 key/framework/provider，不得回显 apiKey、secret、token、连接串或完整 properties。

## Java API 迁移

业务代码不应注入 Spring AI Alibaba、Spring AI 或 LangChain4j registry，而应依赖中立 API：

```java
AiModel model = aiModelRegistry.get("knowledge-chat");
AiEmbeddingStore store = aiEmbeddingStoreRegistry.get("knowledge");
AiEmbeddingSearchResult result = store.search(
        new AiEmbeddingQuery(queryVector, 10, 0.75d));
```

当前 `AiModel` 仅暴露可诊断元数据，尚无可伪造的通用推理方法。后续 adapter 必须先为实际使用的 chat/embedding/store 能力扩展或实现中立契约并完成测试，再把 Spring AI Alibaba/DashScope 原生对象封装在 adapter 内部。目标 SDK 类型不得进入 Java 8 公共签名。

## Magic-API 影响

Magic-API 的 `model[...]`、`vector[...]` 访问约定以及具体 `model['knowledge-chat']`、`vector['knowledge']` 和动态资源 key 保持不变。插件只从中立 registry/factory 取对象，不感知 DashScope 或 Spring AI Alibaba 原生类型。

真实 adapter 尚未实现时，资源声明 `framework=spring-ai-alibaba` 必须明确失败，错误只包含 key/framework/provider/能力。不得创建半初始化 Bean、空实现，也不得因为基础 Spring AI class 恰好存在而错误装配。

## 验证

当前执行文档与 Java 8 隔离校验：

```bash
zsh docs/ai-framework-migration/verify-migration-docs.sh
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
! rg -n -i --glob 'pom.xml' 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project
```

以下是未来 reactor 创建后的验收模板，当前不存在对应 POM：

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml -pl fw-plus-ai-spring-ai-alibaba-j17-starter dependency:tree test
```

未来测试至少覆盖：成组 SDK 版本探针、Java 17 编译、Boot 3 条件装配、与基础 Spring AI 自动配置的冲突、缺依赖/缺配置时无半初始化 Bean、能力拒绝、真实 DashScope 模型与目标向量后端、凭证脱敏和 Java 8 依赖隔离。

## 回滚

1. 停用 `knowledge-chat`、`knowledge` 的 Spring AI Alibaba 中立定义。
2. 恢复同 key 的旧 LangChain4j 配置和既有 starter。
3. 移除未来 Spring AI Alibaba starter 及其 BOM；不要改动 Magic-API `model[...]`、`vector[...]` 或动态资源 key。
4. 重新执行 LangChain4j、Magic-API 和 Java 8 依赖隔离回归，确认没有残留的 Spring AI Alibaba/Spring AI 传递依赖。

## 当前支持状态

本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。

当前完整默认实现仍是 LangChain4j。Spring AI Alibaba 与 Spring AI 的版本组合、provider 能力、自动配置排他性和真实 SDK 集成均未验证；`framework=spring-ai-alibaba` 只是未来配置值，当前不可启用。
