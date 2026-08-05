# Tinyflow Starter 与 Magic-API 插件实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Java 8 / Spring Boot 2.7.18 的 `dev` 分支中交付 Tinyflow 2.0.7 Core、Starter、Magic-API `resource`/`toolbars` 插件和可真实测试、异步执行及恢复的工作流设计器。

**Architecture:** Core 层提供框架无关的执行、模型、知识库、节点和仓库契约，并以同名类适配 LangChain4j 0.35.0；AI Boot 自动配置组装 Tinyflow 运行时，Starter 物理打包 `@tinyflow-ai/ui:1.3.6`；Magic-API 插件单向依赖 Starter，用原生 Tinyflow JSON 作为资源正文，并把现有 Model/Vector 注册中心暴露给编辑器。默认定义和运行状态在内存中，Magic-API 启用时由资源仓库替换定义仓库。

**Tech Stack:** Java 8、Maven 3.6.3、Spring Boot 2.7.18、Magic-API 2.2.2、Tinyflow Core 2.0.7、LangChain4j 0.35.0、Vue 3、Vite、`@tinyflow-ai/ui:1.3.6`、Node Test Runner、Playwright。

## Global Constraints

- 只修改当前 `dev` 分支；不切换、不修改 `springboot2` 或 `springboot3`。
- `tinyflow-core` 固定为 `2.0.7`；不得依赖 `tinyflow-support-langchain4j`。
- LangChain4j 固定为 `0.35.0`；覆盖类完全限定名固定为 `dev.tinyflow.langchain4j.provider.Langchain4jLlm`。
- 所有 Java 源码兼容 Java 8，禁止 `List.of()`、`Map.of()`、`var`、record、文本块及其他 Java 9+ API/语法。
- Magic-API 固定为项目维护的 `2.2.2`，不得修改其仓库地址或版本。
- Starter 物理包含 UI；Magic-API 插件依赖 Starter，Starter 不依赖 Magic-API。
- npm 依赖使用精确版本 `@tinyflow-ai/ui:1.3.6`，使用 `npm ci`，不得使用 `^`、`latest` 或外部 CDN。
- 工作流资源正文是原生 Tinyflow JSON，至少包含 `nodes`、`edges`，不得增加项目私有外层对象。
- 模型、Embedding 和 Vector Store 只使用现有注册中心，不新增 Tinyflow 私有连接配置。
- 节点温度、Top-P、Top-K 等逐请求采样参数保留在 JSON，但 LangChain4j 0.35.0 执行时以所选模型 Bean 的配置为准。
- 默认状态仓库只承诺单进程生命周期内的异步执行与恢复，不宣称跨重启持久化。
- 仓库 Maven Wrapper 缺少 `.mvn/wrapper`；本计划全部使用系统 `mvn`。

---

### Task 1: 建立 Core Tinyflow 模块与依赖边界

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/TinyflowDependencyBoundaryTest.java`

**Interfaces:**
- Consumes: Core BOM 中的 `langchain4j.version=0.35.0`、现有 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` 和 `fw-plus-dify`。
- Produces: 三个可独立构建的制品 `fw-plus-ai-tinyflow-core`、`fw-plus-ai-tinyflow-local`、`fw-plus-ai-tinyflow-dify`，以及 Core BOM 中的 `tinyflow.version=2.0.7`。

- [ ] **Step 1: 写失败的依赖边界测试**

```java
public class TinyflowDependencyBoundaryTest {
	@Test
	public void shouldLoadJava8TinyflowCoreWithoutOfficialLangchain4jAdapter() {
		assertNotNull(dev.tinyflow.core.parser.ChainParser.class);
		assertNull(TinyflowDependencyBoundaryTest.class.getClassLoader()
				.getResource("META-INF/maven/dev.tinyflow/tinyflow-support-langchain4j/pom.properties"));
	}
}
```

- [ ] **Step 2: 运行测试确认模块尚不存在**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -am -Dtest=TinyflowDependencyBoundaryTest test`

Expected: FAIL，Maven 报告找不到 reactor module 或测试中的 Tinyflow/覆盖类。

- [ ] **Step 3: 增加聚合、BOM 和严格依赖方向**

在 Core BOM 增加：

```xml
<tinyflow.version>2.0.7</tinyflow.version>
<dependency>
	<groupId>dev.tinyflow</groupId>
	<artifactId>tinyflow-core</artifactId>
	<version>${tinyflow.version}</version>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-ai-tinyflow-core</artifactId>
	<version>${project.version}</version>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-ai-tinyflow-local</artifactId>
	<version>${project.version}</version>
</dependency>
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-ai-tinyflow-dify</artifactId>
	<version>${project.version}</version>
