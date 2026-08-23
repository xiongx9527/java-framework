# Spring AI adapter 当前实现说明（Java 17 / Boot 3）

## 当前支持状态

当前 `springboot3` 发布线已经提供 `com.xiongx9527.fw.plus:fw-plus-ai-spring-adapter`。该 artifact 位于 `fw-plus-ai-spring` 聚合内，在 Java 17、Spring Boot 3.5.8 与 Spring AI 1.1.2 组合下构建；它实现同步 `CHAT` 与 `EMBEDDING`，framework 标识为 `spring-ai`。

首版不实现 `AiEmbeddingStoreAdapter`，也不把 Spring AI 的文本型 `VectorStore` 适配成中立预计算向量查询。仓库没有、也不需要 `fw-plus-ai-j17` 独立 reactor 或 `*-j17-starter`。

## 前置条件

1. 使用 Java 17 与 Spring Boot 3.5.8。
2. 应用显式引入中立 model starter、`fw-plus-ai-spring-adapter` 及一个官方 Spring AI provider starter。
3. Spring AI 1.1.2 与 Boot 3.5.7（官方对照）及项目 Boot 3.5.8 的 API 编译和依赖树探针必须保持通过。
4. provider client 必须由 Spring 管理；adapter 不反射创建 client，也不读取凭据。

## Maven Parent 与 Starter

Core 模块布局：

```text
fw-plus-ai/
└── fw-plus-ai-spring/
    └── fw-plus-ai-spring-adapter/
```

应用示例：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-model-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-spring-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

fw-plus 不提供额外 Spring AI starter，也不从 adapter 或中立 starter 传递具体 provider starter。Boot 自动配置位于现有 `fw-plus-ai-boot-autoconfigure`，通过 `AutoConfiguration.imports` 注册。

## 同 key 配置迁移

继续使用中立配置前缀和业务 key：

```yaml
fw-plus:
  ai:
    model:
      enabled: true
      default-model: knowledge-chat
      models:
        knowledge-chat:
          framework: spring-ai
          provider: openai
          type: CHAT
          properties:
            beanName: openAiChatModel
```

`properties.beanName` 按名称精确选择 Spring 管理的 `ChatModel` 或 `EmbeddingModel`。未指定时，应用必须提供唯一的 `SpringAiProviderBinding`。零候选、多候选、类型错误或 framework 不匹配均脱敏失败，不回退 LangChain4j。

## Java API 迁移

业务继续面向 `AiModelRegistry`、`AiChatModel`、`AiEmbeddingModel` 和中立 request/result。Spring AI `ChatModel`、`EmbeddingModel`、`Prompt` 与 response 类型只存在于 adapter 和自动配置边界；中立 facade 不提供 native accessor。

- Chat 映射文本、finish reason、usage 与实际 model key。
- Embedding 使用批量 response，按 `Embedding.index` 恢复输入顺序并防御性复制向量。
- 未白名单化的 per-request parameters 明确拒绝。
- timeout/transport 映射为可重试 infrastructure failure；请求拒绝和畸形响应映射为不可重试 request rejected。

## Magic-API 影响

`model['knowledge-chat']` 保持原 key，并只返回中立模型 facade。Spring AI adapter 已通过 `AiModelRegistry` 到 `MagicDynamicModelDataSource` 的集成测试。

`vector[...]` 不属于本次 Spring AI adapter：标准 `VectorStore.similaritySearch(SearchRequest)` 接收文本并可能重新嵌入，而中立 `AiEmbeddingQuery` 接收预计算 `float[]`。首版不会丢弃、字符串化或重新计算调用方向量来伪装兼容。

## 验证

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

mvn -f fw-plus-core/pom.xml \
  -pl fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter \
  -am test

mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am test

mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter \
  -am validate
```

先执行 `docs/ai-framework-migration/probe-spring-ai-compatibility.sh`，复验 Boot 3.5.7/3.5.8 的关键 API 与依赖树。还应检查 adapter dependency tree 只包含 Spring AI 公共 API、不含 provider starter；中立 starter 的 Enforcer 必须禁止 Spring AI、Spring AI Alibaba 与新 adapter 的传递依赖。没有受控凭据时，离线验收不声称完成真实远端 smoke test。

## 回滚

回滚时同时恢复上一发布版本的 adapter、BOM 与应用配置。若改回 LangChain4j，将 `framework` 改为 `langchain4j` 并恢复对应 adapter/provider SDK；业务 key 与 `model[...]` 不变。不得在当前版本混入已删除的旧 model/store artifact，也不得引用不存在的 `fw-plus-ai-j17` 坐标。
