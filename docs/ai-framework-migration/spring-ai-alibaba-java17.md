# Spring AI Alibaba adapter 当前实现说明（Java 17 / Boot 3）

## 当前支持状态

Spring AI Alibaba 与通用 Spring AI 共用 `com.xiongx9527.fw.plus:fw-plus-ai-spring-adapter`。Alibaba framework 标识为 `spring-ai-alibaba`，首批 provider 为 `dashscope`；在 Java 17、Spring Boot 3.5.8、Spring AI 1.1.2、Spring AI Alibaba 与 extensions 1.1.2.2 组合下提供同步 `CHAT` 与 `EMBEDDING`。

Alibaba 模型实现上游 `ChatModel` / `EmbeddingModel`，因此共享中立 facade、usage、timeout 和异常映射；系统注册独立 `AiModelAdapter` 以保证 framework 精确选择。首版不实现 Alibaba `AiEmbeddingStoreAdapter` 或 `VectorStore` bridge。

## 前置条件

1. 应用使用 Java 17 / Boot 3.5.8。
2. 显式引入 `fw-plus-ai-spring-adapter` 与 `com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope`。
3. 官方 Boot 3.5.7 对照组合和项目 Boot 3.5.8 组合的 BOM/API 探针必须通过。
4. DashScope client、凭据、HTTP client 和 observation 由官方 starter/Spring 管理。

## Maven Parent 与 Starter

不创建 Alibaba 专用 fw-plus module 或 starter；模块仍是：

```text
fw-plus-ai-spring/
└── fw-plus-ai-spring-adapter/
```

应用额外引入官方 provider：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-spring-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
```

中立 starter 与 adapter 都不传递 DashScope starter。仓库不存在 `fw-plus-ai-j17`、`fw-plus-ai-spring-alibaba` 或 `*-j17-starter`。

## 同 key 配置迁移

```yaml
fw-plus:
  ai:
    model:
      models:
        knowledge-chat:
          framework: spring-ai-alibaba
          provider: dashscope
          type: CHAT
          properties:
            beanName: dashScopeChatModel
```

`spring-ai-alibaba` 与 `spring-ai`、`langchain4j` 可并存。显式 Bean 必须是已知 DashScope Chat/Embedding 类型且 provider 为 `dashscope`；不能由通用 framework 跨认领。未指定 beanName 时需提供唯一、匹配 framework/provider/type 的 `SpringAiProviderBinding`。

## Java API 迁移

业务继续依赖 `AiModelRegistry`、`AiChatModel`、`AiEmbeddingModel`。DashScope 与 Alibaba 类型只存在于 provider starter、adapter 条件装配和测试；不会成为中立 API 参数、返回值或 native accessor。

## Magic-API 影响

`model[...]` 的 key 与中立 facade 不变。缺 Alibaba SDK、Chat/Embedding 某一能力、Bean 或 binding 时，仅对应能力独立 back off/失败，不影响通用 Spring AI 或 LangChain4j，也不产生半初始化 registry 条目。

`vector[...]` 不因 DashScope 实现了上游模型接口而获得支持。只有后续 provider bridge 能完整保留预计算向量、ID、metadata、score/filter 与幂等 delete 语义时，才能新增 store adapter。

## 验证

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

mvn -f fw-plus-core/pom.xml \
  -pl fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter \
  -am test

mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am test -Dtest=SpringAiAdapterAutoConfigurationTest
```

验证应覆盖 DashScope Chat/Embedding class backoff、两个 Spring framework 与 LangChain4j 共存、cross-framework Bean 拒绝、batch embedding 顺序、timeout、异常脱敏及中立 starter 依赖边界。真实 DashScope 调用仅在受控凭据下运行，未运行时在验收报告中明确记录。

## 回滚

回滚按完整版本恢复 adapter/BOM/provider starter。需要切回 LangChain4j 时只改 framework 与应用依赖，保留业务 key。禁止引用不存在的 Alibaba j17 starter，或将旧 artifact 混入当前发布线。
