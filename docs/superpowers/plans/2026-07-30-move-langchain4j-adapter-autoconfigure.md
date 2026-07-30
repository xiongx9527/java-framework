---
change: move-langchain4j-adapter-autoconfigure
design-doc: docs/superpowers/specs/2026-07-30-move-langchain4j-adapter-autoconfigure-design.md
base-ref: 0212ccdebe60b458811d8ed0e3d20c47310a54d7
---

# LangChain4j Adapter 自动配置合并 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除独立 LangChain4j adapter autoconfigure artifact，并在不改变运行时行为和 provider 依赖边界的前提下，将其全部自动配置合入 `fw-plus-ai-boot-autoconfigure`。

**Architecture:** 保留 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包级隔离，把原模块的源码、测试和 optional 编译依赖迁入统一 Boot autoconfigure。adapter Starter 改为显式依赖统一 autoconfigure；`spring.factories` 与 `AutoConfiguration.imports` 同时注册同一根配置，并用上下文测试证明不会产生重复 Bean。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Maven 3.9.16 Reactor、JUnit 4/5、`ApplicationContextRunner`、`FilteredClassLoader`、Maven Enforcer、OpenSpec 1.6.0。

## Global Constraints

- Java 编译基线保持 1.8，不引入 Java 9+ API。
- LangChain4j 版本保持 0.35.0，不修改内部 Nexus 或 Magic-API 2.2.2 依赖。
- 不修改 Core `fw-plus-ai-langchain4j-adapter`、AskData 业务实现、中立配置前缀或 adapter 选择算法。
- 不新增 provider SDK；迁入统一 autoconfigure 的具体框架/provider 依赖必须保持 `optional`。
- 保留 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 公开坐标，删除 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 坐标且不提供兼容 POM。
- 保留现有 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*` 包、类名、Bean 名称和条件注解。
- 工作区中的 TinyEngine 及 `.superpowers/sdd` 改动不属于本 change，不得暂存、提交、格式化或回滚。
- 当前检出未跟踪 `.mvn/wrapper` 文件，所有构建和测试命令使用已验证的系统 Maven 3.9.16 与 JDK 8。

---

### Task 1: 建立模块归属和依赖边界 RED 测试

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/NeutralStarterDependencyTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/LegacyAiArtifactBoundaryTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/NeutralAiAutoConfigurationBoundaryTest.java`

**Interfaces:**
- Consumes: 现有 XML helper `dependencies(File, boolean)`、`modules(File)` 与源码扫描 helper。
- Produces: 旧 autoconfigure 模块/坐标删除、目标包存在、包级中立边界的可执行结构契约。

- [x] **Step 1: 更新 Starter/BOM/Reactor 期望**

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

- [x] **Step 2: 把旧 autoconfigure artifact 纳入删除边界**

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

- [x] **Step 3: 将中立边界收敛为包级隔离**

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

- [x] **Step 4: 运行目标测试确认 RED**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am -DfailIfNoTests=false \
  -Dtest=NeutralStarterDependencyTest,LegacyAiArtifactBoundaryTest,NeutralAiAutoConfigurationBoundaryTest test
```

Expected: FAIL；失败必须指向旧 module/BOM/Starter 依赖仍存在、目标 `langchain4j` 包尚不存在或 adapter 根配置尚未合并。若失败来自依赖下载或环境问题，先按 systematic-debugging 定位，不把环境失败记为 RED。

- [x] **Step 5: 提交 RED 测试**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/NeutralStarterDependencyTest.java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/starter/LegacyAiArtifactBoundaryTest.java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/NeutralAiAutoConfigurationBoundaryTest.java
git commit -m "test(ai): define consolidated autoconfigure boundary"
```

### Task 2: 迁移源码、测试和 optional 编译依赖

**Files:**
- Move: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**`
- Move: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`

**Interfaces:**
- Consumes: 46 个生产类、4 个测试类以及旧模块 POM 中的编译依赖清单。
- Produces: 统一模块内原包名可加载的 LangChain4j 配置类和完整 adapter 回归测试。

- [x] **Step 1: 移动 Java 源码和测试**

