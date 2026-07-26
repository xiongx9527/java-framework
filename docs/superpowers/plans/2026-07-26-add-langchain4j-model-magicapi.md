---
change: add-langchain4j-model-magicapi
design-doc: docs/superpowers/specs/2026-07-26-add-langchain4j-model-magicapi-design.md
base-ref: 7b1c788fd198b832c41ad83baf58e40bb5e5f8bb
---

# LangChain4j Model Module 与 Magic Model 插件 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增可复用的 LangChain4j 模型 registry、Spring Boot starter 和 Magic-API `model` 插件，并移除旧 Magic `langchain4j` 双入口。

**Architecture:** 模型能力拆成三层：`fw-plus-core` 中的 `fw-plus-ai-langchain4j-model` 只定义轻量契约和默认 registry/factory；`fw-plus-ai-boot-autoconfigure` 负责 Spring Boot 配置绑定、条件装配和 provider adapter；Magic model 插件只处理 `model-datasource` 动态资源、脚本模块和控制台接口。Magic runtime 模型优先于 Spring Boot registry，同 key 更新时先移除旧 runtime 模型再创建新模型。

**Tech Stack:** JDK 1.8、Spring Boot 2.7.18、LangChain4j 0.35.0、Magic-API 2.2.2、Maven Wrapper、JUnit、Spring Boot `ApplicationContextRunner`、Vue3/Vite 控制台插件。

## Global Constraints

- 只在执行本计划时改实现文件；本次计划创建阶段不改源码、不改 OpenSpec 产物、不提交。
- 所有 Java 新代码必须兼容 JDK 1.8，禁止使用 `List.of()`、`Map.copyOf()`、`Optional.orElseThrow()`、`Stream.toList()` 和 Java 9+ API。
- Spring Boot 版本保持 2.7.18，LangChain4j 版本保持 0.35.0，Magic-API 版本保持 2.2.2。
- 使用各顶层目录内 Maven Wrapper：core 相关命令在 `fw-plus-core` 下执行 `./mvnw`，AI Boot 相关命令在 `fw-plus-ai` 下执行 `./mvnw`。
- `org.ssssssss` Magic-API 依赖版本和内部 Nexus 仓库配置不变。
- core 模块不得依赖 OpenAI、Qwen、Ollama、Azure、Anthropic、HuggingFace、Zhipu、Qianfan、LocalAI、Gemini 等具体 provider SDK。
- 新 starter 不默认传递全部 provider SDK；业务应用显式引入所需 provider SDK 或已有 provider 模块。
- Magic 资源类型统一为 `model-datasource`，脚本入口统一为 `model`，不保留运行态 `langchain4j-datasource` 与 `@MagicModule("langchain4j")` 入口。
- controller 响应、日志、`simple()` 输出不得明文暴露 `apiKey`、`secretKey`、`accessToken`、`token`、`password`。
- 保持已有 `fw-plus-ai-langchain4j-store-embeding`、Magic vector 插件和样例中非模型功能行为不变。
- 执行阶段遇到用户未提交改动时保留并绕开；如同文件必须修改，先读取并合并，不回滚用户改动。

---

## File Structure

### Core Model Module

- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/pom.xml`  
  新 core 模型模块 POM，继承 `fw-plus-ai`，设置 `main.basedir`，只依赖 LangChain4j 基础模型接口和测试依赖。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelDefinition.java`  
  通用模型配置 JavaBean。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelType.java`  
  模型类型枚举。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModel.java`  
  provider、modelType 和原生模型对象包装。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelProviderAdapter.java`  
  provider adapter SPI。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelFactory.java`  
  adapter 选择入口。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelFactory.java`  
  默认 factory。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelRegistry.java`  
  registry 查询契约。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelRegistry.java`  
  默认 registry。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelException.java`  
  统一异常类型。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelFactoryTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelRegistryTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

### AI Boot Autoconfigure 与 Starter

- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelProperties.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/ProviderAdapterSupport.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/ReflectiveLangChain4jModelProviderAdapter.java`
- Create provider adapters under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/`:
  `OpenAiModelProviderAdapter.java`, `AzureOpenAiModelProviderAdapter.java`, `DashScopeModelProviderAdapter.java`, `OllamaModelProviderAdapter.java`, `AnthropicModelProviderAdapter.java`, `QianfanModelProviderAdapter.java`, `ZhipuAiModelProviderAdapter.java`, `HuggingFaceModelProviderAdapter.java`, `LocalAiModelProviderAdapter.java`, `GeminiModelProviderAdapter.java`, `VoyageAiModelProviderAdapter.java`, `AllMiniLmModelProviderAdapter.java`.
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelPropertiesTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/LangChain4jModelAutoConfigurationTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider/LangChain4jModelProviderAdapterTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/README.md`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`

### Magic Model Plugin

- Create backend package root: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/`
- Create: `model/MagicDynamicModelDataSource.java`
- Create: `model/ModelDataSourceInfo.java`
- Create: `service/ModelDataSourceInfoMagicResourceStorage.java`
- Create: `service/ModelDataSourceMagicDynamicRegistry.java`
- Create: `module/ModelModule.java`
- Create: `web/MagicModelController.java`
- Create: `starter/MagicAPIModelConfig.java`
- Create: `starter/MagicAPIModelConfiguration.java`
- Create tests under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/`:
  `MagicDynamicModelDataSourceTest.java`, `ModelDataSourceMagicDynamicRegistryTest.java`, `ModelModuleTest.java`, `MagicModelControllerTest.java`, `MagicAPIModelConfigurationTest.java`, `MagicAPIModelAutoConfigurationIntegrationTest.java`.
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Delete after replacement: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j/`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/pom.xml`
- Create console package under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/`
- Create built resource: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/main/resources/magic-editor/plugins/magic-model.1.0.0.iife.js`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
- Delete after replacement: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/`

### Samples 与 Docs

- Modify: `fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-ai/README.zh-CN.md`
- Modify: `fw-plus-ai/README.md`

### Task 1: Core Model 模块契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelDefinition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelType.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelProviderAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/model/LangChain4jModelDefinitionTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

**Interfaces:**
- Consumes: LangChain4j 0.35.0 基础接口包；不消费具体 provider SDK。
- Produces:
  - `LangChain4jModelDefinition` fields: `String key`, `String provider`, `LangChain4jModelType modelType`, `boolean enabled`, `String baseUrl`, `String apiKey`, `String modelName`, `Map<String, Object> properties`
  - `LangChain4jModelType` values: `CHAT`, `STREAMING_CHAT`, `EMBEDDING`, `IMAGE`, `LANGUAGE`, `STREAMING_LANGUAGE`, `MODERATION`, `SCORING`
  - `LangChain4jModel` constructor: `LangChain4jModel(String provider, LangChain4jModelType modelType, Object model)`
  - `LangChain4jModelProviderAdapter.supports(LangChain4jModelDefinition definition): boolean`
  - `LangChain4jModelProviderAdapter.create(LangChain4jModelDefinition definition): LangChain4jModel`
  - `LangChain4jModelFactory.create(LangChain4jModelDefinition definition, List<LangChain4jModelProviderAdapter> adapters): LangChain4jModel`
  - `LangChain4jModelRegistry.get(String key): LangChain4jModel`
  - `LangChain4jModelRegistry.getDefault(): LangChain4jModel`
  - `LangChain4jModelRegistry.contains(String key): boolean`
  - `LangChain4jModelRegistry.keys(): Set<String>`

- [x] **Task 1 Step 1: Write the failing contract test**

```java
package com.xiongx9527.fw.plus.ai.langchain4j.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LangChain4jModelDefinitionTest {

    @Test
    public void shouldExposeJavaBeanModelDefinitionFields() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("temperature", 0.3);

        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey("chat-default");
        definition.setProvider("openai");
        definition.setModelType(LangChain4jModelType.CHAT);
        definition.setEnabled(true);
        definition.setBaseUrl("http://localhost:11434/v1");
        definition.setApiKey("demo");
        definition.setModelName("gpt-4o-mini");
        definition.setProperties(properties);

        assertEquals("chat-default", definition.getKey());
        assertEquals("openai", definition.getProvider());
        assertEquals(LangChain4jModelType.CHAT, definition.getModelType());
        assertTrue(definition.isEnabled());
        assertEquals("http://localhost:11434/v1", definition.getBaseUrl());
        assertEquals("demo", definition.getApiKey());
        assertEquals("gpt-4o-mini", definition.getModelName());
        assertEquals(0.3, definition.getProperties().get("temperature"));
    }

    @Test
    public void shouldExposeAllSupportedModelTypes() {
        assertNotNull(LangChain4jModelType.valueOf("CHAT"));
        assertNotNull(LangChain4jModelType.valueOf("STREAMING_CHAT"));
        assertNotNull(LangChain4jModelType.valueOf("EMBEDDING"));
        assertNotNull(LangChain4jModelType.valueOf("IMAGE"));
        assertNotNull(LangChain4jModelType.valueOf("LANGUAGE"));
        assertNotNull(LangChain4jModelType.valueOf("STREAMING_LANGUAGE"));
        assertNotNull(LangChain4jModelType.valueOf("MODERATION"));
        assertNotNull(LangChain4jModelType.valueOf("SCORING"));
    }
}
```

- [x] **Task 1 Step 2: Run test to verify it fails**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am -Dtest=LangChain4jModelDefinitionTest test
```

