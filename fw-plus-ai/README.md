# fw-plus-ai

## Neutral adapter breaking migration

This release line removes the former LangChain4j model/store artifacts and their old configuration prefixes. The Java 8 `fw-plus-ai-model` and `fw-plus-ai-store-embedding` APIs remain framework-neutral. LangChain4j is still the complete default adapter, installed explicitly through:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

Replace both former core artifacts, `fw-plus-ai-langchain4j-model` and `fw-plus-ai-langchain4j-store-embeding`, with `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter`. Replace both former Boot starters with the adapter starter above. Do not mix a new adapter artifact with any of the four removed artifacts.

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

AskData is retained and is not migrated by this documentation task. Spring AI, Spring AI Alibaba, and AgentScope 2 adapters are future Java 17 / Spring Boot 3 work, not artifacts implemented by this release.

See the Chinese [migration guide](../docs/ai-framework-migration/overview.md) for complete mappings, diagnostics, rollback limits, and the Java 17 roadmap.
