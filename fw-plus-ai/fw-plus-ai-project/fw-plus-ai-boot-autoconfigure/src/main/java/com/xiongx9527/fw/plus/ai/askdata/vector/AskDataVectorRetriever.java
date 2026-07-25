package com.xiongx9527.fw.plus.ai.askdata.vector;

import java.util.List;

public interface AskDataVectorRetriever {

    List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query);
}
