package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.service.MagicNotifyService;

import static org.assertj.core.api.Assertions.assertThat;

class McpClusterBeanConditionTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MagicAPIMcpConfiguration.class);

	@Test
	void shouldNotCreateClusterPublisherWhenMagicApiClusterIsDisabled() {
		contextRunner.withBean(MagicAPIProperties.class, MagicAPIProperties::new)
				.withBean(MagicNotifyService.class, () -> magicNotify -> {
				})
				.run(context -> assertThat(context).doesNotHaveBean(McpChangeEventPublisher.class));
	}

	@Test
	void shouldCreateClusterPublisherOnlyWhenClusterPropertyAndMagicNotifyServiceArePresent() {
		contextRunner.withPropertyValues("magic-api.plugin.cluster.enabled=true")
				.withBean(MagicAPIProperties.class, MagicAPIProperties::new)
				.withBean(MagicNotifyService.class, () -> magicNotify -> {
				})
				.run(context -> assertThat(context).hasSingleBean(McpChangeEventPublisher.class));
	}
}
