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

| Check | Result |
| --- | --- |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-magicapi,fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -am install -DskipTests` | Passed. Refreshed local `fw-plus-magicapi` and TinyEngine core snapshots used by Boot verification. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-template -am install -DskipTests` | Passed. Refreshed local template snapshot needed by existing Boot autoconfigure sources. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAutoConfigurationTest test` | Passed: 15 tests, 0 failures, 0 errors, 0 skipped. |
| `rg -n "fw-plus-ai-langchain4j-model\|fw-plus-ai-langchain4j-store-embeding\|langchain4j-.*starter\|openai\|dashscope\|qianfan\|milvus\|qdrant\|weaviate" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \|\| true` | No matches. TinyEngine Boot autoconfigure additions do not use legacy LangChain4j model/store artifacts or direct AI provider SDKs. |

## Known Environment Notes

- Boot autoconfigure verification initially failed because local `0.1.1-SNAPSHOT` core artifacts were stale: `fw-plus-magicapi` lacked `RequestHandler.ApiInfoRequestLookup`, and `fw-plus-template` lacked `TemplateParser.parseWordByUrl`. Installing the current repo modules resolved those environment mismatches without changing Magic-API 2.2.2 versions.
- Maven still reports duplicate dependency-management warnings for `fw-plus-ai-model` and `fw-plus-ai-store-embedding` in the installed Core BOM. This predates the TinyEngine Boot starter work and was not changed here.