Expected: FAIL because the new module and classes do not exist.

- [x] **Task 1 Step 3: Create the module and public contracts**

Create the module POM, add `<module>fw-plus-ai-langchain4j-model</module>` to `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`, and add dependency management for `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model:${project.version}` in `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`.

Create the public classes and interfaces with the signatures listed in **Interfaces**. Use JavaBean getters/setters for `LangChain4jModelDefinition`; initialize `properties` as an empty `LinkedHashMap<String, Object>` when not provided.

- [x] **Task 1 Step 4: Run test to verify it passes**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am -Dtest=LangChain4jModelDefinitionTest test
```

Expected: PASS, and no Java 9+ API compilation errors.

- [x] **Task 1 Step 5: Commit this task during execution**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model
git commit -m "feat: add langchain4j model core contracts"
```

### Task 2: Core Factory 与 Registry

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelFactory.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelFactoryTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/model/DefaultLangChain4jModelRegistryTest.java`

**Interfaces:**
- Consumes: Task 1 contracts.
- Produces:
  - `DefaultLangChain4jModelFactory.create(definition, adapters)` selects the first adapter whose `supports(definition)` returns true.
  - Factory throws `LangChain4jModelException` when definition is null, key/provider/modelType is blank or null, no adapter supports the definition, adapter throws, or adapter returns null.
  - Factory exception messages include `key`, `provider`, and `modelType` when those values are available.
  - `DefaultLangChain4jModelRegistry(Map<String, LangChain4jModel> models, String defaultModelKey)` stores an unmodifiable copy.
  - Registry throws `LangChain4jModelException` when `defaultModelKey` is nonblank and missing from `models`.

- [x] **Task 2 Step 1: Write failing factory tests**

```java
package com.xiongx9527.fw.plus.ai.langchain4j.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultLangChain4jModelFactoryTest {

    private final DefaultLangChain4jModelFactory factory = new DefaultLangChain4jModelFactory();

    @Test
    public void shouldCreateModelWithFirstSupportedAdapter() {
        LangChain4jModelDefinition definition = definition("chat");
        LangChain4jModel expected = new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object());

        LangChain4jModel model = factory.create(definition, Arrays.<LangChain4jModelProviderAdapter>asList(
                new FakeAdapter(false, null),
                new FakeAdapter(true, expected)
        ));

        assertSame(expected, model);
    }

    @Test
    public void shouldFailWhenProviderIsUnsupported() {
        try {
            factory.create(definition("chat"), Collections.<LangChain4jModelProviderAdapter>emptyList());
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("chat"));
            assertTrue(e.getMessage().contains("openai"));
            assertTrue(e.getMessage().contains("CHAT"));
        }
    }

    @Test
    public void shouldWrapAdapterExceptionWithContext() {
        try {
            factory.create(definition("chat"), Collections.<LangChain4jModelProviderAdapter>singletonList(new ThrowingAdapter()));
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("chat"));
            assertTrue(e.getMessage().contains("openai"));
            assertTrue(e.getMessage().contains("CHAT"));
            assertTrue(e.getCause().getMessage().contains("boom"));
        }
    }

    @Test
    public void shouldFailWhenAdapterReturnsNull() {
        try {
            factory.create(definition("chat"), Collections.<LangChain4jModelProviderAdapter>singletonList(new FakeAdapter(true, null)));
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("returned null"));
        }
    }

    private LangChain4jModelDefinition definition(String key) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey(key);
        definition.setProvider("openai");
        definition.setModelType(LangChain4jModelType.CHAT);
        definition.setEnabled(true);
        return definition;
    }

    private static class FakeAdapter implements LangChain4jModelProviderAdapter {
        private final boolean supports;
        private final LangChain4jModel model;

        private FakeAdapter(boolean supports, LangChain4jModel model) {
            this.supports = supports;
            this.model = model;
        }

        public boolean supports(LangChain4jModelDefinition definition) {
            return supports;
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            return model;
        }
    }

    private static class ThrowingAdapter implements LangChain4jModelProviderAdapter {
        public boolean supports(LangChain4jModelDefinition definition) {
            return true;
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            throw new IllegalStateException("boom");
        }
    }
}
```

- [x] **Task 2 Step 2: Write failing registry tests**

```java
package com.xiongx9527.fw.plus.ai.langchain4j.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultLangChain4jModelRegistryTest {

    @Test
    public void shouldResolveModelsAndDefaultModel() {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        LangChain4jModel chat = new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object());
        models.put("chat", chat);

        DefaultLangChain4jModelRegistry registry = new DefaultLangChain4jModelRegistry(models, "chat");

        assertSame(chat, registry.get("chat"));
        assertSame(chat, registry.getDefault());
        assertTrue(registry.contains("chat"));
        assertTrue(registry.keys().contains("chat"));
    }

    @Test
    public void shouldFailWhenDefaultModelKeyIsMissing() {
        try {
            new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), "missing");
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("missing"));
        }
    }

    @Test
    public void shouldFailWhenModelKeyIsMissing() {
        DefaultLangChain4jModelRegistry registry =
                new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), null);
        try {
            registry.get("missing");
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("missing"));
        }
    }
}
```

- [x] **Task 2 Step 3: Run tests to verify they fail**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am \
  -Dtest=DefaultLangChain4jModelFactoryTest,DefaultLangChain4jModelRegistryTest test
```

