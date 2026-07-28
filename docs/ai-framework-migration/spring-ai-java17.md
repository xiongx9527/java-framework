# 从 LangChain4j 迁移到 Spring AI（Java 17）

## 前置条件

Spring Boot 2.7 可以运行在 Java 17；本方案选择 Spring Boot 3.x，仅因为目标 Spring AI SDK 属于高版本 Spring 生态，不是升级 Java 17 的必然条件。迁移前必须保留 Java 8 / Boot 2.7 / LangChain4j 基线，并准备独立 JDK 17 构建环境。

后续 change 首先执行 SDK 版本探针，确认候选 Spring AI BOM 与 Boot 3.x、Maven 编译器、`jakarta.*` API、模型 provider 和 VectorStore provider 的实际兼容组合。探针通过前不得把版本写入 Core BOM，也不得声称 chat、embedding 或 vector store 能力受支持。

## Maven Parent 与 Starter

未来在独立 `fw-plus-ai-j17` reactor 中创建 Java 17 / Boot 3.x parent，并由该 parent 管理 Spring AI BOM。计划 starter 坐标为 `com.xiongx9527.fw.plus.ai:fw-plus-ai-spring-ai-j17-starter:${revision}`；该坐标当前不存在，不能加入应用 POM。

starter 只消费已发布的 Java 8 中立 artifact `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 和经探针锁定的 Spring AI 依赖。它不得加入 Java 8 Core reactor/Core BOM，也不得传递依赖 LangChain4j adapter。

后续自动配置必须同时满足：应用显式选择 `framework=spring-ai`、所需 Spring AI class 存在、对应 `AiModelAdapter`/`AiEmbeddingStoreAdapter` 唯一、provider 配置完整。任一条件失败时输出包含 key/framework/provider/能力的受控诊断，不注册 registry 条目或半初始化 Bean，不静默回退到 LangChain4j。

## 同 key 配置迁移

以下是未来中立配置绑定约定，不是当前可运行配置。旧配置先保持不变：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: knowledge-chat
        models:
          knowledge-chat:
            provider: openai
            model-type: chat
            model-name: gpt-4o-mini
      embedding-store:
        default-store: knowledge
        stores:
          knowledge:
            provider: qdrant
```

真实 adapter 交付后，按同 key 改为中立定义，只替换 framework 和目标 SDK 专属 properties：

```yaml
fw-plus:
  ai:
    model:
      default-model: knowledge-chat
      models:
        knowledge-chat:
          framework: spring-ai
          provider: openai
          type: chat
          properties:
            model-name: gpt-4o-mini
    embedding-store:
      default-store: knowledge
      stores:
        knowledge:
          framework: spring-ai
          provider: qdrant
          type: vector
```

兼容期内，同一 key 同时出现在中立配置和旧 LangChain4j 配置时，中立定义优先；日志只报告 key/framework/provider，不输出 apiKey、token、连接串或完整 properties。

## Java API 迁移

业务代码应从 `LangChain4jModelRegistry`、`EmbeddingStoreRegistry` 和框架原生类型迁到 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 及中立数据类型：

```java
AiModel model = aiModelRegistry.get("knowledge-chat");
AiEmbeddingStore store = aiEmbeddingStoreRegistry.get("knowledge");

AiEmbeddingSearchResult result = store.search(
        new AiEmbeddingQuery(queryVector, 10, 0.75d));
```

当前 `AiModel` 只提供 key/framework/provider/type 元数据，不能把尚不存在的推理方法写进业务代码。后续 Spring AI adapter 必须先在中立契约中补齐并测试所需能力，再把 Spring AI `ChatModel`、`EmbeddingModel` 或 `VectorStore` 封装在 adapter 内部；这些 SDK 类型不得成为 Java 8 公共 API 的参数或返回值。

## Magic-API 影响

Magic-API 的 `model[...]`、`vector[...]` 访问约定保持不变；具体 `model['knowledge-chat']`、`vector['knowledge']` 和动态资源 key 不随 framework 切换。Magic-API 插件只面向中立 registry/factory，不直接接收 Spring AI 原生对象。

真实 Spring AI adapter 完成前，资源声明 `framework=spring-ai` 必须明确失败；不得生成半初始化 Bean、空代理或回退到同 key 的 LangChain4j 实例。错误可包含 key/framework/provider/能力，但不得包含凭证。

## 验证

当前可运行的校验只有文档检查和 Java 8 隔离检查：

```bash
zsh docs/ai-framework-migration/verify-migration-docs.sh
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
! rg -n -i --glob 'pom.xml' 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project
```

以下命令仅是未来 reactor 创建后的验收模板，当前不得执行并据此宣称成功：

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml -pl fw-plus-ai-spring-ai-j17-starter dependency:tree test
```

未来测试至少覆盖：SDK 版本探针、Java 17 编译、Boot 3 条件装配、缺 class/缺 adapter/缺配置时无半初始化 Bean、model/store 能力契约、真实模型与向量后端、错误脱敏，以及 Java 8 依赖树隔离。

## 回滚

1. 将 `knowledge-chat` 和 `knowledge` 的中立定义停用或移除。
2. 恢复同 key 的 `fw-plus.ai.langchain4j.model`、`fw-plus.ai.langchain4j.embedding-store` 配置和既有 LangChain4j starter。
3. 从 Java 17 应用移除未来 Spring AI starter；不要修改 `model[...]`、`vector[...]` 或动态资源 key。
4. 重新运行 LangChain4j model/store、Magic-API 与依赖隔离回归。兼容旧前缀的能力在明确的后续破坏性 change 前不得删除。

## 当前支持状态

本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。

当前完整默认实现仍是 LangChain4j。Spring AI 的 BOM 版本、provider 能力、条件装配、真实 SDK 对象封装与集成测试均属于后续 change；在这些证据齐备前，`framework=spring-ai` 只是未来配置值，不可启用。
