### Task 3: TinyEngine Provider SPI 与默认 LangChain4j Provider

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiProvider.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiCapability.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiProviderRegistry.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/LangChain4jTinyEngineAiProvider.java`
- Create: TinyEngine request/response DTOs under `.../tinyengine/chat` and `.../tinyengine/rag`.
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/provider/TinyEngineAiProviderRegistryTest.java`

**Interfaces:**
- Consumes: `AiModelRegistry`, `AiModel`, `AiModelType`.
- Produces: Provider registry, capability checks, default provider selection, unsupported capability errors.

- [ ] **Task 3 Step 1: Write provider registry tests**

Assert default provider key is `langchain4j`, custom provider can replace it, unsupported capability returns a deterministic exception containing provider and capability.

- [ ] **Task 3 Step 2: Define SPI and DTOs**

Add Java 8 interfaces/classes for chat request, chat response, streaming chunk, provider capability enum, provider registry. Keep DTOs free of `dev.langchain4j.*` imports.

- [ ] **Task 3 Step 3: Implement LangChain4j default provider through neutral registry**

Provider should resolve configured model key from `AiModelRegistry`, verify `AiModelType.CHAT` or `STREAMING_CHAT`, and only unwrap `LangChain4jAiModel` inside provider implementation. If unwrapping is impossible, return a clear unsupported framework/provider error.

- [ ] **Task 3 Step 4: Connect chat service to provider registry**

`TinyEngineAiChatService` delegates to current provider and maps stream/non-stream requests.

- [ ] **Task 3 Step 5: Run tests and commit**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiProviderRegistryTest,TinyEngineAiAutoConfigurationTest`

Commit: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && git commit -m "feat(ai): add tiny engine ai provider spi"`

