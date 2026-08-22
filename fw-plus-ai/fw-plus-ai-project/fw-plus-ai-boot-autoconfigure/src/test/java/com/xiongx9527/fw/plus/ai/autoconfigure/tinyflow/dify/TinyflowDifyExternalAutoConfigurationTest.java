package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowKnowledgeLifecycle;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeManagerAdapter;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.local.FwPlusLocalKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval.TinyflowLocalRetriever;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.dify.dataset.IKnowledgeService;
import dev.tinyflow.core.knowledge.KnowledgeManager;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TinyflowDifyExternalAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyflowDifyExternalAutoConfiguration.class));

	@Test
	public void shouldRemainDisabledByDefault() {
		withRequiredBeans(contextRunner).run(context -> {
			assertFalse(context.containsBean("tinyflowDifyExternalKnowledgeService"));
			assertFalse(context.containsBean("tinyflowDifyExternalController"));
		});
	}

	@Test
	public void shouldPublishValidatedDefaultsOnlyWhenExplicitlyEnabled() {
		withRequiredBeans(contextRunner).withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> {
					assertTrue(context.getBeansOfType(IKnowledgeService.class).size() == 1);
					assertTrue(context.getBeansOfType(TinyflowDifyExternalController.class).size() == 1);
					assertTrue(context.getBeansOfType(TinyflowKnowledgeManagerAdapter.class).size() == 1);
					assertTrue(context.getBeansOfType(TinyflowKnowledgeLifecycle.class).size() == 1);
					TinyflowDifyExternalProperties properties = context.getBean(TinyflowDifyExternalProperties.class);
					assertEquals("/external-knowledge", properties.getBasePath());
					assertEquals(Duration.ofSeconds(5), properties.getRequestTimeout());
					assertEquals(20, properties.getMaxTopK());
					assertEquals(60, properties.getRateLimit());
				});
	}

	@Test
	public void shouldBackOffDefaultServiceAndControllerForUserBeans() {
		TinyflowDifyExternalKnowledgeService service = mock(TinyflowDifyExternalKnowledgeService.class);
		TinyflowDifyExternalController controller = new TinyflowDifyExternalController(service);
		withRequiredBeans(contextRunner)
				.withBean(TinyflowDifyExternalKnowledgeService.class, () -> service)
				.withBean(TinyflowDifyExternalController.class, () -> controller)
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> {
					assertSame(service, context.getBean(TinyflowDifyExternalKnowledgeService.class));
					assertSame(controller, context.getBean(TinyflowDifyExternalController.class));
				});
	}

	@Test
	public void shouldNotCreateEndpointWhenAnyRequiredBeanIsMissing() {
		contextRunner.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> assertFalse(context.containsBean("tinyflowDifyExternalKnowledgeService")));
		contextRunner.withBean(TinyflowKnowledgeRepository.class, () -> mock(TinyflowKnowledgeRepository.class))
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> assertFalse(context.containsBean("tinyflowDifyExternalKnowledgeService")));
	}

	@Test
	public void shouldNotCreateEndpointWhenLocalProviderIsMissing() {
		contextRunner
				.withBean(TinyflowKnowledgeProviderRegistry.class,
						() -> new TinyflowKnowledgeProviderRegistry(Collections.<TinyflowKnowledgeProvider>emptyList()))
				.withBean(TinyflowKnowledgeRepository.class, () -> mock(TinyflowKnowledgeRepository.class))
				.withBean(TinyflowKnowledgeAccessKeyRepository.class,
						() -> mock(TinyflowKnowledgeAccessKeyRepository.class))
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> {
					assertFalse(context.getStartupFailure() != null);
					assertFalse(context.getBeansOfType(TinyflowDifyExternalKnowledgeService.class).size() > 0);
					assertFalse(context.getBeansOfType(TinyflowDifyExternalController.class).size() > 0);
				});
	}

	@Test
	public void shouldNotInstantiateRegistryBeforeBeanPostProcessorsAreActive() {
		AtomicInteger registryCreations = new AtomicInteger();
		AtomicInteger registryPostProcesses = new AtomicInteger();
		TinyflowKnowledgeRepository repository = mock(TinyflowKnowledgeRepository.class);
		FwPlusLocalKnowledgeProvider localProvider = localProvider(repository);

		contextRunner
				.withBean(BeanPostProcessor.class, () -> new BeanPostProcessor() {
					@Override public Object postProcessAfterInitialization(Object bean, String beanName) {
						if (bean instanceof TinyflowKnowledgeProviderRegistry) {
							registryPostProcesses.incrementAndGet();
						}
						return bean;
					}
				})
				.withBean(FwPlusLocalKnowledgeProvider.class, () -> localProvider)
				.withBean(TinyflowKnowledgeProviderRegistry.class, () -> {
					registryCreations.incrementAndGet();
					return new TinyflowKnowledgeProviderRegistry(Collections.singletonList(localProvider));
				})
				.withBean(TinyflowKnowledgeRepository.class, () -> repository)
				.withBean(TinyflowKnowledgeAccessKeyRepository.class,
						() -> mock(TinyflowKnowledgeAccessKeyRepository.class))
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> {
					assertFalse(context.getStartupFailure() != null);
					assertTrue(context.containsBean("tinyflowDifyExternalController"));
					assertEquals(1, registryCreations.get());
					assertEquals(1, registryPostProcesses.get());
				});
	}

	@Test
	public void shouldNotLoadWhenDifyKnowledgeContractIsMissing() {
		withRequiredBeans(contextRunner).withClassLoader(new FilteredClassLoader(IKnowledgeService.class))
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true")
				.run(context -> assertFalse(context.containsBean("tinyflowDifyExternalKnowledgeService")));
	}

	@Test
	public void shouldRejectInvalidPropertiesBeforePublishingEndpoint() {
		assertInvalid("fw.plus.ai.tinyflow.dify.external.base-path=relative");
		assertInvalid("fw.plus.ai.tinyflow.dify.external.request-timeout=0ms");
		assertInvalid("fw.plus.ai.tinyflow.dify.external.max-top-k=0");
		assertInvalid("fw.plus.ai.tinyflow.dify.external.rate-limit=0");
	}

	@Test
	public void shouldUnregisterTheSameKnowledgeAdapterAfterConsecutiveContextsClose() {
		int initial = KnowledgeManager.getInstance().providers.size();
		ApplicationContextRunner enabled = withRequiredBeans(contextRunner)
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true");
		enabled.run(context -> {
			TinyflowKnowledgeManagerAdapter adapter = context.getBean(TinyflowKnowledgeManagerAdapter.class);
			assertTrue(KnowledgeManager.getInstance().providers.contains(adapter));
			assertEquals(initial + 1, KnowledgeManager.getInstance().providers.size());
		});
		assertEquals(initial, KnowledgeManager.getInstance().providers.size());
		enabled.run(context -> assertEquals(initial + 1, KnowledgeManager.getInstance().providers.size()));
		assertEquals(initial, KnowledgeManager.getInstance().providers.size());
	}

	private void assertInvalid(String property) {
		withRequiredBeans(contextRunner)
				.withPropertyValues("fw.plus.ai.tinyflow.dify.external.enabled=true", property)
				.run(context -> assertTrue(context.getStartupFailure() != null));
	}

	private ApplicationContextRunner withRequiredBeans(ApplicationContextRunner runner) {
		TinyflowKnowledgeRepository repository = mock(TinyflowKnowledgeRepository.class);
		FwPlusLocalKnowledgeProvider local = localProvider(repository);
		return runner
				.withBean(FwPlusLocalKnowledgeProvider.class, () -> local)
				.withBean(TinyflowKnowledgeProviderRegistry.class,
						() -> new TinyflowKnowledgeProviderRegistry(Collections.singletonList(local)))
				.withBean(TinyflowKnowledgeRepository.class, () -> repository)
				.withBean(TinyflowKnowledgeAccessKeyRepository.class,
						() -> mock(TinyflowKnowledgeAccessKeyRepository.class));
	}

	private FwPlusLocalKnowledgeProvider localProvider(TinyflowKnowledgeRepository repository) {
		TinyflowLocalRetriever retriever = new TinyflowLocalRetriever(repository,
				mock(TinyflowModelProvider.class), mock(TinyflowVectorStoreProvider.class), null);
		return new FwPlusLocalKnowledgeProvider(repository, retriever);
	}
}
