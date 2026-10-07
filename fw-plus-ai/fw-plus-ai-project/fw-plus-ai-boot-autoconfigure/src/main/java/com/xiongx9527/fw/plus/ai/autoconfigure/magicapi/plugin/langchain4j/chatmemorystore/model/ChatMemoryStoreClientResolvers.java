package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.DisposableWorkerIdAssigner;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.common.uid.impl.DefaultUidGenerator;
import org.elasticsearch.client.RestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 可选存储客户端（jdbc/redis/mongo/es）的懒解析辅助，<b>不是 Spring bean</b>。
 *
 * <p><b>为什么必须与 bean 类隔离：</b>Spring 创建每个 bean 时会对 bean 类调用
 * {@link Class#getDeclaredMethods()}（如 {@code PersistenceAnnotationBeanPostProcessor}
 * 的 merged bean definition 后置处理），该调用会解析该类<b>全部方法的描述符</b>
 * （擦除后的参数/返回类型），包括编译器为 lambda 生成的合成方法。只要其中任何一处
 * 引用了 classpath 上缺失的类，就会在<b>应用启动期</b>抛
 * {@code NoClassDefFoundError}，导致整个应用启动失败——而这些依赖（spring-data-redis、
 * spring-data-mongodb、elasticsearch、spring-jdbc、fw-plus-boot-autoconfigure）在
 * autoconfigure 里均为 optional，使用方不一定引。</p>
 *
 * <p>本类仅在用户实际选用对应存储分支时才会被类加载与调用：类缺失时错误发生在对应
 * 功能的调用点（快速失败并给出可读提示），启动期与其余分支完全不受影响。</p>
 */
final class ChatMemoryStoreClientResolvers {

	private ChatMemoryStoreClientResolvers() {
	}

	/** 按类型懒取容器 Bean（缺失返回 null）。 */
	static <T> T optionalBean(ApplicationContext context, Class<T> type) {
		return context.getBeanProvider(type).getIfAvailable();
	}

	/**
	 * JDBC 模板解析：指定了 datasourceKey 走 magic-api 命名数据源（运行期变更即时生效），
	 * 否则回退 Spring 默认 JdbcTemplate。
	 */
	static Supplier<JdbcTemplate> jdbcTemplate(ApplicationContext context, String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicDataSource holder = requireHolder(optionalBean(context, MagicDynamicDataSource.class),
					"MagicDynamicDataSource", type);
			return () -> {
				try {
					return new JdbcTemplate(holder.getDataSource(instanceKey).getDataSource());
				} catch (Exception e) {
					throw new IllegalStateException(
							String.format("获取 magic-api 数据源 '%s' 失败", instanceKey), e);
				}
			};
		}
		JdbcTemplate template = require(optionalBean(context, JdbcTemplate.class), "JdbcTemplate", type);
		return () -> template;
	}

	/**
	 * JDBC 主键生成器：随数据源构建 {@link DisposableWorkerIdAssigner}（workerId 由所选
	 * 数据源的 WORKER_NODE 表分配，动态解析）+ 框架 {@link DefaultUidGenerator}。
	 */
	static LongSupplier jdbcIdSupplier(ApplicationContext context, String type, String instanceKey) {
		Supplier<JdbcTemplate> templateSupplier = jdbcTemplate(context, type, instanceKey);
		DisposableWorkerIdAssigner assigner = new DisposableWorkerIdAssigner(templateSupplier);
		DefaultUidGenerator generator = new DefaultUidGenerator();
		generator.setWorkerIdAssigner(assigner);
		try {
			generator.afterPropertiesSet();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("初始化 DefaultUidGenerator 失败", e);
		}
		return generator::getUID;
	}

	/** Redis 模板解析：命名数据源按连接工厂动态构建，否则回退 Spring 默认 StringRedisTemplate。 */
	static Supplier<StringRedisTemplate> redisTemplate(ApplicationContext context, String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicRedisDataSource holder = requireHolder(optionalBean(context, MagicDynamicRedisDataSource.class),
					"MagicDynamicRedisDataSource", type);
			return () -> buildStringRedisTemplate(requireNamed(
					holder.getRedisConnectionFactory(instanceKey), instanceKey, "redis 连接工厂"));
		}
		StringRedisTemplate template = require(optionalBean(context, StringRedisTemplate.class),
				"StringRedisTemplate", type);
		return () -> template;
	}

	/** 从 Redis 连接工厂构建 StringRedisTemplate。 */
	private static StringRedisTemplate buildStringRedisTemplate(
			org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();
		return template;
	}

	/** Mongo 模板解析：命名数据源按数据库工厂动态构建，否则回退 Spring 默认 MongoTemplate。 */
	static Supplier<MongoTemplate> mongoTemplate(ApplicationContext context, String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicMongoDataSource holder = requireHolder(optionalBean(context, MagicDynamicMongoDataSource.class),
					"MagicDynamicMongoDataSource", type);
			return () -> new MongoTemplate(requireNamed(holder.getMongoDatabaseFactory(instanceKey),
					instanceKey, "mongo 数据库工厂"));
		}
		MongoTemplate template = require(optionalBean(context, MongoTemplate.class), "MongoTemplate", type);
		return () -> template;
	}

	/** ES 客户端解析：命名数据源按低层 RestClient 动态构建，否则回退容器 ElasticsearchClient/RestClient。 */
	static Supplier<ElasticsearchClient> elasticsearchClient(ApplicationContext context, String type,
			String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicElasticSearchDataSource holder = requireHolder(
					optionalBean(context, MagicDynamicElasticSearchDataSource.class),
					"MagicDynamicElasticSearchDataSource", type);
			return () -> new ElasticsearchClient(new RestClientTransport(requireNamed(
					holder.getElasticSearchRestClient(instanceKey), instanceKey, "elasticsearch 客户端"),
					new JacksonJsonpMapper()));
		}
		ElasticsearchClient client = optionalBean(context, ElasticsearchClient.class);
		if (client != null) {
			return () -> client;
		}
		RestClient lowLevelClient = require(optionalBean(context, RestClient.class), "RestClient", type);
		return () -> new ElasticsearchClient(new RestClientTransport(lowLevelClient, new JacksonJsonpMapper()));
	}

	private static <T> T requireHolder(T holder, String holderName, String type) {
		if (holder == null) {
			throw new IllegalStateException(String.format(
					"chat-memory-store type=%s 指定了数据源实例 key，但容器中不存在 %s（magic-api 对应插件未启用）",
					type, holderName));
		}
		return holder;
	}

	private static <T> T requireNamed(T client, String instanceKey, String description) {
		if (client == null) {
			throw new IllegalArgumentException(
					String.format("找不到名为 '%s' 的%s，请确认 magic-api 数据源已配置", instanceKey, description));
		}
		return client;
	}

	private static <T> T require(T bean, String beanName, String type) {
		if (bean == null) {
			throw new IllegalStateException(String.format(
					"chat-memory-store type=%s 未指定数据源实例 key，且 Spring 容器中不存在 %s Bean，请先配置对应客户端",
					type, beanName));
		}
		return bean;
	}
}
