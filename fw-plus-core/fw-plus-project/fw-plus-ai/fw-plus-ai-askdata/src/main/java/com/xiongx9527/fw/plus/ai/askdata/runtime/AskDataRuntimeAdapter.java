package com.xiongx9527.fw.plus.ai.askdata.runtime;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent; import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel; import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher; import com.xiongx9527.fw.plus.ai.askdata.tool.AskDataTool; import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataEmbeddingModel; import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRagRetriever; import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorStore;
/** 将特定运行时能力适配为 Core SPI。 */
public interface AskDataRuntimeAdapter {
	AskDataChatModel chatModel(); AskDataEmbeddingModel embeddingModel(); AskDataVectorStore vectorStore(); AskDataTool tool(String key); AskDataRagRetriever ragRetriever(); AskDataAgent agent(String key); AskDataRuntimeStatus status(); AskDataEventPublisher eventPublisher();
	default boolean supports(String capability) { return "chat".equals(capability) && chatModel() != null || "embedding".equals(capability) && embeddingModel() != null || "vector".equals(capability) && vectorStore() != null || "rag".equals(capability) && ragRetriever() != null; }
}
