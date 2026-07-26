### Task 3: Spring Boot Model Properties 与 Registry AutoConfiguration

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelProperties.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelPropertiesTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfigurationTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`

**Interfaces:**
- Consumes:
  - `LangChain4jModelDefinition`
  - `LangChain4jModelFactory`
  - `LangChain4jModelProviderAdapter`
  - `LangChain4jModelRegistry`
- Produces:
  - `LangChain4jModelProperties` bound from prefix `fw-plus.ai.langchain4j.model`.
  - `LangChain4jModelProperties.Model` nested JavaBean fields: `String provider`, `LangChain4jModelType modelType`, `boolean enabled`, `String baseUrl`, `String apiKey`, `String modelName`, `Map<String, Object> properties`.
  - `LangChain4jModelAutoConfiguration.langChain4jModelFactory(): LangChain4jModelFactory` with `@ConditionalOnMissingBean`.
  - `LangChain4jModelAutoConfiguration.langChain4jModelRegistry(properties, factory, adapters): LangChain4jModelRegistry` with enabled model filtering and default key validation.

- [ ] **Task 3 Step 1: Write failing properties binding test**

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class LangChain4jModelPropertiesTest {

    @Test
    public void shouldBindModelProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("fw-plus.ai.langchain4j.model.enabled", "true")
                .withProperty("fw-plus.ai.langchain4j.model.default-model", "chat-default")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.provider", "openai")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-type", "chat")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.base-url", "http://localhost:11434/v1")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.api-key", "demo")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.model-name", "gpt-4o-mini")
                .withProperty("fw-plus.ai.langchain4j.model.models.chat-default.properties.temperature", "0.2");

        LangChain4jModelProperties properties = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()))
                .bind("fw-plus.ai.langchain4j.model", LangChain4jModelProperties.class)
                .get();

        assertTrue(properties.isEnabled());
        assertEquals("chat-default", properties.getDefaultModel());
        assertEquals("openai", properties.getModels().get("chat-default").getProvider());
        assertEquals(LangChain4jModelType.CHAT, properties.getModels().get("chat-default").getModelType());
        assertEquals("gpt-4o-mini", properties.getModels().get("chat-default").getModelName());
        assertEquals("0.2", String.valueOf(properties.getModels().get("chat-default").getProperties().get("temperature")));
    }
}
```

- [ ] **Task 3 Step 2: Write failing auto-configuration tests**

Create tests with `ApplicationContextRunner`:

```java
package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

public class LangChain4jModelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
            .withUserConfiguration(FakeAdapterConfiguration.class);

    @Test
    public void shouldCreateRegistryFromEnabledDefinitions() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.model.enabled=true",
                        "fw-plus.ai.langchain4j.model.default-model=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.chat.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.chat.model-name=gpt-4o-mini",
                        "fw-plus.ai.langchain4j.model.models.disabled.provider=openai",
                        "fw-plus.ai.langchain4j.model.models.disabled.model-type=chat",
                        "fw-plus.ai.langchain4j.model.models.disabled.enabled=false"
                )
                .run(context -> {
                    LangChain4jModelRegistry registry = context.getBean(LangChain4jModelRegistry.class);
                    assertTrue(registry.contains("chat"));
                    assertFalse(registry.contains("disabled"));
                    assertEquals("fake-model", registry.getDefault().getModel());
                });
    }

    @Test
    public void shouldBackOffWhenUserProvidesRegistry() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LangChain4jModelAutoConfiguration.class))
                .withUserConfiguration(CustomRegistryConfiguration.class)
                .run(context -> assertSame(context.getBean("customRegistry"), context.getBean(LangChain4jModelRegistry.class)));
    }

    @Configuration
    static class FakeAdapterConfiguration {
        @Bean
        LangChain4jModelProviderAdapter fakeAdapter() {
            return new LangChain4jModelProviderAdapter() {
                public boolean supports(LangChain4jModelDefinition definition) {
                    return "openai".equals(definition.getProvider());
                }

                public LangChain4jModel create(LangChain4jModelDefinition definition) {
                    return new LangChain4jModel(definition.getProvider(), definition.getModelType(), "fake-model");
                }
            };
        }
    }

    @Configuration
    static class CustomRegistryConfiguration {
        @Bean("customRegistry")
        LangChain4jModelRegistry customRegistry() {
            return new DefaultLangChain4jModelRegistry(new java.util.LinkedHashMap<String, LangChain4jModel>(), null);
        }
    }
}
```

- [ ] **Task 3 Step 3: Run tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest test
```

Expected: FAIL because properties and auto-configuration do not exist.

- [ ] **Task 3 Step 4: Add dependencies and auto-configuration**

Modify `fw-plus-ai-boot-autoconfigure/pom.xml` to depend on `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model`. Implement properties and auto-configuration. Register `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.LangChain4jModelAutoConfiguration` in `META-INF/spring.factories` after `LangChain4jAutoConfiguration` and before Magic-API base auto-configuration.

- [ ] **Task 3 Step 5: Run tests to verify they pass**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest test
```

Expected: PASS.

- [ ] **Task 3 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model
git commit -m "feat: auto configure langchain4j model registry"
```

