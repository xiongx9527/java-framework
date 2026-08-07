package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter.MagicAPITinyflowConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.web.MagicTinyflowController;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.utils.Mapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}
