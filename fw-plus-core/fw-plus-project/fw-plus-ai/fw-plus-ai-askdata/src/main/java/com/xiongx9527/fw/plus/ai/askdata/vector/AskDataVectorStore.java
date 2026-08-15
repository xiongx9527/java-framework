package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.List;

/**
 * 向量数据库产品无关的写入和检索 SPI。
 *
 * <p>所有操作必须以 {@code projectId + operatorId} 隔离，并执行请求中的数据源和授权 filters。若后端无法
 * 证明过滤条件已应用，必须拒绝检索，不能取回全量结果后静默忽略。实现需要支持并发调用；集群部署应使用
 * 共享索引和一致的语义版本，默认内存实现只允许单机开发且进程重启后丢失。</p>
 */
public interface AskDataVectorStore {

	/** 将已脱敏、已授权的文档写入当前隔离范围。 */
	void upsert(AskDataProjectContext context, AskDataVectorDocument document);

	/**
	 * 在当前隔离范围内检索文档。
	 *
	 * @return 新列表；不得包含不满足请求 filters 的文档
	 */
	List<AskDataVectorDocument> search(AskDataProjectContext context, AskDataVectorSearchRequest request);
}
