package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter.MagicAPITinyflowConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.web.MagicTinyflowController;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.utils.Mapping;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MagicTinyflowKnowledgeHttpConfigurationTest {

	@Test
	public void shouldExposeCompleteBoundedKnowledgeManagementHttpSurface() {
		Map<String, RequestMethod> expected = new LinkedHashMap<String, RequestMethod>();
		expected.put("/tinyflow/knowledge/create", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/get", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/list", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/update", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/delete", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/import/text", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/import/file", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/import/url", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/chunk/list", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/chunk/update", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/task/list", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/task/retry", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/task/cancel", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/recall", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/options/model", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/options/vector", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/options/s3", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/key/create", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/key/list", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/key/revoke", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/dify/save", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/dify/get", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/dify/list", RequestMethod.GET);
		expected.put("/tinyflow/knowledge/dify/delete", RequestMethod.POST);
		expected.put("/tinyflow/knowledge/dify/test", RequestMethod.POST);

		Map<String, RequestMethod> actual = new LinkedHashMap<String, RequestMethod>();
		for (Method method : MagicTinyflowKnowledgeHttpController.class.getDeclaredMethods()) {
			RequestMapping mapping = method.getAnnotation(RequestMapping.class);
			if (mapping == null) continue;
			assertTrue(method.getName(), method.isAnnotationPresent(Valid.class));
			assertTrue(method.getName(), method.getAnnotation(Valid.class).requireLogin());
			assertEquals(method.getName(), 1, mapping.value().length);
			assertEquals(method.getName(), 1, mapping.method().length);
			actual.put(mapping.value()[0], mapping.method()[0]);
		}
		assertEquals(expected, actual);
	}

	@Test
	public void shouldRegisterWorkflowAndKnowledgeControllersAndExactlySixStorages() {
		MagicAPITinyflowConfiguration configuration = new MagicAPITinyflowConfiguration();
		Mapping mapping = mock(Mapping.class);

		configuration.controllerRegister().register(mapping, new MagicConfiguration());

		verify(mapping, times(2)).registerController(any());
		verify(mapping).registerController(org.mockito.ArgumentMatchers.isA(MagicTinyflowController.class));
		verify(mapping).registerController(org.mockito.ArgumentMatchers.isA(MagicTinyflowKnowledgeHttpController.class));
		long storageBeans = Arrays.stream(MagicAPITinyflowConfiguration.class.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(Bean.class))
				.filter(method -> method.getReturnType().getName().contains("knowledge.storage"))
				.count();
		assertEquals(6L, storageBeans);
	}

	@Test
	public void shouldMapDefaultKnowledgeActionsToMagicApiAuthorization() {
		AuthorizationInterceptor authorization = mock(AuthorizationInterceptor.class);
		when(authorization.allowVisit(any(), any(), any())).thenReturn(true);
		MagicHttpServletRequest request = request(new MagicUser("tenant-a", "tester", "token"));

		contextRunner(authorization).run(context -> {
			MagicTinyflowKnowledgePermissionEvaluator evaluator =
					context.getBean(MagicTinyflowKnowledgePermissionEvaluator.class);
			evaluator.check("tenant-a", "knowledge:view", "kb-1", request);
			evaluator.check("tenant-a", "knowledge:recall", "kb-1", request);
			evaluator.check("tenant-a", "dify:test", "dify-1", request);
			evaluator.check("tenant-a", "knowledge:create", "kb-1", request);
			evaluator.check("tenant-a", "key:revoke", "key-1", request);
			evaluator.check("tenant-a", "task:cancel", "task-1", request);
			evaluator.check("tenant-a", "knowledge:delete", "kb-1", request);
			evaluator.check("tenant-a", "dify:delete", "dify-1", request);

			verify(authorization, times(3)).allowVisit(any(), eq(request), eq(Authorization.VIEW));
			verify(authorization, times(3)).allowVisit(any(), eq(request), eq(Authorization.SAVE));
			verify(authorization, times(2)).allowVisit(any(), eq(request), eq(Authorization.DELETE));
		});
	}

	@Test
	public void shouldRejectMissingIdentityOrDeniedKnowledgeActionByDefault() {
		AuthorizationInterceptor authorization = mock(AuthorizationInterceptor.class);
		when(authorization.allowVisit(any(), any(), any())).thenReturn(false);

		contextRunner(authorization).run(context -> {
			MagicTinyflowKnowledgePermissionEvaluator evaluator =
					context.getBean(MagicTinyflowKnowledgePermissionEvaluator.class);
			assertThrows(SecurityException.class,
					() -> evaluator.check("tenant-a", "knowledge:view", "kb-1", null));
			assertThrows(SecurityException.class,
					() -> evaluator.check("tenant-a", "knowledge:view", "kb-1", request(null)));
			assertThrows(SecurityException.class,
					() -> evaluator.check("tenant-a", "knowledge:view", "kb-1",
							request(new MagicUser("tenant-a", "tester", "token"))));
		});
	}

	@Test
	public void shouldStartWithoutMagicConfigurationAndKeepDefaultPermissionFailClosed() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MagicAPITinyflowConfiguration.class))
				.withBean(MagicResourceService.class, () -> mock(MagicResourceService.class))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					MagicTinyflowKnowledgePermissionEvaluator evaluator =
							context.getBean(MagicTinyflowKnowledgePermissionEvaluator.class);
					assertThrows(SecurityException.class, () -> evaluator.check("tenant-a", "knowledge:view", "kb-1",
							request(new MagicUser("tenant-a", "tester", "token"))));
				});
	}

	@Test
	public void shouldPersistDefaultAuditEventsWithoutRegisteringSeventhMagicResource() throws Exception {
		File root = Files.createTempDirectory("tinyflow-audit").toFile();
		MagicResourceService resources = mock(MagicResourceService.class);
		when(resources.getResource()).thenReturn(new FileResource(root, false, root.getAbsolutePath()));
		AuthorizationInterceptor authorization = mock(AuthorizationInterceptor.class);

		contextRunner(authorization, resources).run(context -> context
				.getBean(MagicTinyflowKnowledgeAuditSink.class)
				.record(new MagicTinyflowKnowledgeAuditSink.AuditEvent(
						"tenant-a", "knowledge:create", "kb-1", false, IllegalStateException.class.getName())));

		File auditDirectory = new File(root, ".tinyflow-audit");
		assertTrue(auditDirectory.isDirectory());
		File[] auditFiles = auditDirectory.listFiles((directory, name) -> name.endsWith(".json"));
		assertEquals(1, auditFiles == null ? 0 : auditFiles.length);
		JSONObject stored = JSON.parseObject(new String(
				Files.readAllBytes(auditFiles[0].toPath()), StandardCharsets.UTF_8));
		assertEquals("tenant-a", stored.getString("tenantId"));
		assertEquals("knowledge:create", stored.getString("action"));
		assertEquals("kb-1", stored.getString("resourceId"));
		assertEquals(Boolean.FALSE, stored.getBoolean("success"));
		assertEquals(IllegalStateException.class.getName(), stored.getString("errorType"));
		assertTrue(stored.getLongValue("recordedAt") > 0L);
	}

	private ApplicationContextRunner contextRunner(AuthorizationInterceptor authorization) {
		return contextRunner(authorization, mock(MagicResourceService.class));
	}

	private ApplicationContextRunner contextRunner(AuthorizationInterceptor authorization,
			MagicResourceService resources) {
		MagicConfiguration magicConfiguration = new MagicConfiguration();
		magicConfiguration.setAuthorizationInterceptor(authorization);
		return new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MagicAPITinyflowConfiguration.class))
				.withBean(MagicResourceService.class, () -> resources)
				.withBean(MagicConfiguration.class, () -> magicConfiguration);
	}

	private MagicHttpServletRequest request(MagicUser user) {
		MagicHttpServletRequest request = mock(MagicHttpServletRequest.class);
		when(request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER)).thenReturn(user);
		return request;
	}
}
