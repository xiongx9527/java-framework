package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public abstract class AbstractTinyflowKnowledgeRepositoryContractTest {

	protected TinyflowKnowledgeRepository repository;

	protected abstract TinyflowKnowledgeRepository createRepository();

	protected abstract void failNextCompletionAt(TinyflowKnowledgeRepository target, String step);

	@Before
	public void setUpRepository() {
		repository = createRepository();
	}

	@Test
	public void shouldOnlyExposeActiveDocumentVersionAfterAtomicSwitch() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-v1", "sum-1"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-v2", "sum-2"));
		repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-v2");

		assertEquals("doc-v2", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
		assertNull(repository.findActiveDocument("tenant-b", "kb-1", "source-1"));
		assertEquals("doc-v1", repository.findBySourceAndChecksum("tenant-a", "kb-1", "source-1", "sum-1").getDocumentId());
	}

	@Test
	public void shouldKeepPreviousActiveVersionWhenSwitchTargetIsInvalid() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-active", "sum-active"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-2", "doc-source", "sum-source"));
		repository.saveDocument(document("tenant-a", "kb-2", "source-1", "doc-knowledge", "sum-knowledge"));
		repository.saveDocument(document("tenant-b", "kb-1", "source-1", "doc-tenant", "sum-tenant"));
		repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-active");

		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "missing");
		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "doc-source");
		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "doc-knowledge");
		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "doc-tenant");
	}

	@Test
	public void shouldRejectStaleKnowledgeDefinitionVersion() {
		TinyflowKnowledgeDefinition definition = knowledge("tenant-a", "kb-1");
		repository.saveKnowledge(definition, 0L);
		assertEquals(1L, repository.findKnowledgeVersion("tenant-a", "kb-1"));
		assertThrows(TinyflowKnowledgeRepository.OptimisticLockException.class,
				() -> repository.saveKnowledge(definition, 0L));
	}

	@Test
	public void shouldDeleteKnowledgeWithExpectedVersionAndRejectStaleDelete() {
		TinyflowKnowledgeDefinition initial = knowledge("tenant-a", "kb-delete");
		repository.saveKnowledge(initial, 0L);
		long staleVersion = repository.findKnowledgeVersion("tenant-a", "kb-delete");
		TinyflowKnowledgeDefinition concurrent = TinyflowKnowledgeDefinition.builder()
				.providerKey("local").tenantId("tenant-a").knowledgeId("kb-delete")
				.name("Concurrent update").description("must survive stale delete").build();
		repository.saveKnowledge(concurrent, staleVersion);

		assertThrows(TinyflowKnowledgeRepository.OptimisticLockException.class,
				() -> repository.deleteKnowledge("tenant-a", "kb-delete", staleVersion));
		assertEquals("Concurrent update", repository.findKnowledge("tenant-a", "kb-delete").getName());
		long currentVersion = repository.findKnowledgeVersion("tenant-a", "kb-delete");
		repository.deleteKnowledge("tenant-a", "kb-delete", currentVersion);

		assertNull(repository.findKnowledge("tenant-a", "kb-delete"));
		assertEquals(0L, repository.findKnowledgeVersion("tenant-a", "kb-delete"));
	}

	@Test
	public void shouldDeleteOnlyRequestedSourceVersionsWithinTenantAndKnowledge() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-a1", "sum-a1"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-a2", "sum-a2"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-2", "doc-a3", "sum-a3"));
		repository.saveDocument(document("tenant-b", "kb-1", "source-1", "doc-b1", "sum-b1"));

		assertEquals(2, repository.findDocumentVersions("tenant-a", "kb-1", "source-1").size());
		repository.deleteDocumentsBySource("tenant-a", "kb-1", "source-1");
		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertEquals(1, repository.findDocumentVersions("tenant-a", "kb-1", "source-2").size());
		assertEquals(1, repository.findDocumentVersions("tenant-b", "kb-1", "source-1").size());
	}

	@Test
	public void shouldIsolateChunkAndDocumentIdsAcrossKnowledgeWithinTenant() {
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-shared", "chunk-shared", "knowledge-one", true, "vec-1"),
				chunk("tenant-a", "kb-2", "doc-shared", "chunk-shared", "knowledge-two", true, "vec-2")));

		assertEquals("knowledge-one", repository.findChunk("tenant-a", "kb-1", "chunk-shared").getContent());
		assertEquals("knowledge-two", repository.findChunk("tenant-a", "kb-2", "chunk-shared").getContent());
		assertEquals("knowledge-one",
				repository.findChunksByDocument("tenant-a", "kb-1", "doc-shared").get(0).getContent());
		assertEquals("knowledge-two",
				repository.findChunksByDocument("tenant-a", "kb-2", "doc-shared").get(0).getContent());
	}

	@Test
	public void shouldPersistAndResolveVectorIdsWithinTenantAndKnowledge() {
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-1", "chunk-1", "old", true, "vec-1", "vec-2"),
				chunk("tenant-b", "kb-1", "doc-2", "chunk-2", "other", true, "vec-1")));

		assertEquals(Arrays.asList("vec-1", "vec-2"),
				repository.findChunk("tenant-a", "kb-1", "chunk-1").getVectorIds());
		List<TinyflowChunk> matches = repository.findChunksByVectorIds(
				"tenant-a", "kb-1", Arrays.asList("vec-2", "vec-1"));
		assertEquals(1, matches.size());
		assertEquals("chunk-1", matches.get(0).getChunkId());
	}

	@Test
	public void shouldUpdateChunkWithoutLosingSourceIdentityOrChangingMetadataShape() {
		repository.saveChunks(Collections.singletonList(
				chunk("tenant-a", "kb-1", "doc-1", "chunk-1", "old", true, "vec-1")));
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("page", 2);
		repository.updateChunk("tenant-a", "kb-1", "chunk-1", false, "new", metadata);

		TinyflowChunk updated = repository.findChunk("tenant-a", "kb-1", "chunk-1");
		assertEquals("tenant-a", updated.getTenantId());
		assertEquals("kb-1", updated.getKnowledgeId());
		assertEquals("source-1", updated.getSourceId());
		assertEquals("file", updated.getSourceType());
		assertEquals("doc-1", updated.getDocumentId());
		assertEquals("new", updated.getContent());
		assertFalse(updated.isEnabled());
		assertEquals(Collections.<String, Object>singletonMap("page", 2), updated.getMetadata());
	}

	@Test
	public void shouldDefensivelyPersistVectorCompensationRecords() {
		List<String> vectorIds = new ArrayList<String>(Arrays.asList("vec-1", "vec-2"));
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				new TinyflowKnowledgeRepository.VectorCompensation(
						"comp-1", "tenant-a", "kb-1", "source-1", "doc-1", vectorIds, "delete");
		repository.saveVectorCompensation(compensation);
		vectorIds.add("vec-3");

		List<TinyflowKnowledgeRepository.VectorCompensation> records =
				repository.findVectorCompensations("tenant-a", "kb-1");
		assertEquals(Arrays.asList("vec-1", "vec-2"), records.get(0).getVectorIds());
		assertThrows(UnsupportedOperationException.class, () -> records.get(0).getVectorIds().add("vec-4"));
		assertTrue(repository.findVectorCompensations("tenant-b", "kb-1").isEmpty());
	}

	@Test
	public void shouldListTasksWithinTenantAndKnowledge() {
		repository.saveTask(task("task-a", "tenant-a", "kb-1"));
		repository.saveTask(task("task-b", "tenant-b", "kb-1"));
		assertEquals(1, repository.findTasks("tenant-a", "kb-1").size());
		assertEquals("task-a", repository.findTasks("tenant-a", "kb-1").get(0).getTaskId());
	}

	@Test
	public void shouldAtomicallyCompleteIngestionAndRollbackEveryFailedStep() {
		for (String step : Arrays.asList("chunks", "document", "active", "task")) {
			repository = createRepository();
			CompletionFixture fixture = completionFixture();
			prepareCompletion(fixture);
			failNextCompletionAt(repository, step);

			assertThrows(IllegalStateException.class, () -> repository.completeIngestion(
					fixture.completedDocument, fixture.chunks, fixture.completedTask));
			assertEquals("doc-old", repository.findActiveDocument("tenant-a", "kb-atomic", "source-1")
					.getDocumentId());
			assertTrue(repository.findChunksByDocument("tenant-a", "kb-atomic", "doc-new").isEmpty());
			assertTrue(repository.findBySourceAndChecksum(
					"tenant-a", "kb-atomic", "source-1", "sum-new").getChunks().isEmpty());
			assertFalse(hasTaskStatus(repository.findTasks("tenant-a", "kb-atomic"), "COMPLETED"));
		}

		repository = createRepository();
		CompletionFixture fixture = completionFixture();
		prepareCompletion(fixture);
		repository.completeIngestion(fixture.completedDocument, fixture.chunks, fixture.completedTask);

		assertEquals("doc-new", repository.findActiveDocument("tenant-a", "kb-atomic", "source-1")
				.getDocumentId());
		assertEquals("chunk-new", repository.findChunksByDocument(
				"tenant-a", "kb-atomic", "doc-new").get(0).getChunkId());
		assertEquals(1, repository.findBySourceAndChecksum(
				"tenant-a", "kb-atomic", "source-1", "sum-new").getChunks().size());
		assertTrue(hasTaskStatus(repository.findTasks("tenant-a", "kb-atomic"), "COMPLETED"));
	}

	private CompletionFixture completionFixture() {
		TinyflowChunk chunk = chunk("tenant-a", "kb-atomic", "doc-new", "chunk-new", "new", true, "vec-new");
		TinyflowDocument completed = TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-atomic")
				.sourceId("source-1").sourceType("file").documentId("doc-new").checksum("sum-new")
				.name("new").content("new").chunks(Collections.singletonList(chunk)).build();
		TinyflowIngestionTask task = TinyflowIngestionTask.builder().taskId("task-new").providerKey("local")
				.tenantId("tenant-a").knowledgeId("kb-atomic").documentId("doc-new")
				.status("COMPLETED").build();
		return new CompletionFixture(completed, Collections.singletonList(chunk), task);
	}

	private void prepareCompletion(CompletionFixture fixture) {
		repository.saveDocument(document("tenant-a", "kb-atomic", "source-1", "doc-old", "sum-old"));
		repository.activateDocumentVersion("tenant-a", "kb-atomic", "source-1", "doc-old");
		repository.saveDocument(document("tenant-a", "kb-atomic", "source-1", "doc-new", "sum-new"));
		repository.saveTask(TinyflowIngestionTask.builder().taskId("task-new").providerKey("local")
				.tenantId("tenant-a").knowledgeId("kb-atomic").documentId("doc-new")
				.status("PENDING").build());
	}

	private boolean hasTaskStatus(List<TinyflowIngestionTask> tasks, String status) {
		for (TinyflowIngestionTask task : tasks) {
			if (status.equals(task.getStatus())) {
				return true;
			}
		}
		return false;
	}

	private static final class CompletionFixture {
		private final TinyflowDocument completedDocument;
		private final List<TinyflowChunk> chunks;
		private final TinyflowIngestionTask completedTask;

		private CompletionFixture(TinyflowDocument completedDocument, List<TinyflowChunk> chunks,
				TinyflowIngestionTask completedTask) {
			this.completedDocument = completedDocument;
			this.chunks = chunks;
			this.completedTask = completedTask;
		}
	}

	private void assertSwitchFailsAndActiveRemains(String tenantId, String knowledgeId,
			String sourceId, String documentId) {
		assertThrows(IllegalArgumentException.class,
				() -> repository.activateDocumentVersion(tenantId, knowledgeId, sourceId, documentId));
		assertEquals("doc-active", repository.findActiveDocument(tenantId, knowledgeId, sourceId).getDocumentId());
	}

	protected TinyflowKnowledgeDefinition knowledge(String tenantId, String knowledgeId) {
		return TinyflowKnowledgeDefinition.builder().providerKey("local").tenantId(tenantId)
				.knowledgeId(knowledgeId).name("Knowledge").build();
	}

	protected TinyflowDocument document(String tenantId, String knowledgeId, String sourceId,
			String documentId, String checksum) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("checksum", "metadata-must-not-control-identity");
		return TinyflowDocument.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId(sourceId)
				.sourceType("file").documentId(documentId).checksum(checksum).name(documentId)
				.metadata(metadata).build();
	}

	protected TinyflowChunk chunk(String tenantId, String knowledgeId, String documentId,
			String chunkId, String content, boolean enabled, String... vectorIds) {
		return TinyflowChunk.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId("source-1")
				.sourceType("file").documentId(documentId).chunkId(chunkId).content(content)
				.enabled(enabled).vectorIds(Arrays.asList(vectorIds)).build();
	}

	protected TinyflowIngestionTask task(String taskId, String tenantId, String knowledgeId) {
		return TinyflowIngestionTask.builder().taskId(taskId).providerKey("local").tenantId(tenantId)
				.knowledgeId(knowledgeId).documentId("doc-1").status("pending").build();
	}
}
