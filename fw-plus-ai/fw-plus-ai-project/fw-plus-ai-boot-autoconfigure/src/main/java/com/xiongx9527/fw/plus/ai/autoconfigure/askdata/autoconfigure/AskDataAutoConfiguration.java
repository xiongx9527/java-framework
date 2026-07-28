package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.CoreAskDataServiceAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.DefaultAskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.InMemoryAskDataMonitoringLogStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataSubAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.DefaultAskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.SequentialAskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.EmptyAskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jAskDataModelSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.DefaultAskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.DefaultAskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.JdbcAskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.CompositeAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.ContentRetrieverAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnProperty(prefix = "fw.plus.ai.askdata", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties
public class AskDataAutoConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "fw.plus.ai.askdata")
	@ConditionalOnMissingBean
	public AskDataProperties askDataProperties() {
		return new AskDataProperties();
	}

    /**
     * 创建单机 AskData Core 状态集合。
     *
     * <p>该集合包含按 {@code projectId + operatorId} 隔离的会话、检查点、智能体状态和监控日志。它不共享进程间数据，
     * 因而仅可在 {@code state.cluster-mode=false} 时作为默认值；集群模式选择内存状态或内存向量时必须在 Spring 启动期间
     * 明确失败，不能将恢复、审计或授权范围静默降级到本地节点。</p>
     *
     * @param properties AskData 部署配置
     * @return 单机默认状态集合
     */
    @Bean
    @ConditionalOnMissingBean({ com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class,
            AskDataCheckpointStore.class, AskDataAgentStateStore.class })
    public InMemoryAskDataStores askDataInMemoryStores(AskDataProperties properties) {
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode();
    }

    /**
     * 暴露默认的 Core 会话存储，供新 Core API 使用。
     *
     * <p>旧流程的 {@link AskDataConversationStore} 保持独立，以避免旧 DTO 泄漏到 Core 契约。业务若提供共享 Core 存储 SPI
     * 实现，本 Bean 会让位；集群场景仍必须同时将状态和向量能力配置为非本地实现。</p>
     */
    @Bean
    @ConditionalOnMissingBean(com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class)
    public com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore coreAskDataConversationStore(
            InMemoryAskDataStores stores) {
        return stores.conversations();
    }

    /**
     * 暴露默认的 Core 检查点存储，保证恢复记录随项目和操作人共同分区。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataCheckpointStore.class)
    public AskDataCheckpointStore askDataCheckpointStore(InMemoryAskDataStores stores) {
        return stores.checkpoints();
    }

    /**
     * 暴露默认的 Core 智能体状态存储，单机以外必须由共享存储 SPI 替换。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataAgentStateStore.class)
    public AskDataAgentStateStore askDataAgentStateStore(InMemoryAskDataStores stores) {
        return stores.agentStates();
    }

    /**
     * 暴露默认的基础监控日志存储。日志由项目和操作人共同隔离，且只可保存已脱敏摘要。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataMonitoringLogStore.class)
    public AskDataMonitoringLogStore askDataMonitoringLogStore(AskDataProperties properties) {
        Integer capacity = properties.getState().getMonitoringLogCapacity();
        return new InMemoryAskDataMonitoringLogStore(capacity == null || capacity.intValue() <= 0 ? 1000
                : capacity.intValue());
    }

    /**
     * 创建强制写入基础监控日志的生命周期事件发布器。
     *
     * <p>外部监控导出可另行组合，但不能替代本发布器；任何导出失败策略也不得改变本地脱敏日志的隔离和保存行为。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataEventPublisher askDataEventPublisher(AskDataMonitoringLogStore monitoringLogStore) {
        return AskDataEventPublisher.toStore(monitoringLogStore);
    }

    /**
     * 创建默认管理员日志访问策略。
     *
     * <p>仅宿主系统已授权的同项目管理员可读；普通操作人默认拒绝，跨项目管理员也必须由业务提供替代 SPI 显式授权。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataMonitoringLogAccessPolicy askDataMonitoringLogAccessPolicy() {
        return AskDataMonitoringLogAccessPolicy.projectAdministratorOnly();
    }

	@Bean
	@ConditionalOnMissingBean
	public AskDataModelRegistry askDataModelRegistry(AskDataProperties properties,
			Map<String, ChatLanguageModel> chatModels,
			Map<String, EmbeddingModel> embeddingModels,
			ObjectProvider<LangChain4jAskDataModelSource> modelSourceProvider) {
		return new LangChain4jAskDataModelRegistry(properties, chatModels, embeddingModels,
				modelSourceProvider.orderedStream().collect(Collectors.toList()));
	}

    @Bean
    @ConditionalOnMissingBean
    public AskDataMetadataProvider askDataMetadataProvider() {
        return new EmptyAskDataMetadataProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataDialectRegistry askDataDialectRegistry() {
        return new AskDataDialectRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataPromptTemplateProvider askDataPromptTemplateProvider(AskDataDialectRegistry dialectRegistry) {
        return new DefaultAskDataPromptTemplateProvider(dialectRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataSqlValidator askDataSqlValidator(AskDataProperties properties) {
        return new DefaultAskDataSqlValidator(properties.getSql().isEnforceLimit());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataSqlExecutor askDataSqlExecutor(AskDataProperties properties,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        Integer defaultMaxRows = properties.getSql().getDefaultMaxRows();
        return new JdbcAskDataSqlExecutor(jdbcTemplateProvider,
                defaultMaxRows == null ? 100 : defaultMaxRows.intValue());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataVectorRetriever askDataVectorRetriever(AskDataProperties properties,
            ObjectProvider<ContentRetriever> contentRetrieverProvider) {
        validateDefaultVectorConfiguration(properties);
        List<AskDataVectorRetriever> retrievers = contentRetrieverProvider.orderedStream()
                .map(ContentRetrieverAskDataVectorRetriever::new)
                .collect(Collectors.<AskDataVectorRetriever>toList());
        return new CompositeAskDataVectorRetriever(retrievers, properties.getVector().getDefaultTopK());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataAgentDispatcher askDataAgentDispatcher(ObjectProvider<AskDataSubAgent> subAgentProvider) {
        return new SequentialAskDataAgentDispatcher(subAgentProvider.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataConversationStore askDataConversationStore(AskDataProperties properties) {
        validateLegacyConversationConfiguration(properties);
        Integer capacity = properties.getConversation().getMaxCheckpointsPerConversation();
        return new InMemoryAskDataConversationStore(capacity == null || capacity.intValue() <= 0 ? 100
                : capacity.intValue());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataContextCompressor askDataContextCompressor(AskDataProperties properties) {
        return new DefaultAskDataContextCompressor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataFlowExecutor askDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            ObjectProvider<AskDataAgent> agentProvider, ObjectProvider<AskDataEventPublisher> eventPublisherProvider) {
        List<AskDataAgent> mainAgents = agentProvider.orderedStream()
                .filter(agent -> !(agent instanceof AskDataSubAgent))
                .collect(Collectors.toList());
        return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                promptTemplateProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, contextCompressor,
                agentDispatcher, mainAgents, eventPublisherProvider.getIfAvailable());
    }

	/**
	 * 创建旧问数流程使用的兼容服务。
	 *
	 * <p>该 Bean 仅供当前自动配置内部流及仍依赖旧 DTO 的存量扩展使用；新业务代码应注入
	 * Core 的 {@code com.xiongx9527.fw.plus.ai.askdata.api.AskDataService}。保留该适配边界可避免
	 * 旧流程的模型、SQL 与会话实现类型泄漏到 Core 公共契约。</p>
	 *
	 * @param flowExecutor 旧流程执行器
	 * @return 旧流程兼容服务
	 */
	@Bean
	@ConditionalOnMissingBean
	public AskDataService legacyAskDataService(AskDataFlowExecutor flowExecutor) {
		return new DefaultAskDataService(flowExecutor);
	}

	/**
	 * 创建面向外部消费者的 Core AskData 服务。
	 *
	 * <p>当业务自行提供 Core 服务实现时，本方法不会装配默认 Bean，确保业务可在其实现中完成项目、操作人权限、
	 * 审计和恢复策略。默认实现只负责将稳定的 Core 输入输出与旧流程适配，绝不向 Core API 暴露 Spring、模型 SDK
	 * 或数据存储厂商类型。</p>
	 *
	 * @param legacyService 已隔离的旧流程兼容服务
	 * @return Core AskData 服务实现
	 */
	@Bean
	@ConditionalOnMissingBean(value = com.xiongx9527.fw.plus.ai.askdata.api.AskDataService.class,
			name = "askDataService")
	public com.xiongx9527.fw.plus.ai.askdata.api.AskDataService askDataService(AskDataService legacyService) {
		return new CoreAskDataServiceAdapter(legacyService);
	}

    private void validateLocalStateConfiguration(AskDataProperties properties) {
        if (!properties.getState().isClusterMode()) {
            return;
        }
        throw new IllegalStateException("AskData 集群模式不能使用默认本地内存状态；请提供共享状态存储 SPI 实现");
    }

    private void validateDefaultVectorConfiguration(AskDataProperties properties) {
        if (properties.getState().isClusterMode()) {
            throw new IllegalStateException("AskData 集群模式不能使用默认本地向量能力；请提供共享向量检索 SPI 实现");
        }
    }

    private void validateLegacyConversationConfiguration(AskDataProperties properties) {
        if (properties.getState().isClusterMode()) {
            throw new IllegalStateException("AskData 集群模式不能使用默认本地会话状态；请提供共享会话存储实现");
        }
    }
}