保持 `langchain4j/**` 相对目录不变，将旧模块的 main/test Java 树移动到统一模块对应 source root。移动后验证数量：

```bash
find fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j -type f | wc -l
find fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j -type f | wc -l
```

Expected: `46` 与 `4`。

- [x] **Step 2: 合并 POM 依赖并去重**

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

- [x] **Step 3: 编译统一模块并运行 provider 单测**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -DfailIfNoTests=false \
  -Dtest=LangChain4jModelProviderAdapterTest,EmbeddingStoreProviderAdapterTest test
```

Expected: BUILD SUCCESS；迁移测试全部通过，且无重复类编译错误。

- [x] **Step 4: 提交源码和依赖迁移**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/main/java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/test/java
git commit -m "refactor(ai): move langchain4j autoconfigure sources"
```

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

- [x] **Step 1: 先补充双注册和去重断言**

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

- [x] **Step 2: 运行元数据测试确认 RED**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -DfailIfNoTests=false \
  -Dtest=AiEmbeddingStoreAutoConfigurationMetadataTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
```

Expected: FAIL，原因是统一模块尚无 imports 文件且 `spring.factories` 尚未注册 adapter 根配置。

- [x] **Step 3: 合并两个注册资源**

在统一 `spring.factories` 的 `EnableAutoConfiguration` 列表末尾追加：

```properties
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration
```

创建 imports 文件，内容严格为：

```text
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration
```

- [x] **Step 4: 运行注册、缺 SDK 和条件反射测试确认 GREEN**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am \
  -DfailIfNoTests=false \
  -Dtest=AiEmbeddingStoreAutoConfigurationMetadataTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest#shouldStartWhenOptionalEmbeddingStoreProviderSdksAreMissing+shouldDeclareClassConditionsOnDirectLinkedEmbeddingStoreProviderBeans test
```

Expected: BUILD SUCCESS；缺 SDK 场景无 startup failure，六类 provider Bean 不注册，in-memory adapter 保留。

- [x] **Step 5: 提交双注册实现**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure
git commit -m "test(ai): preserve langchain4j dual registration"
```

### Task 4: 切换 Starter 并删除旧 Maven 模块

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/`

**Interfaces:**
- Consumes: Task 2/3 已迁移的类、测试和注册资源。
- Produces: 只保留 adapter Starter 与统一 autoconfigure 的发布结构。

- [x] **Step 1: 替换 adapter Starter 依赖**

将旧依赖替换为：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.ai</groupId>
	<artifactId>fw-plus-ai-boot-autoconfigure</artifactId>
</dependency>
```

保留 model/store Starter、Core adapter 和 Maven Enforcer 配置不变。

- [x] **Step 2: 删除 Reactor/BOM 坐标和旧目录**

从 AI project `<modules>` 删除旧 module，从 Boot BOM `<dependencyManagement>` 删除旧 dependency。确认源码、测试和资源均已迁移后，删除旧模块剩余 POM/README/空目录。

- [x] **Step 3: 运行 Task 1 的结构测试确认 GREEN**

Run Task 1 Step 4 的同一命令。

Expected: BUILD SUCCESS；旧目录、module、BOM 和 Starter 依赖断言全部通过；中立包扫描只排除精确 `langchain4j` 子树。

- [x] **Step 4: 验证 Starter Enforcer**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter \
  -am validate
```

Expected: BUILD SUCCESS，日志包含 `BannedDependencies passed`；不得解析为 provider SDK 传递依赖。

- [x] **Step 5: 提交坐标切换与模块删除**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure
git commit -m "refactor(ai): fold langchain4j autoconfigure module"
```

### Task 5: 更新发布文档和旧坐标扫描

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/README.md`
- Modify: `docs/ai-framework-migration/overview.md`
- Modify: `docs/ai-framework-migration/compatibility-matrix.md`
- Modify: `docs/ai-framework-migration/verify-migration-docs.sh`
- Modify: `docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md`

**Interfaces:**
- Consumes: 删除后的 Maven 坐标和统一模块测试命令。
- Produces: 用户可执行的依赖迁移说明与自动拒绝旧坐标的文档验证。

