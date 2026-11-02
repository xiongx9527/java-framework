package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 决策请求。
 *
 * <p><b>作用</b>：把「决策输入 + 一组问题 + 参数」收成一个不可变对象，
 * 让决策模型一次完成「多问题、多形态」的判定。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.model.decision.request.DecisionRequest}。</p>
 *
 * <p><b>实现约定</b>：{@link #questions()} 不可为空。</p>
 *
 * @author xiongxiao
 */
public final class DecisionRequest {

    private final DecisionInput input;
    private final Map<String, Question> questions;
    private final DecisionRequestParameters parameters;
    private final String modelName;

    private DecisionRequest(Builder builder) {
        if (builder.questions == null || builder.questions.isEmpty()) {
            throw new IllegalArgumentException("questions must not be empty");
        }
        this.input = builder.input;
        this.questions = Collections.unmodifiableMap(new LinkedHashMap<String, Question>(builder.questions));
        this.parameters = builder.parameters == null ? DecisionRequestParameters.EMPTY : builder.parameters;
        this.modelName = builder.modelName;
    }

    /**
     * 决策输入。
     *
     * @return 输入对象；未提供时为 {@code null}
     */
    public DecisionInput input() {
        return input;
    }

    /**
     * 问题集合（标识 → 问题）。
     *
     * @return 只读 Map，非 {@code null}
     */
    public Map<String, Question> questions() {
        return questions;
    }

    /**
     * 请求参数。
     *
     * @return 参数对象，非 {@code null}
     */
    public DecisionRequestParameters parameters() {
        return parameters;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未设置时为 {@code null}
     */
    public String modelName() {
        return modelName != null ? modelName : parameters.modelName();
    }

    /**
     * 基于当前请求创建构建器。
     *
     * @return 构建器
     */
    public Builder toBuilder() {
        Builder builder = builder().input(input).parameters(parameters).modelName(modelName);
        for (Map.Entry<String, Question> entry : questions.entrySet()) {
            builder.question(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DecisionRequest)) {
            return false;
        }
        DecisionRequest that = (DecisionRequest) o;
        return Objects.equals(input, that.input)
                && Objects.equals(questions, that.questions)
                && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, questions, parameters);
    }

    @Override
    public String toString() {
        return "DecisionRequest{input=" + input + ", questions=" + questions.size() + "}";
    }

    /** {@link DecisionRequest} 构建器。 */
    public static class Builder {

        private DecisionInput input;
        private final Map<String, Question> questions = new LinkedHashMap<String, Question>();
        private DecisionRequestParameters parameters;
        private String modelName;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置决策输入（自由文本）。
         *
         * @param text 文本
         * @return 本构建器
         */
        public Builder input(String text) {
            this.input = TextInput.from(text);
            return this;
        }

        /**
         * 设置决策输入（结构化字段）。
         *
         * @param fields 字段映射
         * @return 本构建器
         */
        public Builder inputFields(Map<String, ?> fields) {
            this.input = FieldsInput.from(fields);
            return this;
        }

        /**
         * 设置决策输入（多模态内容）。
         *
         * @param contents 内容块列表
         * @return 本构建器
         */
        public Builder inputContents(List<? extends com.xiongx9527.fw.plus.ai.message.Content> contents) {
            this.input = ContentsInput.from(contents);
            return this;
        }

        /**
         * 设置决策输入（多模态内容，变参）。
         *
         * @param contents 内容块
         * @return 本构建器
         */
        public Builder inputContents(com.xiongx9527.fw.plus.ai.message.Content... contents) {
            return inputContents(java.util.Arrays.asList(contents));
        }

        /**
         * 设置决策输入（对话消息）。
         *
         * @param messages 消息列表
         * @return 本构建器
         */
        public Builder inputMessages(List<? extends com.xiongx9527.fw.plus.ai.message.ChatMessage> messages) {
            this.input = MessagesInput.from(messages);
            return this;
        }

        /**
         * 设置决策输入（对话消息，变参）。
         *
         * @param messages 消息
         * @return 本构建器
         */
        public Builder inputMessages(com.xiongx9527.fw.plus.ai.message.ChatMessage... messages) {
            return inputMessages(java.util.Arrays.asList(messages));
        }

        /**
         * 设置决策输入（任意形态）。
         *
         * @param input 输入对象
         * @return 本构建器
         */
        public Builder input(DecisionInput input) {
            this.input = input;
            return this;
        }

        /**
         * 批量设置问题。
         *
         * @param questions 问题集合
         * @return 本构建器
         */
        public Builder questions(Map<String, ? extends Question> questions) {
            this.questions.clear();
            if (questions != null) {
                this.questions.putAll(questions);
            }
            return this;
        }

        /**
         * 追加一个问题。
         *
         * @param key      问题标识
         * @param question 问题对象
         * @return 本构建器
         */
        public Builder question(String key, Question question) {
            this.questions.put(key, question);
            return this;
        }

        /**
         * 设置请求参数。
         *
         * @param parameters 参数
         * @return 本构建器
         */
        public Builder parameters(DecisionRequestParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * 设置模型名（会并入参数）。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 构建请求。
         *
         * @return 请求对象
         */
        public DecisionRequest build() {
            return new DecisionRequest(this);
        }
    }
}
