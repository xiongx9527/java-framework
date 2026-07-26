### Task 4: Provider Adapter 迁移与 SDK 隔离

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/ProviderAdapterSupport.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/ReflectiveLangChain4jModelProviderAdapter.java`
- Create provider adapter files listed in **File Structure / AI Boot Autoconfigure 与 Starter**.
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/LangChain4jModelProviderAdapterTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`

**Interfaces:**
- Consumes: `LangChain4jModelDefinition`, `LangChain4jModelType`, `LangChain4jModelProviderAdapter`.
- Produces:
  - `ProviderAdapterSupport.classPresent(String className): boolean`
  - `ProviderAdapterSupport.property(definition, name): Object` reads first from direct fields then from `properties`.
  - `ReflectiveLangChain4jModelProviderAdapter` supports provider/modelType metadata and instantiates LangChain4j builder classes only when the provider SDK class is present.
  - Bean methods in `LangChain4jModelAutoConfiguration` for each concrete adapter with `@ConditionalOnMissingBean`.

- [ ] **Task 4 Step 1: Write failing adapter tests**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LangChain4jModelProviderAdapterTest {

    @Test
    public void shouldNotSupportProviderWhenRequiredClassIsMissing() {
        ReflectiveLangChain4jModelProviderAdapter adapter =
                new ReflectiveLangChain4jModelProviderAdapter(
                        "missing",
                        LangChain4jModelType.CHAT,
                        "com.example.DoesNotExist",
                        "builder"
                );

        assertFalse(adapter.supports(definition("missing", LangChain4jModelType.CHAT)));
    }

    @Test
    public void shouldIncludeProviderAndTypeWhenCreateFails() {
        ReflectiveLangChain4jModelProviderAdapter adapter =
                new ReflectiveLangChain4jModelProviderAdapter(
                        "openai",
                        LangChain4jModelType.CHAT,
                        "java.lang.String",
                        "builder"
                );

        try {
            adapter.create(definition("openai", LangChain4jModelType.CHAT));
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("openai"));
            assertTrue(e.getMessage().contains("CHAT"));
        }
    }

    private LangChain4jModelDefinition definition(String provider, LangChain4jModelType type) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey("chat");
        definition.setProvider(provider);
        definition.setModelType(type);
        definition.setEnabled(true);
        definition.setBaseUrl("http://localhost:11434/v1");
        definition.setApiKey("demo");
        definition.setModelName("gpt-4o-mini");
        return definition;
    }
}
```

- [ ] **Task 4 Step 2: Run adapter tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelProviderAdapterTest test
```

Expected: FAIL because provider adapter support classes do not exist.

- [ ] **Task 4 Step 3: Implement reflective provider adapter support**

Implement provider adapter classes so each adapter declares exact provider names:
`openai`, `azure-openai`, `qwen`, `ollama`, `anthropic`, `qianfan`, `zhipu`, `hugging-face`, `local-ai`, `gemini`, `voyage-ai`, `all-mini-lm-l6-v2-quantized`.

For each provider, register only the model types backed by LangChain4j 0.35.0 classes present in this repository or in the existing 0.35.0 provider integrations. Keep the core `LangChain4jModelType` enum complete (`MODERATION` and `SCORING` remain valid core types), but do not register provider adapters for Moderation or Scoring unless the exact 0.35.0 provider builder class is locally confirmed. Audio, Video and PDF are Chat multimodal content capabilities, not independent provider adapter model types.

```text
openai: CHAT, STREAMING_CHAT, EMBEDDING, IMAGE, LANGUAGE, STREAMING_LANGUAGE
azure-openai: CHAT, STREAMING_CHAT, EMBEDDING, IMAGE, LANGUAGE, STREAMING_LANGUAGE
qwen: CHAT, STREAMING_CHAT, EMBEDDING, IMAGE, LANGUAGE, STREAMING_LANGUAGE
ollama: CHAT, EMBEDDING, LANGUAGE
anthropic: CHAT, STREAMING_CHAT
qianfan: CHAT, STREAMING_CHAT, EMBEDDING, LANGUAGE
zhipu: CHAT, STREAMING_CHAT, EMBEDDING, IMAGE
hugging-face: CHAT, EMBEDDING, LANGUAGE
local-ai: CHAT, EMBEDDING, LANGUAGE
gemini: CHAT, EMBEDDING
all-mini-lm-l6-v2-quantized: EMBEDDING
```

Document that LangChain4j 0.35.0 core API contains `ModerationModel` and `ScoringModel`, but this migration does not expose them through provider adapters until a concrete 0.35.0 provider builder is verified in the current dependency set.

Use class-name checks before reflective creation so an application without a provider SDK can still start.

- [ ] **Task 4 Step 4: Wire adapters as beans**

Add adapter bean methods to `LangChain4jModelAutoConfiguration`. The registry creation path must accept `List<LangChain4jModelProviderAdapter>` and pass it to `LangChain4jModelFactory.create`.

- [ ] **Task 4 Step 5: Run provider and auto-configuration tests**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jModelProviderAdapterTest,LangChain4jModelAutoConfigurationTest test
```

Expected: PASS.

- [ ] **Task 4 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider
git commit -m "feat: add langchain4j model provider adapters"
```

