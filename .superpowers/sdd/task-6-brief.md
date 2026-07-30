### Task 6: TinyEngine RAG 与知识库服务

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/DefaultTinyEngineRagService.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/DefaultTinyEngineKnowledgeBaseService.java`
- Create DTOs under `.../tinyengine/rag` for query, result, document, indexing result.
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/TinyEngineRagServiceTest.java`
- Test: `.../TinyEngineKnowledgeBaseServiceTest.java`

**Interfaces:**
- Consumes: `AiModelRegistry`, `AiEmbeddingStoreRegistry`, `AiEmbeddingDocument`, `AiEmbeddingQuery`.
- Produces: RAG search and knowledge indexing services using neutral store.

- [ ] **Task 6 Step 1: Write failing RAG tests**

Assert search maps score/content/metadata, collection filter is applied through metadata conventions, missing store key fails fast by default, and degrade returns unavailable reason.

- [ ] **Task 6 Step 2: Write failing knowledge indexing tests**

Assert unsupported format is counted as skipped, missing path is reported, supported text content is chunked with configured chunk size/overlap, and metadata includes `source`, `format`, `timestamp`, `collection`.

- [ ] **Task 6 Step 3: Implement RAG service**

Use neutral `AiEmbeddingStore.search()` and TinyEngine DTOs. Keep collection filtering in service layer if store does not support metadata filtering directly.

- [ ] **Task 6 Step 4: Implement knowledge service**

Start with plain text/resource content support that is Java 8 compatible. Return diagnostic counts for loaded/skipped/failed.

- [ ] **Task 6 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineRagServiceTest,TinyEngineKnowledgeBaseServiceTest,TinyEngineAiAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "feat(ai): add tiny engine rag services"`