</dependency>
```

`core` 只依赖 `tinyflow-core`、`langchain4j-core` 和测试依赖；`local` 依赖 `core`、`fw-plus-ai-model`、`fw-plus-ai-store-embedding`；`dify` 依赖 `core`、`fw-plus-dify`。所有 POM 均不声明 Spring、Magic-API、Servlet 或 `tinyflow-support-langchain4j`。

- [ ] **Step 4: 运行依赖边界测试和依赖树检查**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -am -Dtest=TinyflowDependencyBoundaryTest test`

Expected: PASS。

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow -am dependency:tree -Dincludes=dev.tinyflow`

Expected: 只出现 `dev.tinyflow:tinyflow-core:2.0.7`，不出现 `tinyflow-support-langchain4j`。

- [ ] **Step 5: 提交模块骨架**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow
git commit -m "feat(tinyflow): 建立 Core 模块与依赖边界"
```

### Task 2: 定义工作流、节点、模型与仓库契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/TinyflowException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/workflow/TinyflowWorkflow.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/workflow/TinyflowExecutionResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/workflow/TinyflowWorkflowRepository.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/workflow/InMemoryTinyflowWorkflowRepository.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/model/TinyflowModelDescriptor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/model/TinyflowEmbeddingModel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/model/TinyflowModelProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/vector/TinyflowVectorStoreDescriptor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/vector/TinyflowVectorStore.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/vector/TinyflowVectorDocument.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/vector/TinyflowVectorMatch.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/vector/TinyflowVectorStoreProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/node/TinyflowNodeDescriptor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/node/TinyflowNodeProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/node/TinyflowNodeProviderRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/workflow/TinyflowWorkflowValidator.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/workflow/TinyflowWorkflowValidatorTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/node/TinyflowNodeProviderRegistryTest.java`

**Interfaces:**
- Consumes: Tinyflow `NodeParser<?>`、LangChain4j 0.35.0 `ChatLanguageModel` 和项目 JSON 能力。
- Produces: `TinyflowWorkflowRepository.findById/findByPath/save/delete`、`TinyflowNodeProvider.getType/getParser/getDescriptor/getOrder`、`TinyflowWorkflowValidator.validate(String resourceId, String json)`、中立的模型/向量运行接口。

- [ ] **Step 1: 写非法 JSON、未知节点和重复 Provider 的失败测试**

```java
@Test
public void shouldRejectUnknownNodeBeforeTinyflowParserSilentlyDropsIt() {
	String json = "{\"nodes\":[{\"id\":\"n1\",\"type\":\"missing-node\"}],\"edges\":[]}";
	TinyflowException error = assertThrows(TinyflowException.class,
			() -> validator.validate("customer/order", json));
	assertTrue(error.getMessage().contains("customer/order"));
	assertTrue(error.getMessage().contains("n1"));
	assertTrue(error.getMessage().contains("missing-node"));
}

@Test
public void shouldRejectSameTypeWithSamePriority() {
	assertThrows(TinyflowException.class,
			() -> new TinyflowNodeProviderRegistry(Arrays.asList(provider("custom", 0), provider("custom", 0))));
}
```

- [ ] **Step 2: 运行测试确认契约未实现**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=TinyflowWorkflowValidatorTest,TinyflowNodeProviderRegistryTest test`

Expected: FAIL，缺少上述类型。

- [ ] **Step 3: 实现最小稳定接口与校验器**

```java
public interface TinyflowNodeProvider {
	String getType();
	dev.tinyflow.core.parser.NodeParser<?> getParser();
	TinyflowNodeDescriptor getDescriptor();
	int getOrder();
}

public interface TinyflowModelProvider {
	List<TinyflowModelDescriptor> listChatModels();
	List<TinyflowModelDescriptor> listEmbeddingModels();
	ChatLanguageModel getChatModel(String key);
	TinyflowEmbeddingModel getEmbeddingModel(String key);
}

public interface TinyflowEmbeddingModel {
	float[] embed(String text);
}

public interface TinyflowVectorStore {
	List<String> addAll(List<TinyflowVectorDocument> documents);
	void removeAll(List<String> ids);
	List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore);
}

public interface TinyflowVectorStoreProvider {
	List<TinyflowVectorStoreDescriptor> list();
	TinyflowVectorStore get(String key);
}

public interface TinyflowWorkflowRepository {
	TinyflowWorkflow findById(String id);
	TinyflowWorkflow findByPath(String path);
	void save(TinyflowWorkflow workflow);
	void delete(String id);
}

