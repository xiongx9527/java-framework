package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;

import java.util.List;

public interface AskDataPromptTemplateProvider {

    String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
            List<AskDataRetrievedDocument> retrievedDocuments, int maxRows);
}
