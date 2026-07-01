package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server.MagicAPIAdminServerConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.starter.MagicAPIClusterConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter.MagicAPIConfAutoConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.starter.MagicAPIDebeziumConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter.MagicAPIDruidConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter.MagicAPIDtpConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.starter.MagicAPIElasticSearchConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.starter.MagicAPIFreemarkerConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.starter.MagicAPIInfluxConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.starter.MagicAPIKafkaConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.starter.MagicAPIMongoConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.starter.MagicAPIMqttConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.starter.MagicAPIMqttClientConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.starter.MagicAPIPulsarConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.starter.MagicAPIRabbitMqConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.starter.MagicAPIRedisConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.starter.MagicAPIRocketMqConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.starter.MagicAPIS3Configuration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.starter.MagicAPISaTokenConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.starter.MagicAPISseConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicSwaggerConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.starter.MagicAPITaskConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.starter.MagicAPIVueConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter.MagicAPIWsConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MagicAPIConfAutoConfiguration.class,
         MagicAPIFreemarkerConfiguration.class,
         MagicAPIVueConfiguration.class,
         MagicSwaggerConfiguration.class,
         MagicAPIElasticSearchConfiguration.class,
         MagicAPIRedisConfiguration.class,
         MagicAPIMongoConfiguration.class,
         MagicAPIRocketMqConfiguration.class,
         MagicAPIRabbitMqConfiguration.class,
         MagicAPIKafkaConfiguration.class,
         MagicAPIPulsarConfiguration.class,
         MagicAPIDebeziumConfiguration.class,
         MagicAPIS3Configuration.class,
         MagicAPITaskConfiguration.class,
         MagicAPIDtpConfiguration.class,
         MagicAPIWsConfiguration.class,
         MagicAPISseConfiguration.class,
         MagicAPIClusterConfiguration.class,
         MagicAPIMqttConfiguration.class,
         MagicAPIMqttClientConfiguration.class,
         MagicAPIDruidConfiguration.class,
         MagicAPIInfluxConfiguration.class,
         MagicAPIAdminServerConfiguration.class,
         MagicAPISaTokenConfiguration.class,

})
public class MagicApiPluginAutoConfiguration {
}