Expected: FAIL because default factory and registry do not exist yet.

- [x] **Task 2 Step 4: Implement factory and registry behavior**

Implement `DefaultLangChain4jModelFactory` and `DefaultLangChain4jModelRegistry` exactly against the **Interfaces** block. Use `ArrayList`, `LinkedHashMap`, `LinkedHashSet`, `Collections.unmodifiableMap`, and `Collections.unmodifiableSet` for JDK 1.8 compatibility.

- [x] **Task 2 Step 5: Run core model tests**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am test
```

Expected: PASS.

- [x] **Task 2 Step 6: Commit this task during execution**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model
git commit -m "feat: add langchain4j model factory and registry"
```

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

- [x] **Task 3 Step 1: Write failing properties binding test**

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

- [x] **Task 3 Step 2: Write failing auto-configuration tests**

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

- [x] **Task 3 Step 3: Run tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest test
```

Expected: FAIL because properties and auto-configuration do not exist.

- [x] **Task 3 Step 4: Add dependencies and auto-configuration**

Modify `fw-plus-ai-boot-autoconfigure/pom.xml` to depend on `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model`. Implement properties and auto-configuration. Register `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.LangChain4jModelAutoConfiguration` in `META-INF/spring.factories` after `LangChain4jAutoConfiguration` and before Magic-API base auto-configuration.

- [x] **Task 3 Step 5: Run tests to verify they pass**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest test
```

Expected: PASS.

- [x] **Task 3 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model
git commit -m "feat: auto configure langchain4j model registry"
```

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

- [x] **Task 4 Step 1: Write failing adapter tests**

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

- [x] **Task 4 Step 2: Run adapter tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelProviderAdapterTest test
```

Expected: FAIL because provider adapter support classes do not exist.

- [x] **Task 4 Step 3: Implement reflective provider adapter support**

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

- [x] **Task 4 Step 4: Wire adapters as beans**

Add adapter bean methods to `LangChain4jModelAutoConfiguration`. The registry creation path must accept `List<LangChain4jModelProviderAdapter>` and pass it to `LangChain4jModelFactory.create`.

- [x] **Task 4 Step 5: Run provider and auto-configuration tests**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jModelProviderAdapterTest,LangChain4jModelAutoConfigurationTest test
```

Expected: PASS.

- [x] **Task 4 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model/provider
git commit -m "feat: add langchain4j model provider adapters"
```

