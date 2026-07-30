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
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAutoConfigurationTest test -DtrimStackTrace=false` | Passed: 17 tests, 0 failures, 0 errors, 0 skipped. Verifies host Controller override by type, `mapper.enabled=false` service/controller backoff, and no TinyEngine type handler package pollution. |
| `rg -n "fw-plus-ai-langchain4j-model\|fw-plus-ai-langchain4j-store-embeding\|langchain4j-.*starter\|openai\|dashscope\|qianfan\|milvus\|qdrant\|weaviate" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \|\| true` | No matches. TinyEngine Boot autoconfigure additions do not use legacy LangChain4j model/store artifacts or direct AI provider SDKs. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter validate` | Passed. TinyEngine starter module is present in the Boot reactor and validates. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter dependency:tree '-Dincludes=...'` | Passed. Starter dependency tree includes `fw-plus-boot-starter`, `fw-plus-tiny-engine-core`, `mybatis-plus-boot-starter`, and `spring-boot-starter-web`; no LangChain4j/model/store/provider SDK matches. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineModelContractTest test -DtrimStackTrace=false` | Passed: 9 tests, 0 failures, 0 errors, 0 skipped. Verified TinyEngine mapper XML parsing, explicit JSON TypeHandler usage without broad `@MappedTypes`, and `Page#occupier` non-table mapping. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/pom.xml test -DtrimStackTrace=false` | Passed: 2 tests, 0 failures, 0 errors, 0 skipped. Enabled sample starts with ResourceService, ResourceMapper proxy, TinyEngineResourceController, registered `/tiny-engine/resources` MVC route, and SQL resources; disabled sample does not register TinyEngine beans and does not auto-initialize SQL. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/pom.xml -pl fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample validate` | Passed. The sample aggregator can resolve the TinyEngine sample module path. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter validate` | Passed after sample/core fixes. TinyEngine starter POM remains valid. |
| `rg -n "fw-plus-ai-langchain4j-model\|fw-plus-ai-langchain4j-store-embeding\|langchain4j-.*starter\|openai\|dashscope\|qianfan\|milvus\|qdrant\|weaviate" <TinyEngine Boot starter/autoconfigure/sample/report/spec paths> \|\| true` | Only documentation lines explaining prohibited AI dependencies matched; no TinyEngine code or POM introduced legacy LangChain4j model/store artifacts or direct provider SDKs. |
| `git diff --check` | Passed. |

## Latest Verification

2026-07-30 16:10-16:11 CST reran the closing checks after reviewer follow-up fixes:

| Check | Result |
| --- | --- |
| `git diff --check` | Passed. |
| `rg -n "fw-plus-ai-langchain4j-model\|fw-plus-ai-langchain4j-store-embeding\|fw-plus-ai-langchain4j-store-embedding\|langchain4j-.*starter\|openai\|dashscope\|qianfan\|milvus\|qdrant\|weaviate" <TinyEngine Boot starter/autoconfigure/sample/report/spec paths> \|\| true` | Only documentation/report/spec lines describing prohibited AI dependencies matched; TinyEngine code and POM additions remain free of legacy LangChain4j model/store artifacts and direct provider SDKs. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter validate` | Passed. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/pom.xml -pl fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample validate` | Passed. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineModelContractTest test -DtrimStackTrace=false` | Passed: 9 tests, 0 failures, 0 errors, 0 skipped. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAutoConfigurationTest test -DtrimStackTrace=false` | Passed: 17 tests, 0 failures, 0 errors, 0 skipped. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/pom.xml test -DtrimStackTrace=false` | Passed: 2 tests, 0 failures, 0 errors, 0 skipped. |

## Review Follow-up

- `ComponentLibrary.name` is a plain `String`, so it no longer declares the JSON `TypeHandler`; nested component `name` JSON fields keep explicit handlers.
- The sample verifies the exact `GET /tiny-engine/resources` MVC mapping and the handler type is `TinyEngineResourceController`.
- Sample-generated `.flattened-pom.xml` and `target/` were removed after verification.

## Known Environment Notes

- Boot autoconfigure verification initially failed because local `0.1.1-SNAPSHOT` core artifacts were stale: `fw-plus-magicapi` lacked `RequestHandler.ApiInfoRequestLookup`, and `fw-plus-template` lacked `TemplateParser.parseWordByUrl`. Installing the current repo modules resolved those environment mismatches without changing Magic-API 2.2.2 versions.
- Maven still reports duplicate dependency-management warnings for `fw-plus-ai-model` and `fw-plus-ai-store-embedding` in the installed Core BOM. This predates the TinyEngine Boot starter work and was not changed here.
