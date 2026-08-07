package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.DefaultMagicTinyflowDifyConnectionTester;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.DefaultMagicTinyflowKnowledgeOperations;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.DefaultMagicTinyflowKnowledgeOptionProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicApiTinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicApiTinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeAuditSink;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicResourceTinyflowKnowledgeAuditSink;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeHttpController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeOperations;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeOptionProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgePermissionEvaluator;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowSafeUrlImporter;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.provider.MagicApiTinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.MagicApiTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.web.MagicTinyflowController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify.TinyflowKnowledgeAccessKeyService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.local.DefaultTinyflowIngestionService;
import com.xiongx9527.fw.plus.ai.tinyflow.local.FwPlusLocalKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.NormalChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.ParentChildChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.QaChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowLocalIndexer;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowKnowledgeMaintenanceService;
import com.xiongx9527.fw.plus.ai.tinyflow.local.parser.CompositeTinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval.TinyflowLocalRetriever;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import dev.tinyflow.core.chain.runtime.ChainExecutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@AutoConfigureAfter(value = MagicAPIBaseAutoConfiguration.class,
		name = "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration")
@AutoConfigureBefore(TinyflowAutoConfiguration.class)
@ConditionalOnClass({MagicResourceService.class, ChainExecutor.class})
@ConditionalOnBean(MagicResourceService.class)
@ConditionalOnExpression("${magic-api.plugin.tinyflow.enabled:true} && ${fw.plus.ai.tinyflow.enabled:true}")
public class MagicAPITinyflowConfiguration implements MagicPluginConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TinyflowWorkflowRepository magicApiTinyflowWorkflowRepository() {
		return new MagicApiTinyflowWorkflowRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MagicDynamicVectorDataSource.class)
	public TinyflowVectorStoreProvider magicApiTinyflowVectorStoreProvider(MagicDynamicVectorDataSource dataSource) {
		return new MagicApiTinyflowVectorStoreProvider(dataSource);
	}

	@Bean public TinyflowKnowledgeMagicResourceStorage tinyflowKnowledgeMagicResourceStorage() { return new TinyflowKnowledgeMagicResourceStorage(); }
	@Bean public TinyflowDocumentMagicResourceStorage tinyflowDocumentMagicResourceStorage() { return new TinyflowDocumentMagicResourceStorage(); }
	@Bean public TinyflowChunkMagicResourceStorage tinyflowChunkMagicResourceStorage() { return new TinyflowChunkMagicResourceStorage(); }
	@Bean public TinyflowIngestionTaskMagicResourceStorage tinyflowIngestionTaskMagicResourceStorage() { return new TinyflowIngestionTaskMagicResourceStorage(); }
	@Bean public TinyflowKnowledgeKeyMagicResourceStorage tinyflowKnowledgeKeyMagicResourceStorage() { return new TinyflowKnowledgeKeyMagicResourceStorage(); }
	@Bean public TinyflowDifyConnectionMagicResourceStorage tinyflowDifyConnectionMagicResourceStorage() { return new TinyflowDifyConnectionMagicResourceStorage(); }

	@Bean @ConditionalOnMissingBean
	public MagicApiTinyflowKnowledgeRepository magicApiTinyflowKnowledgeRepository(MagicResourceService resources) {
		return new MagicApiTinyflowKnowledgeRepository(resources);
	}

	@Bean @ConditionalOnMissingBean
	public TinyflowKnowledgeAccessKeyService tinyflowKnowledgeAccessKeyService(MagicApiTinyflowKnowledgeRepository repository) {
		return new TinyflowKnowledgeAccessKeyService(repository);
	}

	@Bean @ConditionalOnMissingBean
	public MagicTinyflowTenantResolver magicTinyflowTenantResolver() {
		return request -> {
			MagicUser user = request == null ? null : (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
			if (user == null || user.getId() == null || user.getId().trim().length() == 0) {
				throw new SecurityException("Tinyflow tenant is unavailable");
			}
			return user.getId().trim();
		};
	}

	@Bean @ConditionalOnMissingBean
	public MagicTinyflowKnowledgePermissionEvaluator magicTinyflowKnowledgePermissionEvaluator(
			ObjectProvider<MagicConfiguration> configurations) {
		return (tenantId, action, resourceId, request) -> {
			if (request == null) throw new SecurityException("Tinyflow request is unavailable");
			Object principal = request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
			if (!(principal instanceof MagicUser)) throw new SecurityException("Tinyflow user is unavailable");
			MagicUser user = (MagicUser) principal;
			if (user.getId() == null || user.getId().trim().length() == 0) {
				throw new SecurityException("Tinyflow user is unavailable");
			}
			MagicConfiguration configuration = configurations.getIfAvailable();
			if (configuration == null || configuration.getAuthorizationInterceptor() == null
					|| !configuration.getAuthorizationInterceptor().allowVisit(
							user, request, knowledgeAuthorization(action))) {
				throw new SecurityException("Tinyflow permission denied");
			}
		};
	}

	private Authorization knowledgeAuthorization(String action) {
		if (action == null || action.trim().length() == 0) {
			throw new SecurityException("Tinyflow action is unavailable");
		}
		if (action.endsWith(":delete")) return Authorization.DELETE;
		if (action.endsWith(":view") || action.endsWith(":list") || action.startsWith("option:")
				|| "knowledge:recall".equals(action) || "dify:test".equals(action)) {
			return Authorization.VIEW;
		}
		return Authorization.SAVE;
	}

	@Bean @ConditionalOnMissingBean
	public MagicTinyflowKnowledgeAuditSink magicTinyflowKnowledgeAuditSink(MagicResourceService resources) {
		return new MagicResourceTinyflowKnowledgeAuditSink(resources);
	}

	@Bean @ConditionalOnMissingBean
	public MagicTinyflowSafeUrlImporter magicTinyflowSafeUrlImporter() { return new MagicTinyflowSafeUrlImporter(); }

	@Bean @ConditionalOnMissingBean @ConditionalOnBean(MagicDynamicS3DataSource.class)
	public TinyflowObjectStorage magicApiTinyflowObjectStorage(MagicDynamicS3DataSource dataSource) {
		return new MagicApiTinyflowObjectStorage(dataSource);
	}

	@Bean @ConditionalOnMissingBean
	public TinyflowDocumentParser tinyflowKnowledgeDocumentParser() { return CompositeTinyflowDocumentParser.defaults(); }

	@Bean @ConditionalOnMissingBean(NormalChunkStrategy.class)
	public NormalChunkStrategy tinyflowNormalChunkStrategy() { return new NormalChunkStrategy(); }

	@Bean @ConditionalOnMissingBean(QaChunkStrategy.class)
	public QaChunkStrategy tinyflowQaChunkStrategy() { return new QaChunkStrategy(); }

	@Bean @ConditionalOnMissingBean(ParentChildChunkStrategy.class)
	public ParentChildChunkStrategy tinyflowParentChildChunkStrategy() { return new ParentChildChunkStrategy(); }

	@Bean(name = "tinyflowKnowledgeExecutor", destroyMethod = "shutdown")
	@ConditionalOnMissingBean(name = "tinyflowKnowledgeExecutor")
	public ExecutorService tinyflowKnowledgeExecutor() {
		return Executors.newFixedThreadPool(2, new ThreadFactory() {
			private final AtomicInteger sequence = new AtomicInteger();
			@Override public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable, "tinyflow-knowledge-" + sequence.incrementAndGet());
				thread.setDaemon(true); return thread;
			}
		});
	}

	@Bean @ConditionalOnMissingBean @ConditionalOnBean({TinyflowModelProvider.class, TinyflowVectorStoreProvider.class})
	public TinyflowLocalIndexer tinyflowLocalIndexer(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowModelProvider models, TinyflowVectorStoreProvider vectors) {
		return new TinyflowLocalIndexer(repository, models, vectors, "default", "default", 64);
	}

	@Bean @ConditionalOnMissingBean @ConditionalOnBean(TinyflowLocalIndexer.class)
	public TinyflowKnowledgeMaintenanceService tinyflowKnowledgeMaintenanceService(
			MagicApiTinyflowKnowledgeRepository repository, TinyflowLocalIndexer indexer,
			TinyflowVectorStoreProvider vectors, TinyflowObjectStorage objectStorage) {
		return new TinyflowKnowledgeMaintenanceService(repository, indexer, vectors, objectStorage);
	}

	@Bean @ConditionalOnMissingBean @ConditionalOnBean(TinyflowLocalIndexer.class)
	public DefaultTinyflowIngestionService tinyflowIngestionService(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowObjectStorage objectStorage, TinyflowDocumentParser parser, NormalChunkStrategy normal,
			QaChunkStrategy qa, ParentChildChunkStrategy parentChild,
			@Qualifier("tinyflowKnowledgeExecutor") Executor executor, TinyflowLocalIndexer indexer) {
		Map<String, TinyflowChunkStrategy> strategies = new LinkedHashMap<String, TinyflowChunkStrategy>();
		strategies.put("normal", normal); strategies.put("qa", qa); strategies.put("parent-child", parentChild);
		return new DefaultTinyflowIngestionService(repository, objectStorage, parser, strategies, indexer, executor);
	}

	@Bean @ConditionalOnMissingBean @ConditionalOnBean({TinyflowModelProvider.class, TinyflowVectorStoreProvider.class})
	public TinyflowLocalRetriever tinyflowLocalRetriever(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowModelProvider models, TinyflowVectorStoreProvider vectors) {
		return new TinyflowLocalRetriever(repository, models, vectors, null);
	}

	@Bean @ConditionalOnMissingBean @ConditionalOnBean({DefaultTinyflowIngestionService.class, TinyflowLocalRetriever.class})
	public FwPlusLocalKnowledgeProvider fwPlusLocalKnowledgeProvider(MagicApiTinyflowKnowledgeRepository repository,
			DefaultTinyflowIngestionService ingestion, TinyflowLocalRetriever retriever) {
		return new FwPlusLocalKnowledgeProvider(repository, ingestion, retriever);
	}

	@Bean @ConditionalOnMissingBean
	public TinyflowKnowledgeProviderRegistry tinyflowKnowledgeProviderRegistry(List<TinyflowKnowledgeProvider> providers) {
		return new TinyflowKnowledgeProviderRegistry(providers);
	}

	@Bean @ConditionalOnMissingBean
	@ConditionalOnBean({TinyflowObjectStorage.class, DefaultTinyflowIngestionService.class,
			TinyflowKnowledgeMaintenanceService.class})
	public MagicTinyflowKnowledgeOperations magicTinyflowKnowledgeOperations(MagicApiTinyflowKnowledgeRepository repository,
			TinyflowObjectStorage storage, DefaultTinyflowIngestionService ingestion,
			TinyflowKnowledgeProviderRegistry providers, TinyflowVectorStoreProvider vectors,
			TinyflowKnowledgeMaintenanceService maintenance, MagicTinyflowKnowledgeOptionProvider options) {
		return new DefaultMagicTinyflowKnowledgeOperations(
				repository, storage, ingestion, providers, vectors, maintenance, options);
	}

	@Bean @ConditionalOnMissingBean
	@ConditionalOnBean({MagicDynamicModelDataSource.class, MagicDynamicVectorDataSource.class, MagicDynamicS3DataSource.class})
	public MagicTinyflowKnowledgeOptionProvider magicTinyflowKnowledgeOptionProvider(
			MagicDynamicModelDataSource models, MagicDynamicVectorDataSource vectors, MagicDynamicS3DataSource s3) {
		return new DefaultMagicTinyflowKnowledgeOptionProvider(models, vectors, s3);
	}

	@Bean @ConditionalOnMissingBean(MagicTinyflowKnowledgeController.DifyConnectionTester.class)
	@ConditionalOnBean(TinyflowCredentialResolver.class)
	public MagicTinyflowKnowledgeController.DifyConnectionTester magicTinyflowDifyConnectionTester(
			TinyflowCredentialResolver resolver) {
		return new DefaultMagicTinyflowDifyConnectionTester(resolver);
	}

	@Bean @ConditionalOnMissingBean
	public MagicTinyflowKnowledgeController magicTinyflowKnowledgeController(
			MagicApiTinyflowKnowledgeRepository repository, TinyflowKnowledgeAccessKeyService accessKeys,
			MagicTinyflowTenantResolver tenants, MagicTinyflowKnowledgePermissionEvaluator permissions,
			MagicTinyflowKnowledgeAuditSink audit,
			ObjectProvider<MagicTinyflowKnowledgeController.DifyConnectionTester> tester,
			ObjectProvider<MagicTinyflowKnowledgeOperations> operations,
			MagicTinyflowSafeUrlImporter urlImporter,
			ObjectProvider<MagicTinyflowKnowledgeOptionProvider> options) {
		return new MagicTinyflowKnowledgeController(repository, accessKeys, tenants, permissions, audit,
				tester.getIfAvailable(), operations.getIfAvailable(), urlImporter, options.getIfAvailable());
	}

	@Override
	public Plugin plugin() {
		return new Plugin("tinyflow", "Tinyflow", "magic-tinyflow.1.0.0.iife.js");
	}

	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> {
			mapping.registerController(new MagicTinyflowController(configuration));
			mapping.registerController(new MagicTinyflowKnowledgeHttpController(configuration));
		};
	}
}
