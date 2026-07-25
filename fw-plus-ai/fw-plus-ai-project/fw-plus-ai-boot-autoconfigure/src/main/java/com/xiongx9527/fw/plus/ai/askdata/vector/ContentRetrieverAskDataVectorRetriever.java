package com.xiongx9527.fw.plus.ai.askdata.vector;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContentRetrieverAskDataVectorRetriever implements AskDataVectorRetriever {

    private final ContentRetriever contentRetriever;

    public ContentRetrieverAskDataVectorRetriever(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Override
    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
        if (contentRetriever == null || query == null) {
            return Collections.emptyList();
        }
        List<Content> contents = contentRetriever.retrieve(Query.from(query.getQuestion()));
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        for (Content content : contents) {
            if (content != null && content.textSegment() != null) {
                documents.add(new AskDataRetrievedDocument(content.textSegment().text()));
            }
        }
        return documents;
    }
}
