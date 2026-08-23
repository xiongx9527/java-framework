package com.xiongx9527.fw.plus.ai.spring.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpringAiProviderResolutionException extends RuntimeException {

    private final int candidateCount;
    private final List<String> beanRefs;

    public SpringAiProviderResolutionException(int candidateCount, List<String> beanRefs) {
        super("Spring AI provider resolution failed");
        this.candidateCount = candidateCount;
        this.beanRefs = beanRefs == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(beanRefs));
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public List<String> getBeanRefs() {
        return beanRefs;
    }
}