- [x] **Step 1: 更新 Starter README 和构建命令**

README 依赖清单改为 `fw-plus-ai-boot-autoconfigure`，明确旧 autoconfigure artifact 已删除，业务仍只需引入 adapter Starter。`overview.md`、`compatibility-matrix.md` 和历史验证报告中的 `-pl` 命令只保留统一模块。

- [x] **Step 2: 扩展文档校验脚本**

增加旧坐标变量和检查：

```zsh
removed_autoconfigure_artifact="fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure"

for doc_path in $breaking_docs $starter_docs; do
	if rg -n -F -- "$removed_autoconfigure_artifact" "$doc_path"; then
		print -u2 -- "文档仍引用已删除 autoconfigure artifact: ${doc_path:t}"
		failures=$((failures + 1))
	fi
done
```

对说明历史事实的归档 OpenSpec/Design Doc 不做全仓禁止；扫描范围只覆盖当前发布和迁移文档。

- [x] **Step 3: 运行文档校验与当前发布引用扫描**

Run:

```bash
docs/ai-framework-migration/verify-migration-docs.sh
rg -n -F --glob 'pom.xml' \
  'fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure' \
  fw-plus-ai/fw-plus-ai-project/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters
rg -n -F --glob '*.md' \
  'fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure' \
  docs/ai-framework-migration \
  docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md
```

Expected: 文档脚本输出 `迁移文档校验通过: 3 份目标框架文档`；两个 `rg` 分别扫描可发布 POM 与当前 Markdown，均无匹配并以退出码 1 结束。负向测试源码和校验脚本实现不属于发布引用扫描范围。

- [x] **Step 4: 提交文档和校验规则**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/README.md \
  docs/ai-framework-migration \
  docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md
git commit -m "docs(ai): document consolidated autoconfigure"
```

### Task 6: 完整验证、任务同步和验证报告

**Files:**
- Create: `docs/superpowers/reports/2026-07-30-move-langchain4j-adapter-autoconfigure-verify.md`

**Interfaces:**
- Consumes: 前五个任务的提交、测试与文档证据。
- Produces: 可审计的 JDK 8 构建结果和验证报告；OpenSpec 任务状态由主协调会话在验收证据通过后同步。

- [x] **Step 1: 运行统一 autoconfigure 全量测试**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am test
```

Expected: BUILD SUCCESS，0 failures/0 errors；报告记录实际测试数量。

- [x] **Step 2: 运行统一模块与 adapter Starter 目标 Reactor**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter \
  -am test
```

Expected: BUILD SUCCESS；Maven Enforcer 通过且迁移后的 4 个 adapter 测试类均在统一模块执行。

- [x] **Step 3: 执行规格、文档和 diff 校验**

```bash
openspec validate move-langchain4j-adapter-autoconfigure --strict
docs/ai-framework-migration/verify-migration-docs.sh
git diff --check 0212ccdebe60b458811d8ed0e3d20c47310a54d7..HEAD
```

Expected: OpenSpec valid、迁移文档通过、`git diff --check` 无输出。

- [x] **Step 4: 编写验证报告**

报告必须列出：删除的公开坐标、46/4 文件迁移事实、双注册去重证据、缺 SDK 测试、Maven Enforcer、实际测试数量、未修改 Core/provider/AskData 边界和任何未执行验证。实现代理不得修改 plan 或 OpenSpec 复选框；主协调会话仅在对应证据真实通过后同步这些状态。

- [x] **Step 5: 提交验证证据**

```bash
git add \
  docs/superpowers/reports/2026-07-30-move-langchain4j-adapter-autoconfigure-verify.md
git commit -m "test(ai): verify consolidated autoconfigure"
```

- [x] **Step 6: 最终状态检查**

```bash
git status --short
git log --oneline 0212ccdebe60b458811d8ed0e3d20c47310a54d7..HEAD
```

Expected: 当前 change 的实现文件无未提交改动；输出只包含本计划产生的聚焦提交。若仍有 TinyEngine 等无关改动，只报告并保持不处理。
