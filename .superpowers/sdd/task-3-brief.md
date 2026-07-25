### Task 3: Core Spring Boot starter 与 Maven/BOM 接入

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Create/Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfiguration.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationMetadataTest.java`

**Interfaces:**
- Consumes: `EmbeddingStoreProperties` with prefix `fw-plus.ai.langchain4j.embedding-store`.
- Produces: `EmbeddingStoreFactory` bean when missing.
- Produces: `EmbeddingStoreRegistry` bean when `fw-plus.ai.langchain4j.embedding-store.enabled=true` or missing.
- Produces: Spring Boot 2.7 `spring.factories` registration only.

- [ ] **Step 1: Write failing metadata test**

Create `EmbeddingStoreAutoConfigurationMetadataTest`:

```java
@Test
public void shouldRegisterAutoConfigurationInSpringFactoriesOnly() throws Exception {
    String factories = read("META-INF/spring.factories");
    assertTrue(factories.contains("com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.EmbeddingStoreAutoConfiguration"));
    assertFalse(new ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports").exists());
    assertFalse(new ClassPathResource("META-INF/spring/org.springframework.boot.autoconfig.AutoConfiguration.imports").exists());
}
```

- [ ] **Step 2: Write failing auto-configuration tests**

Use `ApplicationContextRunner` to cover enabled=false, multiple stores, default store valid, default store invalid, unknown provider:

```java
@Test
public void shouldCreateRegistryWithMultipleStoresAndDefaultKey() {
    this.contextRunner
            .withPropertyValues(
                    "fw-plus.ai.langchain4j.embedding-store.enabled=true",
                    "fw-plus.ai.langchain4j.embedding-store.default-store=knowledge",
                    "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                    "fw-plus.ai.langchain4j.embedding-store.stores.local-cache.provider=fake")
            .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
            .run(context -> {
                assertTrue(context.containsBean("embeddingStoreRegistry"));
                EmbeddingStoreRegistry registry = context.getBean(EmbeddingStoreRegistry.class);
                assertTrue(registry.contains("knowledge"));
                assertTrue(registry.contains("local-cache"));
                assertSame(registry.get("knowledge"), registry.getDefault());
            });
}
```

- [ ] **Step 3: Run focused tests and verify failure**

Run: `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -Dtest=EmbeddingStoreAutoConfigurationMetadataTest,EmbeddingStoreAutoConfigurationTest test`

Expected: FAIL because starter module and auto-configuration are incomplete.

- [ ] **Step 4: Register Maven module and dependencyManagement**

Add module after `fw-plus-ai-langchain4j-store-embeding` in `fw-plus-ai-langchain4j/pom.xml`:

```xml
<module>fw-plus-ai-langchain4j-store-embeding-spring-boot-starter</module>
```

Add Core BOM entry near existing LangChain4j modules:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-store-embeding-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 5: Implement auto-configuration**

`EmbeddingStoreAutoConfiguration` should be `@Configuration`, `@EnableConfigurationProperties(EmbeddingStoreProperties.class)`, `@ConditionalOnProperty(prefix = "fw-plus.ai.langchain4j.embedding-store", name = "enabled", havingValue = "true", matchIfMissing = true)`. It should:

- register `DefaultEmbeddingStoreFactory` when missing;
- collect all `EmbeddingStoreProviderAdapter` beans;
- build enabled store definitions from `properties.getStores()`;
- copy map key into `EmbeddingStoreDefinition.key` when the object key is blank;
- validate duplicate key, blank provider, invalid default store, unknown provider;
- expose a single `EmbeddingStoreRegistry` bean.

- [ ] **Step 6: Add `spring.factories`**

Create:

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.EmbeddingStoreAutoConfiguration
```

- [ ] **Step 7: Run starter tests**

Run the command from Step 3.

Expected: PASS.

- [ ] **Step 8: Run Core LangChain4j reactor build**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

Commit message: `feat(vector): add embedding store spring boot starter`.

