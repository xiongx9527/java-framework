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
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataMemoryRecallAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataRecommendationAdapter;
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
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService;
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
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * 创建单节点默认长期记忆 SPI；业务可替换为持久化实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataLongTermMemory askDataLongTermMemory(AskDataProperties properties) {
        validateLocalStateConfiguration(properties);
        return AskDataLongTermMemory.inMemory();
    }

    /**
     * 创建单节点默认个人推荐 SPI；推荐始终由 Core 按项目和操作人隔离。
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataRecommendationService askDataRecommendationService(AskDataProperties properties) {
        validateLocalStateConfiguration(properties);
        return AskDataRecommendationService.inMemory();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataMemoryRecallAdapter askDataMemoryRecallAdapter(AskDataLongTermMemory memory) {
        return new AskDataMemoryRecallAdapter(memory);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataRecommendationAdapter askDataRecommendationAdapter(
            AskDataRecommendationService recommendationService) {
        return new AskDataRecommendationAdapter(recommendationService);
    }

    /**
     * 提供不依赖模型的默认问题规划器，支持显式列表和中英文分号复合问题。
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataQuestionPlanner askDataQuestionPlanner() {
        return AskDataQuestionPlanner.simple();
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
            AskDataProperties properties) {
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().conversations();
    }

    /**
     * 暴露默认的 Core 检查点存储，保证恢复记录随项目和操作人共同分区。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataCheckpointStore.class)
    public AskDataCheckpointStore askDataCheckpointStore(AskDataProperties properties) {
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().checkpoints();
    }

    /**
     * 暴露默认的 Core 智能体状态存储，单机以外必须由共享存储 SPI 替换。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataAgentStateStore.class)
    public AskDataAgentStateStore askDataAgentStateStore(AskDataProperties properties) {
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().agentStates();
    }

    /**
     * 暴露默认的基础监控日志存储。日志由项目和操作人共同隔离，且只可保存已脱敏摘要。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataMonitoringLogStore.class)
    public AskDataMonitoringLogStore askDataMonitoringLogStore(AskDataProperties properties) {
        validateDefaultMonitoringLogConfiguration(properties);
        Integer capacity = properties.getState().getMonitoringLogCapacity();
        return new InMemoryAskDataMonitoringLogStore(capacity == null || capacity.intValue() <= 0 ? 1000
                : capacity.intValue());
    }

    /**
     * 创建强制写入基础监控日志的生命周期事件发布器。
     *
     * <p>外部监控导出可另行组合，但不能替代本发布器；任何导出失败策略也不得改变本地脱敏日志的隔离和保存行为。</p>
     */
    @Bean(name = "askDataMonitoringLogEventPublisher")
    @Primary
    public AskDataEventPublisher askDataMonitoringLogEventPublisher(AskDataMonitoringLogStore monitoringLogStore) {
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
    @ConditionalOnMissingBean(com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator.class)
    public com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreAskDataSqlValidator() {
        return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator() {
            @Override
            public Result validate(com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand command) {
                String normalizedSql = command.getSql().trim().toLowerCase();
                if (normalizedSql.indexOf(';') >= 0) {
                    return Result.failure(FailureCategory.MULTI_STATEMENT, "SQL 只允许单条语句");
                }
                if (normalizedSql.startsWith("insert") || normalizedSql.startsWith("update")
                        || normalizedSql.startsWith("delete") || normalizedSql.startsWith("merge")
                        || normalizedSql.startsWith("create") || normalizedSql.startsWith("drop")
                        || normalizedSql.startsWith("alter")) {
                    return Result.failure(FailureCategory.WRITE_SQL, "SQL 仅允许只读查询");
                }
                if (!command.getAuthorizedObjects().isEmpty()) {
                    String[] tokens = normalizedSql.split("\\s+");
                    for (int index = 0; index < tokens.length - 1; index++) {
                        if ("from".equals(tokens[index]) && !command.getAuthorizedObjects().contains(tokens[index + 1])) {
                            return Result.failure(FailureCategory.UNAUTHORIZED_OBJECT, "对象未授权: " + tokens[index + 1]);
                        }
                    }
                }
                return Result.success();
            }
        };
    }

	@Bean
	@ConditionalOnMissingBean
	public AskDataSqlExecutor askDataSqlExecutor(AskDataProperties properties,
			ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
		Integer defaultMaxRows = properties.getSql().getDefaultMaxRows();
		return new JdbcAskDataSqlExecutor(jdbcTemplateProvider,
				defaultMaxRows == null ? 100 : defaultMaxRows.intValue());
	}

    /**
     * 暴露 SQL 插件链的审计记录器。
     *
     * <p>审计器只保存插件标识和处理阶段，不包含 SQL 参数、连接或其他敏感执行对象。业务可替换该 Bean
     * 将记录转存到其审计基础设施。</p>
     */
    @Bean
    @ConditionalOnMissingBean(com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit.class)
    public com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit askDataSqlAudit() {
        return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit();
    }

    /**
     * 将 Spring 管理的 SQL 插件桥接到 Core SQL 安全链。
     *
     * <p>插件按 Spring 顺序收集，再由 Core 按 SPI 顺序和 key 稳定排序。Core 链会保留输入命令的隔离
     * 上下文、命名参数和授权对象，并在所有插件改写后强制调用最终校验器，插件不能绕过 SQL 安全边界。</p>
     */
    @Bean
    @ConditionalOnMissingBean(com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain.class)
    public com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain askDataSqlInterceptorChain(
            ObjectProvider<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor> interceptorProvider,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator validator,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit audit) {
        return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain(
                interceptorProvider.orderedStream().collect(Collectors.toList()), validator, audit);
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
            ObjectProvider<AskDataAgent> agentProvider,
            @Qualifier("askDataMonitoringLogEventPublisher") AskDataEventPublisher monitoringLogEventPublisher,
            ObjectProvider<AskDataEventPublisher> eventPublisherProvider, AskDataQuestionPlanner questionPlanner,
            ObjectProvider<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain> sqlInterceptorChainProvider) {
        List<AskDataAgent> mainAgents = agentProvider.orderedStream()
                .filter(agent -> !(agent instanceof AskDataSubAgent))
                .collect(Collectors.toList());
        return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                promptTemplateProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, contextCompressor,
                agentDispatcher, mainAgents, combineEventPublishers(monitoringLogEventPublisher, eventPublisherProvider),
                questionPlanner, sqlInterceptorChainProvider.getIfAvailable());
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

    /**
     * 拒绝在集群节点中创建本地监控日志存储，避免基础审计记录只留在单个节点。
     */
    private void validateDefaultMonitoringLogConfiguration(AskDataProperties properties) {
        if (properties.getState().isClusterMode()) {
            throw new IllegalStateException("AskData 集群模式不能使用默认本地监控日志；请提供共享 AskDataMonitoringLogStore 实现");
        }
    }

    /**
     * 基础监控日志必须先写入，再调用业务提供的外部观测发布器。
     *
     * <p>业务发布器属于非关键扩展边界，其运行时异常会被隔离，不能中断主对话或阻断后续生命周期日志。</p>
     */
    private AskDataEventPublisher combineEventPublishers(final AskDataEventPublisher monitoringLogEventPublisher,
            ObjectProvider<AskDataEventPublisher> eventPublisherProvider) {
        final List<AskDataEventPublisher> externalPublishers = eventPublisherProvider.orderedStream()
                .filter(publisher -> publisher != monitoringLogEventPublisher)
                .collect(Collectors.toList());
        if (externalPublishers.isEmpty()) {
            return monitoringLogEventPublisher;
        }
        return new AskDataEventPublisher() {
            @Override
            public void publish(com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent event) {
                monitoringLogEventPublisher.publish(event);
                for (AskDataEventPublisher externalPublisher : externalPublishers) {
                    try {
                        externalPublisher.publish(event);
                    } catch (RuntimeException ex) {
                        // 外部观测导出失败不能影响已持久化的基础审计日志和业务对话。
                    }
                }
            }
        };
    }
}