### Task 5: Model Starter、BOM 与自动配置注册

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter/README.md`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/LangChain4jAutoConfigurationRegistrationTest.java`

**Interfaces:**
- Consumes: Task 3 and Task 4 auto-configuration.
- Produces:
  - Maven artifact `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter:${revision}`
  - Starter dependencies: `fw-plus-ai-boot-starter`, `fw-plus-ai-boot-autoconfigure`, `fw-plus-ai-langchain4j-model`
  - No direct dependencies on all concrete provider SDK artifacts.
  - `fw-plus-ai-boot-dependencies` manages the starter with `${project.version}`.

- [x] **Task 5 Step 1: Write failing registration and dependency test**

Extend `LangChain4jAutoConfigurationRegistrationTest` with assertions that `spring.factories` contains `LangChain4jModelAutoConfiguration` and that the new starter POM does not contain bulk provider artifacts such as `langchain4j-open-ai`, `langchain4j-ollama`, `langchain4j-dashscope`, `langchain4j-anthropic`, `langchain4j-azure-open-ai`.

- [x] **Task 5 Step 2: Run test to verify it fails**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

Expected: FAIL because the starter POM does not exist and registration assertion is not satisfied.

- [x] **Task 5 Step 3: Create starter and BOM entries**

Create the starter module POM, register it in `fw-plus-ai-boot-starters/pom.xml`, and add dependency management in `fw-plus-ai-boot-dependencies/pom.xml`.

- [x] **Task 5 Step 4: Add README usage sample**

Document this minimal configuration in the new starter README:

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        enabled: true
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: demo
            model-name: gpt-4o-mini
            properties: {}
```

State that provider SDK dependencies are introduced by the application when needed.

- [x] **Task 5 Step 5: Run starter build and registration test**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter -am -DskipTests install
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

Expected: both commands PASS.

- [x] **Task 5 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/LangChain4jAutoConfigurationRegistrationTest.java
git commit -m "feat: add langchain4j model starter"
```

### Task 6: Magic `model-datasource` 动态资源与运行态 Registry

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/model/ModelDataSourceInfo.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/model/MagicDynamicModelDataSource.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/service/ModelDataSourceInfoMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/service/ModelDataSourceMagicDynamicRegistry.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/MagicDynamicModelDataSourceTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/ModelDataSourceMagicDynamicRegistryTest.java`

**Interfaces:**
- Consumes:
  - `LangChain4jModelDefinition`
  - `LangChain4jModelFactory`
  - `LangChain4jModelProviderAdapter`
  - `LangChain4jModelRegistry`
- Produces:
  - `ModelDataSourceInfo.toDefinition(): LangChain4jModelDefinition`
  - `ModelDataSourceInfo.simple(): ModelDataSourceInfo` returns a copy with sensitive fields removed.
  - `ModelDataSourceInfoMagicResourceStorage.folder(): String` returns `model-datasource`.
  - `MagicDynamicModelDataSource.createClient(ModelDataSourceInfo info): void`
  - `MagicDynamicModelDataSource.destroyClientByKey(String key): void`
  - `MagicDynamicModelDataSource.datasourceNodes(): List<ModelDataSourceInfo>`
  - `MagicDynamicModelDataSource.getModel(String key): LangChain4jModel`
  - `MagicDynamicModelDataSource.getDefault(): LangChain4jModel`
  - `MagicDynamicModelDataSource.contains(String key): boolean`
  - `MagicDynamicModelDataSource.keys(): Set<String>`

- [x] **Task 6 Step 1: Write failing dynamic data source tests**

Create tests that prove:

```text
1. runtime model with key "chat" overrides Spring Boot registry key "chat".
2. destroyClientByKey("chat") removes runtime model and exposes Spring Boot registry key "chat" again.
3. datasourceNodes() returns runtime nodes with apiKey, secretKey, accessToken, token, password cleared.
4. createClient(null), blank key, blank provider, or null modelType throws LangChain4jModelException.
```

Use a fake `LangChain4jModelFactory` and fake `LangChain4jModelRegistry` in the test; no network call is allowed in this task.

- [x] **Task 6 Step 2: Write failing resource registry tests**

Create tests that prove:

```text
1. ModelDataSourceInfoMagicResourceStorage.folder() equals "model-datasource".
2. ModelDataSourceMagicDynamicRegistry registers a new ModelDataSourceInfo by calling MagicDynamicModelDataSource.createClient(info).
3. ModelDataSourceMagicDynamicRegistry unregisters by calling MagicDynamicModelDataSource.destroyClientByKey(key).
4. Updating the same key calls destroyClientByKey(key) before createClient(info).
```

- [x] **Task 6 Step 3: Run tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

Expected: FAIL because Magic model classes do not exist.

- [x] **Task 6 Step 4: Implement model datasource, storage, and registry**

Implement the classes listed in **Files**. Follow the existing Magic vector plugin style for storage and registry lifecycle, but use `model-datasource` as the folder and `LangChain4jModelDefinition` as the target definition.

- [x] **Task 6 Step 5: Run tests to verify they pass**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

Expected: PASS.

- [x] **Task 6 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
git commit -m "feat: add magic model datasource registry"
```

