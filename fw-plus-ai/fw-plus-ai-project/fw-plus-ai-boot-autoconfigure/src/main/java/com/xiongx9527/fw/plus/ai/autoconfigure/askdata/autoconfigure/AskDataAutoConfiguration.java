package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.CoreAskDataServiceAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.DefaultAskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.CoreAskDataConversationStoreAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.CoreAskDataContextCompressorAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.FallbackAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.InMemoryAskDataMonitoringLogStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.AskDataMonitoringLogQueryService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.AskDataObservationExporter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.AsyncAskDataObservationExporter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.OpenTelemetryAskDataObservationExporter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.LangfuseAskDataObservationExporter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.LangSmithAskDataObservationExporter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.AskDataObservationExporterProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability.AskDataObservationTransport;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataSubAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.DefaultAskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataRuntimeStateServices;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.SequentialAskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.EmptyAskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AiModelAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AiModelRegistryAskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataMemoryRecallAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataRecommendationAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.DefaultAskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlAuthorizationProcessor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.DefaultAskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.JdbcAskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AiEmbeddingStoreAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.CompositeAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityRegistry;
import com.xiongx9527.fw.plus.ai.askdata.capability.DefaultAskDataCapabilityRegistry;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationPurpose;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.DefaultDenyAskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAuditStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataIdempotencyKeyStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataLeaseStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataJdbcStorageClient;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataRedisStorageClient;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataSharedStoreAdapter;
import com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorStore;
import com.xiongx9527.fw.plus.ai.askdata.vector.InMemoryAskDataVectorStore;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AskData 的中立 Spring Boot 自动配置入口。
 *
 * <p>该配置只负责发现、校验和连接 Core SPI。业务 Bean 优先于默认实现；通过配置选择的能力必须在唯一
 * {@link AskDataCapabilityRegistry} 中存在且版本兼容。公共 Starter 不选择 LangChain4j、Ollama 或任何 Provider，
 * 不读取模型凭据，也不会在集群模式下把本地内存状态伪装为共享状态。</p>
 *
 * <p>默认 Flow 始终保留项目与操作人隔离、治理 fail-closed、SQL 最终校验、租约/幂等和脱敏观测边界。
 * 业务替换模型、存储、图运行时、压缩器或导出器时不得绕过这些约束。</p>
 */
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

	@Bean
	@ConditionalOnMissingBean
	public AskDataCapabilityRegistry askDataCapabilityRegistry(AskDataProperties properties,
			ObjectProvider<AskDataCapabilityProvider> provider) {
		return new DefaultAskDataCapabilityRegistry("0.1.1-SNAPSHOT",
				properties.getState().isClusterMode(), provider.orderedStream().collect(Collectors.toList()));
	}

	@Bean(name = "askDataMemoryVectorCapability")
	@ConditionalOnProperty(prefix = "fw.plus.ai.askdata.state", name = "cluster-mode", havingValue = "false",
			matchIfMissing = true)
	public AskDataCapabilityProvider askDataMemoryVectorCapability() {
		return new AskDataVectorStoreProvider("memory", "fw.plus.ai.askdata.vector.memory", false,
				new InMemoryAskDataVectorStore());
	}

	@Bean(name = "askDataJdbcSharedStoreCapability")
	@ConditionalOnBean(AskDataJdbcStorageClient.class)
	public AskDataCapabilityProvider askDataJdbcSharedStoreCapability(
			ObjectProvider<AskDataJdbcStorageClient> clients) {
		List<AskDataJdbcStorageClient> values = clients.orderedStream().collect(Collectors.toList());
		if (values.size() != 1) {
			throw new IllegalStateException("AskData JDBC 状态能力必须存在且只能存在一个 "
					+ AskDataJdbcStorageClient.class.getName() + " Bean，实际数量: " + values.size());
		}
		return new AskDataSharedStoreProvider("jdbc", AskDataSharedStoreAdapter.jdbc(values.get(0)));
	}

	@Bean(name = "askDataRedisSharedStoreCapability")
	@ConditionalOnBean(AskDataRedisStorageClient.class)
	public AskDataCapabilityProvider askDataRedisSharedStoreCapability(
			ObjectProvider<AskDataRedisStorageClient> clients) {
		List<AskDataRedisStorageClient> values = clients.orderedStream().collect(Collectors.toList());
		if (values.size() != 1) {
			throw new IllegalStateException("AskData Redis 状态能力必须存在且只能存在一个 "
					+ AskDataRedisStorageClient.class.getName() + " Bean，实际数量: " + values.size());
		}
		return new AskDataSharedStoreProvider("redis", AskDataSharedStoreAdapter.redis(values.get(0)));
	}

	@Bean
	@ConditionalOnMissingBean(AskDataVectorStore.class)
	public AskDataVectorStore askDataVectorStore(AskDataProperties properties,
			AskDataCapabilityRegistry capabilityRegistry) {
		validateDefaultVectorConfiguration(properties);
		String key = properties.getVector().getStore();
		if (key == null || key.trim().length() == 0) {
			key = "memory";
		}
		return capabilityRegistry.getCapability(key.trim(),
				com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.VECTOR,
				AskDataVectorStore.class);
	}

    /**
     * 创建单节点默认长期记忆 SPI；业务可替换为持久化实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataLongTermMemory askDataLongTermMemory(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getLongTermMemoryStore(), properties.getState().getStore());
        if (!isMemory(key)) {
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataLongTermMemory.class);
        }
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
     * 在未接入业务权限服务时默认拒绝 AskData 请求，避免 Starter 以元数据推断代替授权。
     */
    @Bean
    @ConditionalOnMissingBean
    public AskDataGovernanceProvider askDataGovernanceProvider() {
        return new DefaultDenyAskDataGovernanceProvider();
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
            AskDataProperties properties, AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getConversationStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.conversations();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class);
        }
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().conversations();
    }

    /**
     * 暴露默认的 Core 检查点存储，保证恢复记录随项目和操作人共同分区。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataCheckpointStore.class)
    public AskDataCheckpointStore askDataCheckpointStore(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getCheckpointStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.checkpoints();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataCheckpointStore.class);
        }
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().checkpoints();
    }

    /**
     * 暴露默认的 Core 智能体状态存储，单机以外必须由共享存储 SPI 替换。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataAgentStateStore.class)
    public AskDataAgentStateStore askDataAgentStateStore(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getAgentStateStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.agentStates();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataAgentStateStore.class);
        }
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().agentStates();
    }

    @Bean
    @ConditionalOnMissingBean(AskDataLeaseStore.class)
    public AskDataLeaseStore askDataLeaseStore(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getLeaseStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.leases();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataLeaseStore.class);
        }
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().leases();
    }

    @Bean
    @ConditionalOnMissingBean(AskDataIdempotencyKeyStore.class)
    public AskDataIdempotencyKeyStore askDataIdempotencyKeyStore(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getIdempotencyStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.idempotencyKeys();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataIdempotencyKeyStore.class);
        }
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().idempotencyKeys();
    }

    @Bean
    @ConditionalOnMissingBean(AskDataAuditStateStore.class)
    public AskDataAuditStateStore askDataAuditStateStore(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getAuditStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.auditStates();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataAuditStateStore.class);
        }
        validateLocalStateConfiguration(properties);
        return InMemoryAskDataStores.singleNode().auditStates();
    }

    /**
     * 暴露默认的基础监控日志存储。日志由项目和操作人共同隔离，且只可保存已脱敏摘要。
     */
    @Bean
    @ConditionalOnMissingBean(AskDataMonitoringLogStore.class)
    public AskDataMonitoringLogStore askDataMonitoringLogStore(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        String key = configuredStore(properties.getState().getMonitoringStore(), properties.getState().getStore());
        if (!isMemory(key)) {
			AskDataSharedStoreAdapter shared = sharedStore(capabilityRegistry, key);
			if (shared != null) {
				return shared.monitoringLogs();
			}
            return capabilityRegistry.getCapability(key,
                    com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
                    AskDataMonitoringLogStore.class);
        }
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
    public AskDataEventPublisher askDataMonitoringLogEventPublisher(AskDataMonitoringLogStore monitoringLogStore,
            AskDataProperties properties) {
        Integer capacity = properties.getState().getMonitoringLogCapacity();
        int fallbackCapacity = capacity == null || capacity.intValue() <= 0 ? 1000 : capacity.intValue();
        return AskDataEventPublisher.toStore(monitoringLogStore,
                new InMemoryAskDataMonitoringLogStore(fallbackCapacity));
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
    public AskDataMonitoringLogQueryService askDataMonitoringLogQueryService(AskDataMonitoringLogStore store,
            AskDataMonitoringLogAccessPolicy accessPolicy, AskDataGovernanceProvider governanceProvider) {
        return new AskDataMonitoringLogQueryService(store, accessPolicy, governanceProvider);
    }

    @Bean(name = "askDataObservationEventPublisher")
    @ConditionalOnMissingBean(name = "askDataObservationEventPublisher")
    public AskDataEventPublisher askDataObservationEventPublisher(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry) {
        List<AskDataObservationExporter> exporters = new ArrayList<AskDataObservationExporter>();
        if (properties.getObservability().isEnabled()) {
            for (String name : properties.getObservability().getExporters()) {
                String key = "otel".equalsIgnoreCase(name) ? "opentelemetry" : name;
                exporters.add(capabilityRegistry.getCapability(key,
                        com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.OBSERVABILITY_EXPORTER,
                        AskDataObservationExporter.class));
            }
        }
        Integer capacity = properties.getState().getMonitoringLogCapacity();
        int diagnosticCapacity = capacity == null || capacity.intValue() <= 0 ? 1000 : capacity.intValue();
        AskDataEventPublisher diagnosticPublisher = AskDataEventPublisher.toStore(
                new InMemoryAskDataMonitoringLogStore(diagnosticCapacity));
        return new AsyncAskDataObservationExporter(exporters,
                new org.springframework.core.task.SimpleAsyncTaskExecutor("askdata-observability-"),
                diagnosticPublisher);
    }

    @Bean(name = "askDataLangSmithObservationCapability")
    @ConditionalOnBean(AskDataObservationTransport.class)
    public AskDataCapabilityProvider askDataLangSmithObservationCapability(AskDataObservationTransport transport) {
        return new AskDataObservationExporterProvider("langsmith", "fw.plus.ai.askdata.observability.langsmith",
                new LangSmithAskDataObservationExporter(transport));
    }

    @Bean(name = "askDataLangfuseObservationCapability")
    @ConditionalOnBean(AskDataObservationTransport.class)
    public AskDataCapabilityProvider askDataLangfuseObservationCapability(AskDataObservationTransport transport) {
        return new AskDataObservationExporterProvider("langfuse", "fw.plus.ai.askdata.observability.langfuse",
                new LangfuseAskDataObservationExporter(transport));
    }

    @Bean(name = "askDataOpenTelemetryObservationCapability")
    @ConditionalOnBean(AskDataObservationTransport.class)
    public AskDataCapabilityProvider askDataOpenTelemetryObservationCapability(AskDataObservationTransport transport) {
        return new AskDataObservationExporterProvider("opentelemetry", "fw.plus.ai.askdata.observability.otel",
                new OpenTelemetryAskDataObservationExporter(transport));
    }

	@Bean
	@ConditionalOnMissingBean
	public AskDataModelRegistry askDataModelRegistry(AskDataProperties properties,
			ObjectProvider<AiModelRegistry> modelRegistryProvider) {
		return new AiModelAskDataModelRegistry(properties, modelRegistryProvider.getIfAvailable());
	}

    @Bean
    @ConditionalOnMissingBean(com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter.class)
    public com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter askDataModelRouter() {
        return new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
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
                AskDataAuthorizationSummary authorizationSummary = command.getAuthorizationSummary();
                if (!hasValidQueryBinding(command, authorizationSummary)) {
                    return Result.failure(FailureCategory.POLICY_DENIED, "授权摘要与当前 SQL 请求不匹配");
                }
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
                List<String> tokens = coreSqlTokens(normalizedSql);
                for (int index = 0; index < tokens.size() - 1; index++) {
                    String token = tokens.get(index);
                    if ("from".equals(token) || "join".equals(token)) {
                        String object = coreObjectName(tokens.get(index + 1));
                        if (object.length() == 0 || !command.getAuthorizedObjects().contains(object)) {
                            return Result.failure(FailureCategory.UNAUTHORIZED_OBJECT, "对象未授权: " + object);
                        }
                    }
                }
                AskDataMetadata authorizationMetadata = authorizationMetadata(authorizationSummary);
                String columnFailure = AskDataSqlAuthorizationProcessor.validateColumns(command.getSql(),
                        authorizationMetadata);
                if (columnFailure != null) {
                    return Result.failure(FailureCategory.UNAUTHORIZED_OBJECT, columnFailure);
                }
                return Result.success();
            }
        };
    }

    private static boolean hasValidQueryBinding(
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand command,
            AskDataAuthorizationSummary authorizationSummary) {
        if (command == null || authorizationSummary == null || !authorizationSummary.isAllowed()
                || !AskDataAuthorizationPurpose.QUERY.equals(authorizationSummary.getPurpose())
                || !AskDataAdministrativeScope.none().equals(authorizationSummary.getAdministrativeScope())) {
            return false;
        }
        try {
            AskDataAuthorizationRequest request = new AskDataAuthorizationRequest(command.getContext(),
                    authorizationSummary.getDatasourceKey(), AskDataAuthorizationPurpose.QUERY,
                    AskDataAdministrativeScope.none());
            if (!authorizationSummary.isBoundTo(request)) {
                return false;
            }
            Set<String> authorizedTables = new LinkedHashSet<String>();
            for (AskDataAuthorizedObject object : authorizationSummary.getAuthorizedObjects()) {
                if (object == null || !authorizationSummary.getDatasourceKey().equals(object.getDatasourceKey())) {
                    return false;
                }
                authorizedTables.add(object.getTableName());
            }
            return command.getAuthorizedObjects().containsAll(authorizedTables);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static AskDataMetadata authorizationMetadata(AskDataAuthorizationSummary authorizationSummary) {
        Map<String, List<AskDataColumn>> columnsByTable = new LinkedHashMap<String, List<AskDataColumn>>();
        for (AskDataAuthorizedObject object : authorizationSummary.getAuthorizedObjects()) {
            List<AskDataColumn> columns = columnsByTable.get(object.getTableName());
            if (columns == null) {
                columns = new ArrayList<AskDataColumn>();
                columnsByTable.put(object.getTableName(), columns);
            }
            if (!containsColumn(columns, object.getColumnName())) {
                columns.add(new AskDataColumn(object.getColumnName(), "authorized"));
            }
        }
        List<AskDataTable> tables = new ArrayList<AskDataTable>();
        for (Map.Entry<String, List<AskDataColumn>> entry : columnsByTable.entrySet()) {
            tables.add(new AskDataTable(entry.getKey(), entry.getValue(), containsColumn(entry.getValue(), "*")));
        }
        return new AskDataMetadata(null, tables, Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList());
    }

    private static boolean containsColumn(List<AskDataColumn> columns, String name) {
        for (AskDataColumn column : columns) {
            if (column != null && (name == null ? column.getName() == null : name.equals(column.getName()))) {
                return true;
            }
        }
        return false;
    }

	@Bean
	@ConditionalOnMissingBean
	public AskDataSqlExecutor askDataSqlExecutor(AskDataProperties properties,
			ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
		Integer defaultMaxRows = properties.getSql().getDefaultMaxRows();
		Integer queryTimeoutSeconds = properties.getSql().getQueryTimeoutSeconds();
		return new JdbcAskDataSqlExecutor(jdbcTemplateProvider,
				defaultMaxRows == null ? 100 : defaultMaxRows.intValue(),
				queryTimeoutSeconds == null ? 30 : queryTimeoutSeconds.intValue(),
				properties.getSql().isReadOnlyConnectionRequired());
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
            AskDataCapabilityRegistry capabilityRegistry,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator validator,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit audit) {
        List<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor> interceptors =
                new ArrayList<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor>();
        List<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResultMapper> resultMappers =
                new ArrayList<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResultMapper>();
        for (AskDataCapabilityProvider provider : capabilityRegistry.listProviders(
                com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.SQL_PLUGIN)) {
            Object capability = provider.getCapability();
            if (capability instanceof com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor) {
                interceptors.add((com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor) capability);
            }
            if (capability instanceof com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResultMapper) {
                resultMappers.add((com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResultMapper) capability);
            }
        }
        return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain(
                interceptors, resultMappers, validator, audit);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataVectorRetriever askDataVectorRetriever(AskDataProperties properties,
            ObjectProvider<AiEmbeddingStoreRegistry> embeddingStoreRegistryProvider,
            ObjectProvider<AskDataModelRegistry> modelRegistryProvider,
            ObjectProvider<AiModelRegistry> aiModelRegistryProvider,
            com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter modelRouter) {
        return createAskDataVectorRetriever(properties, embeddingStoreRegistryProvider, modelRegistryProvider,
                aiModelRegistryProvider.getIfAvailable(), modelRouter);
    }

    public AskDataVectorRetriever askDataVectorRetriever(AskDataProperties properties,
            ObjectProvider<AiEmbeddingStoreRegistry> embeddingStoreRegistryProvider,
            ObjectProvider<AskDataModelRegistry> modelRegistryProvider) {
        return createAskDataVectorRetriever(properties, embeddingStoreRegistryProvider, modelRegistryProvider,
                null, null);
    }

    private AskDataVectorRetriever createAskDataVectorRetriever(AskDataProperties properties,
            ObjectProvider<AiEmbeddingStoreRegistry> embeddingStoreRegistryProvider,
            ObjectProvider<AskDataModelRegistry> modelRegistryProvider, AiModelRegistry aiModelRegistry,
            com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter modelRouter) {
        validateDefaultVectorConfiguration(properties);
        AiEmbeddingStoreRegistry registry = embeddingStoreRegistryProvider.getIfAvailable();
        AskDataModelRegistry modelRegistry = modelRegistryProvider.getIfAvailable();
        if (registry == null || (modelRegistry == null && aiModelRegistry == null)) {
            return new CompositeAskDataVectorRetriever(Collections.<AskDataVectorRetriever>emptyList(),
                    properties.getVector().getDefaultTopK());
        }
        return new AiEmbeddingStoreAskDataVectorRetriever(registry, modelRegistry, aiModelRegistry, modelRouter,
                properties, properties.getVector().getStore());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataAgentDispatcher askDataAgentDispatcher(ObjectProvider<AskDataSubAgent> subAgentProvider) {
        return new SequentialAskDataAgentDispatcher(subAgentProvider.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean(com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime.class)
    public com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime askDataAgentGraphRuntime(
            AskDataProperties properties, AskDataCapabilityRegistry capabilityRegistry) {
        String key = properties.getFlow().getGraphRuntime();
        if (key == null || key.trim().length() == 0 || "sequential".equalsIgnoreCase(key)) {
            return new com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphExecutor();
        }
        return capabilityRegistry.getCapability(key,
                com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.RUNTIME_ADAPTER,
                com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepRegistry askDataAgentStepRegistry(
            ObjectProvider<com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent> agentProvider) {
        return new com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepRegistry(
                agentProvider.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataConversationStore askDataConversationStore(
            com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore conversationStore,
            AskDataCheckpointStore checkpointStore) {
        return new CoreAskDataConversationStoreAdapter(conversationStore, checkpointStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataContextCompressor askDataContextCompressor(AskDataProperties properties,
            AskDataCapabilityRegistry capabilityRegistry,
            @Qualifier("askDataMonitoringLogEventPublisher") AskDataEventPublisher eventPublisher) {
        AskDataContextCompressor deterministic = new DefaultAskDataContextCompressor(properties);
        String key = properties.getConversation().getCompressor();
        if (key == null || key.trim().length() == 0 || "deterministic".equalsIgnoreCase(key)) {
            return deterministic;
        }
        com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressor semantic =
                capabilityRegistry.getCapability(key,
                        com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.RUNTIME_ADAPTER,
                        com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressor.class);
        return new FallbackAskDataContextCompressor(
                new CoreAskDataContextCompressorAdapter(semantic, deterministic), deterministic, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataRuntimeStateServices askDataRuntimeStateServices(AskDataAgentStateStore agentStateStore,
            AskDataLeaseStore leaseStore, AskDataIdempotencyKeyStore idempotencyKeyStore,
            AskDataAuditStateStore auditStateStore, AskDataLongTermMemory longTermMemory,
            com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime graphRuntime) {
        return new AskDataRuntimeStateServices(agentStateStore, leaseStore, idempotencyKeyStore, auditStateStore,
                longTermMemory, graphRuntime);
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
            ObjectProvider<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain> sqlInterceptorChainProvider,
            ObjectProvider<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator> coreSqlValidatorProvider,
            AskDataGovernanceProvider governanceProvider,
            ObjectProvider<com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel> coreChatModelProvider,
            com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter modelRouter,
            ObjectProvider<AiModelRegistry> aiModelRegistryProvider,
            com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepRegistry agentStepRegistry,
            AskDataRuntimeStateServices runtimeStateServices) {
        List<AskDataAgent> mainAgents = agentProvider.orderedStream()
                .filter(agent -> !(agent instanceof AskDataSubAgent))
                .collect(Collectors.toList());
        com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel coreChatModel =
                coreChatModelProvider.getIfAvailable();
        if (coreChatModel == null) {
            AiModelRegistry aiModelRegistry = aiModelRegistryProvider.getIfAvailable();
            if (aiModelRegistry != null) {
                coreChatModel = new AiModelRegistryAskDataChatModel(aiModelRegistry);
            }
        }
        return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                promptTemplateProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, contextCompressor,
                agentDispatcher, mainAgents, combineEventPublishers(monitoringLogEventPublisher, eventPublisherProvider),
                questionPlanner, sqlInterceptorChainProvider.getIfAvailable(), coreSqlValidatorProvider.getIfAvailable(),
                governanceProvider, coreChatModel, modelRouter, agentStepRegistry, runtimeStateServices);
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

    private static String configuredStore(String categoryStore, String legacyStore) {
        String category = categoryStore == null ? null : categoryStore.trim();
        String legacy = legacyStore == null ? null : legacyStore.trim();
        if ((category == null || category.length() == 0 || "memory".equalsIgnoreCase(category))
                && legacy != null && legacy.length() > 0 && !"memory".equalsIgnoreCase(legacy)) {
            return legacy;
        }
        return category == null || category.length() == 0 ? "memory" : category;
    }

	private AskDataSharedStoreAdapter sharedStore(AskDataCapabilityRegistry capabilityRegistry, String key) {
		if (!"jdbc".equalsIgnoreCase(key) && !"redis".equalsIgnoreCase(key)) {
			return null;
		}
		try {
			return capabilityRegistry.getCapability(key.toLowerCase(),
					com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType.STORE,
					AskDataSharedStoreAdapter.class);
		} catch (IllegalArgumentException ex) {
			String clientType = "jdbc".equalsIgnoreCase(key) ? AskDataJdbcStorageClient.class.getName()
					: AskDataRedisStorageClient.class.getName();
			throw new IllegalStateException("AskData 状态配置选择 " + key + " 时必须提供唯一 " + clientType
					+ " Bean 并注册共享 STORE 能力", ex);
		}
	}

    private static boolean isMemory(String key) {
        return "memory".equalsIgnoreCase(key);
    }

    private void validateDefaultVectorConfiguration(AskDataProperties properties) {
        if (properties.getState().isClusterMode() && "memory".equalsIgnoreCase(properties.getVector().getStore())) {
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

    private List<String> coreSqlTokens(String sql) {
        List<String> tokens = new ArrayList<String>();
        String stripped = stripSqlComments(sql);
        StringBuilder token = new StringBuilder();
        for (int index = 0; index < stripped.length(); index++) {
            char ch = stripped.charAt(index);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '`' || ch == '"'
                    || ch == '[' || ch == ']') {
                token.append(Character.toLowerCase(ch));
            } else if (token.length() > 0) {
                tokens.add(token.toString());
                token.setLength(0);
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    private String stripSqlComments(String sql) {
        StringBuilder stripped = new StringBuilder(sql.length());
        for (int index = 0; index < sql.length(); index++) {
            char ch = sql.charAt(index);
            if (ch == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                stripped.append(' ');
                index += 2;
                while (index + 1 < sql.length() && !(sql.charAt(index) == '*' && sql.charAt(index + 1) == '/')) {
                    index++;
                }
                if (index + 1 < sql.length()) {
                    index++;
                }
                stripped.append(' ');
            } else if (ch == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                stripped.append(' ');
                index += 2;
                while (index < sql.length() && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
                    index++;
                }
                stripped.append(' ');
            } else {
                stripped.append(ch);
            }
        }
        return stripped.toString();
    }

    private String coreObjectName(String token) {
        String object = token;
        while (object.startsWith("`") || object.startsWith("\"") || object.startsWith("[")) {
            object = object.substring(1);
        }
        while (object.endsWith("`") || object.endsWith("\"") || object.endsWith("]")) {
            object = object.substring(0, object.length() - 1);
        }
        return object;
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
                try {
                    monitoringLogEventPublisher.publish(event);
                } catch (RuntimeException monitoringFailure) {
                    // 自定义基础发布器也不能改变 AskData 业务结果。
                }
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
