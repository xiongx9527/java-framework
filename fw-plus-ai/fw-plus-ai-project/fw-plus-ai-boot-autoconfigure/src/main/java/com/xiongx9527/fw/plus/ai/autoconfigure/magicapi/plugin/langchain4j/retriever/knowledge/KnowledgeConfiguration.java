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
					if (datasetService == null || documentService == null || segmentService == null) {
						return;
					}
					mapping.registerController(new KbConsoleController(configuration, datasetService,
							documentService, segmentService));
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
	@ConditionalOnMissingBean(KbOriginalFileStore.class)
	public LocalKbOriginalFileStore kbOriginalFileStore(KnowledgeConfiguration.KnowledgeProperties properties) {
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
			KbSegmentMapper segmentMapper) {
		return new KbKnowledgeState(datasetMapper, documentMapper, segmentMapper);
	}

	/** 全部单例就绪后启动 waiting 轮询兜底（设计 §2.3：入库即时触发 + 定时轮询）。 */
	@Bean
	public org.springframework.beans.factory.SmartInitializingSingleton kbIndexingPollingStarter(
			final KbIndexingExecutor executor, final KbIndexingService indexingService) {
		return new org.springframework.beans.factory.SmartInitializingSingleton() {
			@Override
			public void afterSingletonsInstantiated() {
				executor.startPolling(indexingService::pollWaiting);
			}
		};
	}

	// ---------------- 服务层 ----------------

	@Bean
	public KbIndexingService kbIndexingService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, KbOriginalFileStore fileStore, DocumentPipeline documentPipeline,
			KbIndexingExecutor executor, ContentRetrieverContext retrieverContext, KbKnowledgeState state) {
		return new KbIndexingService(datasetMapper, documentMapper, segmentMapper, fileStore, documentPipeline,
				executor, retrieverContext, state);
	}

	@Bean
	public KbSearchService kbSearchService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<Langchain4jScoringModelModule> scoringModels, KbIndexingExecutor executor,
			KbKnowledgeState state) {
		return new KbSearchService(datasetMapper, documentMapper, segmentMapper, retrieverContext, scoringModels,
				executor, state);
	}

	@Bean
	public KbDocumentService kbDocumentService(KbDocumentMapper documentMapper, KbIndexingService indexingService,
			KbOriginalFileStore fileStore, KbKnowledgeState state) {
		return new KbDocumentService(documentMapper, indexingService, fileStore, state);
	}

	@Bean
	public KbSegmentService kbSegmentService(KbSegmentMapper segmentMapper, KbDocumentMapper documentMapper,
			KbDatasetMapper datasetMapper, ContentRetrieverContext retrieverContext,
			ObjectProvider<KbDocumentService> documentServices, KbKnowledgeState state) {
		return new KbSegmentService(segmentMapper, documentMapper, datasetMapper, retrieverContext,
				documentServices, state);
	}

	@Bean
	public KbDatasetService kbDatasetService(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSearchService searchService, KbIndexingService indexingService,
			ObjectProvider<MagicResourceService> resourceServices,
			ObjectProvider<Langchain4jContentRetrieverMagicResourceStorage> retrieverStorages,
			KbKnowledgeState state) {
		return new KbDatasetService(datasetMapper, documentMapper, searchService, indexingService,
				resourceServices, retrieverStorages, state);
	}

	// ---------------- 检索器与脚本入口 ----------------

	/** knowledge 类型检索器工厂：由 ContentRetrieverProviderRegistry 的 provider bean 收集注册。 */
	@Bean
	public KnowledgeContentRetrieverProvider knowledgeContentRetrieverProvider(
			ObjectProvider<KbSearchService> searchServices) {
		return new KnowledgeContentRetrieverProvider(searchServices);
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
			KbSaTokenAuthChecker authChecker) {
		return new KbOpenApiController(documentService, searchService, authChecker);
	}

	/** 知识库配置项（设计 §5）：总开关与原始文件目录。 */
	public static class KnowledgeProperties {

		/** 总开关（由 @ConditionalOnProperty 读取，此处保留绑定供扩展） */
		private boolean enabled = true;

		/** 原始文件目录，默认 ${user.home}/.fw-plus/kb */
		private String storagePath;

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
	}
}
