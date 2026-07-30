### Task 7: 文档、README 与样例配置

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/README.md`
- Modify/Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md`
- Optional Modify: `fw-plus-ai/README.zh-CN.md`

**Interfaces:**
- Consumes: Completed starter and AskData property names.
- Produces: Chinese configuration examples for neutral model/store and LangChain4j adapter.

- [ ] **Task 7 Step 1: Document TinyEngine AI starter**

Include Maven dependency, `fw.plus.tiny-engine.ai.enabled`, model keys, vector store key, missing dependency policy, disable behavior, and note that LangChain4j is adapter/provider only.

- [ ] **Task 7 Step 2: Document AskData neutral AI configuration**

Explain `fw.plus.ai.askdata` model/vector keys now resolve against `AiModelRegistry` and `AiEmbeddingStoreRegistry`. Show default LangChain4j adapter dependency and business Bean override options.

- [ ] **Task 7 Step 3: Add boundary warning**

State that old LangChain4j model/store starter coordinates are not used by the current starter line.

- [ ] **Task 7 Step 4: Run docs grep checks and commit**

Run: `rg -n "fw-plus-ai-langchain4j-model-spring-boot-starter|fw-plus-ai-langchain4j-store-embeding-spring-boot-starter|LangChain4jModelRegistry|EmbeddingStoreRegistry" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter`

Expected: no usage that presents old artifacts/registries as current main path.

Commit: `git add fw-plus-ai fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters && git commit -m "docs(ai): document tiny engine and askdata neutral ai setup"`