public final class TinyflowWorkflowValidator {
	public dev.tinyflow.core.chain.ChainDefinition validate(String resourceId, String json) {
		JSONObject root = JSON.parseObject(json);
		JSONArray nodes = requireArray(root, "nodes", resourceId);
		requireArray(root, "edges", resourceId);
		validateNodeTypes(resourceId, nodes);
		ChainDefinition definition = chainParser.parse(json);
		definition.setId(resourceId);
		validateStartToEndPath(resourceId, definition);
		return definition;
	}
}
```

仓库的 `findByPath` 必须区分不存在和歧义；节点注册按 `getOrder()` 选择唯一最高优先级，相同 type 且最高优先级相同则启动失败；不得修改 Tinyflow 的全局静态解析器表。

- [ ] **Step 4: 运行 Core 契约测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=TinyflowWorkflowValidatorTest,TinyflowNodeProviderRegistryTest test`

Expected: PASS，非法 JSON、空节点、未知节点、无开始/结束路径、路径歧义和 Provider 冲突均有断言。

- [ ] **Step 5: 提交 Core 契约**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core
git commit -m "feat(tinyflow): 定义工作流与节点扩展契约"
```

### Task 3: 覆盖 LangChain4j 0.35.0 适配类

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/dev/tinyflow/langchain4j/provider/Langchain4jLlm.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/dev/tinyflow/langchain4j/provider/Langchain4jLlmTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/dev/tinyflow/langchain4j/provider/Langchain4jLlmClasspathTest.java`

**Interfaces:**
- Consumes: `ChatLanguageModel.generate(List<ChatMessage>) -> Response<AiMessage>` 与 Tinyflow `Llm.chat(MessageInfo, ChatOptions, LlmNode, Chain)`。
- Produces: Java 8 版本 `dev.tinyflow.langchain4j.provider.Langchain4jLlm(ChatLanguageModel)`。

- [ ] **Step 1: 写消息映射和失败转换测试**

```java
@Test
public void shouldGenerateWithSystemTextAndImageMessages() {
	CapturingChatLanguageModel model = new CapturingChatLanguageModel(Response.from(AiMessage.from("ok")));
	Langchain4jLlm llm = new Langchain4jLlm(model);
	String answer = llm.chat(messageInfo("system", "question", "https://example.test/a.png"),
			new Llm.ChatOptions(), null, null);
	assertEquals("ok", answer);
	assertTrue(model.messages.get(0) instanceof SystemMessage);
	assertTrue(((UserMessage) model.messages.get(1)).contents().get(0) instanceof TextContent);
	assertTrue(((UserMessage) model.messages.get(1)).contents().get(1) instanceof ImageContent);
}

@Test
public void shouldRejectNullAiMessage() {
	Langchain4jLlm llm = new Langchain4jLlm(messages -> null);
	assertThrows(TinyflowException.class,
			() -> llm.chat(messageInfo(null, "q", null), new Llm.ChatOptions(), null, null));
}
```

- [ ] **Step 2: 运行测试确认同名兼容类不存在**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=Langchain4jLlmTest,Langchain4jLlmClasspathTest test`

Expected: FAIL，缺少 `Langchain4jLlm` 或构造器签名不匹配。

- [ ] **Step 3: 用 0.35.0 API 实现同名类**

```java
public final class Langchain4jLlm implements Llm {
	private final ChatLanguageModel chatLanguageModel;

	public Langchain4jLlm(ChatLanguageModel chatLanguageModel) {
		this.chatLanguageModel = Objects.requireNonNull(chatLanguageModel, "chatLanguageModel");
	}

	@Override
	public String chat(MessageInfo message, ChatOptions options, LlmNode node, Chain chain) {
		Response<AiMessage> response = chatLanguageModel.generate(toMessages(message));
		if (response == null || response.content() == null || isBlank(response.content().text())) {
			throw new TinyflowException("LangChain4j model returned an empty response");
		}
		return response.content().text();
	}
}
```

`toMessages` 使用 0.35.0 的 `SystemMessage`、`UserMessage`、`TextContent`、`ImageContent`；不调用 1.x 的 `ChatModel.chat()`、`ChatResponse.aiMessage()`；不在此通用接口上伪造逐请求 sampling 覆盖。

- [ ] **Step 4: 验证适配测试、重复类和 Java 8 字节码**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=Langchain4jLlmTest,Langchain4jLlmClasspathTest test`

Expected: PASS，覆盖系统消息、文本、图片、模型异常、空 Response、空 AiMessage 和空文本。

Run: `javap -verbose fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/target/classes/dev/tinyflow/langchain4j/provider/Langchain4jLlm.class | rg "major version"`

Expected: `major version: 52`。

- [ ] **Step 5: 提交低版本适配**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core
git commit -m "feat(tinyflow): 适配 LangChain4j 0.35.0"
```

### Task 4: 实现 Tinyflow 执行服务与状态边界

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/runtime/TinyflowExecutionRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/runtime/TinyflowAsyncResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/runtime/TinyflowService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/runtime/DefaultTinyflowService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/runtime/InMemoryChainDefinitionRepository.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/runtime/DefaultTinyflowServiceTest.java`

