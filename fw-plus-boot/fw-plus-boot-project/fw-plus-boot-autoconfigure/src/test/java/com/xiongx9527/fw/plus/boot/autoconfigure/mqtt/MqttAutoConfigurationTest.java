package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.MqttAuthHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.MqttPublishPermission;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.MqttSubscribeValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.MqttServerCreator;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerAuthHandler;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerPublishPermission;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import net.dreamlu.iot.mqtt.spring.client.config.MqttClientConfiguration;
import net.dreamlu.iot.mqtt.spring.server.MqttServerTemplate;
import net.dreamlu.iot.mqtt.spring.server.config.MqttServerLauncher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MqttAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MqttAutoConfiguration.class));

	@Test
	void serverBacksOffWhenDisabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=false", "mqtt.client.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MqttServerCreator.class)
						.doesNotHaveBean(MqttServerTemplate.class)
						.doesNotHaveBean(MqttAuthHandler.class));
	}

	@Test
	void serverCreatesDefaultSecurityAndMemoryBeansWhenEnabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=true", "mqtt.client.enabled=false")
				.withUserConfiguration(ServerNetworkIsolationConfiguration.class)
				.run(context -> assertThat(context)
						.hasSingleBean(FwPlusMqttServerSecurityProperties.class)
						.hasSingleBean(IMqttServerAuthHandler.class)
						.hasSingleBean(IMqttServerPublishPermission.class)
						.hasSingleBean(IMqttServerSubscribeValidator.class)
						.hasSingleBean(IMqttSessionManager.class)
						.hasSingleBean(IMqttMessageStore.class)
						.hasSingleBean(MqttServerTemplate.class));
	}

	@Test
	void serverBacksOffForUserSecurityBeans() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=true", "mqtt.client.enabled=false")
				.withUserConfiguration(ServerNetworkIsolationConfiguration.class, UserSecurityConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(IMqttServerAuthHandler.class);
					assertThat(context).hasSingleBean(IMqttServerPublishPermission.class);
					assertThat(context).hasSingleBean(IMqttServerSubscribeValidator.class);
					assertThat(context).doesNotHaveBean(MqttAuthHandler.class);
					assertThat(context).doesNotHaveBean(MqttPublishPermission.class);
					assertThat(context).doesNotHaveBean(MqttSubscribeValidator.class);
				});
	}

	@Test
	void clientImportsMicaClientConfigurationWhenEnabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=false", "mqtt.client.enabled=true")
				.withUserConfiguration(ClientNetworkIsolationConfiguration.class)
				.run(context -> assertThat(context)
						.hasSingleBean(MqttClientConfiguration.class)
						.hasSingleBean(MqttClientTemplate.class));
	}

	@Test
	void clientBacksOffWhenDisabled() {
		contextRunner
				.withPropertyValues("mqtt.server.enabled=false", "mqtt.client.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MqttClientConfiguration.class)
						.doesNotHaveBean(MqttClientTemplate.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ServerNetworkIsolationConfiguration {

		@Bean
		MqttServer mqttServer() {
			return MqttServer.create().port(0).build();
		}

		@Bean
		MqttServerLauncher mqttServerLauncher() {
			return mock(MqttServerLauncher.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ClientNetworkIsolationConfiguration {

		@Bean
		MqttClientTemplate mqttClientTemplate() {
			return mock(MqttClientTemplate.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class UserSecurityConfiguration {

		@Bean
		IMqttServerAuthHandler customAuthHandler() {
			return mock(IMqttServerAuthHandler.class);
		}

		@Bean
		IMqttServerPublishPermission customPublishPermission() {
			return mock(IMqttServerPublishPermission.class);
		}

		@Bean
		IMqttServerSubscribeValidator customSubscribeValidator() {
			return mock(IMqttServerSubscribeValidator.class);
		}
	}
}
