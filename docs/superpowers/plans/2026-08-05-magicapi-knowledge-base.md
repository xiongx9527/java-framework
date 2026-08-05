# Tinyflow 本地知识库与 Dify 接口实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Tinyflow 统一知识库 SPI 上交付 `fw-plus-local` 和 `dify` 两个 Provider、Dify External Knowledge `/retrieval`、六类 Magic Resource、完整本地知识库管理 UI，以及带 `sourceId`/`sourceType` 的导入、索引、检索、补偿和引用闭环。

**Architecture:** `fw-plus-ai-tinyflow-core` 保存框架无关契约与领域对象，`fw-plus-ai-tinyflow-local` 复用现有模型、Embedding 和 Vector Store 完成可管理本地知识库，`fw-plus-ai-tinyflow-dify` 只组合现有 `fw-plus-dify` 客户端；AI Boot 发布受保护的 Dify External Knowledge Web 入口，Magic-API 插件用六类资源持久化定义、文档、chunk、任务、密钥和 Dify 连接，并通过 Starter 内的同一前端工程提供全屏管理 UI。

**Tech Stack:** Java 8、Maven 3.6.3、Spring Boot 2.7.18、Magic-API 2.2.2、Tinyflow 2.0.7、LangChain4j 0.35.0、现有 `AiModelRegistry`/`AiEmbeddingStoreRegistry`、现有 S3、现有 `fw-plus-dify`、Vue 3、Vite、Node Test Runner、Playwright。

## Global Constraints

- 只修改当前 `dev` 分支，不修改 `springboot2` 或 `springboot3`。
- 本计划在 `2026-08-05-tinyflow-magicapi.md` 的 Task 1-9 完成后执行；不得改变其依赖方向。
- Provider key 固定为 `fw-plus-local` 和 `dify`；外部 `/retrieval` 只允许 `fw-plus-local`，禁止转发到 `dify`。
- `fw-plus-ai-tinyflow-core` 不依赖 Spring、Magic-API、Servlet、S3 实现、Dify 或具体向量库。
- `fw-plus-ai-tinyflow-dify` 必须依赖现有 `fw-plus-dify`；`fw-plus-dify` 不得反向依赖 Tinyflow。
- 本地知识库只引用现有 Embedding 模型 key、Vector Store key 和 S3 key，不保存或创建私有连接。
- 每个 chunk 强制保存 `sourceId`、`sourceType`、`documentId`、`chunkId`；Dify 响应 metadata 强制返回对应 snake_case 字段。
- `sourceId + checksum` 内容未变时幂等跳过；内容变化时创建新 `documentId`，新索引成功后才切换并清理旧版本。
- 不新增关系数据库表；结构化状态使用 Magic Resource，原始文件使用现有 S3。
- 默认一个本地知识库独占一个 Vector Store；只有具体实现声明支持知识库过滤时才允许共享。
- Neo4j 非必需；只提供 `TinyflowKnowledgeGraphProvider` SPI，不提供默认 GraphRAG 实现。
- API Key 只保存强哈希、前缀、租户和授权范围；明文仅创建时返回一次。
- 所有 Java 代码兼容 Java 8；构建命令使用系统 `mvn`。

---

### Task 1: 对齐现有 fw-plus-dify External Knowledge DTO

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/dataset/types/external/KnowledgeArgs.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/dataset/types/external/KnowledgeResult.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/dataset/types/external/KnowledgeException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/main/java/com/xiongx9527/fw/plus/dify/dataset/types/external/KnowledgeMetadataCondition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-dify/src/test/java/com/xiongx9527/fw/plus/dify/dataset/types/external/KnowledgeExternalContractTest.java`

**Interfaces:**
- Consumes: 现有 `IKnowledgeService.retrieval(String apiKey, KnowledgeArgs args)`。
- Produces: `top_k: Integer`、`metadata_condition`、对象类型 metadata、错误字段 `error_code`/`error_msg` 的 Dify JSON 契约。

- [ ] **Step 1: 写官方 JSON 反序列化和错误字段测试**

```java
@Test
public void shouldReadIntegerTopKAndMetadataCondition() {
	KnowledgeArgs args = JSON.parseObject(fixture("dify-external-request.json"), KnowledgeArgs.class);
	assertEquals(Integer.valueOf(5), args.getRetrieval_setting().getTop_k());
	assertEquals("and", args.getMetadata_condition().getLogical_operator());
	assertEquals("source_type", args.getMetadata_condition().getConditions().get(0).getName());
}

@Test
public void shouldSerializeOfficialErrorFieldNames() {
	String json = JSON.toJSONString(new KnowledgeException(1001, "invalid authorization"));
	assertTrue(json.contains("\"error_code\":1001"));
	assertTrue(json.contains("\"error_msg\":\"invalid authorization\""));
	assertFalse(json.contains("error_message"));
}
```

- [ ] **Step 2: 运行测试确认当前 DTO 不兼容**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dify -Dtest=KnowledgeExternalContractTest test`

