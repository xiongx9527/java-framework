### Task 1: 建立模块归属和依赖边界 RED 测试

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/NeutralStarterDependencyTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/LegacyAiArtifactBoundaryTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/NeutralAiAutoConfigurationBoundaryTest.java`

**Interfaces:**
- Consumes: 现有 XML helper `dependencies(File, boolean)`、`modules(File)` 与源码扫描 helper。
- Produces: 旧 autoconfigure 模块/坐标删除、目标包存在、包级中立边界的可执行结构契约。

- [ ] **Step 1: 更新 Starter/BOM/Reactor 期望**

将 adapter Starter 的必需依赖从旧坐标改为统一 autoconfigure，并增加旧坐标不得出现的断言：

```java
assertDependencies("fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml",
		"fw-plus-ai-model-spring-boot-starter",
		"fw-plus-ai-store-embedding-spring-boot-starter",
		"fw-plus-ai-langchain4j-adapter",
		"fw-plus-ai-boot-autoconfigure");
assertDoesNotDependOn("fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml",
		"fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure");
assertFalse(bomArtifacts.contains("fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure"));
assertFalse(modules(findProjectFile("fw-plus-ai-project/pom.xml"))
		.contains("fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure"));
```

把 `modules(...)` 的调用范围扩展到 AI project POM；helper 仍使用现有 DOM 解析，不新增字符串解析。

- [ ] **Step 2: 把旧 autoconfigure artifact 纳入删除边界**

在 `LegacyAiArtifactBoundaryTest` 增加旧目录和 artifact marker：

```java
"fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure"
```

同时断言目标目录存在：

```java
File langChain4jPackage = new File(projectRoot,
		"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/"
				+ "com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j");
assertTrue("Unified autoconfigure must own LangChain4j package", langChain4jPackage.isDirectory());
```

- [ ] **Step 3: 将中立边界收敛为包级隔离**

`NeutralAiAutoConfigurationBoundaryTest.isIgnored(...)` 只新增 `autoconfigure/langchain4j` 精确目录例外，保留 AskData 既有例外；注册断言改为允许 adapter 根配置，但继续禁止已删除的 legacy model/store/openapi 配置：

```java
|| path.contains("/autoconfigure/langchain4j/")
|| path.endsWith("/autoconfigure/langchain4j")
```

```java
assertTrue(content.contains(
		"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter."
				+ "LangChain4jAdapterAutoConfiguration"));
```

- [ ] **Step 4: 运行目标测试确认 RED**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am -DfailIfNoTests=false \
  -Dtest=NeutralStarterDependencyTest,LegacyAiArtifactBoundaryTest,NeutralAiAutoConfigurationBoundaryTest test
```

Expected: FAIL；失败必须指向旧 module/BOM/Starter 依赖仍存在、目标 `langchain4j` 包尚不存在或 adapter 根配置尚未合并。若失败来自依赖下载或环境问题，先按 systematic-debugging 定位，不把环境失败记为 RED。

- [ ] **Step 5: 提交 RED 测试**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/NeutralStarterDependencyTest.java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/LegacyAiArtifactBoundaryTest.java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/NeutralAiAutoConfigurationBoundaryTest.java
git commit -m "test(ai): define consolidated autoconfigure boundary"
```

