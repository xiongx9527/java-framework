package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.List;

/** 查询路由：按问题挑出要用的检索器（多知识库时避免全量打）。 */
public interface QueryRouter {

    List<Retriever<Document>> route(String query);
}