Expected: FAIL，当前 `top_k` 为 `String`、无 `metadata_condition`、错误字段为 `error_message`。

- [ ] **Step 3: 实现兼容 DTO**

```java
public static class Setting {
	private Integer top_k;
	private Double score_threshold;
}

public final class KnowledgeMetadataCondition {
	private String logical_operator;
	private List<Condition> conditions;
	public static final class Condition {
		private String name;
		private String comparison_operator;
		private Object value;
	}
}
```

`KnowledgeResult.Record.metadata` 改为 `Map<String,Object>` 并在构造/序列化前归一为空 map；`KnowledgeException` 保留 `RuntimeException` 语义，字段改为 `error_code`、`error_msg`，`toObject()` 输出相同名称。

- [ ] **Step 4: 运行 fw-plus-dify 全部回归测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dify test`

Expected: PASS，旧客户端测试与新 External 契约测试全部通过。

- [ ] **Step 5: 提交 DTO 契约修正**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dify
git commit -m "fix(dify): 对齐 External Knowledge 契约"
```

### Task 2: 定义统一知识库 SPI、领域对象与来源标识

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeManagementProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeProviderRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeReference.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeManagerAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeCapabilities.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeMatch.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowMetadataCondition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeDefinition.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowDocument.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowChunk.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowIngestionTask.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowDocumentImport.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeProviderRegistryTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeManagerAdapterTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowSourceIdentityTest.java`

**Interfaces:**
- Consumes: Task 1 的中立 DTO 只在 Dify 模块使用；Core 本身不依赖 Dify。
- Produces: `TinyflowKnowledgeProvider.retrieve`、可选管理接口、Provider Registry、Tinyflow `KnowledgeProvider` 适配器，以及四级来源 ID 的不可变领域模型。

- [ ] **Step 1: 写 Provider 冲突和四级 ID 强制测试**

```java
@Test
public void shouldRejectDuplicateProviderKey() {
	assertThrows(TinyflowException.class, () ->
			new TinyflowKnowledgeProviderRegistry(Arrays.asList(provider("dify"), provider("dify"))));
}

@Test
public void shouldRequireAllChunkSourceIdentifiers() {
	assertThrows(IllegalArgumentException.class, () -> TinyflowChunk.builder()
			.sourceId("source-1").sourceType("file").documentId("doc-1").content("text").build());
}
```

- [ ] **Step 2: 运行测试确认 SPI 未定义**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=TinyflowKnowledgeProviderRegistryTest,TinyflowKnowledgeManagerAdapterTest,TinyflowSourceIdentityTest test`

Expected: FAIL，缺少知识库类型。

- [ ] **Step 3: 实现稳定接口和不可变请求/结果**

```java
public interface TinyflowKnowledgeProvider {
	String getKey();
	TinyflowKnowledgeCapabilities capabilities();
	TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request);
}

public interface TinyflowKnowledgeManagementProvider extends TinyflowKnowledgeProvider {
	TinyflowKnowledgeDefinition create(TinyflowKnowledgeDefinition definition);
	TinyflowKnowledgeDefinition update(TinyflowKnowledgeDefinition definition);
	void delete(String tenantId, String knowledgeId);
	TinyflowIngestionTask importDocument(TinyflowDocumentImport request);
}

public final class TinyflowKnowledgeManagerAdapter implements dev.tinyflow.core.knowledge.KnowledgeProvider {
	public Knowledge getKnowledge(Object id) {
		final TinyflowKnowledgeReference ref = TinyflowKnowledgeReference.from(id);
		return (keyword, limit, node, chain) -> toMaps(registry.require(ref.getProvider())
				.retrieve(TinyflowKnowledgeRequest.from(ref, keyword, limit, chain)));
	}
}
```

`TinyflowKnowledgeRequest` 必填 `providerKey/tenantId/knowledgeId/query`，校验 `topK > 0`、阈值 0..1；`TinyflowChunk` 必填 `sourceId/sourceType/documentId/chunkId/content`，并保存 `sourceUri/sourceVersion/location/position/parentChunkId/tokenCount/metadata/enabled/vectorIds`。`sourceType` 内置 `file/text/url/database/api/connector`，同时允许非空稳定扩展 key。

- [ ] **Step 4: 运行 SPI 与来源测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=TinyflowKnowledgeProviderRegistryTest,TinyflowKnowledgeManagerAdapterTest,TinyflowSourceIdentityTest test`

Expected: PASS，覆盖重复/缺失 Provider、`provider + knowledgeId` 节点路由、能力判断、必填 ID、sourceType 和 metadata 防御性复制；缺失知识库必须抛错而不是返回空结果。

- [ ] **Step 5: 提交知识库 Core 契约**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core
git commit -m "feat(tinyflow): 定义知识库 SPI 与来源模型"
```