**Interfaces:**
- Consumes: `ChainParser`、`ChainExecutor.execute/executeAsync/resume/resumeAsync`、三个 Tinyflow repository。
- Produces: `validate`、`register`、`remove`、`execute`、`executeAsync`、`resume`、`resumeAsync` 的稳定门面。

- [ ] **Step 1: 写同步、异步、恢复、超时与线程中断测试**

```java
@Test
public void shouldReturnInstanceIdAndResumeSuspendedChain() {
	TinyflowAsyncResult started = service.executeAsync("flow-1", singletonMap("approved", false));
	assertNotNull(started.getInstanceId());
	TinyflowExecutionResult resumed = service.resume(started.getInstanceId(), singletonMap("approved", true));
	assertEquals("COMPLETED", resumed.getStatus());
}

@Test
public void shouldRestoreInterruptFlagWhenWaitingIsInterrupted() {
	Thread.currentThread().interrupt();
	assertThrows(TinyflowException.class, () -> service.execute("slow-flow", emptyMap()));
	assertTrue(Thread.currentThread().isInterrupted());
}
```

- [ ] **Step 2: 运行测试确认执行门面不存在**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=DefaultTinyflowServiceTest test`

Expected: FAIL，缺少执行服务类型。

- [ ] **Step 3: 实现最小门面并清理返回状态**

```java
public interface TinyflowService {
	void register(TinyflowWorkflow workflow);
	void remove(String workflowId);
	TinyflowExecutionResult execute(String workflowId, Map<String, Object> variables);
	TinyflowAsyncResult executeAsync(String workflowId, Map<String, Object> variables);
	TinyflowExecutionResult resume(String instanceId, Map<String, Object> variables);
	TinyflowAsyncResult resumeAsync(String instanceId, Map<String, Object> variables);
}
```

同步等待使用构造器传入的有限 `Duration executionTimeout`；超时时取消 future 并抛出稳定错误；捕获 `InterruptedException` 后先 `Thread.currentThread().interrupt()` 再转换异常。结果对象不得暴露 Tinyflow 内部 repository/state 对象。

- [ ] **Step 4: 运行无外部依赖的完整链测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=DefaultTinyflowServiceTest test`

Expected: PASS，开始 -> 模板 -> 结束、异步 instance ID、挂起/恢复、超时和中断全部通过。

- [ ] **Step 5: 提交执行门面**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core
git commit -m "feat(tinyflow): 实现工作流执行与恢复服务"
```

### Task 5: 增加 AI Boot 自动配置和 Starter

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/TinyflowProperties.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/TinyflowAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/LangChain4jTinyflowLlmProvider.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/TinyflowLlmLifecycle.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/RegistryTinyflowModelProvider.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/RegistryTinyflowVectorStoreProvider.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/TinyflowAutoConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/LangChain4jTinyflowLlmProviderTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/starter/TinyflowStarterDependencyTest.java`

**Interfaces:**
- Consumes: `AiModelRegistry`、`AiEmbeddingStoreRegistry`、`LangChain4jModelRegistry`、Task 2-4 的 Core 契约。
- Produces: `fw.plus.ai.tinyflow.enabled=true`、`execution-timeout=5m` 和可覆盖的运行时 Bean。

- [ ] **Step 1: 写 ApplicationContextRunner 和模型解析失败测试**

```java
private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TinyflowAutoConfiguration.class));

@Test
public void shouldBackOffEveryReplaceableRepository() {
	contextRunner.withBean(ChainStateRepository.class, StubChainStateRepository::new)
			.run(context -> assertSame(context.getBean(StubChainStateRepository.class),
					context.getBean(ChainStateRepository.class)));
}

@Test
public void shouldRejectNonChatModelAndIncludeLlmId() {
	TinyflowException error = assertThrows(TinyflowException.class, () -> provider.get("embedding-only"));
	assertTrue(error.getMessage().contains("embedding-only"));
}
```

