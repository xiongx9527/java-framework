package com.xiongx9527.fw.plus.ai.askdata.prompt;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRetrievedDocument;

import java.util.List;

public interface AskDataPromptTemplateProvider {

    String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
            List<AskDataRetrievedDocument> retrievedDocuments, int maxRows);
}
