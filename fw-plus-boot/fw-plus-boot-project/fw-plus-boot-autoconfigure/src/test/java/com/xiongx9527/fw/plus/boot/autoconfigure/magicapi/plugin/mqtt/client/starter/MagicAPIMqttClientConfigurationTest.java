package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service.MqttClientDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIMqttClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					MagicAPIMqttClientConfiguration.class,
					ResourceServiceConfiguration.class);

	@Test
	void createsStorageBeforeServiceAndLetsCoreBackfillService() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(MagicResourceService.class);
			assertThat(context).hasSingleBean(MqttClientDataSourceInfoMagicResourceStorage.class);

			MagicResourceService service = context.getBean(MagicResourceService.class);
			MqttClientDataSourceInfoMagicResourceStorage storage =
					context.getBean(MqttClientDataSourceInfoMagicResourceStorage.class);
			assertThat(ReflectionTestUtils.getField(storage, "magicResourceService"))
					.isSameAs(service);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceServiceConfiguration {

		@Bean
		MagicResourceService magicResourceService(
				ObjectProvider<List<MagicResourceStorage<? extends MagicEntity>>> storagesProvider,
				ApplicationEventPublisher publisher) {
			return new MyDefaultMagicResourceService(
					mock(Resource.class), storagesProvider.getObject(), publisher);
		}
	}
}
