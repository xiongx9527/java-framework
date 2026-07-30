### Task 3: 合并双注册元数据并证明 Bean 不重复

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/store/embedding/AiEmbeddingStoreAutoConfigurationMetadataTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/LangChain4jAutoConfigurationRegistrationTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/adapter/LangChain4jAdapterAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `LangChain4jAdapterAutoConfiguration` 现有条件装配。
- Produces: Boot 2.7 双注册资源与“每类 adapter/provider 只产生一个 Bean”的回归契约。

- [ ] **Step 1: 先补充双注册和去重断言**

把 `AiEmbeddingStoreAutoConfigurationMetadataTest` 的 imports 不存在断言改为读取该文件并检查 adapter 根配置。`LangChain4jAutoConfigurationRegistrationTest` 同时读取两个资源，并使用出现次数 helper：

```java
private int occurrences(String content, String value) {
	int count = 0;
	int index = 0;
	while ((index = content.indexOf(value, index)) >= 0) {
		count++;
		index += value.length();
	}
	return count;
}
```

断言每个资源中目标类恰好出现一次。

在 `LangChain4jAdapterAutoConfigurationTest` 增加一次通过元数据启动的上下文测试，断言：

```java
assertTrue(context.containsBean("openAiChatModelProviderAdapter"));
assertEquals(1, context.getBeansOfType(AiModelAdapter.class).size());
assertEquals(1, context.getBeansOfType(AiEmbeddingStoreAdapter.class).size());
```

- [ ] **Step 2: 运行元数据测试确认 RED**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -DfailIfNoTests=false \
  -Dtest=AiEmbeddingStoreAutoConfigurationMetadataTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
```

Expected: FAIL，原因是统一模块尚无 imports 文件且 `spring.factories` 尚未注册 adapter 根配置。

- [ ] **Step 3: 合并两个注册资源**

在统一 `spring.factories` 的 `EnableAutoConfiguration` 列表末尾追加：

```properties
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration
```

创建 imports 文件，内容严格为：

```text
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration
```

- [ ] **Step 4: 运行注册、缺 SDK 和条件反射测试确认 GREEN**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -DfailIfNoTests=false \
  -Dtest=AiEmbeddingStoreAutoConfigurationMetadataTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest#shouldStartWhenOptionalEmbeddingStoreProviderSdksAreMissing+shouldDeclareClassConditionsOnDirectLinkedEmbeddingStoreProviderBeans test
```

Expected: BUILD SUCCESS；缺 SDK 场景无 startup failure，六类 provider Bean 不注册，in-memory adapter 保留。

- [ ] **Step 5: 提交双注册实现**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure
git commit -m "test(ai): preserve langchain4j dual registration"
```

