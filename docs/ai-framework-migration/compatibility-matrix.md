# AI 框架兼容矩阵

## 当前构建基线

| 线路 | Java / Spring 基线 | artifact | 当前状态 | 依赖边界 |
| --- | --- | --- | --- | --- |
| 中立模型与向量存储契约 | Java 17 / Spring Boot 3.5.8 | `fw-plus-ai-model`、`fw-plus-ai-store-embedding` | 已实现并由中立 starter 自动配置 | 公开签名不含具体 AI framework 类型；中立 starter 禁止传递 adapter/SDK |
| LangChain4j Adapter | Java 17 / Spring Boot 3.5.8 | `fw-plus-ai-langchain4j-adapter` | 当前支持模型与向量 store，业务显式安装 | SDK 类型只在 adapter/Boot provider 层 |
| Spring AI | Java 17 / Spring Boot 3.5.8 | `fw-plus-ai-spring-adapter` | Spring AI 1.1.2；framework=`spring-ai`；同步 CHAT、EMBEDDING | adapter 只依赖公共 model API；provider starter 由应用显式安装 |
| Spring AI Alibaba | Java 17 / Spring Boot 3.5.8 | 与 Spring AI 共用 `fw-plus-ai-spring-adapter` | Alibaba/Extensions 1.1.2.2；framework=`spring-ai-alibaba`；DashScope CHAT、EMBEDDING | 与通用 Spring AI 共用 facade，独立 adapter 实例与 provenance 校验 |
| AgentScope 2 Java | 未实现 | 无当前 artifact | 仍是后续需求 | 不在本 change 中创建或声明支持 |

仓库不使用也不创建 `fw-plus-ai-j17`、`*-j17-adapter` 或 `*-j17-starter`。当前 Core 与 AI Boot reactor 本身已经是 Java 17 / Boot 3.5.8。

## 能力矩阵

| 能力 | LangChain4j | Spring AI | Spring AI Alibaba |
| --- | --- | --- | --- |
| 同步 CHAT | 支持 | 支持 | DashScope 支持 |
| EMBEDDING | 支持 | 支持 | DashScope 支持 |
| STREAMING_CHAT | LangChain4j 专项路径 | 中立契约未提供 | 中立契约未提供 |
| 中立 VECTOR store | 支持选定 provider | **首版不支持** | **首版不支持** |
| Magic-API `model[...]` | 中立 facade | 中立 facade，已做集成回归 | 中立 facade |
| Magic-API `vector[...]` | 中立 facade | 不注册 store adapter | 不注册 store adapter |

Spring AI 标准 `VectorStore` 使用文本查询并可能自行嵌入，中立 `AiEmbeddingQuery` 使用预计算向量。只有后续 provider bridge 能完整保留向量、ID、metadata、score/filter 与幂等 delete 语义时，才能宣称 VECTOR 支持。

## 隔离规则

1. 业务通过 `AiModelRegistry`、`AiChatModel`、`AiEmbeddingModel` 使用模型；配置继续使用 `fw-plus.ai.model` 与 `fw-plus.ai.embedding-store`，中立 facade 不暴露 native accessor。
2. 应用显式引入 `fw-plus-ai-spring-adapter` 和官方 provider starter；中立 model/store starter、adapter 本身都不传递 provider starter。
3. `spring-ai`、`spring-ai-alibaba`、`langchain4j` adapter 可同时注册，定义必须用 framework 精确选择；失败不静默回退。
4. provider 模型由 Spring 管理。`properties.beanName` 精确选 Bean，或使用唯一 `SpringAiProviderBinding`；supports 不初始化 Bean、不调用远端。
5. Chat/Embedding class 条件相互独立；缺某一 SDK/能力只 back off 对应 Bean。
6. 异常、日志与 actuator 不得包含 API key、Authorization/header、完整 URL、properties 值或原始 provider 响应。

## 已验证命令

固定 Java 17：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

Core adapter 与 API：

```bash
mvn -f fw-plus-core/pom.xml \
  -pl fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter \
  -am test
```

AI Boot 条件装配与 Magic-API model：

```bash
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am test
```

中立 starter 边界：

```bash
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter \
  -am validate
```

依赖审计：

```bash
mvn -f fw-plus-core/pom.xml \
  -pl fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter \
  dependency:tree -Dincludes=org.springframework.ai:*,com.alibaba.cloud.ai:*
```

adapter 依赖树应包含 Spring AI 公共 model API，但不能包含 OpenAI、DashScope 等 provider starter。真实远端 smoke test 只有在提供受控凭据时执行；未执行时应在验收报告中明确记录。

## 当前发布边界

当前发布物实现 Spring AI 与 Spring AI Alibaba 的同步 CHAT/EMBEDDING，并完成离线契约、自动配置、Magic-API model 和依赖边界测试。它不实现 Spring AI/Alibaba VectorStore、AgentScope、远端凭据验证或任何不存在的 j17 reactor/starter。AskData starter 仍不默认引入具体 adapter/provider。
