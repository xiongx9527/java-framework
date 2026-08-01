### Task 2: 迁移源码、测试和 optional 编译依赖

**Files:**
- Move: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**`
- Move: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`

**Interfaces:**
- Consumes: 46 个生产类、4 个测试类以及旧模块 POM 中的编译依赖清单。
- Produces: 统一模块内原包名可加载的 LangChain4j 配置类和完整 adapter 回归测试。

- [ ] **Step 1: 移动 Java 源码和测试**

保持 `langchain4j/**` 相对目录不变，将旧模块的 main/test Java 树移动到统一模块对应 source root。移动后验证数量：

```bash
find fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j -type f | wc -l
find fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j -type f | wc -l
```

Expected: `46` 与 `4`。

- [ ] **Step 2: 合并 POM 依赖并去重**

在统一 POM 保留已存在的 `fw-plus-ai-langchain4j-core`、`fw-plus-ai-langchain4j-adapter`、`langchain4j`、`langchain4j-core`，并把旧模块其余生产依赖迁入。具体框架/provider 依赖必须使用：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-ai-langchain4j-openai</artifactId>
	<scope>compile</scope>
	<optional>true</optional>
</dependency>
```

直接 SDK 同样保持 optional，例如：

```xml
<dependency>
	<groupId>dev.langchain4j</groupId>
	<artifactId>langchain4j-qdrant</artifactId>
	<scope>compile</scope>
	<optional>true</optional>
</dependency>
```

逐项覆盖旧 POM 的 wrapper/provider 列表，不改变版本属性；测试依赖只补缺失项。

- [ ] **Step 3: 编译统一模块并运行 provider 单测**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -DfailIfNoTests=false \
  -Dtest=LangChain4jModelProviderAdapterTest,EmbeddingStoreProviderAdapterTest test
```

Expected: BUILD SUCCESS；迁移测试全部通过，且无重复类编译错误。

- [ ] **Step 4: 提交源码和依赖迁移**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/main/java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/test/java
git commit -m "refactor(ai): move langchain4j autoconfigure sources"
```