### Task 3: 定义仓库、解析、切片、Rerank 与可选 GraphRAG 契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeRepository.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeAccessKeyRepository.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowDifyConnectionRepository.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowDocumentParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowParsedDocument.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowChunkStrategy.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowReranker.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeGraphProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowObjectStorage.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowCredentialResolver.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeAuditSink.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/knowledge/TinyflowKnowledgeRepositoryContractTest.java`

**Interfaces:**
- Consumes: Task 2 领域对象。
- Produces: 可由 Magic Resource/S3 实现的持久化与对象存储边界，不带 Spring/Magic 类型。

- [ ] **Step 1: 写版本切换、向量映射和租户隔离契约测试**

```java
@Test
public void shouldOnlyExposeActiveDocumentVersionAfterAtomicSwitch() {
	repository.saveDocument(version1);
	repository.saveDocument(version2);
	repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-v2");
	assertEquals("doc-v2", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
	assertNull(repository.findActiveDocument("tenant-b", "kb-1", "source-1"));
}

@Test
public void shouldPersistVectorIdsForCompensation() {
	repository.saveChunk(chunkWithVectorIds("vec-1", "vec-2"));
	assertEquals(Arrays.asList("vec-1", "vec-2"), repository.findChunk("tenant-a", "chunk-1").getVectorIds());
}
```

- [ ] **Step 2: 运行测试确认仓库契约未实现**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=TinyflowKnowledgeRepositoryContractTest test`

Expected: FAIL，缺少仓库接口和测试内存实现。

- [ ] **Step 3: 实现精确仓库与扩展接口**

```java
public interface TinyflowKnowledgeRepository {
	TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId);
	void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion);
	TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId, String sourceId, String checksum);
	TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId);
	void saveDocument(TinyflowDocument document);
	void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId);
	void saveChunks(List<TinyflowChunk> chunks);
	List<TinyflowChunk> findChunksByDocument(String tenantId, String documentId);
	List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds);
	void saveTask(TinyflowIngestionTask task);
}
```

补充按 `sourceId` 列出所有版本、按 chunk ID 更新启停/正文/metadata、列出任务、保存待补偿 vector ID 和乐观版本冲突方法。`TinyflowDocumentParser.supports(mediaType,fileName)`、`parse(InputStream,context)`；`TinyflowChunkStrategy.chunk(parsed,options)`；`TinyflowCredentialResolver.resolve(tenantId, credentialRef)` 返回短生命周期凭证值；`TinyflowKnowledgeAuditSink.record(event)` 不接收明文正文或凭证；GraphRAG 只定义 `retrieve(request)`，不注册默认实现。

- [ ] **Step 4: 运行仓库契约测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=TinyflowKnowledgeRepositoryContractTest test`

Expected: PASS，覆盖租户、乐观锁、版本切换、sourceId 删除范围和 vector ID 补偿记录。

- [ ] **Step 5: 提交仓库与扩展契约**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core
git commit -m "feat(tinyflow): 定义知识库仓库与扩展契约"
```

### Task 4: 实现 fw-plus-local 导入、解析与切片

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/FwPlusLocalKnowledgeProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/DefaultTinyflowIngestionService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/parser/CompositeTinyflowDocumentParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/parser/TextMarkdownHtmlParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/parser/PdfTinyflowDocumentParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/parser/DocxTinyflowDocumentParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/parser/TabularTinyflowDocumentParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/chunk/NormalChunkStrategy.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/chunk/QaChunkStrategy.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/chunk/ParentChildChunkStrategy.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/TinyflowIngestionServiceTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/TinyflowDocumentParserTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/TinyflowChunkStrategyTest.java`

**Interfaces:**
- Consumes: `TinyflowObjectStorage`、`TinyflowKnowledgeRepository`、Task 3 parser/chunk 契约。
- Produces: Provider key `fw-plus-local` 的 CRUD/import 前半段，以及 TXT/Markdown/PDF/DOCX/HTML/CSV/XLSX 和三种切片策略。

- [ ] **Step 1: 写幂等导入、版本更新和解析/切片测试**

```java
@Test
public void shouldSkipSameSourceAndChecksum() {
	TinyflowIngestionTask first = service.importDocument(fileImport("source-1", "sha256-a"));
	TinyflowIngestionTask second = service.importDocument(fileImport("source-1", "sha256-a"));
	assertEquals(first.getDocumentId(), second.getDocumentId());
	assertEquals("COMPLETED", second.getStatus().name());
	assertEquals(1, embeddingCalls.get());
}

@Test
public void shouldPreserveSourceIdAndCreateNewDocumentVersion() {
	TinyflowDocument v1 = importAndAwait(textImport("source-1", "old"));
	TinyflowDocument v2 = importAndAwait(textImport("source-1", "new"));
	assertEquals(v1.getSourceId(), v2.getSourceId());
	assertNotEquals(v1.getDocumentId(), v2.getDocumentId());
}
```