### Task 7: Magic `model` 模块、Controller 与插件配置

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/module/ModelModule.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/web/MagicModelController.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/starter/MagicAPIModelConfig.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/starter/MagicAPIModelConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/ModelModuleTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/MagicModelControllerTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/MagicAPIModelConfigurationTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/MagicAPIModelAutoConfigurationIntegrationTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`

**Interfaces:**
- Consumes: Task 6 `MagicDynamicModelDataSource`.
- Produces:
  - `@MagicModule("model") public class ModelModule implements DynamicAttribute<ModelModule, ModelModule>, DynamicModule<ModelModule>`
  - `ModelModule.getDynamicAttribute(String key): ModelModule`
  - `ModelModule.getDynamicModule(MagicScriptContext context): ModelModule`
  - `ModelModule.getModelObject(): LangChain4jModel`
  - `ModelModule.getModel(): Object`
  - `MagicAPIModelConfiguration.plugin()` returns `new Plugin("model", "MagicModel", "magic-model.1.0.0.iife.js")`.
  - `MagicAPIModelConfiguration.controllerRegister()` registers `MagicModelController`.
  - Controller list response uses `ModelDataSourceInfo.simple()`.
  - Controller test endpoint calls `LangChain4jModelFactory.create(definition, adapters)` for real factory validation and masks sensitive fields on failure.

- [ ] **Task 7 Step 1: Write failing module tests**

Create tests that prove:

```text
1. @MagicModule value on ModelModule is "model".
2. new ModelModule(dataSource).getDynamicAttribute("chat").getModelObject() returns dataSource.getModel("chat").
3. getDynamicModule(context) resolves Options.DEFAULT_DATA_SOURCE when it is set.
4. getModel() returns the wrapped native LangChain4j model object.
```

- [ ] **Task 7 Step 2: Write failing controller/configuration tests**

Create tests that prove:

```text
1. MagicAPIModelConfiguration.plugin() exposes id "model", name "MagicModel", and asset "magic-model.1.0.0.iife.js".
2. MagicAPIModelConfiguration creates MagicDynamicModelDataSource, ModelDataSourceInfoMagicResourceStorage, ModelDataSourceMagicDynamicRegistry, and ModelModule beans.
3. MagicApiPluginAutoConfiguration imports MagicAPIModelConfiguration and no longer imports MagicAPILangchain4jConfiguration.
4. MagicModelController test endpoint calls the factory and returns no raw apiKey, secretKey, accessToken, token, or password in success or failure payloads.
```

- [ ] **Task 7 Step 3: Run tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=ModelModuleTest,MagicModelControllerTest,MagicAPIModelConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest test
```

Expected: FAIL because module, controller, and configuration do not exist.

- [ ] **Task 7 Step 4: Implement module, controller, and configuration**

Implement the classes listed in **Files**. The configuration property prefix for the plugin is `magic-api.plugin.model.enabled`, with `havingValue = "true"` and `matchIfMissing = true`.

- [ ] **Task 7 Step 5: Run Magic model backend tests**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest,ModelModuleTest,MagicModelControllerTest,MagicAPIModelConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest test
```

Expected: PASS.

- [ ] **Task 7 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
git commit -m "feat: add magic model plugin backend"
```

