package com.xiongx9527.fw.plus.ai.base.intent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次意图分类请求。
 */
public final class IntentRequest {

    private final String text;

    private final List<Intent> candidates;

    private final Map<String, Object> attributes;

    private IntentRequest(Builder builder) {
        this.text = builder.text;
        this.candidates = Collections.unmodifiableList(builder.candidates);
        this.attributes = Collections.unmodifiableMap(new HashMap<String, Object>(builder.attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getText() {
        return text;
    }

    /** 候选意图；分类器在它们之中选，而不是自由发挥。 */
    public List<Intent> getCandidates() {
        return candidates;
    }

    /** 附带的结构化上下文（来源、渠道、用户标签等）；供规则引擎类分类器做条件匹配。 */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static final class Builder {

        private String text;

        private final List<Intent> candidates = new java.util.ArrayList<Intent>();

        private final Map<String, Object> attributes = new java.util.HashMap<String, Object>();

        public Builder text(String value) {
            this.text = value;
            return this;
        }

        public Builder candidates(List<Intent> value) {
            if (value != null) {
                this.candidates.addAll(value);
            }
            return this;
        }

        public Builder addCandidate(Intent value) {
            if (value != null) {
                this.candidates.add(value);
            }
            return this;
        }

        /** 附带一个结构化属性，供规则引擎类分类器匹配。 */
        public Builder attribute(String key, Object value) {
            if (key != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public Builder attributes(Map<String, Object> value) {
            if (value != null) {
                attributes.putAll(value);
            }
            return this;
        }

        public IntentRequest build() {
            return new IntentRequest(this);
        }
    }
}