- [ ] **Step 2: 运行 local 测试确认实现不存在**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -am -Dtest=TinyflowIngestionServiceTest,TinyflowDocumentParserTest,TinyflowChunkStrategyTest test`

Expected: FAIL，缺少 local Provider、解析器和切片器。

- [ ] **Step 3: 实现可替换解析器和异步状态机**

```java
public TinyflowIngestionTask importDocument(TinyflowDocumentImport request) {
	validateTenantKnowledgeAndSource(request);
	TinyflowDocument same = repository.findBySourceAndChecksum(request.getTenantId(),
			request.getKnowledgeId(), request.getSourceId(), request.getChecksum());
	if (same != null) return completedTaskFor(same);
	TinyflowDocument next = createDocumentVersion(request);
	repository.saveDocument(next);
	return submit(next, request);
}
```

状态严格按 `PENDING -> PARSING -> CHUNKING -> EMBEDDING -> INDEXING -> COMPLETED`，取消使用 `CANCELLING/CANCELLED`，失败保存阶段、计数、重试次数和脱敏错误。解析结果保存解析器 key/version 和页码/工作表/行号定位；所有切片复制 `sourceId/sourceType/documentId` 并生成唯一 `chunkId`。

- [ ] **Step 4: 运行解析、切片和幂等测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -am -Dtest=TinyflowIngestionServiceTest,TinyflowDocumentParserTest,TinyflowChunkStrategyTest test`

Expected: PASS，七类文件 fixture、普通/Q&A/父子切片、解析器扩展、幂等和版本更新全部通过。

- [ ] **Step 5: 提交本地导入链路**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local
git commit -m "feat(tinyflow): 实现本地知识库导入与切片"
```

### Task 5: 实现向量索引、补偿、删除与重建

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/index/TinyflowLocalIndexer.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/index/TinyflowVectorCompensator.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/index/TinyflowKnowledgeMaintenanceService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/DefaultTinyflowIngestionService.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/index/TinyflowLocalIndexerTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/index/TinyflowKnowledgeMaintenanceServiceTest.java`

**Interfaces:**
- Consumes: `TinyflowModelProvider.getEmbeddingModel(String)`、`TinyflowVectorStoreProvider.get(String)`；Starter/Magic 适配器在内部桥接现有 `AiEmbeddingStore`，以及仓库 vector ID 映射。
- Produces: 先建新索引后切换、可追踪补偿、按 sourceId 删除和重建。

- [ ] **Step 1: 写向量成功/资源失败和旧版本保留测试**

```java
@Test
public void shouldRecordAndRemoveOrphanVectorWhenChunkSaveFails() {
	repository.failNextChunkSave();
	assertThrows(TinyflowException.class, () -> indexer.index(document, chunks));
	assertEquals(singletonList("vec-new"), repository.pendingCompensationVectorIds(taskId));
	compensator.retry(taskId);
	verify(store).remove("vec-new");
}

@Test
public void shouldKeepOldVersionSearchableUntilNewIndexCompletes() {
	indexer.failAfterFirstBatch();
	assertThrows(TinyflowException.class, () -> maintenance.rebuild("tenant-a", "kb-1", "source-1"));
	assertEquals("doc-v1", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
}

@Test
public void shouldRejectVectorStoreReuseWithoutKnowledgeFilterCapability() {
	repository.saveKnowledge(knowledge("kb-1", "vector-main"));
	assertThrows(TinyflowException.class,
			() -> maintenance.create(knowledge("kb-2", "vector-main")));
}
```

- [ ] **Step 2: 运行索引测试确认失败**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -Dtest=TinyflowLocalIndexerTest,TinyflowKnowledgeMaintenanceServiceTest test`

Expected: FAIL，索引/补偿服务不存在。

- [ ] **Step 3: 实现两阶段索引与 DELETE_PENDING**

每个 `TinyflowVectorDocument` 的 id 使用 `chunkId`，metadata 强制写入 `knowledge_id/source_id/source_type/document_id/chunk_id`。`store.addAll()` 成功后先保存 vectorIds 映射，再原子激活新 document；任一步失败把已写 vectorIds 记入 task 的补偿列表。删除先标记 `DELETE_PENDING`，`removeAll(vectorIds)` 成功后删资源；失败创建补偿任务且不得返回“删除完成”。

- [ ] **Step 4: 运行故障矩阵测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -Dtest=TinyflowLocalIndexerTest,TinyflowKnowledgeMaintenanceServiceTest test`

Expected: PASS，覆盖模型缺失、向量库缺失、部分批次失败、资源保存失败、补偿重试、删除失败和重建回滚。