- [ ] **Step 2: 运行测试确认自动配置不存在**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=TinyflowAutoConfigurationTest,LangChain4jTinyflowLlmProviderTest test`

Expected: FAIL，缺少自动配置和 Bean。

- [ ] **Step 3: 实现 Boot 2.7 自动配置与生命周期**

```java
@Configuration
@ConditionalOnClass(ChainExecutor.class)
@ConditionalOnProperty(prefix = "fw.plus.ai.tinyflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TinyflowProperties.class)
public class TinyflowAutoConfiguration {
	@Bean @ConditionalOnMissingBean
	public TinyflowWorkflowRepository tinyflowWorkflowRepository() {
		return new InMemoryTinyflowWorkflowRepository();
	}

	@Bean @ConditionalOnMissingBean
	public TinyflowService tinyflowService(ChainParser chainParser,
			ChainExecutor chainExecutor,
			TinyflowWorkflowRepository workflowRepository,
			TinyflowProperties properties) {
		return new DefaultTinyflowService(chainParser, chainExecutor,
				workflowRepository, properties.getExecutionTimeout());
	}
}
```

`TinyflowLlmLifecycle` 在 `afterPropertiesSet()` 注册唯一 Provider，在 `destroy()` 删除同一个 Provider；两个连续关闭的 ApplicationContext 不得留下静态注册。默认模型/向量 Provider 只返回真实存在且能力匹配的 key、名称、类型和 provider，不返回配置详情。

`RegistryTinyflowModelProvider` 遇到 `LangChain4jAiModel` 时从 `getNativeModel().getModel()` 取得 `ChatLanguageModel` 或 `EmbeddingModel`，把后者的 `embed(text).content().vector()` 转成 `float[]`；能力或类型不匹配时抛出含模型 key 的脱敏错误。`RegistryTinyflowVectorStoreProvider` 把现有 `AiEmbeddingStore` 的 add/remove/search 和 `AiEmbeddingVector` 显式桥接为 Task 2 的中立接口，禁止把底层连接配置返回 UI。

- [ ] **Step 4: 运行自动配置和 Starter 依赖测试**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter -am -Dtest=TinyflowAutoConfigurationTest,LangChain4jTinyflowLlmProviderTest,TinyflowStarterDependencyTest test`

Expected: PASS，覆盖默认启用、显式关闭、自定义 Bean 覆盖、缺失 class、按 key/default 模型解析、错误类型及关闭注销。

- [ ] **Step 5: 提交 Starter 后端**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters
git commit -m "feat(tinyflow): 增加 Starter 自动配置"
```

### Task 6: 在 Starter 中构建并物理打包 Tinyflow UI

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/package.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/package-lock.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/vite.config.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/index.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/tinyflow-host.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/test/tinyflow-host.test.cjs`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/resources/META-INF/resources/fw-plus/tinyflow-ui/NOTICE`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/starter/TinyflowUiClasspathTest.java`

**Interfaces:**
- Consumes: npm `@tinyflow-ai/ui:1.3.6` 的公开 `Tinyflow` 和 `customNodes` 扩展点。
- Produces: `window.FwPlusTinyflowUi.mount(options)`，以及 classpath 下版本化 `tinyflow-ui.js`、`tinyflow-ui.css`、`NOTICE`。

- [ ] **Step 1: 写 Node 生命周期测试和 JAR 资源测试**

```javascript
test('mount forwards data and destroy releases instance', () => {
  const fake = fakeTinyflow()
  const host = createTinyflowHost({ Tinyflow: fake.ctor, element, data, customNodes: [] })
  host.setData(nextData)
  assert.deepEqual(host.getData(), nextData)
  host.destroy()
  assert.equal(fake.destroyCalls, 1)
})
```

```java
@Test
public void shouldPackagePinnedUiAssetsAndLicense() {
	assertNotNull(resource("/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.js"));
	assertNotNull(resource("/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.css"));
	assertNotNull(resource("/META-INF/resources/fw-plus/tinyflow-ui/NOTICE"));
}
```

- [ ] **Step 2: 运行前端与资源测试确认失败**

Run: `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui && npm test`

Expected: FAIL，前端工程或 host 不存在。

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter -Dtest=TinyflowUiClasspathTest test`

Expected: FAIL，classpath 资源不存在。

- [ ] **Step 3: 实现固定版本前端和 Maven 打包**

```json
{
  "scripts": {"test": "node --test test/*.test.cjs", "build": "vite build"},
  "dependencies": {"@tinyflow-ai/ui": "1.3.6"},
  "devDependencies": {"vite": "5.4.14"}
}
```

POM 在 `generate-resources` 依次执行 `npm ci`、`npm test`、`npm run build`，再复制到 `target/classes/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/`。输出文件名由 Vite 固定为 `tinyflow-ui.js` 和 `tinyflow-ui.css`；NOTICE 写明包名、版本、上游地址和 LGPL-3.0-or-later。

- [ ] **Step 4: 运行 npm、Maven 与 JAR 内容验证**

Run: `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui && npm ci && npm test && npm run build`

Expected: 全部退出码 0，lock 文件解析版本为 `1.3.6`。

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter -am package -DskipTests=false`

Expected: BUILD SUCCESS。

Run: `jar tf fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/target/fw-plus-ai-tinyflow-spring-boot-starter-0.1.1-SNAPSHOT.jar | rg "fw-plus/tinyflow-ui/(1.3.6/(tinyflow-ui.js|tinyflow-ui.css)|NOTICE)"`

Expected: 三类资源全部存在。

- [ ] **Step 5: 提交 UI 源码与打包配置**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter
git commit -m "feat(tinyflow): 在 Starter 打包官方 UI"
```

### Task 7: 注册 Tinyflow Magic Resource 与运行时仓库

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/model/TinyflowResource.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/service/TinyflowMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/service/MagicApiTinyflowWorkflowRepository.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/service/TinyflowResourceDynamicRegistry.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/starter/MagicAPITinyflowConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/TinyflowMagicResourceStorageTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/TinyflowResourceDynamicRegistryTest.java`

**Interfaces:**
- Consumes: `MagicResourceStorage<TinyflowResource>`、`MagicResourceService`、`TinyflowWorkflowRepository`、`TinyflowService.register/remove`。
- Produces: Magic 资源类型/folder `tinyflow`，`.json` 正文读写，以及保存/删除后立即刷新运行时定义。

- [ ] **Step 1: 写原生 JSON、失败不覆盖和动态刷新测试**

```java
@Test
public void shouldWriteNativeTinyflowJsonWithoutWrapper() {
	byte[] bytes = storage.write(resourceWith("{\"nodes\":[],\"edges\":[]}"));
	String json = new String(bytes, StandardCharsets.UTF_8);
	assertTrue(json.startsWith("{\"nodes\""));
	assertFalse(json.contains("\"content\""));
}

@Test
public void shouldKeepPreviousDefinitionWhenUpdatedJsonIsInvalid() {
	registry.onSaved(validResource);
	assertThrows(TinyflowException.class, () -> registry.onSaved(invalidResource));
	assertSame(previousDefinition, definitions.get("flow-id"));
}
```

- [ ] **Step 2: 运行测试确认资源实现不存在**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=TinyflowMagicResourceStorageTest,TinyflowResourceDynamicRegistryTest test`

Expected: FAIL，缺少 storage/registry。

- [ ] **Step 3: 实现资源存储和原子刷新**

```java
public final class TinyflowMagicResourceStorage implements MagicResourceStorage<TinyflowResource> {
	public String folder() { return "tinyflow"; }
	public String suffix() { return ".json"; }
	public boolean requirePath() { return true; }
	public boolean requiredScript() { return false; }
	public void validate(TinyflowResource entity) { validator.validate(entity.getId(), entity.getContent()); }
}
```

读取、验证、解析成功后才原子替换定义；删除事件按资源 ID 移除定义；路径索引在重命名时同步更新并拒绝歧义。`MagicAPITinyflowConfiguration` 实现 `MagicPluginConfiguration`，插件 metadata 使用 `new Plugin("tinyflow", "Tinyflow", "magic-tinyflow.1.0.0.iife.js")`。

- [ ] **Step 4: 运行资源与自动配置测试**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am -Dtest=TinyflowMagicResourceStorageTest,TinyflowResourceDynamicRegistryTest test`

Expected: PASS，覆盖创建、读取、重命名、保存、删除、路径歧义、非法 JSON 和旧版本保留。

- [ ] **Step 5: 提交 Magic Resource**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin
git commit -m "feat(tinyflow): 注册 Magic 工作流资源"
```

### Task 8: 提供 MagicScript 执行模块和编辑器后端接口

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/module/TinyflowModule.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/module/TinyflowResourceModule.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/web/MagicTinyflowController.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/provider/MagicApiTinyflowModelProvider.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/provider/MagicApiTinyflowVectorStoreProvider.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/starter/MagicAPITinyflowConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/TinyflowModuleTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/web/MagicTinyflowControllerTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/provider/MagicApiTinyflowProviderTest.java`

**Interfaces:**
- Consumes: `TinyflowService`、`MagicDynamicModelDataSource`、`MagicDynamicVectorDataSource`、Magic `DynamicModule` 和 `MagicControllerRegister`。
- Produces: `tinyflow[path].execute(Map)`、`executeAsync(Map)`、`tinyflow.resume(instanceId, Map)`；编辑器读取/保存/校验/测试运行/模型/向量/节点描述接口。

- [ ] **Step 1: 写脚本调用和敏感字段过滤测试**

```java
@Test
public void shouldResolvePathAndExecuteSavedWorkflow() {
	Object result = module.getDynamicModule("customer-service/order-agent")
			.execute(singletonMap("orderId", "A-1"));
	assertEquals("done", result);
}

@Test
public void shouldOnlyExposeChatModelDescriptor() {
	List<TinyflowModelDescriptor> models = controller.models().getData();
	assertEquals(singletonList("chat-main"), keys(models));
	assertFalse(asJson(models).contains("apiKey"));
}
```

- [ ] **Step 2: 运行测试确认接口未注册**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyflowModuleTest,MagicTinyflowControllerTest,MagicApiTinyflowProviderTest test`

Expected: FAIL，缺少 module/controller/provider。

- [ ] **Step 3: 实现明确的后端 API 和 MagicScript 门面**

```java
@RequestMapping("/tinyflow/run")
@ResponseBody
public JsonBean<TinyflowExecutionResult> run(@RequestBody TinyflowRunRequest request) {
	return new JsonBean<TinyflowExecutionResult>(service.execute(request.getResourceId(), request.getVariables()));
}

@RequestMapping("/tinyflow/provider/llm")
@ResponseBody
public JsonBean<List<TinyflowModelDescriptor>> models() {
	return new JsonBean<List<TinyflowModelDescriptor>>(modelProvider.listChatModels());
}
```

同一 Controller 还提供 `/tinyflow/resource/get`、`/save`、`/validate`、`/run-async`、`/resume`、`/provider/embedding`、`/provider/vector`、`/nodes`。保存接口先校验再委托 Magic 资源服务；测试运行只接受已保存资源 ID，不执行浏览器未保存 JSON。

- [ ] **Step 4: 运行脚本与 Controller 测试**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyflowModuleTest,MagicTinyflowControllerTest,MagicApiTinyflowProviderTest test`

Expected: PASS，覆盖资源不存在、路径歧义、同步/异步/resume、模型能力过滤和错误脱敏。

- [ ] **Step 5: 提交后端入口**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "feat(tinyflow): 提供 MagicScript 与编辑器接口"
```

### Task 9: 实现工作流 Resource 和 Toolbars 前端宿主

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/package.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/package-lock.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/vite.config.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/index.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/components/tinyflow-workbench.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/components/tinyflow-toolbar.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/components/tinyflow-knowledge-workbench.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/service/magic-tinyflow-resource.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/service/tinyflow-service.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/service/tinyflow-ui-loader.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/i18n/zh-cn.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/i18n/en.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/test/tinyflow-ui-loader.test.cjs`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/test/tinyflow-workbench.test.cjs`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/pom.xml`

**Interfaces:**
- Consumes: Starter 资源 `/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.js|css` 和 Task 8 API。
- Produces: Magic 插件 metadata 中的 `resource` 类型 `tinyflow`、工作流和知识库 `toolbars` 全屏入口、保存/测试/关闭交互。

- [ ] **Step 1: 写加载失败、脏状态和销毁测试**

```javascript
test('keeps dirty state when save fails', async () => {
  const workbench = createWorkbench({ save: async () => { throw new Error('500') } })
  workbench.onDataChange(nextData)
  await assert.rejects(workbench.save())
  assert.equal(workbench.isDirty(), true)
})

test('reports missing global constructor instead of blank canvas', async () => {
  await assert.rejects(loadTinyflowUi({ window: {}, baseUrl: '/ctx' }),
    /FwPlusTinyflowUi/)
})
```

- [ ] **Step 2: 运行 Node 测试确认前端宿主不存在**

Run: `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console && npm test`

Expected: FAIL，前端工程或被测模块不存在。

- [ ] **Step 3: 实现 Resource/Toolbars 插件定义与全屏工作台**

```javascript
export default (opt) => ({
  name: 'tinyflow',
  resource: [{
    type: 'tinyflow',
    title: 'tinyflow.resource.name',
    icon: '#magic-tinyflow-flow',
    service: MagicTinyflowResource(opt.bus, opt.constants, opt.i18n.format, opt.Message, opt.request)
  }],
  toolbars: [
    { type: 'tinyflow', title: 'tinyflow.workflow.title', icon: '#magic-tinyflow-flow',
      order: 600, component: TinyflowWorkbench },
    { id: 'tinyflow-knowledge', title: 'tinyflow.knowledge.title', icon: '#magic-tinyflow-knowledge',
      order: 610, component: TinyflowKnowledgeWorkbench, adminOnly: true, allowClose: true, fullscreen: true }
  ]
})
```

本任务先为知识库 Toolbar 注册可路由的空状态页面，完整功能由知识库计划实现。工作流宿主根据 `opt.request` 的 context path 生成静态资源 URL，执行 `setData/getData/onDataChange/destroy`；资源切换和关闭时若 dirty 则确认；保存成功才清 dirty；运行前 dirty 则要求保存；404、版本不匹配和构造器缺失显示错误区。

- [ ] **Step 4: 运行 Node 构建和插件 JAR 测试**

Run: `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console && npm ci && npm test && npm run build`

Expected: PASS，生成 `magic-tinyflow.1.0.0.iife.js`。

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am package -DskipTests=false`

Expected: BUILD SUCCESS，插件 JAR 同时依赖 Starter 且包含 Magic 插件 IIFE，不复制 Starter UI JS/CSS。

- [ ] **Step 5: 提交 Toolbars 前端**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow
git commit -m "feat(tinyflow): 增加工作流 Resource 与 Toolbar"
```

### Task 10: 接入样例并完成真实工作流验收

**Files:**
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yml`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/test/java/com/xiongx9527/fw/plus/sample/ai/tinyflow/TinyflowSampleIT.java`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/test/resources/tinyflow/simple-flow.json`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/test/resources/tinyflow/suspend-flow.json`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/test/resources/tinyflow/llm-flow.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/e2e/tinyflow-workbench.spec.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/playwright.config.js`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/package.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/README.md`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/README.md`

**Interfaces:**
- Consumes: 完整 Starter、Magic 插件、假的 `ChatLanguageModel` 和浏览器可访问 sample。
- Produces: 可复制的依赖/配置/脚本示例，以及工作流创建、编辑、保存、重新打开、同步/异步/恢复测试证据。

- [ ] **Step 1: 写失败的 sample 集成与浏览器场景**

```java
@Test
public void shouldSaveReloadAndExecuteNativeWorkflowJson() {
	String id = resourceClient.create("tinyflow", "sample/simple", fixture("simple-flow.json"));
	assertEquals("hello Codex", tinyflowService.execute(id, singletonMap("name", "Codex")).getOutput());
	assertEquals(fixtureTree("simple-flow.json"), resourceClient.readJson(id));
}
```

```javascript
test('create edit save reload and run workflow', async ({ page }) => {
  await page.goto('/magic/web/index.html')
  await openTinyflowToolbar(page)
  await addTemplateNode(page)
  await saveAndReload(page)
  await expect(page.getByText('运行成功')).toBeVisible()
})
```

- [ ] **Step 2: 运行集成测试确认 sample 尚未接入**

Run: `mvn -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -Dtest=TinyflowSampleIT test`

Expected: FAIL，sample 缺少插件依赖或 Tinyflow Bean/资源类型。

- [ ] **Step 3: 接入 sample 与文档**

sample 依赖 `fw-plus-ai-magicapi-plugin-tinyflow`，配置 `fw.plus.ai.tinyflow.enabled=true` 和 `execution-timeout=5m`。测试用 `@TestConfiguration` 注册假的 CHAT 模型，禁止外部模型调用；README 写明 MagicScript：

```javascript
tinyflow['sample/simple'].execute({name: 'Codex'})
tinyflow['sample/suspend'].executeAsync({approved: false})
tinyflow.resume(instanceId, {approved: true})
```

并明确采样参数和内存恢复边界。

- [ ] **Step 4: 执行完整验证矩阵**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow -am clean install -DskipTests=false`

Expected: BUILD SUCCESS。

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am clean install -DskipTests=false`

Expected: BUILD SUCCESS。

Run: `mvn -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -Dtest=TinyflowSampleIT test`

Expected: PASS，原生 JSON、同步、异步、挂起/恢复和假 LLM 节点通过。

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow dependency:tree | rg "tinyflow-support-langchain4j|tinyflow-core"`

Expected: 只出现 `tinyflow-core:2.0.7`。

启动 sample 后运行：`cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console && npm run test:e2e`

Expected: 桌面 `1440x900` 与移动 `390x844` 均通过；画布非空，Toolbar、按钮、长错误文本不重叠；新增/删除节点回归覆盖 Tinyflow UI Issue #67。

- [ ] **Step 5: 检查最终差异并提交验收**

Run: `git diff --check && git status --short`

Expected: 无 whitespace error，只出现本计划列出的 Tinyflow、sample 和文档文件。

```bash
git add fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/README.md \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow
git commit -m "test(tinyflow): 增加样例与端到端验收"
```

## 最终验收清单

- [ ] Core、Starter、插件和 sample 的 Maven 测试全部通过，新增 class 的 major version 为 52。
- [ ] 依赖树只包含 `tinyflow-core:2.0.7`，不存在官方 `tinyflow-support-langchain4j` 或重复 `Langchain4jLlm`。
- [ ] Starter JAR 物理包含 `@tinyflow-ai/ui:1.3.6` 构建产物与许可证；插件 JAR 不重复复制 UI。
- [ ] Magic 插件 metadata 同时注册 `tinyflow` resource、工作流 Toolbar 和知识库 Toolbar。
- [ ] 原生工作流 JSON 可创建、编辑、保存、重载、删除并立即刷新运行时。
- [ ] 同步、异步、挂起、恢复、超时、线程中断和假 LLM 节点均通过测试。
- [ ] UI 的模型、Embedding、Vector Store 选项只来自现有注册中心且无敏感字段。
- [ ] 自定义节点前后端 type 一致；未知节点在执行前明确失败。
- [ ] 浏览器桌面/移动验收无空白画布、资源 404 静默失败或 UI 重叠。
- [ ] `git branch --show-current` 输出 `dev`，`springboot2` 与 `springboot3` 未被修改。
