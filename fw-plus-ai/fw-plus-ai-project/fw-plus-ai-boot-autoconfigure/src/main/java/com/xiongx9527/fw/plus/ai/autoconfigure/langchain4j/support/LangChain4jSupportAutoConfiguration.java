package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.LangChain4jIntentAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.logging.ChatContextLogger;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.Mem0ChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.DifyContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.DifyRagProperties;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.utils.DatasetBuilder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

/**
 * LangChain4j 支撑组件装配。
 *
 * <p>按 {@code fw.plus.ai.langchain4j.chat-memory.type} 条件注入一个
 * {@link ChatMemoryStore} Bean（含进程内实现）：{@code in-memory}（默认兜底）/
 * {@code mem0}（mem0 兼容 REST 服务，连接参数见 {@link ChatMemoryStoreProperties.Mem0}，
 * 以 {@code @ConditionalOnMissingBean} 优先使用方自定义 Bean）。
 * mem0 服务地址错误或不可达时调用期快速失败，避免静默降级。</p>
 *
 * <p>按 {@code fw.plus.ai.langchain4j.dify-rag.enabled=true} 条件注入
 * {@link ContentRetriever} Bean（{@link DifyContentRetriever}，调用 dify 知识库检索接口，
 * 配置见 {@link DifyRagProperties}），检索故障降级空列表不打断对话。</p>
 *
 * <p>两个存储配置类与 DifyRagProperties 配置类经 {@code @Import} 显式导入
 * （而非依赖嵌套 member 自动发现）；DifyRagProperties 因 {@code fw-plus-dify} 是
 * optional 依赖而按类存在性条件注册——消费方没有 dify 类时跳过，
 * 避免配置类方法签名 introspection 时 {@code NoClassDefFoundError} 拉垮整个上下文。</p>
 *
 * <p>意图识别自动配置 {@link LangChain4jIntentAutoConfiguration} 亦在此 {@code @Import}
 * 收口，其自身条件与本源一致，无需在 {@code AutoConfiguration.imports} 中单独登记。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.Experimental")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatMemoryStoreProperties.class)
@Import({LangChain4jSupportAutoConfiguration.Mem0ChatMemoryStoreConfiguration.class,
		LangChain4jSupportAutoConfiguration.InMemoryChatMemoryStoreConfiguration.class,
		LangChain4jSupportAutoConfiguration.DifyRagPropertiesConfiguration.class,
		LangChain4jIntentAutoConfiguration.class})
public class LangChain4jSupportAutoConfiguration {

	@Bean
	public ChatContextLogger chatContextLogger() {
		return new ChatContextLogger();
	}

	/** 默认租户解析器：返回配置的 default-tenant-id；使用方自定义 Bean 即自动覆盖 */
	@Bean
	@ConditionalOnMissingBean(ChatMemoryTenantResolver.class)
	public ChatMemoryTenantResolver chatMemoryTenantResolver(ChatMemoryStoreProperties properties) {
		return () -> properties.getDefaultTenantId();
	}

	/**
	 * {@code type} 取值校验：未知取值（如旧配置残留 {@code type=redis/jdbc}）启动即失败，
	 * 避免条件全不匹配导致 ChatMemoryStore 静默缺失、会话记忆无声消失。
	 */
	@Bean
	public InitializingBean chatMemoryStoreTypeValidator(ChatMemoryStoreProperties properties) {
		return () -> {
			String type = properties.getType();
			if (type == null) {
				return;
			}
			String normalized = type.trim();
			if (!ChatMemoryStoreProperties.TYPE_IN_MEMORY.equals(normalized)
					&& !ChatMemoryStoreProperties.TYPE_MEM0.equals(normalized)) {
				throw new IllegalStateException(String.format(
						"%s 取值非法: '%s'，仅支持 %s / %s；使用自定义 ChatMemoryStore Bean 时请移除该配置",
						ChatMemoryStoreProperties.PREFIX + ".type", type,
						ChatMemoryStoreProperties.TYPE_IN_MEMORY, ChatMemoryStoreProperties.TYPE_MEM0));
			}
		};
	}

	/**
	 * Dify 知识库检索器：{@code fw.plus.ai.langchain4j.dify-rag.enabled=true} 时装配，
	 * 多知识库并行检索后按分数降序合并截断；以 {@code @ConditionalOnMissingBean}
	 * 优先使用方自定义 {@link ContentRetriever}。base-url / api-key / dataset-ids
	 * 任一缺失即启动快速失败（配置了却不生效比不配更危险），元数据过滤段名同步校验。
	 */
	@Bean
	@ConditionalOnProperty(prefix = DifyRagProperties.PREFIX, name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(ContentRetriever.class)
	public ContentRetriever difyContentRetriever(DifyRagProperties properties) {
		requireDifyProperty(properties.getBaseUrl(), "base-url");
		requireDifyProperty(properties.getApiKey(), "api-key");
		if (properties.getDatasetIds() == null || properties.getDatasetIds().isEmpty()) {
			throw new IllegalStateException(
					DifyRagProperties.PREFIX + ".dataset-ids 未配置，请填写至少一个知识库 ID");
		}
		properties.getMetaFields().validate();
		IDatasetClient client = DatasetBuilder
				.builder(properties.toBaseUrl(), properties.getApiKey().trim())
				.build();
		return new DifyContentRetriever(client, properties);
	}

	private static void requireDifyProperty(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalStateException(DifyRagProperties.PREFIX + "." + field + " 未配置");
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Mem0ChatMemoryStoreConfiguration {

		@Bean
		@ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
				havingValue = ChatMemoryStoreProperties.TYPE_MEM0)
		@ConditionalOnMissingBean(ChatMemoryStore.class)
		public ChatMemoryStore mem0ChatMemoryStore(ChatMemoryTenantResolver tenantResolver,
				ChatMemoryStoreProperties properties) {
			ChatMemoryStoreProperties.Mem0 mem0 = properties.getMem0();
			if (mem0.getBaseUrl() == null || mem0.getBaseUrl().trim().isEmpty()) {
				throw new IllegalStateException(String.format(
						"%s=%s，但 %s.mem0.base-url 未配置，请填写 mem0 服务地址",
						ChatMemoryStoreProperties.PREFIX + ".type", ChatMemoryStoreProperties.TYPE_MEM0,
						ChatMemoryStoreProperties.PREFIX));
			}
			return new Mem0ChatMemoryStore(tenantResolver, properties.getDefaultAppId(),
					mem0.getBaseUrl(), mem0.getApiKey(), mem0.getConnectTimeout(), mem0.getReadTimeout());
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class InMemoryChatMemoryStoreConfiguration {

		/** 声明在最后：仅当 type=in-memory（或缺省）且容器中没有其他 ChatMemoryStore 时兜底 */
		@Bean
		@ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
				havingValue = ChatMemoryStoreProperties.TYPE_IN_MEMORY, matchIfMissing = true)
		@ConditionalOnMissingBean(ChatMemoryStore.class)
		public ChatMemoryStore inMemoryChatMemoryStore() {
			return new InMemoryChatMemoryStore();
		}
	}

	/**
	 * Dify 检索配置类：按 {@code fw-plus-dify}（optional 依赖）类存在性条件注册。
	 * <p>dify 类缺席的消费方（如样例工程）直接跳过——DifyRagProperties 的方法签名引用了
	 * {@code RetrievalModel}，无条件注册会在 introspection 时 NoClassDefFoundError。</p>
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel")
	@EnableConfigurationProperties(DifyRagProperties.class)
	static class DifyRagPropertiesConfiguration {
	}
}
