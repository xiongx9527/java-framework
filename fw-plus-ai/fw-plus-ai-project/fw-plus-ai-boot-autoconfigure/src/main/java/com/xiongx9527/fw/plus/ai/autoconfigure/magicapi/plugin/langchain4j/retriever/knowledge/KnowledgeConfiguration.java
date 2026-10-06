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
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.KbOriginalFileStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.SettingsAwareKbOriginalFileStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbConsoleController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbOpenApiController;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * knowledge 知识库装配（设计 §2.1）：content-retriever 插件的知识库后端实现（管理端、索引管道、
 * knowledge 检索器、对外 API）。
 *
 * <p>装配条件：SqlSessionFactory 存在（元数据表落应用主数据源）。应用未配置 JDBC 数据源时知识库
 * 整体不装配，不影响其余 langchain4j 功能。启动时校验四张业务元数据表存在性，缺失时警告并禁用
 * （{@code kb_setting} 不在其中：该表缺失只回落默认设置，不影响检索功能）。</p>
 *
 * <p><b>无 yaml 总开关</b>：原先的 {@code magic-api.plugin.knowledge.enabled} 是
 * {@code @ConditionalOnProperty} 装配条件，装配期决策、控制台页面上配不到。现全部设置项落
 * {@code kb_setting} 单行表，由 {@link KbSettingService} 持有快照并热替换，各消费方每次现读，
 * 控制台「设置」页保存即生效。其中 {@code enabled} 降级为**运行期**开关（控制
 * {@link KbKnowledgeState#checkReady()}），停用后 Bean 仍装配、控制台仍可进入以便重新启用。</p>
 *
 * <p>knowledge 检索器工厂以 {@code ContentRetrieverProvider} bean 注册，经
 * {@code Langchain4jContentRetrieverConfiguration} 的分发表收集进入短名 {@code knowledge}。</p>
 *
 * <p>管理端 UI 为独立插件产物（magic-kb-browser iife.js）：本装配生效（知识库启动）时才声明
 * {@code langchain4j-knowledge} 插件并注册 {@code /kb/**} Controller；控制台入口按
 * {@code magic-api.plugin.knowledge.admin.enabled} 开关激活（默认关闭，与 s3/kafka/db 等插件
 * 的 admin.enabled 惯例一致——不启动，不激活）。该开关属控制台可见性惯例，不在知识库设置页范围内。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SqlSessionFactory.class)
@ConditionalOnBean(SqlSessionFactory.class)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.knowledge", name = "enabled",
		havingValue = "true", matchIfMissing = true)
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

	@Autowired
	private ObjectProvider<KbSettingService> settingServices;

	@Autowired
	private ObjectProvider<InstanceAuthRegistry> authRegistries;

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
				String adminEnabled = SpringUtils.getAppConfig("magic-api.plugin.knowledge.admin.enabled", "true");
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
					KbSettingService settingService = settingServices.getIfAvailable();
					if (datasetService == null || documentService == null || segmentService == null
							|| metadataFieldService == null || indexingService == null || settingService == null) {
						return;
					}
					InstanceAuthRegistry authRegistry = authRegistries.getIfAvailable(InstanceAuthRegistry::new);
					mapping.registerController(new KbConsoleController(configuration, datasetService,
							documentService, segmentService, metadataFieldService, indexingService, settingService,
							authRegistry));
				};
			}
		};
	}

	// ---------------- 配置与存储 ----------------

	@Bean
	public KbSettingService kbSettingService(
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSettingMapper settingMapper) {
		return new KbSettingService(settingMapper);
	}

	@Bean
	public KbOriginalFileStore kbOriginalFileStore(KbSettingService settingService,
			ObjectProvider<com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module.S3Module> s3Modules) {
		// 按当前设置委派，实现 storageType/storagePath/s3Key 的页面热生效
		return new SettingsAwareKbOriginalFileStore(settingService, s3Modules);
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
	public MapperFactoryBean<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSettingMapper> kbSettingMapper(
			SqlSessionFactory sqlSessionFactory) {
		return mapper(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSettingMapper.class,
				sqlSessionFactory);
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
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper metadataFieldMapper,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService settingService) {
		return new KbKnowledgeState(datasetMapper, documentMapper, segmentMapper, metadataFieldMapper, settingService);
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
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService settingService) {
		return new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient(
				esModules, settingService);
	}

	@Bean
	public KbSearchService kbSearchService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<Langchain4jScoringModelModule> scoringModels, KbIndexingExecutor executor,
			KbKnowledgeState state,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService settingService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.es.EsSegmentSearchClient esClient) {
		return new KbSearchService(datasetMapper, documentMapper, segmentMapper, retrieverContext, scoringModels,
				executor, state, settingService, esClient);
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
			KbKnowledgeState state, KbSettingService settingService) {
		return new KbDatasetService(datasetMapper, documentMapper, segmentMapper, documentPipeline,
				searchService, indexingService, resourceServices, retrieverStorages, state, settingService);
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

	/**
	 * 实例认证内核：容器已有（如同时启用 MCP/WS）时不重复创建，供知识库对外 API 按 provider 解析。
	 */
	@Bean
	@ConditionalOnMissingBean
	public InstanceAuthRegistry instanceAuthRegistry() {
		return new InstanceAuthRegistry();
	}

	@Bean
	@ConditionalOnMissingBean
	public KbSaTokenAuthChecker kbSaTokenAuthChecker(InstanceAuthRegistry instanceAuthRegistry,
			KbSettingService kbSettingService) {
		return new KbSaTokenAuthChecker(instanceAuthRegistry, kbSettingService);
	}

	@Bean
	public KbOpenApiController kbOpenApiController(KbDocumentService documentService, KbSearchService searchService,
			KbDatasetService datasetService, KbSegmentService segmentService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService settingService,
			KbSaTokenAuthChecker authChecker) {
		return new KbOpenApiController(documentService, searchService, datasetService, segmentService,
				metadataFieldService, settingService, authChecker);
	}


}
