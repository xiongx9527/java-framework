# TinyEngine AI Starter 业务依赖对齐实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让业务只引入 `fw-plus-ai-tiny-engine-spring-boot-starter` 时，同时获得现有 TinyEngine Boot Starter 与最新中立 AI Starter 的完整能力。

**Architecture:** AI 层 BOM 导入现有 Boot BOM，统一管理 Boot Starter 版本；TinyEngine AI Starter 将对 Core 的直接依赖替换为对现有 TinyEngine Boot Starter 的直接依赖。运行时代码保持不变，使用 POM 结构测试和 Spring Boot 自动配置发现测试锁定依赖方向与聚合行为。

**Tech Stack:** Java 8、Maven、Spring Boot 2.7.18、JUnit 4、SpringFactoriesLoader、LangChain4j 0.35.0

## Global Constraints

- 必须复用 `fw-plus-tiny-engine-spring-boot-starter`，不得新建、复制、内嵌或重新实现 Boot Starter。
- `fw-plus-ai-tiny-engine-spring-boot-starter` 必须直接依赖 Boot Starter、`fw-plus-ai-model-spring-boot-starter`、`fw-plus-ai-store-embedding-spring-boot-starter` 和 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
- AI Starter 不得直接依赖 `fw-plus-tiny-engine-core`；Core 只能通过 Boot Starter 传递提供。
- Boot Starter 不得依赖 AI 层。
- TinyEngine 业务继续面向 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 和中立门面；不得向公开接口泄漏 LangChain4j 原生类型。
- 保持 Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0、`${revision}` 和现有构建插件不变。
- 不处理 `upgrade-springboot3-dependencies-and-sync-branches` 或主工作区未提交改动。

---

