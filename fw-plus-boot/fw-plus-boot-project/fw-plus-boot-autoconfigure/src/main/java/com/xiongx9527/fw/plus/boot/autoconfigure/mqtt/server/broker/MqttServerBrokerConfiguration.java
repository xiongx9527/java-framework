package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.config.MqttBrokerConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {"net.dreamlu.mica.redis.cache.MicaRedisCache"})
@ConditionalOnBean(RedisConnectionFactory.class)
@Import({MqttBrokerConfiguration.class})
public class MqttServerBrokerConfiguration {
}
