package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import java.util.List;

/** 向量存储 SPI。 */
public interface AskDataVectorStore {
	void upsert(AskDataProjectContext context, AskDataVectorDocument document);
	List<AskDataVectorDocument> search(AskDataProjectContext context, AskDataVectorSearchRequest request);
}
