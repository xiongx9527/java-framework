package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 知识库运行态：启动时校验元数据表存在性（设计 §2.1），缺失时警告并禁用知识库功能；
 * 同时叠加控制台「设置」页里的运行期总开关 {@code kb_setting.enabled}。
 *
 * <p>{@code enabled} 原先是 yaml 装配条件（{@code @ConditionalOnProperty}），装配期决策、
 * 页面上配不到；改为落库后，停用只是让业务入口前置检查失败，控制台仍可进入以便重新启用。
 *
 * <p>表校验失败同样不「一次判死」：{@code spring.sql.init} 在所有单例实例化之后才执行 DDL，
 * 早于它创建的 bean 首次读表必然失败。此时按 {@link #RETRY_INTERVAL_MILLIS} 间隔在就绪判定时重试，
 * 建表后自动恢复，无需重启。
 */
public class KbKnowledgeState implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(KbKnowledgeState.class);

	/**
	 * 表校验失败后的重试间隔（毫秒）。
	 *
	 * <p>与 {@link KbSettingService#RETRY_INTERVAL_MILLIS} 同源同值：两张表要么都随
	 * {@code spring.sql.init} 一起晚建、要么都早就在，一次判死会让刚建好的库仍需重启才能用。
	 */
	private static final long RETRY_INTERVAL_MILLIS = KbSettingService.RETRY_INTERVAL_MILLIS;

	private final KbDatasetMapper datasetMapper;

	private final KbDocumentMapper documentMapper;

	private final KbSegmentMapper segmentMapper;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper metadataFieldMapper;

	/** 知识库全局设置（运行期总开关每次现读，控制台改完立即生效） */
	private final KbSettingService settings;

	private volatile boolean tablesReady;

	private volatile long lastCheckTime;

	public KbKnowledgeState(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper metadataFieldMapper,
			KbSettingService settings) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.segmentMapper = segmentMapper;
		this.metadataFieldMapper = metadataFieldMapper;
		this.settings = settings;
	}

	@Override
	public void afterPropertiesSet() {
		refresh();
	}

	/** 就绪态：已就绪直接返回；未就绪则按间隔重试，让启动后才落表的场景能自愈。 */
	private boolean ready() {
		if (!tablesReady && System.currentTimeMillis() - lastCheckTime >= RETRY_INTERVAL_MILLIS) {
			refresh();
		}
		return tablesReady;
	}

	private void refresh() {
		tablesReady = checkTables();
		lastCheckTime = System.currentTimeMillis();
		if (tablesReady) {
			logger.info("知识库元数据表校验通过，知识库功能已启用");
			return;
		}
		logger.warn("知识库元数据表（kb_dataset/kb_document/kb_segment/kb_metadata_field）暂不可用，"
				+ "知识库功能暂禁用；请执行 docs/sql/fw-plus-ai-knowledge.sql，建表后将自动恢复，无需重启");
	}

	private boolean checkTables() {
		try {
			datasetMapper.selectCount(null);
			documentMapper.selectCount(null);
			segmentMapper.selectCount(null);
			metadataFieldMapper.selectCount(null);
			return true;
		} catch (Throwable e) {
			logger.warn("知识库表存在性校验失败: {}", e.getMessage());
			return false;
		}
	}

	/** 业务入口统一前置检查：表缺失或设置停用时给出可操作提示。 */
	public void checkReady() {
		if (!ready()) {
			throw new IllegalStateException("知识库功能不可用：元数据表缺失或校验未通过，"
					+ "请执行 docs/sql/fw-plus-ai-knowledge.sql 后重试");
		}
		if (settings != null && !settings.enabled()) {
			throw new IllegalStateException("知识库功能不可用：当前设置已停用，"
					+ "请在知识库控制台「设置」页重新启用");
		}
	}

	/** 轮询线程等非阻塞场景的就绪判断。 */
	public boolean canProcess() {
		return ready() && (settings == null || settings.enabled());
	}
}
