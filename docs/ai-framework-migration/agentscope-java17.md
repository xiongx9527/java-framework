# 从 LangChain4j 迁移到 AgentScope 2 Java（Java 17）

## 前置条件

Spring Boot 2.7 可以运行在 Java 17；本方案选择 Spring Boot 3.x 是为了把 AgentScope 2 Java 及其高版本依赖隔离到目标生态，不代表 Java 17 必须使用 Boot 3。迁移前继续保留 Java 8 / Boot 2.7 / LangChain4j 默认线路。

AgentScope 2 Java 在本方案中是进程内 SDK adapter，不是外部 HTTP、MCP 或 A2A 桥接。后续 change 必须先做 SDK 版本探针，验证其 Java/Boot 兼容性、发布坐标、模型 provider、同步/流式 API、工具调用与依赖冲突；不能用协议桥接测试替代 SDK 契约测试。

## Maven Parent 与 Starter

未来在独立 `fw-plus-ai-j17` reactor 中创建 Java 17 / Boot 3.x parent。计划 starter 坐标为 `com.xiongx9527.fw.plus.ai:fw-plus-ai-agentscope-j17-starter:${revision}`；该 artifact 当前不存在，不能加入应用 POM。

独立 parent 管理经探针确认的 AgentScope 2 Java SDK，starter 只依赖已发布的 Java 8 中立 model/store artifact 和必要 SDK。它不得进入 Core BOM、Java 8 reactor 或默认 LangChain4j 路径的传递依赖图。

后续自动配置仅在显式 `framework=agentscope`、SDK class 存在、adapter 唯一、provider 与能力经验证且配置完整时装配。缺失任一条件都必须在启动期给出诊断，不创建 registry 条目或半初始化 Bean，不把远程协议 client 或 LangChain4j 对象冒充 AgentScope adapter。

## 同 key 配置迁移

旧 LangChain4j 模型配置继续保留同一业务 key：

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

真实 AgentScope adapter 交付后，模型可按未来中立绑定约定切换，key `knowledge-chat` 不变：

```yaml
fw-plus:
  ai:
    model:
      default-model: knowledge-chat
      models:
        knowledge-chat:
          framework: agentscope
          provider: openai
          type: chat
          properties:
            model-name: gpt-4o-mini
    embedding-store:
      default-store: knowledge
      stores:
        knowledge:
          framework: langchain4j
          provider: qdrant
          type: vector
```

在 SDK 版本探针证明 AgentScope 2 Java 具备可映射的向量 store 能力前，不得把 `knowledge` 改为 `framework=agentscope`。允许模型与 store 使用不同 framework，但两个 registry key 均保持稳定。同 key 的中立配置优先于旧配置，迁移日志不得输出凭证或完整 properties。

## Java API 迁移

业务代码依赖 `AiModelRegistry` 和 `AiEmbeddingStoreRegistry`，不直接依赖 AgentScope session/agent/model 类：

```java
AiModel model = aiModelRegistry.get("knowledge-chat");
AiEmbeddingStore store = aiEmbeddingStoreRegistry.get("knowledge");
AiEmbeddingSearchResult result = store.search(
        new AiEmbeddingQuery(queryVector, 10, 0.75d));
```

当前 `AiModel` 仅包含 key/framework/provider/type 元数据，没有通用 agent、chat 或 tool-call 执行方法。后续 change 必须根据真实 SDK 能力扩展中立契约或引入经评审的能力门面，并由 adapter 内部持有 AgentScope 原生对象。不得暴露 AgentScope 类型到 Java 8 API，也不得把未验证的 agent orchestration 等同于普通 chat model 能力。

## Magic-API 影响

Magic-API 的 `model[...]`、`vector[...]` 约定以及 `model['knowledge-chat']`、`vector['knowledge']` 和动态资源 key 均保持不变。模型切到 AgentScope 时，向量 store 可以继续由已经支持的 framework 提供，不要求两个 key 同时迁移。

真实 AgentScope adapter 交付前，资源声明 `framework=agentscope` 必须受控失败；不得创建半初始化 Bean、空 agent、HTTP/MCP/A2A 替身或 LangChain4j 回退。错误只报告 key/framework/provider/能力并保持凭证脱敏。

## 验证

当前只运行文档校验和 Java 8 隔离检查：

```bash
zsh docs/ai-framework-migration/verify-migration-docs.sh
./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate
! rg -n -i --glob 'pom.xml' 'spring-ai|spring-ai-alibaba|agentscope' fw-plus-core/fw-plus-project
```

以下命令仅是未来 reactor 创建后的验收模板，当前不能成功运行：

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml -pl fw-plus-ai-agentscope-j17-starter dependency:tree test
```

未来测试至少覆盖：SDK 版本探针、Java 17 编译、Boot 3 条件装配、进程内 SDK 调用、同步/流式与工具能力的逐项声明、缺 SDK/adapter/provider 时无半初始化 Bean、真实模型端点、脱敏和 Java 8 依赖隔离。没有经验证的原生 store 能力时，不得注册 `AiEmbeddingStoreAdapter`。

## 回滚

1. 停用 `knowledge-chat` 的 AgentScope 中立定义。
2. 恢复同 key 的 `fw-plus.ai.langchain4j.model` 配置和既有 LangChain4j starter；`knowledge` store key 无需变化。
3. 移除未来 AgentScope starter，不修改 Magic-API `model[...]`、`vector[...]` 或动态资源 key。
4. 重新执行 LangChain4j model/store、Magic-API 与 Java 8 依赖隔离回归；确认没有残留 AgentScope SDK 依赖。

## 当前支持状态

本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。

当前完整默认实现仍是 LangChain4j。AgentScope 2 Java 的 Maven 坐标、版本、模型/agent 能力、向量能力与真实进程内调用均未形成验证证据；`framework=agentscope` 只是未来配置值，当前不可启用。
