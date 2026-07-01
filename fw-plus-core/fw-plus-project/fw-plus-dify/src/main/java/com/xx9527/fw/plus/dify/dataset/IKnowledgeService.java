package com.xx9527.fw.plus.dify.dataset;

import com.xx9527.fw.plus.dify.dataset.types.external.KnowledgeResult;
import com.xx9527.fw.plus.dify.dataset.types.external.KnowledgeArgs;

public interface IKnowledgeService {
    /**
     * 知识检索
     * @param apiKey Dify传递的API KEY
     * @param args KnowledgeArgs
     */
    KnowledgeResult retrieval(String apiKey, KnowledgeArgs args);

}
