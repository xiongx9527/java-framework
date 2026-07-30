package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AiModelAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AiModelAutoConfiguration.class));

	@Test
	public void shouldKeepNeutralInfrastructureWhenLangChain4jAdapterIsMissing() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.langchain4j.adapter"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.containsBean("aiModelFactory"));
					assertTrue(context.containsBean("aiModelRegistry"));
					assertTrue(context.getBeansOfType(AiModelAdapter.class).isEmpty());
				});
	}

	@Test
	public void shouldFailConfiguredModelWhenNoAdapterExists() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.model.models.chat.provider=test",
						"fw-plus.ai.model.models.chat.type=chat")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"No adapter supports")));
	}

	@Test
	public void shouldAutoDiscoverTheOnlySupportingAdapter() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.model.default-model=chat",
						"fw-plus.ai.model.models.chat.provider=test",
						"fw-plus.ai.model.models.chat.type=chat")
				.withBean(AiModelAdapter.class, () -> new FakeModelAdapter("langchain4j"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					AiModelRegistry registry = context.getBean(AiModelRegistry.class);
					assertEquals("langchain4j", registry.getDefault().getFramework());
				});
	}

	@Test
	public void shouldRejectAmbiguousAdaptersWhenFrameworkIsMissing() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.model.models.chat.provider=test",
						"fw-plus.ai.model.models.chat.type=chat")
				.withBean("langChain4jFakeAdapter", AiModelAdapter.class,
						() -> new FakeModelAdapter("langchain4j"))
				.withBean("springAiFakeAdapter", AiModelAdapter.class,
						() -> new FakeModelAdapter("spring-ai"))
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"Multiple adapters support")));
	}

	@Test
	public void shouldSelectExplicitLangChain4jFramework() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.model.default-model=chat",
						"fw-plus.ai.model.models.chat.framework=langchain4j",
						"fw-plus.ai.model.models.chat.provider=test",
						"fw-plus.ai.model.models.chat.type=chat")
				.withBean("langChain4jFakeAdapter", AiModelAdapter.class,
						() -> new FakeModelAdapter("langchain4j"))
				.withBean("springAiFakeAdapter", AiModelAdapter.class,
						() -> new FakeModelAdapter("spring-ai"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertEquals("langchain4j", context.getBean(AiModelRegistry.class)
							.getDefault().getFramework());
				});
	}

	@Test
	public void shouldIgnoreLegacyLangChain4jModelPrefix() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.langchain4j.model.default-model=legacy",
						"fw-plus.ai.langchain4j.model.models.legacy.provider=test",
						"fw-plus.ai.langchain4j.model.models.legacy.model-type=chat")
				.withBean(AiModelAdapter.class, () -> new FakeModelAdapter("langchain4j"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					AiModelRegistry registry = context.getBean(AiModelRegistry.class);
					assertTrue(registry.keys().isEmpty());
					assertFalse(context.containsBean("langChain4jModelRegistry"));
				});
	}

	private boolean hasCause(Throwable throwable, String message) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof AiModelException && current.getMessage() != null
					&& current.getMessage().contains(message)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static final class FakeModelAdapter implements AiModelAdapter {

		private final String framework;

		private FakeModelAdapter(String framework) {
			this.framework = framework;
		}

		@Override
		public String getFramework() {
			return framework;
		}

		@Override
		public boolean supports(AiModelDefinition definition) {
			return definition != null && "test".equals(definition.getProvider());
		}

		@Override
		public AiModel create(final AiModelDefinition definition) {
			return new AiModel() {
				@Override public String getKey() { return definition.getKey(); }
				@Override public String getFramework() { return framework; }
				@Override public String getProvider() { return definition.getProvider(); }
				@Override public AiModelType getType() { return definition.getType(); }
			};
		}
	}
}
