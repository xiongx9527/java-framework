package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 向量检索文档及其 Embedding 模型路由结果。 */
public final class AskDataVectorRetrievalResult {

    private final List<AskDataRetrievedDocument> documents;
    private final List<AskDataModelAttemptTrace> modelAttempts;
    private final String actualEmbeddingModelKey;

    public AskDataVectorRetrievalResult(List<AskDataRetrievedDocument> documents,
            List<AskDataModelAttemptTrace> modelAttempts, String actualEmbeddingModelKey) {
        this.documents = immutableCopy(documents);
        this.modelAttempts = immutableCopy(modelAttempts);
        this.actualEmbeddingModelKey = normalize(actualEmbeddingModelKey);
    }

    public static AskDataVectorRetrievalResult documents(List<AskDataRetrievedDocument> documents) {
        return new AskDataVectorRetrievalResult(documents, Collections.<AskDataModelAttemptTrace>emptyList(), null);
    }

    public List<AskDataRetrievedDocument> getDocuments() { return documents; }
    public List<AskDataModelAttemptTrace> getModelAttempts() { return modelAttempts; }
    public String getActualEmbeddingModelKey() { return actualEmbeddingModelKey; }

    private static <T> List<T> immutableCopy(List<T> source) {
        return source == null || source.isEmpty() ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(source));
    }

    private static String normalize(String value) {
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }
}
