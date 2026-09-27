# fw-plus-ai

## Neutral adapter breaking migration

This Java 17 / Spring Boot 3.5.8 release line removes the former LangChain4j model/store artifacts and their old configuration prefixes. `fw-plus-ai-model` and `fw-plus-ai-store-embedding` remain framework-neutral. LangChain4j is installed explicitly through its core adapter:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

Replace both former core artifacts, `fw-plus-ai-langchain4j-model` and `fw-plus-ai-langchain4j-store-embeding`, with `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter`. There is no separate LangChain4j Boot starter; Boot auto-configuration is provided by `fw-plus-ai-boot-autoconfigure` when the core adapter is present. Do not mix the new adapter artifact with either removed artifact.

Use `fw-plus.ai.model` and `fw-plus.ai.embedding-store`; the removed `fw-plus.ai.langchain4j.*` bindings are not supported. An omitted `framework` is valid only when exactly one installed adapter supports the provider/type. With more than one candidate, set `framework: langchain4j` explicitly.

```yaml
fw-plus:
  ai:
    model:
      models:
        knowledge-chat:
          framework: langchain4j
          provider: openai
          type: CHAT
          properties:
            model-name: gpt-4o-mini
    embedding-store:
      stores:
        knowledge:
          framework: langchain4j
          provider: qdrant
          type: VECTOR
          properties:
            host: localhost
            port: 6334
```

Magic-API keys such as `model['knowledge-chat']` and `vector['knowledge']` stay stable, but scripts expose only neutral facades and never native LangChain4j objects.

## Spring AI and Spring AI Alibaba

The same core artifact supports both Spring frameworks for synchronous `CHAT` and `EMBEDDING`:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-spring-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

Applications also add an official provider starter. Use `framework: spring-ai` for generic Spring AI providers or `framework: spring-ai-alibaba` with `provider: dashscope`; set `properties.beanName` to a Spring-managed `ChatModel` or `EmbeddingModel`, or register a unique `SpringAiProviderBinding`.

This first release does **not** provide a Spring AI/Alibaba `AiEmbeddingStoreAdapter`: the standard text-query `VectorStore` cannot safely replace the neutral precomputed-vector contract. `vector[...]` therefore remains backed only by an explicitly installed compatible store adapter.

AskData remains neutral and does not pull in any concrete adapter/provider by default. AgentScope 2 is still future work. See the Chinese [migration guide](../docs/ai-framework-migration/overview.md) for complete mappings, diagnostics, capability limits, and rollback guidance.