- [ ] **Step 5: 提交索引与补偿**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local
git commit -m "feat(tinyflow): 实现知识库索引与补偿"
```

### Task 6: 实现向量、关键词、混合检索和 Metadata 运算符

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/retrieval/TinyflowLocalRetriever.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/retrieval/TinyflowKeywordRetriever.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/retrieval/TinyflowHybridFusion.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/retrieval/TinyflowMetadataPredicate.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/local/FwPlusLocalKnowledgeProvider.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/retrieval/TinyflowLocalRetrieverTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/local/retrieval/TinyflowMetadataPredicateTest.java`

**Interfaces:**
- Consumes: `TinyflowVectorStore.search(float[], int, double)`、仓库 chunk/metadata、可选 `TinyflowReranker`；底层由现有 `AiEmbeddingStore.search(AiEmbeddingQuery)` 适配。
- Produces: VECTOR/KEYWORD/HYBRID 检索、候选硬上限、RRF/加权融合、父子展开和完整来源 metadata。

- [ ] **Step 1: 写检索模式、候选上限和全部运算符测试**

```java
@Test
public void shouldApplyThresholdAfterFusionRerankAndParentExpansion() {
	TinyflowKnowledgeResult result = retriever.retrieve(request(HYBRID, 3, 0.7D));
	assertEquals(3, result.getMatches().size());
	assertTrue(result.getMatches().get(0).getScore() >= result.getMatches().get(1).getScore());
	assertEveryMatchHasSourceIds(result);
}

@ParameterizedTest
@MethodSource("allDifyOperators")
public void shouldEvaluateMetadataOperator(String operator, Object expectedValue, boolean expected) {
	assertEquals(expected, predicate.test(metadata, condition("page", operator, expectedValue)));
}
```

- [ ] **Step 2: 运行检索测试确认实现不存在**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -Dtest=TinyflowLocalRetrieverTest,TinyflowMetadataPredicateTest test`

Expected: FAIL，缺少 retriever/predicate。

- [ ] **Step 3: 实现有界检索流水线**

候选数为 `min(maxCandidateCount, topK * candidateMultiplier)`，默认 multiplier 5、硬上限 200；不支持底层 metadata 过滤时，从仓库按 vector ID 加载 chunk 后执行 `and/or` 条件。实现 `contains/not contains/start with/end with/is/is not/in/not in/empty/not empty/=/≠/>/</≥/≤/before/after`，数值和 ISO 日期分别比较。停用 knowledge/document/chunk 直接过滤；可选 reranker 之后再父子展开、阈值和 TopK 截断。

- [ ] **Step 4: 运行本地检索测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -Dtest=TinyflowLocalRetrieverTest,TinyflowMetadataPredicateTest test`

Expected: PASS，三种模式、去重、归一化、候选上限、全部运算符、Rerank、父子展开和来源字段全部通过。

- [ ] **Step 5: 提交本地检索**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local
git commit -m "feat(tinyflow): 实现本地混合知识检索"
```

### Task 7: 实现 Dify Provider

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/dify/DifyTinyflowKnowledgeProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/dify/DifyTinyflowConnection.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/src/main/java/com/xiongx9527/fw/plus/ai/tinyflow/dify/DifyDatasetClientFactory.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/dify/DifyTinyflowKnowledgeProviderTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify/src/test/java/com/xiongx9527/fw/plus/ai/tinyflow/dify/DifyDependencyBoundaryTest.java`

**Interfaces:**
- Consumes: `IDatasetClient.retrieve(String datasetId, String query, RetrievalModel)` 和 `TinyflowDifyConnectionRepository`。
- Produces: 只读 Provider key `dify`，将 Tinyflow 请求映射为 Dify Dataset 检索并映射回统一结果。

- [ ] **Step 1: 写请求映射、只读能力和脱敏错误测试**

```java
@Test
public void shouldMapTopKThresholdAndDatasetId() {
	provider.retrieve(request("conn-1", "dataset-7", "hello", 8, 0.62D));
	verify(client).retrieve(eq("dataset-7"), eq("hello"), argThat(model ->
			model.getTopK() == 8 && model.getScoreThreshold().equals(0.62D)));
}

@Test
public void shouldBeReadOnlyAndNeverLeakApiKey() {
	assertFalse(provider.capabilities().isManageable());
	TinyflowException error = assertThrows(TinyflowException.class, () -> provider.retrieve(request));
	assertFalse(error.getMessage().contains("secret-api-key"));
}
```

