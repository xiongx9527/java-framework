package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.DisposableWorkerIdAssigner;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ElasticsearchChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.JdbcChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.MongoChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.RedisChatMemoryStore;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.common.uid.impl.DefaultUidGenerator;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * {@code langchain4j-chat-memory-store} 提供商工厂：按资源定义的 {@code type}
 * 构建 {@link ChatMemoryStore} 实例。
 *
 * <p>支持的提供商：{@code in-memory}（内存）/ {@code jdbc} / {@code redis} /
 * {@code mongodb} / {@code elasticsearch}。存储模型与参数复用
 * {@code fw.plus.ai.langchain4j.chat-memory.*} 配置（表名/集合/索引/键前缀/TTL 等）。</p>
 *
 * <p>客户端来源（除内存外）二选一：</p>
 * <ul>
 * <li>资源定义了 {@code datasourceKey}：使用 magic-api 同名命名数据源实例，
 * 即脚本中 {@code db['key']} / {@code redis['key']} / {@code mongo['key']} / ES 数据源；</li>
 * <li>未定义：回退 Spring 容器默认客户端 Bean
 * （{@code JdbcTemplate} / {@code StringRedisTemplate} / {@code MongoTemplate} /
 * {@code ElasticsearchClient}，缺省可由低层 {@code RestClient} 构建）。</li>
 * </ul>
 * <p>两条路径都取不到客户端时快速失败。</p>
 */
public class ChatMemoryStoreProviderFactory {

	private final ObjectProvider<MagicDynamicDataSource> dynamicDataSource;

	private final ObjectProvider<MagicDynamicRedisDataSource> redisDataSource;

	private final ObjectProvider<MagicDynamicMongoDataSource> mongoDataSource;

	private final ObjectProvider<MagicDynamicElasticSearchDataSource> elasticSearchDataSource;

	private final ObjectProvider<JdbcTemplate> jdbcTemplate;

	private final ObjectProvider<StringRedisTemplate> redisTemplate;

	private final ObjectProvider<MongoTemplate> mongoTemplate;

	private final ObjectProvider<ElasticsearchClient> elasticsearchClient;

	private final ObjectProvider<RestClient> restClient;

	private final ObjectProvider<ChatMemoryTenantResolver> tenantResolver;

	private final ObjectProvider<ChatMemoryStoreProperties> properties;

	public ChatMemoryStoreProviderFactory(ObjectProvider<MagicDynamicDataSource> dynamicDataSource,
			ObjectProvider<MagicDynamicRedisDataSource> redisDataSource,
			ObjectProvider<MagicDynamicMongoDataSource> mongoDataSource,
			ObjectProvider<MagicDynamicElasticSearchDataSource> elasticSearchDataSource,
			ObjectProvider<JdbcTemplate> jdbcTemplate, ObjectProvider<StringRedisTemplate> redisTemplate,
			ObjectProvider<MongoTemplate> mongoTemplate, ObjectProvider<ElasticsearchClient> elasticsearchClient,
			ObjectProvider<RestClient> restClient, ObjectProvider<ChatMemoryTenantResolver> tenantResolver,
			ObjectProvider<ChatMemoryStoreProperties> properties) {
		this.dynamicDataSource = dynamicDataSource;
		this.redisDataSource = redisDataSource;
		this.mongoDataSource = mongoDataSource;
		this.elasticSearchDataSource = elasticSearchDataSource;
		this.jdbcTemplate = jdbcTemplate;
		this.redisTemplate = redisTemplate;
		this.mongoTemplate = mongoTemplate;
		this.elasticsearchClient = elasticsearchClient;
		this.restClient = restClient;
		this.tenantResolver = tenantResolver;
		this.properties = properties;
	}

	/** 按资源定义的 type 与 datasourceKey 构建实例。 */
	public ChatMemoryStore create(Langchain4jChatMemoryStoreInfo info) {
		String type = normalizeType(info.getType());
		String instanceKey = StringUtils.isEmpty(info.getDatasourceKey())
				? null : info.getDatasourceKey().trim();
		ChatMemoryStoreProperties props = properties.getIfAvailable(ChatMemoryStoreProperties::new);
		ChatMemoryTenantResolver resolver = tenantResolver.getIfAvailable();
		String defaultBizType = props.getDefaultBizType();
		ChatMemoryStoreInstanceParams params = info.getParams() == null
				? new ChatMemoryStoreInstanceParams() : info.getParams();
		switch (type) {
			case ChatMemoryStoreProperties.TYPE_IN_MEMORY:
				return new InMemoryChatMemoryStore();
			case ChatMemoryStoreProperties.TYPE_JDBC:
				return new JdbcChatMemoryStore(resolveJdbcTemplate(type, instanceKey),
						firstNonEmpty(params.getTableName(), props.getJdbc().getTableName()),
						resolver, defaultBizType, resolveJdbcIdSupplier(type, instanceKey));
			case ChatMemoryStoreProperties.TYPE_REDIS:
				return new RedisChatMemoryStore(resolveRedisTemplate(type, instanceKey), resolver, defaultBizType,
						firstNonEmpty(params.getKeyPrefix(), props.getRedis().getKeyPrefix()),
						resolveTtl(params.getTtl(), props.getRedis().getTtl()));
			case ChatMemoryStoreProperties.TYPE_MONGODB:
				return new MongoChatMemoryStore(resolveMongoTemplate(type, instanceKey),
						firstNonEmpty(params.getCollectionName(), props.getMongo().getCollectionName()),
						resolver, defaultBizType);
			case ChatMemoryStoreProperties.TYPE_ELASTICSEARCH:
				return new ElasticsearchChatMemoryStore(resolveElasticsearchClient(type, instanceKey),
						firstNonEmpty(params.getIndexName(), props.getElasticsearch().getIndexName()),
						resolver, defaultBizType);
			default:
				throw new IllegalArgumentException(String.format(
						"不支持的 chat-memory-store 提供商 type='%s'（可选 in-memory/jdbc/redis/mongodb/elasticsearch）",
						info.getType()));
		}
	}

