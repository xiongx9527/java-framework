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

- [ ] **Task 5 Step 1: Write failing registration and dependency test**

Extend `LangChain4jAutoConfigurationRegistrationTest` with assertions that `spring.factories` contains `LangChain4jModelAutoConfiguration` and that the new starter POM does not contain bulk provider artifacts such as `langchain4j-open-ai`, `langchain4j-ollama`, `langchain4j-dashscope`, `langchain4j-anthropic`, `langchain4j-azure-open-ai`.

- [ ] **Task 5 Step 2: Run test to verify it fails**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

Expected: FAIL because the starter POM does not exist and registration assertion is not satisfied.

- [ ] **Task 5 Step 3: Create starter and BOM entries**

Create the starter module POM, register it in `fw-plus-ai-boot-starters/pom.xml`, and add dependency management in `fw-plus-ai-boot-dependencies/pom.xml`.

- [ ] **Task 5 Step 4: Add README usage sample**

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

- [ ] **Task 5 Step 5: Run starter build and registration test**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter -am -DskipTests install
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAutoConfigurationRegistrationTest test
```

Expected: both commands PASS.

- [ ] **Task 5 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/LangChain4jAutoConfigurationRegistrationTest.java
git commit -m "feat: add langchain4j model starter"
```

