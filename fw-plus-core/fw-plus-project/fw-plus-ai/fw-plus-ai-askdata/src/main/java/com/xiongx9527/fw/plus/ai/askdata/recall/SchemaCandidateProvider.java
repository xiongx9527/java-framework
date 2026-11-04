package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import java.util.List;

/** Supplies the table schemas visible to the current askdata request. */
public interface SchemaCandidateProvider {

    /**
     * Returns only schemas the current caller is authorized to query.
     * Implementations may use request-scoped security context while resolving candidates.
     */
    List<TableSchema> findCandidates(RetrievalQuery query);
}
