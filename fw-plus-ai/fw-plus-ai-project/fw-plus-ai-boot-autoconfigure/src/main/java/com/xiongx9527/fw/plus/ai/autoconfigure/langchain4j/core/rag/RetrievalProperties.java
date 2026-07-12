package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.core.rag;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class RetrievalProperties {

    Integer maxResults;
    Double minScore;
}