### Task 1: 锁定 Starter 依赖边界与自动配置发现行为

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/starter/TinyEngineAiStarterDependencyTest.java`

**Interfaces:**
- Consumes: AI Starter 当前 `pom.xml`，以及依赖 classpath 中由 `META-INF/spring.factories` 注册的 `EnableAutoConfiguration` 实现。
- Produces: `shouldAggregateTinyEngineBootAndNeutralAiStarters()` 与 `shouldDiscoverBootAndAiAutoConfigurationsFromStarterClasspath()` 回归测试。

- [ ] **Step 1: 将结构测试改为目标依赖断言**

将现有测试方法替换为以下断言：

```java
@Test
public void shouldAggregateTinyEngineBootAndNeutralAiStarters() throws Exception {
	Set<String> dependencies = directDependencyCoordinates(new File("pom.xml"));

	assertTrue("TinyEngine AI starter must include AI auto-configuration",
			dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-boot-autoconfigure"));
	assertTrue("TinyEngine AI starter must include the existing TinyEngine Boot starter",
			dependencies.contains("com.xiongx9527.fw.plus.boot:fw-plus-tiny-engine-spring-boot-starter"));
	assertFalse("TinyEngine AI starter must obtain TinyEngine core transitively from the Boot starter",
			dependencies.contains("com.xiongx9527.fw.plus:fw-plus-tiny-engine-core"));
	assertTrue("TinyEngine AI starter must include the neutral AI model starter",
			dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter"));
	assertTrue("TinyEngine AI starter must include the neutral embedding store starter",
			dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-store-embedding-spring-boot-starter"));
	assertTrue("TinyEngine AI starter must use the unified LangChain4j adapter",
			dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-adapter-spring-boot-starter"));
	assertFalse("TinyEngine AI starter must not depend on the legacy LangChain4j model starter",
			dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter"));
	assertFalse("TinyEngine AI starter must not depend on the legacy LangChain4j embedding store starter",
			dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-store-embeding-spring-boot-starter"));
}
```

- [ ] **Step 2: 添加 Starter classpath 自动配置发现测试**

添加导入：

```java
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
```

添加测试：

```java
@Test
public void shouldDiscoverBootAndAiAutoConfigurationsFromStarterClasspath() {
	List<String> autoConfigurations = SpringFactoriesLoader.loadFactoryNames(
			EnableAutoConfiguration.class, getClass().getClassLoader());

	assertTrue("TinyEngine Boot auto-configuration must be discoverable",
			autoConfigurations.contains(
					"com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration"));
	assertTrue("TinyEngine AI auto-configuration must be discoverable",
			autoConfigurations.contains(
					"com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiAutoConfiguration"));
}
```

- [ ] **Step 3: 运行测试并确认 RED**

Run:

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter \
  -Dtest=TinyEngineAiStarterDependencyTest test
```

Expected: JDK 8 当前 reactor 基线为 `Tests run: 2, Failures: 1, Errors: 0`。唯一失败是 AI Starter 缺少 Boot Starter 直接依赖；自动配置发现测试因既有 `AI model starter -> AI boot starter -> Boot starter -> Boot autoconfigure` 路径在基线已经通过。

- [ ] **Step 4: 提交 RED 测试**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/starter/TinyEngineAiStarterDependencyTest.java
git commit -m "test(ai): 锁定 TinyEngine Starter 聚合边界"
```

### Task 2: 通过 Boot BOM 接入现有 TinyEngine Boot Starter

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml`

**Interfaces:**
- Consumes: `com.xiongx9527.fw.plus.boot:fw-plus-boot-dependencies:${project.version}` 中已有的 `fw-plus-tiny-engine-spring-boot-starter` dependencyManagement。
- Produces: 不写版本的 `com.xiongx9527.fw.plus.boot:fw-plus-tiny-engine-spring-boot-starter` 直接依赖，以及由它传递提供的 `fw-plus-tiny-engine-core`。

- [ ] **Step 1: 在 AI BOM 中导入现有 Boot BOM**

在 `fw-plus-dependencies` import 后添加：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-boot-dependencies</artifactId>
	<version>${project.version}</version>
	<type>pom</type>
	<scope>import</scope>
</dependency>
```

- [ ] **Step 2: 用 Boot Starter 替换 Core 直连依赖**

将 AI Starter 中：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-tiny-engine-core</artifactId>
</dependency>
```

替换为：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-tiny-engine-spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 3: 运行结构与自动配置发现测试并确认 GREEN**

Run:

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter \
  -Dtest=TinyEngineAiStarterDependencyTest test
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`，`BUILD SUCCESS`。

- [ ] **Step 4: 检查有效依赖树**

Run:

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml \
  dependency:tree \
  -Dincludes=com.xiongx9527.fw.plus.boot:fw-plus-tiny-engine-spring-boot-starter,com.xiongx9527.fw.plus:fw-plus-tiny-engine-core,com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter,com.xiongx9527.fw.plus.ai:fw-plus-ai-store-embedding-spring-boot-starter,com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-adapter-spring-boot-starter
```

Expected: Boot Starter 为 AI Starter 的直接 compile 依赖；TinyEngine Core 只位于 Boot Starter 的传递依赖路径；三个中立 AI Starter 均为直接 compile 依赖。

- [ ] **Step 5: 提交实现**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml
git commit -m "feat(ai): 聚合 TinyEngine Boot Starter"
```

### Task 3: 更新消费说明并完成 Java 8 回归验证

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/README.md`

**Interfaces:**
- Consumes: Task 2 的最终 Maven 依赖关系。
- Produces: 明确说明单一 AI Starter 同时提供 TinyEngine Boot 与中立 AI 能力的消费文档。

- [ ] **Step 1: 更新 Maven 依赖说明**

在 Maven 示例后将说明改为：

```markdown
业务只需引入本 starter，即可同时获得现有 `fw-plus-tiny-engine-spring-boot-starter` 提供的 TinyEngine Boot、Web 与 MyBatis-Plus 基础装配，以及中立 model/store starter 和默认 LangChain4j adapter。Boot Starter 仍保持 AI-free，本 starter 仅通过 Maven 依赖聚合它，不复制或重新实现 Boot 配置。

使用具体 provider 时仍需按 provider 文档显式加入对应 SDK；starter 不会把所有 provider SDK 打包进来。
```

- [ ] **Step 2: 运行 TinyEngine AI 自动配置定向测试**

Run:

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=TinyEngineAiAutoConfigurationTest test
```

Expected: `Tests run: 10, Failures: 0, Errors: 0`，`BUILD SUCCESS`。

- [ ] **Step 3: 运行 AI Starter 模块完整测试**

Run:

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter test
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`，并输出 `BUILD SUCCESS`。

- [ ] **Step 4: 验证 Boot Starter 没有 AI 反向依赖**

Run:

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml \
  dependency:tree -Dincludes=com.xiongx9527.fw.plus.ai
```

Expected: 不出现 `com.xiongx9527.fw.plus.ai` 依赖节点，命令成功结束。

- [ ] **Step 5: 检查变更范围与 Java 版本**

Run:

```bash
git diff --check
git diff --name-only origin/dev...HEAD
java -version
```

Expected: 无空白错误；仅包含设计/计划、AI BOM、AI Starter POM、测试和 README；Java 输出为 1.8。

- [ ] **Step 6: 提交文档**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/README.md docs/superpowers/plans/2026-07-31-align-tiny-engine-ai-starter-dependencies.md
git commit -m "docs(ai): 说明 TinyEngine Starter 聚合用法"
```
