/**
 * 模型端口（聊天 / 补全 / 嵌入 / 分词）。
 *
 * <p>出处（以 langchain4j 1.23.0 同名接口为主，合并 Spring AI 等价与 AgentScope 特殊情况）：
 * <ul>
 *   <li>{@link com.xiongx9527.fw.plus.ai.model.chat.ChatModel} &harr; langchain4j 1.23.0 {@code dev.langchain4j.model.chat.ChatModel}（1.x 已更名）/ Spring AI {@code ChatModel}</li>
 *   <li>{@link com.xiongx9527.fw.plus.ai.model.chat.StreamingChatModel} &harr; langchain4j 1.23.0 {@code StreamingChatModel}（1.x 已更名）/ Spring AI {@code StreamingChatModel}</li>
 *   <li>{@link com.xiongx9527.fw.plus.ai.model.language.LanguageModel} / {@link com.xiongx9527.fw.plus.ai.model.language.StreamingLanguageModel} &harr; langchain4j 1.23.0 {@code dev.langchain4j.model.language.*}（legacy 文本补全，仍存在；Spring AI 无直接等价）</li>
 *   <li>{@link com.xiongx9527.fw.plus.ai.model.embedding.EmbeddingModel} 合并 langchain4j {@code EmbeddingModel} + {@code DimensionAwareEmbeddingModel#dimension()} &harr; Spring AI {@code EmbeddingModel}</li>
 *   <li>{@link com.xiongx9527.fw.plus.ai.model.TokenCountEstimator} &harr; langchain4j 1.23.0 {@code dev.langchain4j.model.TokenCountEstimator}（1.x 已更名；Spring AI 等价 TokenCountEstimator）</li>
 *   <li>AgentScope <b>不提供</b>底层模型接口——它运行在模型端口之上，编排多个模型调用</li>
 * </ul>
 */
package com.xiongx9527.fw.plus.ai.model;