### Task 8: Magic Console 插件模块迁移与旧入口删除

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/package.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/index.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/service/magic-model-datasource.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/components/magic-model-datasource-info.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/i18n/zh-cn.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/i18n/en.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/test/magic-model-contract.test.cjs`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/vite.config.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/main/resources/magic-editor/plugins/magic-model.1.0.0.iife.js`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j/`

**Interfaces:**
- Consumes: Task 7 plugin id `model`, asset `magic-model.1.0.0.iife.js`, resource folder `model-datasource`.
- Produces:
  - Maven artifact `com.xiongx9527.fw.plus.ai:fw-plus-ai-magicapi-plugin-model:${revision}`
  - Magic console service uses resource type `model-datasource`.
  - Console form fields: `name`, `key`, `provider`, `modelType`, `enabled`, `baseUrl`, `apiKey`, `modelName`, `properties`.
  - Sample depends on `fw-plus-ai-magicapi-plugin-model` and no longer depends on `fw-plus-ai-magicapi-plugin-langchain4j`.

- [ ] **Task 8 Step 1: Write failing console contract test**

Create `magic-model-contract.test.cjs` asserting:

```javascript
const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert');

const root = path.resolve(__dirname, '..');

test('uses model datasource resource type and plugin name', () => {
  const files = [
    path.join(root, 'src/service/magic-model-datasource.js'),
    path.join(root, 'src/index.js'),
    path.join(root, 'src/components/magic-model-datasource-info.vue')
  ].map(file => fs.readFileSync(file, 'utf8')).join('\n');

  assert.match(files, /model-datasource/);
  assert.match(files, /MagicModel|magic-model/);
  assert.doesNotMatch(files, /langchain4j-datasource/);
  assert.doesNotMatch(files, /MagicLangchain4j/);
});
```

- [ ] **Task 8 Step 2: Run console test to verify it fails**

Run:

```bash
cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console
npm test
```

Expected: FAIL because the console package does not exist.

- [ ] **Task 8 Step 3: Create model plugin module and console assets**

Create `fw-plus-ai-magicapi-plugin-model` by carrying over the working vector console structure and replacing identifiers with `model`, `model-datasource`, and `magic-model.1.0.0.iife.js`. Keep controls for sensitive fields as password inputs and do not render raw secret values in list rows.

- [ ] **Task 8 Step 4: Delete old Magic LangChain4j plugin entry points**

Remove old backend package `plugin/langchain4j` and old starter module `fw-plus-ai-magicapi-plugin-langchain4j`. Update `fw-plus-ai-magicapi-plugin/pom.xml` to replace the old module with `fw-plus-ai-magicapi-plugin-model`. Update `fw-plus-ai-boot-dependencies/pom.xml` and sample POM references from `fw-plus-ai-magicapi-plugin-langchain4j` to `fw-plus-ai-magicapi-plugin-model`.

- [ ] **Task 8 Step 5: Run console and Maven migration checks**

Run:

```bash
cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console
npm test
npm run build
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model -am -DskipTests install
./mvnw -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -am -DskipTests package
```

Expected: all commands PASS. Built JS asset exists at `src/main/resources/magic-editor/plugins/magic-model.1.0.0.iife.js`.

- [ ] **Task 8 Step 6: Run structural deletion check**

Run:

```bash
test ! -d fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j
test ! -d fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j
rg -n "langchain4j-datasource|MagicLangchain4j|@MagicModule\\(\"langchain4j\"\\)|fw-plus-ai-magicapi-plugin-langchain4j" fw-plus-ai
```

Expected: the two `test ! -d` commands exit 0. The `rg` command returns no matches.

- [ ] **Task 8 Step 7: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin
git commit -m "feat: replace magic langchain4j plugin with model plugin"
```

### Task 9: Documentation、Samples 配置与全量验证

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-ai/README.zh-CN.md`
- Modify: `fw-plus-ai/README.md`
- Modify if needed: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/README.md`

**Interfaces:**
- Consumes: all prior tasks.
- Produces:
  - Sample YAML contains `fw-plus.ai.langchain4j.model` configuration example and `magic-api.plugin.model.enabled=true`.
  - Docs explain model starter minimal dependency, provider SDK explicit dependency rule, `model-datasource` JSON example, and migration from old Magic `langchain4j` plugin.
  - Verification record can be pasted into OpenSpec verification later without changing OpenSpec in this task.

- [ ] **Task 9 Step 1: Add sample configuration**