- [ ] **Step 2: 运行 Dify Provider 测试确认失败**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify -am -Dtest=DifyTinyflowKnowledgeProviderTest,DifyDependencyBoundaryTest test`

Expected: FAIL，Provider 不存在。

- [ ] **Step 3: 实现现有客户端适配**

```java
public final class DifyTinyflowKnowledgeProvider implements TinyflowKnowledgeProvider {
	public String getKey() { return "dify"; }
	public TinyflowKnowledgeCapabilities capabilities() { return TinyflowKnowledgeCapabilities.readOnly(); }
	public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
		DifyTinyflowConnection connection = connections.require(request.getTenantId(), request.getKnowledgeId());
		RetrievalModel model = toRetrievalModel(request);
		return map(clientFactory.create(connection).retrieve(connection.getDatasetId(), request.getQuery(), model));
	}
}
```

连接只保存凭证引用；factory 通过凭证 SPI 获取明文后构造现有客户端。把网络超时、401/403、404、无效响应分别转换为稳定错误，不记录 Authorization/API Key。

- [ ] **Step 4: 运行 Provider 和依赖边界测试**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify -am -Dtest=DifyTinyflowKnowledgeProviderTest,DifyDependencyBoundaryTest test`

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify dependency:tree -Dincludes=com.xiongx9527.fw.plus:fw-plus-dify`

Expected: PASS，依赖树显示 Tinyflow Dify -> `fw-plus-dify`；`fw-plus-dify` 自身依赖树不出现 Tinyflow。

- [ ] **Step 5: 提交 Dify Provider**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-dify
git commit -m "feat(tinyflow): 实现 Dify 知识库 Provider"
```

### Task 8: 发布受保护的 Dify External Knowledge 接口

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalProperties.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalKnowledgeService.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowKnowledgeAccessKeyService.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalController.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfiguration.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/TinyflowKnowledgeLifecycle.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalAutoConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyflow/dify/TinyflowDifyExternalControllerTest.java`

**Interfaces:**
- Consumes: `IKnowledgeService`、`fw-plus-local` Provider、AccessKey repository、Task 1 DTO。
- Produces: `POST {base-path}/retrieval`，配置前缀 `fw.plus.ai.tinyflow.dify.external`。

- [ ] **Step 1: 写 Web 契约、鉴权和循环保护测试**

```java
mockMvc.perform(post("/external-knowledge/retrieval")
		.header("Authorization", "Bearer key-valid")
		.contentType(APPLICATION_JSON)
		.content(fixture("dify-external-request.json")))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.records[0].metadata.source_id").value("source-1"))
		.andExpect(jsonPath("$.records[0].metadata.source_type").value("file"))
		.andExpect(jsonPath("$.records[0].metadata.document_id").value("doc-2"))
		.andExpect(jsonPath("$.records[0].metadata.chunk_id").value("chunk-9"));
```

增加错误断言：缺少/格式错误 Authorization、无效/吊销 key、知识库不在 scope、知识库不存在、`top_k` 超限、超时、内部错误、零结果 200 空数组、请求指向 `dify` Provider 时拒绝。

- [ ] **Step 2: 运行 Web 测试确认入口不存在**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=TinyflowDifyExternalAutoConfigurationTest,TinyflowDifyExternalControllerTest test`

Expected: FAIL，Controller/配置不存在。

- [ ] **Step 3: 实现显式启用、安全鉴权和响应映射**

```java
@PostMapping("${fw.plus.ai.tinyflow.dify.external.base-path:/external-knowledge}/retrieval")
public ResponseEntity<?> retrieval(@RequestHeader("Authorization") String authorization,
		@RequestBody KnowledgeArgs args) {
	String token = bearerToken(authorization);
	return ResponseEntity.ok(service.retrieval(token, args));
}
```

自动配置仅在 `enabled=true`、`IKnowledgeService`、local Provider 和 AccessKey repository 同时存在时注册。默认 `enabled=false`；属性包含 `base-path=/external-knowledge`、`request-timeout=5s`、`max-top-k=20`、`rate-limit=60/min`。密钥创建用 `SecureRandom`，保存 PBKDF2/BCrypt 等强哈希和前缀，常量时间校验，明文只在创建响应出现一次。所有 result metadata 使用非 null `LinkedHashMap` 并强制四个来源字段。`TinyflowKnowledgeLifecycle` 在上下文启动时把同一个 `TinyflowKnowledgeManagerAdapter` 注册到 `KnowledgeManager`，关闭时移除；连续上下文测试不得残留静态 Provider。

- [ ] **Step 4: 运行自动配置和 HTTP 契约测试**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=TinyflowDifyExternalAutoConfigurationTest,TinyflowDifyExternalControllerTest test`

Expected: PASS，全部 Metadata 运算符、错误码/HTTP 状态、rate limit、timeout 和循环保护通过。

- [ ] **Step 5: 提交 External Knowledge 接口**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "feat(tinyflow): 提供 Dify 外部知识检索接口"
```

