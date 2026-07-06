package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.condition;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyConditionTest {

	@Test
	void existingPropertyShouldMatchWhenAllPropertiesHaveText() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("redis.one", "enabled")
				.withProperty("redis.two", "true");

		ConditionOutcome outcome = new OnExistingPropertyCondition()
				.getMatchOutcome(context(environment), metadata(ExistingPropertiesConfig.class));

		assertThat(outcome.isMatch()).isTrue();
	}

	@Test
	void existingPropertyShouldNotMatchWhenAnyPropertyIsBlank() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("redis.one", "enabled")
				.withProperty("redis.two", "   ");

		ConditionOutcome outcome = new OnExistingPropertyCondition()
				.getMatchOutcome(context(environment), metadata(ExistingPropertiesConfig.class));

		assertThat(outcome.isMatch()).isFalse();
	}

	@Test
	void missingPropertyShouldMatchWhenPropertiesAreMissingOrBlank() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("redis.two", "   ");

		ConditionOutcome outcome = new OnMissingPropertyCondition()
				.getMatchOutcome(context(environment), metadata(MissingPropertiesConfig.class));

		assertThat(outcome.isMatch()).isTrue();
	}

	@Test
	void missingPropertyShouldNotMatchWhenAnyPropertyHasText() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("redis.one", "enabled");

		ConditionOutcome outcome = new OnMissingPropertyCondition()
				.getMatchOutcome(context(environment), metadata(MissingPropertiesConfig.class));

		assertThat(outcome.isMatch()).isFalse();
	}

	@Test
	void existingPropertyShouldNotMatchWhenMetadataHasNoAnnotation() {
		ConditionOutcome outcome = new OnExistingPropertyCondition()
				.getMatchOutcome(context(new MockEnvironment()), metadata(PlainConfig.class));

		assertThat(outcome.isMatch()).isFalse();
	}

	@Test
	void missingPropertyShouldNotMatchWhenMetadataHasNoAnnotation() {
		ConditionOutcome outcome = new OnMissingPropertyCondition()
				.getMatchOutcome(context(new MockEnvironment()), metadata(PlainConfig.class));

		assertThat(outcome.isMatch()).isFalse();
	}

	private static ConditionContext context(MockEnvironment environment) {
		ConditionContext context = mock(ConditionContext.class);
		when(context.getEnvironment()).thenReturn(environment);
		when(context.getResourceLoader()).thenReturn(new DefaultResourceLoader());
		return context;
	}

	private static AnnotationMetadata metadata(Class<?> type) {
		return AnnotationMetadata.introspect(type);
	}

	@ConditionalOnExistingProperty({"redis.one", "redis.two"})
	static class ExistingPropertiesConfig {
	}

	@ConditionalOnMissingProperty({"redis.one", "redis.two"})
	static class MissingPropertiesConfig {
	}

	static class PlainConfig {
	}
}
