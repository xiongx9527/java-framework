package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

public final class TinyflowKnowledgeCapabilities {

	private final boolean retrieval;
	private final boolean management;
	private final boolean documentImport;

	private TinyflowKnowledgeCapabilities(boolean retrieval, boolean management, boolean documentImport) {
		this.retrieval = retrieval;
		this.management = management;
		this.documentImport = documentImport;
	}

	public static TinyflowKnowledgeCapabilities retrievalOnly() {
		return new TinyflowKnowledgeCapabilities(true, false, false);
	}

	public static TinyflowKnowledgeCapabilities managed() {
		return new TinyflowKnowledgeCapabilities(true, true, true);
	}

	public static TinyflowKnowledgeCapabilities of(boolean retrieval, boolean management, boolean documentImport) {
		return new TinyflowKnowledgeCapabilities(retrieval, management, documentImport);
	}

	public boolean supportsRetrieval() {
		return retrieval;
	}

	public boolean supportsManagement() {
		return management;
	}

	public boolean supportsDocumentImport() {
		return documentImport;
	}
}