Add this sample block to `application.yaml` using existing indentation style:

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        enabled: true
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: demo
            model-name: gpt-4o-mini
            enabled: true
            properties: {}

magic-api:
  plugin:
    model:
      enabled: true
```

If `fw-plus` or `magic-api.plugin` already exists, merge keys without duplicating top-level blocks.

- [ ] **Task 9 Step 2: Add documentation examples**

Document this `model-datasource` resource example:

```json
{
  "name": "本地 OpenAI 兼容模型",
  "key": "chat-default",
  "provider": "openai",
  "modelType": "CHAT",
  "enabled": true,
  "baseUrl": "http://localhost:11434/v1",
  "apiKey": "demo",
  "modelName": "gpt-4o-mini",
  "properties": {
    "temperature": 0.2
  }
}
```

Document this Magic script usage:

```javascript
import model;
return model['chat-default'].getModel();
```

Document LangChain4j version compatibility:
- 本模块以 LangChain4j 0.35.0 编译，Chat 类型对应 `ChatLanguageModel`，Streaming Chat 类型对应 `StreamingChatLanguageModel`。
- 高版本 LangChain4j 使用 `ChatModel` / `StreamingChatModel` 命名，当前版本不能引用这些类。
- 0.35.0 已有 `TextContent`、`ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 等多模态内容类型；高版本继续通过 Chat 请求承载多模态输入。
- 不存在独立 `AudioModel` / `VideoModel`，不要在 `modelType` 中新增这两个值；音频、视频、PDF 是 Chat 输入内容能力。

- [ ] **Task 9 Step 3: Run targeted core verification**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am test
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -am test
```

Expected: both commands PASS.

- [ ] **Task 9 Step 4: Run targeted AI Boot verification**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest,LangChain4jModelProviderAdapterTest,MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest,ModelModuleTest,MagicModelControllerTest,MagicAPIModelConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicDynamicVectorDataSourceTest,MagicAPIVectorConfigurationTest,MagicAPIVectorAutoConfigurationIntegrationTest,VectorModuleTest,MagicVectorControllerTest test
```

Expected: PASS. Vector plugin tests passing confirms model work did not regress vector behavior.

- [ ] **Task 9 Step 5: Run module build verification**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter -am -DskipTests install
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model -am -DskipTests install
./mvnw -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -am -DskipTests package
```

Expected: all commands PASS.

- [ ] **Task 9 Step 6: Run source compatibility and migration scans**

Run:

```bash
rg -n "List\\.of\\(|Map\\.of\\(|Map\\.copyOf\\(|Set\\.of\\(|Stream\\.toList\\(|Optional\\.orElseThrow\\(" fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
rg -n "langchain4j-datasource|MagicLangchain4j|@MagicModule\\(\"langchain4j\"\\)|fw-plus-ai-magicapi-plugin-langchain4j" fw-plus-ai
rg -n "apiKey|secretKey|accessToken|password|token" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
```

Expected:
- First `rg` returns no matches.
- Second `rg` returns no matches.
- Third `rg` only shows masking, filtering, property mapping, or test-safe handling; no controller list/test response returns raw sensitive values.

- [ ] **Task 9 Step 7: Capture verification notes**

Record command outputs in the implementation handoff or PR body. If a dependency cannot resolve from internal Nexus, capture the exact Maven coordinate and repository URL from the failing output, and do not mark the behavior as failed until dependency resolution is addressed.

- [ ] **Task 9 Step 8: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml \
  fw-plus-ai/README.zh-CN.md \
  fw-plus-ai/README.md \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/README.md
git commit -m "docs: document langchain4j model magicapi plugin"
```

## Coverage Self-Review

- Core reusable model module is covered by Task 1 and Task 2.
- Maven aggregate and BOM registration for core and AI Boot are covered by Task 1 and Task 5.
- Spring Boot properties, factory, provider adapters, registry, and SDK isolation are covered by Task 3 and Task 4.
- Magic `model-datasource`, runtime model priority, script module, controller, sensitive masking, and plugin registration are covered by Task 6 and Task 7.
- Old Magic `langchain4j` implementation deletion and artifact migration are covered by Task 8.
- Sample/docs updates and regression verification are covered by Task 9.
- JDK 1.8, Maven Wrapper usage, no provider SDK in core, and no sensitive output are enforced in Global Constraints and verification scans.
