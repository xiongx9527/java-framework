package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowResourceDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.MagicApiTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.DefaultTinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryChainDefinitionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryTinyflowChainStateRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.chain.repository.InMemoryNodeStateRepository;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import dev.tinyflow.core.parser.ChainParser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.time.Duration;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TinyflowResourceDynamicRegistryTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class, TinyflowAutoConfiguration.class));

	private TinyflowWorkflowRepository workflows;
	private InMemoryChainDefinitionRepository definitions;
	private TinyflowResourceDynamicRegistry registry;

	@Before
	public void setUp() {
		TinyflowWorkflowValidator validator = new TinyflowWorkflowValidator(ChainParser.builder().build());
		workflows = new MagicApiTinyflowWorkflowRepository();
		definitions = new InMemoryChainDefinitionRepository();
		TinyflowService service = new DefaultTinyflowService(validator, workflows, definitions,
				new ChainExecutor(definitions, new InMemoryTinyflowChainStateRepository(), new InMemoryNodeStateRepository()),
				Duration.ofSeconds(2));
		registry = new TinyflowResourceDynamicRegistry(new TinyflowMagicResourceStorage(validator), service);
	}

	@Test
	public void shouldReplaceDefaultWorkflowRepositoryInMagicApiContext() {
		contextRunner.run(context -> {
			assertEquals(1, context.getBeansOfType(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository.class).size());
			assertTrue(context.getBean(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository.class)
					instanceof MagicApiTinyflowWorkflowRepository);
		});
	}

	@Test
	public void shouldRegisterWorkflowAndResolveItByResourcePath() {
		registry.onSaved(resource("flow-id", "orders/create", validWorkflow()));

		TinyflowWorkflow workflow = workflows.findByPath("orders/create");
		assertSame(workflow, workflows.findById("flow-id"));
		assertEquals("flow-id", registry.getMapping("orders/create").getId());
	}

	@Test
	public void shouldReplacePathMappingWhenResourceIsRenamed() {
		registry.onSaved(resource("flow-id", "orders/create", validWorkflow()));
		registry.onSaved(resource("flow-id", "orders/create-v2", validWorkflow()));

		assertNull(workflows.findByPath("orders/create"));
		assertSame(workflows.findById("flow-id"), workflows.findByPath("orders/create-v2"));
		assertNull(registry.getMapping("orders/create"));
		assertEquals("flow-id", registry.getMapping("orders/create-v2").getId());
	}

	@Test
	public void shouldRemoveWorkflowAfterResourceIsDeleted() {
		TinyflowResource resource = resource("flow-id", "orders/create", validWorkflow());
		registry.onSaved(resource);

		registry.onDeleted(resource);

		assertNull(workflows.findById("flow-id"));
		assertNull(definitions.getChainDefinitionById("flow-id"));
	}

	@Test
	public void shouldRejectAmbiguousPaths() {
		registry.onSaved(resource("flow-a", "orders/create", validWorkflow()));

		assertThrows(TinyflowException.class,
				() -> registry.onSaved(resource("flow-b", "orders/create", validWorkflow())));
	}

	@Test
	public void shouldKeepPreviousDefinitionWhenUpdatedJsonIsInvalid() {
		registry.onSaved(resource("flow-id", "orders/create", validWorkflow()));
		Object previousDefinition = definitions.getChainDefinitionById("flow-id");

		assertThrows(TinyflowException.class,
				() -> registry.onSaved(resource("flow-id", "orders/create", "{")));

		assertSame(previousDefinition, definitions.getChainDefinitionById("flow-id"));
		assertEquals(validWorkflow(), workflows.findById("flow-id").getJson());
	}

	private TinyflowResource resource(String id, String path, String json) {
		TinyflowResource resource = new TinyflowResource();
		resource.setId(id);
		resource.setName(id);
		resource.setPath(path);
		resource.setContent(json);
		return resource;
	}

	private String validWorkflow() {
		return "{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\"},"
				+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
				+ "\"edges\":[{\"id\":\"e1\",\"source\":\"start\",\"target\":\"end\"}]}";
	}
}
