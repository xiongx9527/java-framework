package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AskDataRetrievedDocument {

    private final String content;
    private final Map<String, Object> metadata;

    public AskDataRetrievedDocument(String content) {
        this(content, Collections.<String, Object>emptyMap());
    }

    public AskDataRetrievedDocument(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
    }

    public String getContent() { return content; }
    public Map<String, Object> getMetadata() { return metadata; }
}
