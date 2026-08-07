package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify.TinyflowKnowledgeAccessKeyService;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.DefaultMagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.magicapi.utils.WebUtils;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MagicTinyflowKnowledgeControllerSecurityTest {

	private MagicRequestContextHolder previousContextHolder;

	@Before
	public void installMagicRequestContextHolder() {
		previousContextHolder = WebUtils.magicRequestContextHolder;
		WebUtils.magicRequestContextHolder = new MagicRequestContextHolder() {
			@Override public org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest getRequest() { return null; }
			@Override public MagicHttpServletResponse getResponse() { return null; }
		};
	}

	@After
	public void restoreMagicRequestContextHolder() {
		WebUtils.magicRequestContextHolder = previousContextHolder;
	}

	@Test
	public void shouldAlwaysResolveTenantThenAuthorizeThenValidateThenActThenAudit() {
		List<String> sequence = new ArrayList<String>();
		RecordingRepository repository = new RecordingRepository(sequence);
		MagicTinyflowKnowledgeController controller = controller(repository, sequence, null, null);

		controller.saveDifyConnection(validConnection(), null);

		assertEquals(Arrays.asList("tenant", "permission", "action", "audit"), sequence);
	}

	@Test
	public void shouldAuditFailuresWithoutSensitiveContentAndWithoutUnauthorizedSideEffects() {
		assertFailureStage("permission", new SecurityException("Authorization bearer-secret"), true);
		assertFailureStage("validation", null, false);
		assertFailureStage("action", new IllegalStateException("plain-body-secret"), false);
	}

	@Test
	public void shouldIsolateDifyConnectionsAndKeyRevocationByResolvedTenant() throws Exception {
		File root = Files.createTempDirectory("tinyflow-controller-tenants").toFile();
		MagicApiTinyflowKnowledgeRepository repository = new MagicApiTinyflowKnowledgeRepository(resources(root));
		MagicTinyflowKnowledgeController tenantA = controller(repository, "tenant-a");
		MagicTinyflowKnowledgeController tenantB = controller(repository, "tenant-b");
		MagicTinyflowKnowledgeController.DifyConnectionRequest connection = validConnection();
		connection.setTenantId("tenant-b");
		tenantA.saveDifyConnection(connection, null);

		assertNull(tenantB.getDifyConnection("dify-1", null));
		tenantB.deleteDifyConnection("dify-1", null);
		assertNotNull(tenantA.getDifyConnection("dify-1", null));
		assertEquals(1, tenantA.listDifyConnections("kb-1", null).size());
		assertEquals(0, tenantB.listDifyConnections("kb-1", null).size());

		MagicTinyflowKnowledgeController.CreateKeyRequest createKey = new MagicTinyflowKnowledgeController.CreateKeyRequest();
		createKey.setKnowledgeId("kb-1");
		MagicTinyflowKnowledgeController.CreatedKeyResponse created = tenantA.createKey(createKey, null);
		assertEquals(1, tenantA.listKeys("kb-1", null).size());
		assertFalse(tenantA.listKeys("kb-1", null).get(0).toString().contains(created.getToken()));
		tenantB.revokeKey("kb-1", created.getAccessKeyId(), null);
		assertEquals("tenant-a", new TinyflowKnowledgeAccessKeyService(repository)
				.authenticate(created.getToken()).getTenantId());
		tenantA.revokeKey("kb-1", created.getAccessKeyId(), null);
		assertThrows(RuntimeException.class,
				() -> new TinyflowKnowledgeAccessKeyService(repository).authenticate(created.getToken()));
	}

	@Test
	public void shouldTestOnlyTenantScopedDifyConnectionAndAuditResult() {
		List<String> sequence = new ArrayList<String>();
		RecordingRepository repository = new RecordingRepository(sequence);
		repository.connection = connection("tenant-a");
		MagicTinyflowKnowledgeController controller = controller(repository, sequence, null,
				value -> sequence.add("action"));

		controller.testDifyConnection("dify-1", null);

		assertEquals(Arrays.asList("tenant", "permission", "action", "audit"), sequence);
	}

	private void assertFailureStage(String stage, RuntimeException failure, boolean permissionFailure) {
		List<String> sequence = new ArrayList<String>();
		RecordingRepository repository = new RecordingRepository(sequence);
		if ("action".equals(stage)) repository.failure = failure;
		List<MagicTinyflowKnowledgeAuditSink.AuditEvent> audits = new ArrayList<MagicTinyflowKnowledgeAuditSink.AuditEvent>();
		MagicTinyflowKnowledgeController controller = controller(repository, sequence,
				permissionFailure ? failure : null, null, audits);
		MagicTinyflowKnowledgeController.DifyConnectionRequest request = validConnection();
		if ("validation".equals(stage)) request.setBaseUrl(" ");

		assertThrows(RuntimeException.class, () -> controller.saveDifyConnection(request, null));

		assertEquals(1, audits.size());
		assertFalse(audits.get(0).isSuccess());
		assertFalse(audits.get(0).toString().contains("plain-body-secret"));
		assertFalse(audits.get(0).toString().contains("bearer-secret"));
		assertEquals(permissionFailure || "validation".equals(stage) ? 0 : 1, repository.actionCount);
	}

	private MagicTinyflowKnowledgeController controller(MagicApiTinyflowKnowledgeRepository repository,
			String tenantId) {
		return new MagicTinyflowKnowledgeController(repository, new TinyflowKnowledgeAccessKeyService(repository),
				request -> tenantId, (resolvedTenant, action, resourceId, request) -> { }, event -> { });
	}

	private MagicTinyflowKnowledgeController controller(RecordingRepository repository, List<String> sequence,
			RuntimeException permissionFailure, MagicTinyflowKnowledgeController.DifyConnectionTester tester) {
		return controller(repository, sequence, permissionFailure, tester,
				new ArrayList<MagicTinyflowKnowledgeAuditSink.AuditEvent>());
	}

	private MagicTinyflowKnowledgeController controller(RecordingRepository repository, List<String> sequence,
			RuntimeException permissionFailure, MagicTinyflowKnowledgeController.DifyConnectionTester tester,
			List<MagicTinyflowKnowledgeAuditSink.AuditEvent> audits) {
		return new MagicTinyflowKnowledgeController(repository, new TinyflowKnowledgeAccessKeyService(repository),
				request -> { sequence.add("tenant"); return "tenant-a"; },
				(tenantId, action, resourceId, request) -> {
					sequence.add("permission");
					if (permissionFailure != null) throw permissionFailure;
				}, event -> { sequence.add("audit"); audits.add(event); }, tester);
	}

	private MagicTinyflowKnowledgeController.DifyConnectionRequest validConnection() {
		MagicTinyflowKnowledgeController.DifyConnectionRequest request = new MagicTinyflowKnowledgeController.DifyConnectionRequest();
		request.setTenantId("untrusted-tenant"); request.setConnectionId("dify-1"); request.setKnowledgeId("kb-1");
		request.setBaseUrl("https://dify.example.test"); request.setDatasetId("dataset-1");
		request.setCredentialRef("vault://dify-main"); request.setApiKey("plain-body-secret");
		return request;
	}

	private TinyflowDifyConnectionRepository.DifyConnection connection(String tenantId) {
		return new TinyflowDifyConnectionRepository.DifyConnection("dify-1", tenantId, "kb-1",
				"https://dify.example.test", "dataset-1", "vault://dify-main");
	}

	private MagicResourceService resources(File root) {
		List<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>> storages = Arrays
				.<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>>asList(
						new TinyflowKnowledgeMagicResourceStorage(), new TinyflowDocumentMagicResourceStorage(),
						new TinyflowChunkMagicResourceStorage(), new TinyflowIngestionTaskMagicResourceStorage(),
						new TinyflowKnowledgeKeyMagicResourceStorage(), new TinyflowDifyConnectionMagicResourceStorage());
		DefaultMagicResourceService service = new DefaultMagicResourceService(
				new FileResource(root, false, root.getAbsolutePath()), storages, new ApplicationEventPublisher() {
					@Override public void publishEvent(Object event) { }
				});
		service.refresh();
		return service;
	}

	private static final class RecordingRepository extends MagicApiTinyflowKnowledgeRepository {
		private final List<String> sequence;
		private int actionCount;
		private RuntimeException failure;
		private TinyflowDifyConnectionRepository.DifyConnection connection;

		private RecordingRepository(List<String> sequence) {
			super((MagicResourceService) Proxy.newProxyInstance(MagicResourceService.class.getClassLoader(),
					new Class<?>[]{MagicResourceService.class}, (proxy, method, args) -> null));
			this.sequence = sequence;
		}

		@Override public void save(TinyflowDifyConnectionRepository.DifyConnection value) {
			actionCount++; sequence.add("action");
			if (failure != null) throw failure;
			connection = value;
		}

		@Override public TinyflowDifyConnectionRepository.DifyConnection find(String tenantId, String connectionId) {
			return connection != null && tenantId.equals(connection.getTenantId())
					&& connectionId.equals(connection.getConnectionId()) ? connection : null;
		}
	}
}
