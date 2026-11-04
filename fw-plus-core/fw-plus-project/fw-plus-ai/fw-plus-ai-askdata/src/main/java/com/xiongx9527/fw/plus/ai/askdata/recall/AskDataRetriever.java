package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adapts authorized askdata table schemas to the document retriever used by AiServices. */
public class AskDataRetriever implements Retriever<Document> {

    private final SchemaCandidateProvider candidateProvider;
    private final Retriever<Document> delegate;

    public AskDataRetriever(SchemaCandidateProvider candidateProvider, Retriever<Document> delegate) {
        if (candidateProvider == null) {
            throw new IllegalArgumentException("schema candidate provider is required");
        }
        if (delegate == null) {
            throw new IllegalArgumentException("delegate retriever is required");
        }
        this.candidateProvider = candidateProvider;
        this.delegate = delegate;
    }

    @Override
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        if (query == null || query.getText() == null || query.getText().trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<TableSchema> candidates = candidateProvider.findCandidates(query);
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Document> allowed = new LinkedHashMap<String, Document>();
        List<Document> corpus = new ArrayList<Document>();
        for (TableSchema table : candidates) {
            if (table == null || table.getName() == null || table.getName().trim().isEmpty()) {
                continue;
            }
            Document document = Document.builder()
                    .id(table.getName())
                    .content(table.renderBrief())
                    .build();
            allowed.put(document.getId(), document);
            corpus.add(document);
        }
        if (corpus.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> candidateIds = new LinkedHashSet<String>(allowed.keySet());
        if (!query.getCandidateIds().isEmpty()) {
            candidateIds.retainAll(query.getCandidateIds());
            if (candidateIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Document> narrowedCorpus = new ArrayList<Document>();
            for (Document document : corpus) {
                if (candidateIds.contains(document.getId())) {
                    narrowedCorpus.add(document);
                }
            }
            corpus = narrowedCorpus;
        }

        RetrievalQuery scopedQuery = RetrievalQuery.builder()
                .text(query.getText())
                .collection(query.getCollection())
                .topK(query.getTopK())
                .topN(query.getTopN())
                .scoreThreshold(query.getScoreThreshold())
                .filters(query.getFilters())
                .candidateIds(candidateIds)
                .corpus(corpus)
                .build();
        List<Retrieved<Document>> hits = delegate.retrieve(scopedQuery);
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }

        List<Retrieved<Document>> result = new ArrayList<Retrieved<Document>>();
        for (Retrieved<Document> hit : hits) {
            if (hit == null || hit.getItem() == null) {
                continue;
            }
            Document authorized = allowed.get(hit.getItem().getId());
            if (authorized != null && candidateIds.contains(authorized.getId())) {
                result.add(Retrieved.of(authorized, hit.getScore(), hit.getSource()));
            }
        }
        return result;
    }
}
