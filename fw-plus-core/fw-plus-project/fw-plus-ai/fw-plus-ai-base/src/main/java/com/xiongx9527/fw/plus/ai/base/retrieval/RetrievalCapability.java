package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.List;

/**
 * Generic retrieval capability: explicit query -> explicit result, mirroring SqlGenerationCapability.
 * Wraps a Retriever<T> with a typed, domain-friendly API so the framework and the product depend on a
 * Capability rather than the lower-level Retriever contract.
 */
public interface RetrievalCapability<T> {

    RetrievalResult<T> retrieve(RetrievalQuery query);
}
