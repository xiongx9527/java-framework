package com.xiongx9527.fw.plus.ai.support.workflow;

import com.xiongx9527.fw.plus.ai.base.context.CallContext;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 编排请求：目标 + 输入 + 预算 + 上下文。
 */
public final class WorkflowRequest {

    private final String goal;

    private final Map<String, Object> inputs;

    private final WorkflowBudget budget;

    private final CallContext context;

    private final WorkflowSink sink;

    private WorkflowRequest(Builder builder) {
        this.goal = builder.goal;
        this.inputs = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.inputs));
        this.budget = builder.budget == null ? WorkflowBudget.defaults() : builder.budget;
        this.context = builder.context;
        this.sink = builder.sink;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getGoal() {
        return goal;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public WorkflowBudget getBudget() {
        return budget;
    }

    public CallContext getContext() {
        return context;
    }

    public WorkflowSink getSink() {
        return sink;
    }

    public static final class Builder {

        private final Map<String, Object> inputs = new LinkedHashMap<String, Object>();

        private String goal;

        private WorkflowBudget budget;

        private CallContext context;

        private WorkflowSink sink;

        private Builder() {
        }

        public Builder goal(String value) {
            this.goal = value;
            return this;
        }

        public Builder input(String key, Object value) {
            if (key != null) {
                this.inputs.put(key, value);
            }
            return this;
        }

        public Builder budget(WorkflowBudget value) {
            this.budget = value;
            return this;
        }

        public Builder context(CallContext value) {
            this.context = value;
            return this;
        }

        public Builder sink(WorkflowSink value) {
            this.sink = value;
            return this;
        }

        public WorkflowRequest build() {
            return new WorkflowRequest(this);
        }
    }
}