### Task 9: 用六类 Magic Resource 持久化知识库并接入 S3

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/storage/TinyflowKnowledgeMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/storage/TinyflowDocumentMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/storage/TinyflowChunkMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/storage/TinyflowIngestionTaskMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/storage/TinyflowKnowledgeKeyMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/storage/TinyflowDifyConnectionMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicApiTinyflowKnowledgeRepository.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicApiTinyflowObjectStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicTinyflowTenantResolver.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicTinyflowKnowledgePermissionEvaluator.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicTinyflowKnowledgeAuditSink.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicTinyflowKnowledgeController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/starter/MagicAPITinyflowConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/TinyflowKnowledgeMagicResourceTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/tinyflow/knowledge/MagicTinyflowKnowledgeControllerTest.java`

**Interfaces:**
- Consumes: `MagicResourceService`、`MagicDynamicS3DataSource`、`TinyflowKnowledgeRepository`、local 管理 Provider。
- Produces: folders `tinyflow-knowledge`、`tinyflow-document`、`tinyflow-chunk`、`tinyflow-ingestion-task`、`tinyflow-knowledge-key`、`tinyflow-dify-connection` 和完整管理 API。

- [ ] **Step 1: 写六类资源、租户隔离、密钥脱敏和 S3 测试**

```java
@Test
public void shouldRegisterExactlySixKnowledgeResourceFolders() {
	assertEquals(new HashSet<String>(Arrays.asList("tinyflow-knowledge", "tinyflow-document",
			"tinyflow-chunk", "tinyflow-ingestion-task", "tinyflow-knowledge-key",
			"tinyflow-dify-connection")), folders(storages));
}

@Test
public void shouldNeverPersistPlainSecrets() {
	controller.createKey(request("tenant-a", "kb-1"));
	String resources = dumpMagicResources();
	assertFalse(resources.contains(createdPlainKey));
	assertFalse(resources.contains("dify-plain-api-key"));
}
```

- [ ] **Step 2: 运行 Magic 知识库测试确认失败**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyflowKnowledgeMagicResourceTest,MagicTinyflowKnowledgeControllerTest test`

Expected: FAIL，storage/repository/controller 不存在。

- [ ] **Step 3: 实现六类 JSON Storage、仓库和管理 API**

每个 Storage `suffix=.json`、`requiredScript=false`、`allowRoot=true`，validate 强制 tenantId 和资源特有字段；chunk Storage 强制四个来源 ID。Controller 的每个方法先用 `MagicTinyflowTenantResolver` 取得当前租户，再用 `MagicTinyflowKnowledgePermissionEvaluator` 校验资源动作；提供知识库 CRUD、文档上传/文本/URL、chunk 列表/编辑/启停、任务列表/重试/取消、召回测试、密钥创建/吊销、Dify 连接 CRUD/测试、模型/向量/S3 选项。创建、更新、删除、导入、重建、密钥变更、连接测试和外部检索写入审计 sink；返回模型和数据源仅含 key/name/type/provider。

Tinyflow Magic 插件 POM 显式依赖现有 `com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-s3`，不复制 S3 客户端。`MagicApiTinyflowObjectStorage` 用知识库的现有 S3 key 解析 `MagicDynamicS3DataSource`，写入受控 object key，资源只保存 object key/文件名/mediaType/size/checksum；不得保存 S3 Secret。URL 导入仅允许 HTTP/HTTPS，解析 DNS 后拒绝 loopback、link-local、RFC1918、云 metadata，限制重定向、响应大小、类型和超时。

- [ ] **Step 4: 运行 Magic Resource 与安全测试**

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyflowKnowledgeMagicResourceTest,MagicTinyflowKnowledgeControllerTest test`

Expected: PASS，覆盖六类 CRUD、版本冲突、租户隔离、S3、sourceId/sourceType、密钥只显示一次和 SSRF 拦截。

- [ ] **Step 5: 提交 Magic 知识库后端**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "feat(tinyflow): 持久化 Magic 本地知识库"
```

### Task 10: 完整知识库管理 UI 与端到端验收

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/index.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/knowledge-app.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/knowledge-list.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/knowledge-detail.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/document-import.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/chunk-manager.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/retrieval-test.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/api-access.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/task-center.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/knowledge-settings.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/src/knowledge/dify-connection.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui/test/knowledge-app.test.cjs`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/src/components/tinyflow-knowledge-workbench.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console/e2e/tinyflow-knowledge.spec.js`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yml`
- Create: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/test/java/com/xiongx9527/fw/plus/sample/ai/tinyflow/TinyflowKnowledgeSampleIT.java`

**Interfaces:**
- Consumes: Task 9 管理 API、Starter `mountKnowledge(options)`、工作流 Knowledge 节点 Provider registry。
- Produces: 参考 Dify/RAGFlow 信息架构的完整 local 管理 UI、只读 Dify 连接页，以及真实 HTTP/工作流/浏览器验收。

- [ ] **Step 1: 写 UI 能力控制和完整用户路径测试**