	/** 实例参数优先于全局配置。 */
	private static String firstNonEmpty(String value, String fallback) {
		return StringUtils.isEmpty(value) ? fallback : value;
	}

	/** 解析实例 ttl（Spring 风格时长，如 30m/12h/7d），留空回退全局配置。 */
	private static java.time.Duration resolveTtl(String ttl, java.time.Duration fallback) {
		if (StringUtils.isEmpty(ttl)) {
			return fallback;
		}
		try {
			return org.springframework.boot.convert.DurationStyle.detectAndParse(ttl.trim());
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format(
					"非法的 ttl='%s'（支持 30m / 12h / 7d 等 Spring 风格时长）", ttl), e);
		}
	}

	/**
	 * 客户端解析器一律以 {@link Supplier} 交给存储实现，每次操作时动态解析
	 * （类似 {@code MagicDynamicEmbeddingStore}）：指定了 datasourceKey 时每次调用
	 * 都从 magic 命名数据源持有者按 key 取最新实例，数据源运行期变更即时生效。
	 */
	private Supplier<JdbcTemplate> resolveJdbcTemplate(String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicDataSource holder = requireHolder(dynamicDataSource.getIfAvailable(),
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
		JdbcTemplate template = require(jdbcTemplate.getIfAvailable(), "JdbcTemplate", type);
		return () -> template;
	}

	/**
	 * JDBC 主键生成器：随数据源构建 {@link DisposableWorkerIdAssigner}（workerId 由所选
	 * 数据源的 WORKER_NODE 表分配，动态解析）+ 框架 {@link DefaultUidGenerator}。
	 */
	private LongSupplier resolveJdbcIdSupplier(String type, String instanceKey) {
		Supplier<JdbcTemplate> templateSupplier = resolveJdbcTemplate(type, instanceKey);
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

	private Supplier<StringRedisTemplate> resolveRedisTemplate(String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicRedisDataSource holder = requireHolder(redisDataSource.getIfAvailable(),
					"MagicDynamicRedisDataSource", type);
			return () -> buildStringRedisTemplate(requireNamed(
					holder.getRedisConnectionFactory(instanceKey), instanceKey, "redis 连接工厂"));
		}
		StringRedisTemplate template = require(redisTemplate.getIfAvailable(), "StringRedisTemplate", type);
		return () -> template;
	}

	private Supplier<MongoTemplate> resolveMongoTemplate(String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicMongoDataSource holder = requireHolder(mongoDataSource.getIfAvailable(),
					"MagicDynamicMongoDataSource", type);
			return () -> new MongoTemplate(requireNamed(holder.getMongoDatabaseFactory(instanceKey),
					instanceKey, "mongo 数据库工厂"));
		}
		MongoTemplate template = require(mongoTemplate.getIfAvailable(), "MongoTemplate", type);
		return () -> template;
	}

	private Supplier<ElasticsearchClient> resolveElasticsearchClient(String type, String instanceKey) {
		if (instanceKey != null) {
			MagicDynamicElasticSearchDataSource holder = requireHolder(elasticSearchDataSource.getIfAvailable(),
					"MagicDynamicElasticSearchDataSource", type);
			return () -> new ElasticsearchClient(new RestClientTransport(requireNamed(
					holder.getElasticSearchRestClient(instanceKey), instanceKey, "elasticsearch 客户端"),
					new JacksonJsonpMapper()));
		}
		ElasticsearchClient client = elasticsearchClient.getIfAvailable();
		if (client != null) {
			return () -> client;
		}
		RestClient lowLevelClient = require(restClient.getIfAvailable(), "RestClient", type);
		return () -> new ElasticsearchClient(new RestClientTransport(lowLevelClient, new JacksonJsonpMapper()));
	}

	/** 从 Redis 连接工厂构建 StringRedisTemplate。 */
	private StringRedisTemplate buildStringRedisTemplate(
			org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();
		return template;
	}

	private static String normalizeType(String type) {
		return type == null ? "" : type.trim().toLowerCase();
	}

	private <T> T requireHolder(T holder, String holderName, String type) {
		if (holder == null) {
			throw new IllegalStateException(String.format(
					"chat-memory-store type=%s 指定了数据源实例 key，但容器中不存在 %s（magic-api 对应插件未启用）",
					type, holderName));
		}
		return holder;
	}

	private <T> T requireNamed(T client, String instanceKey, String description) {
		if (client == null) {
			throw new IllegalArgumentException(
					String.format("找不到名为 '%s' 的%s，请确认 magic-api 数据源已配置", instanceKey, description));
		}
		return client;
	}

	private <T> T require(T bean, String beanName, String type) {
		if (bean == null) {
			throw new IllegalStateException(String.format(
					"chat-memory-store type=%s 未指定数据源实例 key，且 Spring 容器中不存在 %s Bean，请先配置对应客户端",
					type, beanName));
		}
		return bean;
	}
}
