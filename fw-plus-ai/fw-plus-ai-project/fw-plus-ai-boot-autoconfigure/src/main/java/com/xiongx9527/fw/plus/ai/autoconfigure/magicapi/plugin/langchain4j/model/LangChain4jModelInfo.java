package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Data
public class LangChain4jModelInfo {

    /**
     * 自动注入图像模型映射表
     * key: 模型名称
     * value: 图像模型实例
     */
    @Autowired(required = false)
    private Map<String, ImageModel> imageModelMap ;

    /**
     * 自动注入嵌入模型映射表
     * key: 模型名称
     * value: 嵌入模型实例
     */
    @Autowired(required = false)
    private Map<String, EmbeddingModel> embeddingModelMap ;

    /**
     * 自动注入语言模型映射表
     * key: 模型名称
     * value: 语言模型实例
     */
    @Autowired(required = false)
    private Map<String, LanguageModel> languageModelMap ;

    /**
     * 自动注入流式语言模型映射表
     * key: 模型名称
     * value: 流式语言模型实例
     */
    @Autowired(required = false)
    private Map<String, StreamingLanguageModel> streamingLanguageModelMap ;

    /**
     * 自动注入聊天语言模型映射表
     * key: 模型名称
     * value: 聊天语言模型实例
     */
    @Autowired(required = false)
    private Map<String, ChatLanguageModel> chatLanguageModelMap ;

    /**
     * 自动注入流式聊天语言模型映射表
     * key: 模型名称
     * value: 流式聊天语言模型实例
     */
    @Autowired(required = false)
    private Map<String, StreamingChatLanguageModel> streamingChatLanguageModelMap ;

}
