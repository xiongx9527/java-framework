package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowResourceDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter.MagicAPITinyflowConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter.MagicAPITinyflowRuntimeAutoConfiguration;
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TinyflowResourceDynamicRegistryTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class, MagicAPITinyflowConfiguration.class,
					TinyflowAutoConfiguration.class,
					MagicAPITinyflowRuntimeAutoConfiguration.class))
			.withBean(MagicResourceService.class, TinyflowResourceDynamicRegistryTest::magicResourceService);

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
			assertAutoConfigurationMatches(context, MagicAPITinyflowConfiguration.class);
			assertAutoConfigurationMatches(context, MagicAPITinyflowRuntimeAutoConfiguration.class);
			assertEquals(1, context.getBeansOfType(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository.class).size());
			assertTrue(context.getBean(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository.class)
					instanceof MagicApiTinyflowWorkflowRepository);
		});
	}

	@Test
	public void shouldRunTinyflowConfigurationsAfterBootMagicApiAutoConfiguration() {
		String bootMagicApiConfiguration = "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration";
		assertTrue(Arrays.asList(MagicAPITinyflowConfiguration.class.getAnnotation(AutoConfigureAfter.class).name())
				.contains(bootMagicApiConfiguration));
		assertTrue(Arrays.asList(MagicAPITinyflowRuntimeAutoConfiguration.class
				.getAnnotation(AutoConfigureAfter.class).name()).contains(bootMagicApiConfiguration));
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
	public void shouldRejectResourcesWithoutIdBeforeRuntimeRegistration() {
		TinyflowResource resource = resource(null, "orders/create", validWorkflow());

		assertThrows(TinyflowException.class, () -> registry.onSaved(resource));
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

	@Test
	public void shouldReplaceRuntimeStateWhenFileSaveUpdatesTheSameIdAndPath() {
		registry.onSaved(resource("flow-id", "orders/create", validWorkflow()));
		Object previousDefinition = definitions.getChainDefinitionById("flow-id");
		String updatedWorkflow = validWorkflowWithDifferentEdge();

		registry.onFileEvent(new FileEvent("tinyflow", EventAction.SAVE,
				resource("flow-id", "orders/create", updatedWorkflow)));

		assertEquals(updatedWorkflow, registry.getMapping("orders/create").getContent());
		assertEquals(updatedWorkflow, workflows.findById("flow-id").getJson());
		assertTrue(previousDefinition != definitions.getChainDefinitionById("flow-id"));
	}

	@Test
	public void shouldKeepMappingWorkflowAndDefinitionWhenFileSaveIsInvalid() {
		TinyflowResource original = resource("flow-id", "orders/create", validWorkflow());
		registry.onSaved(original);
		Object previousDefinition = definitions.getChainDefinitionById("flow-id");

		assertThrows(TinyflowException.class, () -> registry.onFileEvent(new FileEvent("tinyflow", EventAction.SAVE,
				resource("flow-id", "orders/create", "{"))));

		assertSame(original, registry.getMapping("orders/create"));
		assertEquals(validWorkflow(), workflows.findById("flow-id").getJson());
		assertSame(previousDefinition, definitions.getChainDefinitionById("flow-id"));
	}

	@Test
	public void shouldNotCreateMagicTinyflowBeansWhenCoreTinyflowIsDisabled() {
		contextRunner.withPropertyValues("fw.plus.ai.tinyflow.enabled=false")
				.run(context -> assertNoMagicTinyflowBeans(context));
	}

	@Test
	public void shouldNotCreateMagicTinyflowBeansWhenPluginIsDisabled() {
		contextRunner.withPropertyValues("magic-api.plugin.tinyflow.enabled=false")
				.run(context -> assertNoMagicTinyflowBeans(context));
	}

	@Test
	public void shouldNotCreateMagicTinyflowBeansWhenTinyflowCoreIsMissing() {
		contextRunner.withClassLoader(new FilteredClassLoader(ChainExecutor.class))
				.run(context -> assertNoMagicTinyflowBeans(context));
	}

	@Test
	public void shouldAllowRepositoryOnlyWhenCoreServiceIsMissing() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class, MagicAPITinyflowConfiguration.class,
						MagicAPITinyflowRuntimeAutoConfiguration.class))
				.withBean(MagicResourceService.class, TinyflowResourceDynamicRegistryTest::magicResourceService)
				.run(context -> {
					assertTrue(context.containsBean("magicApiTinyflowWorkflowRepository"));
					assertNoMagicTinyflowRuntimeBeans(context);
				});
	}

	@Test
	public void shouldNotCreateMagicTinyflowBeansWhenMagicServiceIsMissing() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class, MagicAPITinyflowConfiguration.class,
						TinyflowAutoConfiguration.class,
						MagicAPITinyflowRuntimeAutoConfiguration.class))
				.run(context -> assertNoMagicTinyflowBeans(context));
	}

	@Test
	public void shouldBackOffMagicWorkflowRepositoryForCustomRepository() {
		TinyflowWorkflowRepository customRepository = new MagicApiTinyflowWorkflowRepository();

		contextRunner.withBean(TinyflowWorkflowRepository.class, () -> customRepository)
				.run(context -> {
					assertEquals(1, context.getBeansOfType(TinyflowWorkflowRepository.class).size());
					assertSame(customRepository, context.getBean(TinyflowWorkflowRepository.class));
				});
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

	private String validWorkflowWithDifferentEdge() {
		return "{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\"},"
				+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
				+ "\"edges\":[{\"id\":\"e2\",\"source\":\"start\",\"target\":\"end\"}]}";
	}

	private void assertNoMagicTinyflowBeans(org.springframework.context.ApplicationContext context) {
		assertFalse(context.containsBean("magicApiTinyflowWorkflowRepository"));
		assertNoMagicTinyflowRuntimeBeans(context);
	}

	private void assertNoMagicTinyflowRuntimeBeans(org.springframework.context.ApplicationContext context) {
		assertFalse(context.containsBean("tinyflowMagicResourceStorage"));
		assertFalse(context.containsBean("tinyflowResourceDynamicRegistry"));
	}

	private void assertAutoConfigurationMatches(org.springframework.context.ConfigurableApplicationContext context,
			Class<?> autoConfigurationClass) {
		ConditionEvaluationReport.ConditionAndOutcomes outcomes = ConditionEvaluationReport.get(context.getBeanFactory())
				.getConditionAndOutcomesBySource().get(autoConfigurationClass.getName());
		assertTrue("Expected auto-configuration to match: " + autoConfigurationClass.getName(),
				outcomes != null && outcomes.isFullMatch());
	}

	private static MagicResourceService magicResourceService() {
		return (MagicResourceService) Proxy.newProxyInstance(MagicResourceService.class.getClassLoader(),
				new Class<?>[]{MagicResourceService.class}, (proxy, method, arguments) -> null);
	}

}
