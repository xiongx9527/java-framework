package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompositeAskDataVectorRetriever implements AskDataVectorRetriever {

    private final List<AskDataVectorRetriever> delegates;
    private final int defaultTopK;

    public CompositeAskDataVectorRetriever(List<AskDataVectorRetriever> delegates) {
        this(delegates, 0);
    }

    public CompositeAskDataVectorRetriever(List<AskDataVectorRetriever> delegates, int defaultTopK) {
        this.delegates = delegates == null ? Collections.<AskDataVectorRetriever>emptyList()
                : new ArrayList<AskDataVectorRetriever>(delegates);
        this.defaultTopK = defaultTopK;
    }

    @Override
    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        AskDataVectorQuery effectiveQuery = applyDefaultTopK(query);
        for (AskDataVectorRetriever delegate : delegates) {
            if (delegate == null) {
                continue;
            }
            List<AskDataRetrievedDocument> retrieved = delegate.retrieve(effectiveQuery);
            if (retrieved != null) {
                documents.addAll(retrieved);
            }
        }
        return documents;
    }

    private AskDataVectorQuery applyDefaultTopK(AskDataVectorQuery query) {
        if (query == null || query.getTopK() > 0 || defaultTopK <= 0) {
            return query;
        }
        return new AskDataVectorQuery(query.getRetrieveType(), query.getDatasourceKey(), query.getVectorStoreKey(),
                query.getQuestion(), defaultTopK, query.getFilters());
    }
}
