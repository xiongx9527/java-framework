package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import java.util.List;

public interface AskDataVectorRetriever {

    List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query);

    default AskDataVectorRetrievalResult retrieveWithResult(AskDataVectorQuery query) {
        return AskDataVectorRetrievalResult.documents(retrieve(query));
    }
}
