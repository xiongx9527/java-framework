### Task 4: AskData 模型注册中立化

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistry.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Keep or adapt: `LangChain4jModelAskDataAutoConfiguration.java`, `LangChain4jModelRegistryAskDataModelSource.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistryTest.java`
- Modify Test: `AskDataAutoConfigurationTest.java`, `LangChain4jAskDataModelRegistryTest.java` as needed.

**Interfaces:**
- Consumes: `AiModelRegistry`, `AiModelType.CHAT`, `AiModelType.EMBEDDING`.
- Produces: Default `AskDataModelRegistry` without direct `Map<String, ChatLanguageModel>` / `Map<String, EmbeddingModel>` input.

- [ ] **Task 4 Step 1: Write failing AskData model tests**

Create test registry with neutral `AiModel` stubs for chat and embedding. Assert `getChatModel` and `getEmbeddingModel` resolve configured keys, missing key throws `AskDataModelException`, and custom `AskDataModelRegistry` bean prevents default registration.

- [ ] **Task 4 Step 2: Implement `AiModelAskDataModelRegistry`**

Resolve candidate/default keys against `AiModelRegistry.keys()`. For LangChain4j default adapter, unwrap only inside this implementation when the model is `LangChain4jAiModel`; keep `AskDataModelRegistry` interface unchanged.

- [ ] **Task 4 Step 3: Adjust `AskDataAutoConfiguration`**

Change the default `askDataModelRegistry` bean signature to consume `AiModelRegistry` and optional model source only if still needed. Remove main-path direct collection of `ChatLanguageModel` / `EmbeddingModel` maps.

- [ ] **Task 4 Step 4: Preserve optional compatibility**

If keeping `LangChain4jAskDataModelRegistry`, isolate it behind `@ConditionalOnClass` and never make it the default when `AiModelRegistry` is present.

- [ ] **Task 4 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest,AskDataAutoConfigurationTest,LangChain4jAskDataModelRegistryTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "refactor(ai): route askdata models through neutral registry"`

