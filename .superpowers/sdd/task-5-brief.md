### Task 5: AskData 向量检索中立化

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetriever.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/vector/AiEmbeddingStoreAskDataVectorRetrieverTest.java`
- Modify Test: `AskDataVectorRetrieverTest.java`, `EmbeddingStoreAskDataVectorRetrieverTest.java`.

**Interfaces:**
- Consumes: `AiEmbeddingStoreRegistry`, `AiEmbeddingStore`, `AiEmbeddingQuery`, `AiEmbeddingMatch`.
- Produces: Default AskData vector retriever through neutral store registry.

- [ ] **Task 5 Step 1: Write failing vector tests**

Assert neutral retriever returns documents from `AiEmbeddingSearchResult`, handles blank query with empty list, and missing default store is diagnosed. Assert custom `AskDataVectorRetriever` bean overrides default.

- [ ] **Task 5 Step 2: Implement neutral vector retriever**

Implement `AiEmbeddingStoreAskDataVectorRetriever` that resolves store key from AskData properties, builds `AiEmbeddingQuery`, maps `AiEmbeddingMatch.document().getText()` or equivalent content to `AskDataRetrievedDocument`, and never imports `dev.langchain4j.*`.

- [ ] **Task 5 Step 3: Adjust default vector auto configuration**

Make the default retriever consume `AiEmbeddingStoreRegistry`. Keep `ContentRetrieverAskDataVectorRetriever` and `EmbeddingStoreAskDataVectorRetriever` only as optional compatibility when their classes/beans are present.

- [ ] **Task 5 Step 4: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataVectorRetrieverTest,EmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "refactor(ai): route askdata vectors through neutral store"`

