package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSegmentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 知识库运行态：启动时校验三张元数据表存在性（设计 §2.1），缺失时警告并禁用知识库功能。
 */
public class KbKnowledgeState implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(KbKnowledgeState.class);

	private final KbDatasetMapper datasetMapper;

	private final KbDocumentMapper documentMapper;

	private final KbSegmentMapper segmentMapper;

	private volatile boolean tablesReady;

	public KbKnowledgeState(KbDatasetMapper datasetMapper, KbDocumentMapper documentMapper,
			KbSegmentMapper segmentMapper) {
		this.datasetMapper = datasetMapper;
		this.documentMapper = documentMapper;
		this.segmentMapper = segmentMapper;
	}

	@Override
	public void afterPropertiesSet() {
		tablesReady = checkTables();
		if (!tablesReady) {
			logger.warn("知识库元数据表（kb_dataset/kb_document/kb_segment）缺失，知识库功能禁用；"
					+ "请手工执行 META-INF/sql/fw-plus-ai-knowledge.sql 后重启");
			return;
		}
		logger.info("知识库元数据表校验通过，知识库功能已启用");
	}

	private boolean checkTables() {
		try {
			datasetMapper.selectCount(null);
			documentMapper.selectCount(null);
			segmentMapper.selectCount(null);
			return true;
		} catch (Throwable e) {
			logger.warn("知识库表存在性校验失败: {}", e.getMessage());
			return false;
		}
	}

	/** 业务入口统一前置检查：表缺失时给出可操作提示。 */
	public void checkReady() {
		if (!tablesReady) {
			throw new IllegalStateException("知识库功能不可用：元数据表缺失或校验未通过，"
					+ "请执行 META-INF/sql/fw-plus-ai-knowledge.sql 后重启");
		}
	}

	/** 轮询线程等非阻塞场景的就绪判断。 */
	public boolean canProcess() {
		return tablesReady;
	}
}
