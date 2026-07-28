package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiEmbeddingSearchResult {

    private final List<AiEmbeddingMatch> matches;

    public AiEmbeddingSearchResult(List<AiEmbeddingMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            this.matches = Collections.emptyList();
        } else {
            this.matches = Collections.unmodifiableList(new ArrayList<AiEmbeddingMatch>(matches));
        }
    }

    public List<AiEmbeddingMatch> getMatches() {
        return matches;
    }
}
