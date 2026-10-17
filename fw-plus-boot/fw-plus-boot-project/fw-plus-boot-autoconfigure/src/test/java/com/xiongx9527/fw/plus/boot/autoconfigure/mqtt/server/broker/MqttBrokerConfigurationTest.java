package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDownReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageExchangeReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttServerManage;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.config.MqttBrokerConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.service.IMqttBrokerService;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import org.dromara.mica.mqtt.core.server.event.IMqttConnectStatusListener;
import org.dromara.mica.mqtt.core.server.serializer.IMessageSerializer;
import org.dromara.mica.mqtt.core.server.serializer.JsonMessageSerializer;
import org.dromara.mica.mqtt.core.server.store.IMqttMessageStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MqttBrokerConfigurationTest {

	@Test
	void brokerBeansBackOffForUserDispatcher() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MqttBrokerConfiguration.class))
				.withUserConfiguration(UserDispatcherConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(IMqttMessageDispatcher.class);
					assertThat(context).doesNotHaveBean(RedisMqttMessageDispatcher.class);
				});
	}

	@Test
	void dispatcherFailsClearlyWhenNoRedisStreamTemplateCanBeResolved() {
		RedisMqttMessageDispatcher dispatcher =
				new RedisMqttMessageDispatcher(new JsonMessageSerializer(), "mqtt:exchange");

		assertThatThrownBy(dispatcher::afterSingletonsInstantiated)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RStreamTemplate");
	}

	@Test
	void exchangeReceiverFailsClearlyWhenNoRedisConnectionFactoryCanBeResolved() {
		RedisMqttMessageExchangeReceiver receiver = new RedisMqttMessageExchangeReceiver(
				new JsonMessageSerializer(), MqttServer.create().port(0).build(), "mqtt:exchange");

		assertThatThrownBy(receiver::afterSingletonsInstantiated)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RedisConnectionFactory");
	}

	@Test
	void downReceiverFailsClearlyWhenNoRedisConnectionFactoryCanBeResolved() {
		RedisMqttMessageDownReceiver receiver = new RedisMqttMessageDownReceiver(
				new JsonMessageSerializer(), MqttServer.create().port(0).build(), "mqtt:down");

		assertThatThrownBy(receiver::afterSingletonsInstantiated)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RedisConnectionFactory");
	}

	@Configuration(proxyBeanMethods = false)
	static class UserDispatcherConfiguration {

		@Bean
		IMessageSerializer messageSerializer() {
			return new JsonMessageSerializer();
		}

		@Bean
		IMqttMessageDispatcher customDispatcher() {
			return mock(IMqttMessageDispatcher.class);
		}

		@Bean
		IMqttConnectStatusListener customConnectStatusListener() {
			return mock(IMqttConnectStatusListener.class);
		}

		@Bean
		IMqttMessageStore customMessageStore() {
			return mock(IMqttMessageStore.class);
		}

		@Bean
		RedisMqttServerManage customMqttServerManage() {
			return mock(RedisMqttServerManage.class);
		}

		@Bean
		IMqttBrokerService customBrokerService() {
			return mock(IMqttBrokerService.class);
		}

		@Bean
		MqttServer mqttServer() {
			return MqttServer.create().port(0).build();
		}

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

		@Bean
		RedisMqttMessageExchangeReceiver customExchangeReceiver() {
			return mock(RedisMqttMessageExchangeReceiver.class);
		}

		@Bean
		RedisMqttMessageDownReceiver customDownReceiver() {
			return mock(RedisMqttMessageDownReceiver.class);
		}
	}
}