```javascript
test('read-only dify provider hides management actions', async () => {
  const app = mountKnowledge({ provider: difyReadOnlyProvider })
  assert.equal(app.findAction('upload'), null)
  assert.equal(app.findAction('rebuild'), null)
  assert.ok(app.findAction('test-connection'))
})
```

```javascript
test('manage local knowledge and retrieve through Dify endpoint', async ({ page, request }) => {
  await createLocalKnowledge(page)
  await importFile(page, 'fixtures/source.txt')
  await waitForTask(page, 'COMPLETED')
  await expect(page.getByText('source-001')).toBeVisible()
  const response = await request.post('/external-knowledge/retrieval', difyRequest())
  expect((await response.json()).records[0].metadata.source_type).toBe('file')
})
```

- [ ] **Step 2: 运行 Node 和 sample 测试确认 UI/集成未完成**

Run: `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui && npm test`

Expected: FAIL，知识库 app 测试缺少组件。

Run: `mvn -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -Dtest=TinyflowKnowledgeSampleIT test`

Expected: FAIL，sample 未启用知识库或 External API。

- [ ] **Step 3: 实现完整工作台和 sample fixture**

页面包含列表，以及详情中的概览、文档、切片、召回测试、API 接入、任务、设置七个 tab；上传支持文件/文本/URL/批量、解析器和切片策略；切片显示原文对照、父子、定位和 metadata；任务支持进度、错误、重试、取消；API Key 明文只在创建弹窗出现一次。Dify 页面只提供 base URL、凭证引用、Dataset ID 和测试。所有表格固定列约束并支持长文本换行，不用嵌套卡片。

sample 注册假的 Embedding model 和内存 Vector Store 作为“现有 Bean”，通过 Magic Model/Vector registry 暴露，不在 Tinyflow 配置中创建私有连接；测试 fixture 包含 TXT、PDF、DOCX、HTML、CSV、XLSX。

- [ ] **Step 4: 执行完整 Java、HTTP、Node 和浏览器验证**

Run: `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dify,fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow -am clean install -DskipTests=false`

Expected: BUILD SUCCESS。

Run: `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow -am clean install -DskipTests=false`

Expected: BUILD SUCCESS。

Run: `mvn -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -Dtest=TinyflowKnowledgeSampleIT test`

Expected: PASS，七类导入、索引、local/dify Knowledge 节点、Dify `/retrieval` 和四个来源 ID 全部通过。

Run: `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui && npm ci && npm test && npm run build`

Expected: PASS。

启动 sample 后运行：`cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow/src/console && npm run test:e2e`

Expected: 桌面 `1440x900` 与移动 `390x844` 均通过；列表、导入、切片、任务、召回、API Key、Dify 连接无重叠；桌面是主要操作验收，移动视口允许表格横向滚动但不得遮挡命令按钮。

- [ ] **Step 5: 检查安全/依赖/分支并提交验收**

Run: `rg -n "api[_-]?key|authorization|secret" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/surefire-reports fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/target/surefire-reports`

Expected: 报告中不出现测试使用的明文 secret 值。

Run: `git diff --check && git branch --show-current && git status --short`

Expected: 无 whitespace error，分支为 `dev`，只包含计划内文件。

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tinyflow-spring-boot-starter \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow \
  fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample
git commit -m "test(tinyflow): 验收本地知识库与 Dify 接口"
```

## 最终验收清单

- [ ] Core/local/dify 模块依赖方向正确，`fw-plus-dify` 不依赖 Tinyflow。
- [ ] `fw-plus-local` 提供完整管理能力，`dify` Provider 明确只读。
- [ ] 每个 chunk 和每个检索结果都有 `sourceId/sourceType/documentId/chunkId`；Dify metadata 有四个 snake_case 字段。
- [ ] 相同 `sourceId + checksum` 幂等；内容更新、重建和失败回滚不会提前删除旧可检索版本。
- [ ] 向量写入、资源写入和删除失败都有可追踪补偿，不遗留无法定位的 vector ID。
- [ ] 向量、关键词、混合、Metadata、Rerank、父子展开、阈值与 TopK 测试通过且候选有硬上限。
- [ ] 六类 Magic Resource 完整注册，tenantId/版本/权限隔离通过测试，资源正文无明文凭证。
- [ ] Dify `/retrieval` 对齐官方 JSON、错误和 HTTP 状态，只路由 local，零结果返回 200 空数组。
- [ ] 本地 UI 覆盖知识库、文档、chunk、召回、API、任务和设置；Dify UI 只提供连接、Dataset 引用和测试。
- [ ] 模型、Embedding、Vector Store、S3 均来自现有能力，未新增 Tinyflow 私有连接。
- [ ] Neo4j 不在默认依赖/执行链，只保留可选 GraphRAG SPI。
- [ ] Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、npm、sample、HTTP 和 Playwright 验收全部通过。
