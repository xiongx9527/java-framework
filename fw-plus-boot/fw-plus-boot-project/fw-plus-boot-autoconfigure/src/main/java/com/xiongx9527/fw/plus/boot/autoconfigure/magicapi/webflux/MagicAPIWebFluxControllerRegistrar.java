package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.web.MagicConfigController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.web.MagicDbBrowserController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.web.MagicDebeziumController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.web.MagicDtpController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.web.MagicElasticsearchController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.web.MagicFreemarkerController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.web.MagicInfluxController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.web.MagicKafkaController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.web.MagicMongoController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.web.MagicMqttController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.web.MagicMqttClientController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.web.MagicPulsarController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.web.MagicRabbitMqController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.web.MagicRedisController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.web.MagicRocketMqController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.web.MagicS3Controller;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.webflux.MagicSwaggerWebFluxController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.web.MagicVueController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web.MagicWsController;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.ssssssss.magicapi.backup.web.MagicBackupController;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicResourceController;
import org.ssssssss.magicapi.core.web.MagicWorkbenchController;
import org.ssssssss.magicapi.datasource.web.MagicDataSourceController;
import org.ssssssss.magicapi.task.web.MagicTaskController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MagicAPIWebFluxControllerRegistrar implements SmartInitializingSingleton {

	private final MagicAPIWebFluxMapping mapping;
	private final MagicConfiguration configuration;
	private final MagicAPIProperties properties;
	private final List<MagicPluginConfiguration> pluginConfigurations;
	private final ApplicationContext applicationContext;

	public MagicAPIWebFluxControllerRegistrar(MagicAPIWebFluxMapping mapping,
											  MagicConfiguration configuration,
											  MagicAPIProperties properties,
											  List<MagicPluginConfiguration> pluginConfigurations,
											  ApplicationContext applicationContext) {
		this.mapping = mapping;
		this.configuration = configuration;
		this.properties = properties;
		this.pluginConfigurations = pluginConfigurations == null
				? new ArrayList<>()
				: pluginConfigurations;
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		List<Plugin> plugins = pluginConfigurations.stream()
				.map(MagicPluginConfiguration::plugin)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		mapping.registerController(new MagicWorkbenchController(configuration, properties, plugins))
				.registerController(new MagicResourceController(configuration))
				.registerController(new MagicDataSourceController(configuration))
				.registerController(new MagicBackupController(configuration));
		for (Plugin plugin : plugins) {
			registerPlugin(plugin.getName());
		}
	}

	private void registerPlugin(String name) {
		if ("config".equals(name)) {
			mapping.registerController(new MagicConfigController(configuration));
		} else if ("freemarker".equals(name)) {
			mapping.registerController(new MagicFreemarkerController(configuration));
		} else if ("vue3".equals(name)) {
			mapping.registerController(new MagicVueController(configuration, properties, new VueModule(properties)));
		} else if ("elasticsearch".equals(name)) {
			mapping.registerController(new MagicElasticsearchController(configuration));
		} else if ("redis".equals(name)) {
			mapping.registerController(new MagicRedisController(configuration));
		} else if ("mongo".equals(name)) {
			mapping.registerController(new MagicMongoController(configuration));
		} else if ("rocketmq".equals(name)) {
			mapping.registerController(new MagicRocketMqController(configuration));
		} else if ("rabbitmq".equals(name)) {
			mapping.registerController(new MagicRabbitMqController(configuration));
		} else if ("kafka".equals(name)) {
			mapping.registerController(new MagicKafkaController(configuration));
		} else if ("pulsar".equals(name)) {
			mapping.registerController(new MagicPulsarController(configuration));
		} else if ("db".equals(name)) {
			DbBrowserService service = applicationContext.getBeanProvider(DbBrowserService.class).getIfAvailable();
			if (service != null) {
				mapping.registerController(new MagicDbBrowserController(configuration, service));
			}
		} else if ("debezium".equals(name)) {
			mapping.registerController(new MagicDebeziumController(configuration));
		} else if ("s3".equals(name)) {
			mapping.registerController(new MagicS3Controller(configuration));
		} else if ("task".equals(name)) {
			mapping.registerController(new MagicTaskController(configuration));
		} else if ("dtp".equals(name)) {
			MagicDynamicDtpDataSource dataSource = applicationContext.getBeanProvider(MagicDynamicDtpDataSource.class)
					.getIfAvailable();
			if (dataSource != null) {
				mapping.registerController(new MagicDtpController(configuration, dataSource));
			}
		} else if ("ws".equals(name)) {
			mapping.registerController(new MagicWsController(configuration));
		} else if ("mqtt".equals(name)) {
			mapping.registerController(new MagicMqttController(configuration));
		} else if ("mqttclient".equals(name)) {
			mapping.registerController(new MagicMqttClientController(configuration));
		} else if ("influx".equals(name)) {
			mapping.registerController(new MagicInfluxController(configuration));
		} else if ("swagger".equals(name)) {
			SwaggerConfig swaggerConfig = applicationContext.getBeanProvider(SwaggerConfig.class).getIfAvailable();
			MagicResourceService magicResourceService = applicationContext.getBeanProvider(MagicResourceService.class)
					.getIfAvailable();
			if (swaggerConfig != null && magicResourceService != null) {
				mapping.registerController(new MagicSwaggerWebFluxController(swaggerConfig, magicResourceService,
						properties, applicationContext), "");
			}
		}
	}
}
