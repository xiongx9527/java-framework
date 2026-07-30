# add-tiny-engine-boot-starter Build Evidence

## Core Boundary

- Core artifact: `com.xiongx9527.fw.plus:fw-plus-tiny-engine-core:${revision}`
- Core package: `com.xiongx9527.fw.plus.tinyengine`
- Mapper package: `com.xiongx9527.fw.plus.tinyengine.mapper`
- Mapper XML: `classpath*:mapper/tinyengine/**/*.xml`
- SQL manifest: `META-INF/tiny-engine/sql/manifest.properties`
- SQL resources: `META-INF/tiny-engine/sql/h2/`, `META-INF/tiny-engine/sql/mysql/`, `META-INF/tiny-engine/sql/postgresql/`

## AI Boundary

TinyEngine Boot starter must not add legacy LangChain4j model/store artifacts, old starter coordinates, or direct AI provider SDKs. Future AI/RAG integration must use `fw-plus-ai-model` and `fw-plus-ai-store-embedding`.

## Commands

| Check | Result |
| --- | --- |
| `sed -n '1,180p' fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml` | Confirmed `fw-plus-tiny-engine-core` artifact and Java 8-compatible dependency line. |
| `find fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources -maxdepth 4 -type f \| sort` | Confirmed `mapper/tinyengine/*.xml` and SQL manifest/resources are present. |
| `rg -n "List\\.of\\(\|Map\\.of\\(\|Optional\\.orElseThrow\\(\|var \|record \|sealed " fw-plus-core/fw-plus-project/fw-plus-tiny-engine \|\| true` | Only SQL/mapper/comment text matched `record`; no Java 9+ API or Java 17 `record` syntax found. |
| `rg -n "fw-plus-ai-langchain4j-model\|fw-plus-ai-langchain4j-store-embeding\|langchain4j-.*starter\|openai\|dashscope\|qianfan\|milvus\|qdrant\|weaviate" fw-plus-boot fw-plus-sample \|\| true` | Matches are existing AI/MagicAPI sample resources, not TinyEngine Boot starter additions. |

## Final Build Checks

Pending.
