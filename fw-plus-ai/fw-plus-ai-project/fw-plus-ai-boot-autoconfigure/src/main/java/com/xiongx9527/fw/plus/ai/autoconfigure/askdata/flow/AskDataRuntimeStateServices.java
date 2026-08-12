package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAuditStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataIdempotencyKeyStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataLeaseStore;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.publishing.AskDataInterfacePublisher;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRagRetriever;

/**
 * 默认 Flow 使用的 Core 状态 SPI 集合。
 *
 * <p>该对象只组合中立 Core 接口，不决定 JDBC、Redis 或业务实现。Starter 可为每个类别分别选择不同能力，
 * Flow 不需要知道物理存储产品。</p>
 */
public final class AskDataRuntimeStateServices {

	private final AskDataAgentStateStore agentStateStore;
	private final AskDataLeaseStore leaseStore;
	private final AskDataIdempotencyKeyStore idempotencyKeyStore;
	private final AskDataAuditStateStore auditStateStore;
	private final AskDataLongTermMemory longTermMemory;
	private final AskDataAgentGraphRuntime graphRuntime;
	private final AskDataMetadataProvider metadataProvider;
	private final AskDataEmbeddingModel embeddingModel;
	private final AskDataRagRetriever ragRetriever;
	private final AskDataInterfacePublisher interfacePublisher;

	public AskDataRuntimeStateServices(AskDataAgentStateStore agentStateStore, AskDataLeaseStore leaseStore,
			AskDataIdempotencyKeyStore idempotencyKeyStore, AskDataAuditStateStore auditStateStore,
			AskDataLongTermMemory longTermMemory) {
		this(agentStateStore, leaseStore, idempotencyKeyStore, auditStateStore, longTermMemory, null);
	}

	public AskDataRuntimeStateServices(AskDataAgentStateStore agentStateStore, AskDataLeaseStore leaseStore,
			AskDataIdempotencyKeyStore idempotencyKeyStore, AskDataAuditStateStore auditStateStore,
			AskDataLongTermMemory longTermMemory, AskDataAgentGraphRuntime graphRuntime) {
		this(agentStateStore, leaseStore, idempotencyKeyStore, auditStateStore, longTermMemory, graphRuntime,
				null, null, null, null);
	}

	public AskDataRuntimeStateServices(AskDataAgentStateStore agentStateStore, AskDataLeaseStore leaseStore,
			AskDataIdempotencyKeyStore idempotencyKeyStore, AskDataAuditStateStore auditStateStore,
			AskDataLongTermMemory longTermMemory, AskDataAgentGraphRuntime graphRuntime,
			AskDataMetadataProvider metadataProvider, AskDataEmbeddingModel embeddingModel,
			AskDataRagRetriever ragRetriever, AskDataInterfacePublisher interfacePublisher) {
		this.agentStateStore = agentStateStore;
		this.leaseStore = leaseStore;
		this.idempotencyKeyStore = idempotencyKeyStore;
		this.auditStateStore = auditStateStore;
		this.longTermMemory = longTermMemory;
		this.graphRuntime = graphRuntime;
		this.metadataProvider = metadataProvider;
		this.embeddingModel = embeddingModel;
		this.ragRetriever = ragRetriever;
		this.interfacePublisher = interfacePublisher;
	}

	public AskDataAgentStateStore getAgentStateStore() { return agentStateStore; }
	public AskDataLeaseStore getLeaseStore() { return leaseStore; }
	public AskDataIdempotencyKeyStore getIdempotencyKeyStore() { return idempotencyKeyStore; }
	public AskDataAuditStateStore getAuditStateStore() { return auditStateStore; }
	public AskDataLongTermMemory getLongTermMemory() { return longTermMemory; }
	public AskDataAgentGraphRuntime getGraphRuntime() { return graphRuntime; }
	public AskDataMetadataProvider getMetadataProvider() { return metadataProvider; }
	public AskDataEmbeddingModel getEmbeddingModel() { return embeddingModel; }
	public AskDataRagRetriever getRagRetriever() { return ragRetriever; }
	public AskDataInterfacePublisher getInterfacePublisher() { return interfacePublisher; }
}
