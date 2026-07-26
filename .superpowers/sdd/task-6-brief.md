### Task 6: 端到端验证、兼容扫描与 OpenSpec 校验

**Files:**
- Modify: none required unless verification exposes defects in files changed by Tasks 1-5.
- Test evidence: Maven output and OpenSpec validation output.

**Interfaces:**
- Consumes: all tasks above.
- Produces: verified Core build, AI build, Java 8 compatibility scan, and strict OpenSpec validation.

- [ ] **Step 1: Run Core focused reactor tests**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run Core LangChain4j package build**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j -am -DskipTests package
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run AI Magic-API focused tests**

Run:

```bash
cd fw-plus-ai
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure,fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-vector -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Run AI plugin package build**

Run:

```bash
cd fw-plus-ai
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin -am -DskipTests package
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Scan for Java 9+ APIs in touched Java packages**

Run:

```bash
rg -n "List\\.of|Map\\.of|Set\\.of|copyOf|orElseThrow\\(\\)" \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/vector
```

Expected: no output.

- [ ] **Step 6: Scan Magic-API plugin code for duplicated provider builders**

Run:

```bash
rg -n "RedisEmbeddingStore\\.builder|MilvusEmbeddingStore\\.builder|QdrantEmbeddingStore\\.builder|WeaviateEmbeddingStore\\.builder" \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin
```

Expected: no output outside compatibility comments or Core provider adapter references.

- [ ] **Step 7: Validate OpenSpec**

Run:

```bash
openspec validate unify-vector-magicapi-plugin --strict
```

Expected: passes with no validation errors.

- [ ] **Step 8: Review OpenSpec task coverage without modifying tasks.md**

Open `openspec/changes/unify-vector-magicapi-plugin/tasks.md` and verify every item maps to `OpenSpec Task Mapping` in this plan. Do not change checkbox state in `tasks.md`.

- [ ] **Step 9: Commit**

Commit message: `test(vector): verify unified vector plugin change`.

## Self-Review

- Spec coverage: Design Doc goals are covered by Task 1-5; OpenSpec tasks `1.1` through `4.5` are mapped explicitly.
- Placeholder scan: No task uses deferred-work marker language; unresolved artifact names such as `langchain4j-weaviate` are handled by an implementation-time dependency discovery step with a concrete fallback rule.
- Type consistency: `EmbeddingStoreDefinition`、`EmbeddingStoreRegistry`、`EmbeddingStoreFactory`、`EmbeddingStoreProviderAdapter`、`VectorModule`、`VectorStoreOperations` names are introduced before downstream tasks consume them.
- Compatibility strategy: Plan chooses the lower-risk Design Doc path, keeping old Magic-API provider entries as deprecated thin wrappers unless implementation proves a wrapper causes Maven cycles.
