package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ElasticsearchChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.JdbcChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.MongoChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.RedisChatMemoryStore;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

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
 *
 * <p><b>本类作为 Spring bean，其全部方法描述符（含 lambda 合成方法）不得引用
 * optional 依赖类</b>（Spring 启动期会对其调用 {@link Class#getDeclaredMethods()}
 * 解析所有擦除后的参数/返回类型，引用缺失类会抛 NoClassDefFoundError 导致启动失败）。
 * 因此可选客户端的解析、构建与 lambda 实现全部隔离在非 bean 的
 * {@link ChatMemoryStoreClientResolvers} 中，本类仅通过
 * {@code ApplicationContext} 在对应存储分支被选用时按需解析。</p>
 */
public class ChatMemoryStoreProviderFactory {

	private final ApplicationContext context;

	private final ObjectProvider<ChatMemoryTenantResolver> tenantResolver;

	private final ObjectProvider<ChatMemoryStoreProperties> properties;

	public ChatMemoryStoreProviderFactory(ApplicationContext context,
			ObjectProvider<ChatMemoryTenantResolver> tenantResolver,
			ObjectProvider<ChatMemoryStoreProperties> properties) {
		this.context = context;
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
				return new JdbcChatMemoryStore(
						ChatMemoryStoreClientResolvers.jdbcTemplate(context, type, instanceKey),
						firstNonEmpty(params.getTableName(), props.getJdbc().getTableName()),
						resolver, defaultBizType,
						ChatMemoryStoreClientResolvers.jdbcIdSupplier(context, type, instanceKey));
			case ChatMemoryStoreProperties.TYPE_REDIS:
				return new RedisChatMemoryStore(
						ChatMemoryStoreClientResolvers.redisTemplate(context, type, instanceKey),
						resolver, defaultBizType,
						firstNonEmpty(params.getKeyPrefix(), props.getRedis().getKeyPrefix()),
						resolveTtl(params.getTtl(), props.getRedis().getTtl()));
			case ChatMemoryStoreProperties.TYPE_MONGODB:
				return new MongoChatMemoryStore(
						ChatMemoryStoreClientResolvers.mongoTemplate(context, type, instanceKey),
						firstNonEmpty(params.getCollectionName(), props.getMongo().getCollectionName()),
						resolver, defaultBizType);
			case ChatMemoryStoreProperties.TYPE_ELASTICSEARCH:
				return new ElasticsearchChatMemoryStore(
						ChatMemoryStoreClientResolvers.elasticsearchClient(context, type, instanceKey),
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

	private static String normalizeType(String type) {
		return type == null ? "" : type.trim().toLowerCase();
	}
}
