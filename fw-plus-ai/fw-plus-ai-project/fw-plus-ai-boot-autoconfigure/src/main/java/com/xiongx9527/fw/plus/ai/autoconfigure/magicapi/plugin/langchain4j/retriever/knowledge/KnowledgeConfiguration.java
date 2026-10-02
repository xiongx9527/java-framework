package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jScoringModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth.KbSaTokenAuthChecker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.module.KnowledgeModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.DocumentPipeline;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbIndexingExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.KnowledgeContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDatasetService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDocumentService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbIndexingService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbKnowledgeState;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSegmentService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.KbOriginalFileStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.LocalKbOriginalFileStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbConsoleController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbOpenApiController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * knowledge 知识库装配（设计 §2.1）：content-retriever 插件的知识库后端实现（管理端、索引管道、
 * knowledge 检索器、对外 API）。
 *
 * <p>装配条件：SqlSessionFactory 存在（元数据表落应用主数据源）、总开关
 * {@code fw.plus.ai.knowledge.enabled} 未关闭（默认开启）。应用未配置 JDBC 数据源时知识库
 * 整体不装配，不影响其余 langchain4j 功能。启动时校验三张元数据表存在性，缺失时警告并禁用。</p>
 *
 * <p>knowledge 检索器工厂以 {@code ContentRetrieverProvider} bean 注册，经
 * {@code Langchain4jContentRetrieverConfiguration} 的分发表收集进入短名 {@code knowledge}。</p>
 *
 * <p>管理端 UI 为独立插件产物（magic-kb-browser iife.js）：本装配生效（知识库启动）时才声明
 * {@code langchain4j-knowledge} 插件并注册 {@code /kb/**} Controller；控制台入口按
 * {@code magic-api.plugin.knowledge.admin.enabled} 开关激活（默认关闭，与 s3/kafka/db 等插件
 * 的 admin.enabled 惯例一致——不启动，不激活）。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SqlSessionFactory.class)
@ConditionalOnBean(SqlSessionFactory.class)
@ConditionalOnProperty(prefix = "fw.plus.ai.knowledge", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class KnowledgeConfiguration {

	@Autowired
	private ObjectProvider<KbDatasetService> datasetServices;

	@Autowired
	private ObjectProvider<KbDocumentService> documentServices;

	@Autowired
	private ObjectProvider<KbSegmentService> segmentServices;

	@Autowired
	private ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService> metadataFieldServices;

	@Autowired
	private ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbIndexingService> indexingServices;

	/**
	 * knowledge 管理端插件：声明专属前端产物（magic-kb-browser iife.js，toolbars 四视图入口）
	 * 并注册 {@code /kb/**} Controller。控制台入口经 {@code magic-api.plugin.knowledge.admin.enabled}
	 * 激活（默认关闭）；仅知识库装配成功时本 bean 存在，插件声明随之生效。
	 */
	@Bean
	public MagicPluginConfiguration kbBrowserPluginConfiguration() {
		return new MagicPluginConfiguration() {

			@Override
			public Plugin plugin() {
				Plugin plugin = new Plugin("langchain4j-knowledge", "MagicKbBrowser",
						"magic-kb-browser.1.0.0.iife.js");
				String adminEnabled = SpringUtils.getAppConfig("magic-api.plugin.knowledge.admin.enabled", "false");
				plugin.setAdminEnabled(Boolean.parseBoolean(adminEnabled));
				return plugin;
			}

			@Override
			public MagicControllerRegister controllerRegister() {
				return (mapping, configuration) -> {
					KbDatasetService datasetService = datasetServices.getIfAvailable();
					KbDocumentService documentService = documentServices.getIfAvailable();
					KbSegmentService segmentService = segmentServices.getIfAvailable();
					com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService =
							metadataFieldServices.getIfAvailable();
					com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbIndexingService indexingService =
							indexingServices.getIfAvailable();
					if (datasetService == null || documentService == null || segmentService == null
							|| metadataFieldService == null || indexingService == null) {
						return;
					}
					mapping.registerController(new KbConsoleController(configuration, datasetService,
							documentService, segmentService, metadataFieldService, indexingService));
				};
			}
		};
	}

	// ---------------- 配置与存储 ----------------

	@Bean
	@ConfigurationProperties(prefix = "fw.plus.ai.knowledge")
	public KnowledgeConfiguration.KnowledgeProperties knowledgeProperties() {
		return new KnowledgeConfiguration.KnowledgeProperties();
	}

	@Bean
	public KbOriginalFileStore kbOriginalFileStore(KnowledgeConfiguration.KnowledgeProperties properties,
			ObjectProvider<com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module.S3Module> s3Modules) {
		// 文件存储介质可配置：local 本地目录 / s3 复用 fw-plus-boot magicapi s3 插件（s3 数据源）
		if ("s3".equalsIgnoreCase(properties.getStorageType())) {
			return new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.PluginS3KbOriginalFileStore(
					s3Modules, properties.getS3Key(), "kb");
		}
		return new LocalKbOriginalFileStore(properties.getStoragePath());
	}

	// ---------------- Mapper 显式注册（应用 MapperScan 不覆盖本包） ----------------

	@Bean
	public MapperFactoryBean<KbDatasetMapper> kbDatasetMapper(SqlSessionFactory sqlSessionFactory) {
		return mapper(KbDatasetMapper.class, sqlSessionFactory);
	}

	@Bean
	public MapperFactoryBean<KbDocumentMapper> kbDocumentMapper(SqlSessionFactory sqlSessionFactory) {
		return mapper(KbDocumentMapper.class, sqlSessionFactory);
	}

	@Bean
	public MapperFactoryBean<KbSegmentMapper> kbSegmentMapper(SqlSessionFactory sqlSessionFactory) {
		return mapper(KbSegmentMapper.class, sqlSessionFactory);
	}

	@Bean
	public MapperFactoryBean<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper> kbMetadataFieldMapper(
			SqlSessionFactory sqlSessionFactory) {
		return mapper(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper.class,
				sqlSessionFactory);
	}

	private static <T> MapperFactoryBean<T> mapper(Class<T> type, SqlSessionFactory sqlSessionFactory) {
		MapperFactoryBean<T> factoryBean = new MapperFactoryBean<>(type);
		factoryBean.setSqlSessionFactory(sqlSessionFactory);
		return factoryBean;
	}

	// ---------------- 管道执行器 ----------------

	@Bean(destroyMethod = "close")
	public KbIndexingExecutor kbIndexingExecutor() {
		return new KbIndexingExecutor();
	}

	@Bean
	@ConditionalOnMissingBean
	public DocumentPipeline kbDocumentPipeline() {
		return new DocumentPipeline();
	}

	// ---------------- 启动校验（表存在性，缺失警告并禁用）+ waiting 轮询兜底 ----------------

	@Bean
	public KbKnowledgeState kbKnowledgeState(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper metadataFieldMapper) {
		return new KbKnowledgeState(datasetMapper, documentMapper, segmentMapper, metadataFieldMapper);
	}

	/** 全部单例就绪后启动 waiting 轮询兜底（设计 §2.3）并初始化 ES 索引（es.enabled 时）。 */
	@Bean
	public org.springframework.beans.factory.SmartInitializingSingleton kbIndexingPollingStarter(
			final KbIndexingExecutor executor, final KbIndexingService indexingService,
			final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient esClient) {
		return new org.springframework.beans.factory.SmartInitializingSingleton() {
			@Override
			public void afterSingletonsInstantiated() {
				esClient.ensureIndex();
				executor.startPolling(indexingService::pollWaiting);
			}
		};
	}

	// ---------------- 服务层 ----------------

	@Bean
	public KbIndexingService kbIndexingService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, KbOriginalFileStore fileStore, DocumentPipeline documentPipeline,
			KbIndexingExecutor executor, ContentRetrieverContext retrieverContext, KbKnowledgeState state,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient esClient) {
		return new KbIndexingService(datasetMapper, documentMapper, segmentMapper, fileStore, documentPipeline,
				executor, retrieverContext, state, esClient);
	}

	@Bean
	public com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient esSegmentSearchClient(
			org.springframework.beans.factory.ObjectProvider<com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.module.ElasticSearchModule> esModules,
			KnowledgeConfiguration.KnowledgeProperties properties) {
		return new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient(
				esModules, properties.getEsKey(), properties.getEsIndexPrefix(), properties.isEsEnabled());
	}

	@Bean
	public KbSearchService kbSearchService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<Langchain4jScoringModelModule> scoringModels, KbIndexingExecutor executor,
			KbKnowledgeState state, KnowledgeConfiguration.KnowledgeProperties properties,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient esClient) {
		return new KbSearchService(datasetMapper, documentMapper, segmentMapper, retrieverContext, scoringModels,
				executor, state, properties.isHybridSearch(), esClient);
	}

	@Bean
	public KbDocumentService kbDocumentService(KbDocumentMapper documentMapper, KbDatasetMapper datasetMapper,
			KbIndexingService indexingService, KbOriginalFileStore fileStore,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService,
			KbKnowledgeState state) {
		return new KbDocumentService(documentMapper, datasetMapper, indexingService, fileStore,
				metadataFieldService, state);
	}

	@Bean
	public com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService kbMetadataFieldService(
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper fieldMapper,
			KbDocumentMapper documentMapper, KbKnowledgeState state) {
		return new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService(
				fieldMapper, documentMapper, state);
	}

	@Bean
	public KbSegmentService kbSegmentService(KbSegmentMapper segmentMapper, KbDocumentMapper documentMapper,
			KbDatasetMapper datasetMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<KbDocumentService> documentServices, KbKnowledgeState state,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient esClient) {
		return new KbSegmentService(segmentMapper, documentMapper, datasetMapper, retrieverContext,
				documentServices, state, esClient);
	}

	@Bean
	public KbDatasetService kbDatasetService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, DocumentPipeline documentPipeline, KbSearchService searchService,
			KbIndexingService indexingService, ObjectProvider<MagicResourceService> resourceServices,
			ObjectProvider<Langchain4jContentRetrieverMagicResourceStorage> retrieverStorages,
			KbKnowledgeState state) {
		return new KbDatasetService(datasetMapper, documentMapper, segmentMapper, documentPipeline,
				searchService, indexingService, resourceServices, retrieverStorages, state);
	}

	// ---------------- 检索器与脚本入口 ----------------

	/** knowledge 类型检索器工厂：由 ContentRetrieverProviderRegistry 的 provider bean 收集注册。 */
	@Bean
	public KnowledgeContentRetrieverProvider knowledgeContentRetrieverProvider(
			ObjectProvider<KbSearchService> searchServices,
			ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService> metadataFieldServices) {
		return new KnowledgeContentRetrieverProvider(searchServices, metadataFieldServices);
	}

	/** magic-script：{@code import knowledge} 调 {@code search(datasetId, query)}。 */
	@Bean
	public KnowledgeModule knowledgeModule(KbSearchService searchService) {
		return new KnowledgeModule(searchService);
	}

	// ---------------- 对外 API ----------------

	@Bean
	@ConditionalOnMissingBean
	public KbSaTokenAuthChecker kbSaTokenAuthChecker() {
		return new KbSaTokenAuthChecker();
	}

	@Bean
	public KbOpenApiController kbOpenApiController(KbDocumentService documentService, KbSearchService searchService,
			KbDatasetService datasetService, KbSegmentService segmentService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService,
			KbSaTokenAuthChecker authChecker) {
		return new KbOpenApiController(documentService, searchService, datasetService, segmentService,
				metadataFieldService, authChecker);
	}

	/** 知识库配置项（设计 §5）：总开关与原始文件目录。 */
	public static class KnowledgeProperties {

		/** 总开关（由 @ConditionalOnProperty 读取，此处保留绑定供扩展） */
		private boolean enabled = true;

		/** 原始文件目录（storage-type=local 时生效），默认 ${user.home}/.fw-plus/kb */
		private String storagePath;

		/** 文件存储介质：local 本地目录（默认）/ s3 复用 fw-plus-boot magicapi s3 插件（s3 数据源） */
		private String storageType = "local";

		/** s3 数据源资源 key（storage-type=s3 时生效；空则取 Spring 默认 s3 数据源） */
		private String s3Key;

		/** 混合检索开关：向量路 + 关键词路融合（默认开启；关闭则仅向量路） */
		private boolean hybridSearch = true;

		/** ES 全文检索开关：开启后关键词路优先走 ES（需启用 magicapi elasticsearch 插件并配置 ES 数据源） */
		private boolean esEnabled = false;

		/** ES 数据源资源 key（空则取 Spring 默认 ES 数据源） */
		private String esKey;

		/** ES 索引名前缀（分段全文索引名 = 前缀 + "-segments"），默认 fw-kb */
		private String esIndexPrefix = "fw-kb";

		public boolean isHybridSearch() {
			return hybridSearch;
		}

		public void setHybridSearch(boolean hybridSearch) {
			this.hybridSearch = hybridSearch;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getStoragePath() {
			return storagePath;
		}

		public void setStoragePath(String storagePath) {
			this.storagePath = storagePath;
		}

		public String getStorageType() {
			return storageType;
		}

		public void setStorageType(String storageType) {
			this.storageType = storageType;
		}

		public String getS3Key() {
			return s3Key;
		}

		public void setS3Key(String s3Key) {
			this.s3Key = s3Key;
		}

		public boolean isEsEnabled() {
			return esEnabled;
		}

		public void setEsEnabled(boolean esEnabled) {
			this.esEnabled = esEnabled;
		}

		public String getEsKey() {
			return esKey;
		}

		public void setEsKey(String esKey) {
			this.esKey = esKey;
		}

		public String getEsIndexPrefix() {
			return esIndexPrefix;
		}

		public void setEsIndexPrefix(String esIndexPrefix) {
			this.esIndexPrefix = esIndexPrefix;
		}






	}
}
