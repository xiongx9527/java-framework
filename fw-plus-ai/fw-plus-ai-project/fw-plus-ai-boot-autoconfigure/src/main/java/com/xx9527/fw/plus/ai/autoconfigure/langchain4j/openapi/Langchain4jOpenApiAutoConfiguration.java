package com.xx9527.fw.plus.ai.autoconfigure.langchain4j.openapi;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.model.openai.OpenAiModerationModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingLanguageModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass(name="dev.langchain4j.model.openai.OpenAiTokenizer")
@AutoConfiguration
@EnableConfigurationProperties({Properties.class})
public class Langchain4jOpenApiAutoConfiguration {
    public Langchain4jOpenApiAutoConfiguration() {
    }

    /**
     * 创建OpenAI聊天模型Bean
     * 当配置属性langchain4j.open-ai.chat-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiChatModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.chat-model.api-key"})
    OpenAiChatModel openAiChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getChatModel();
        return OpenAiChatModel.builder().baseUrl(chatModelProperties.getBaseUrl()).apiKey(chatModelProperties.getApiKey()).organizationId(chatModelProperties.getOrganizationId()).modelName(chatModelProperties.getModelName()).temperature(chatModelProperties.getTemperature()).topP(chatModelProperties.getTopP()).stop(chatModelProperties.getStop()).maxTokens(chatModelProperties.getMaxTokens()).maxCompletionTokens(chatModelProperties.getMaxCompletionTokens()).presencePenalty(chatModelProperties.getPresencePenalty()).frequencyPenalty(chatModelProperties.getFrequencyPenalty()).logitBias(chatModelProperties.getLogitBias()).responseFormat(chatModelProperties.getResponseFormat()).strictJsonSchema(chatModelProperties.getStrictJsonSchema()).seed(chatModelProperties.getSeed()).user(chatModelProperties.getUser()).strictTools(chatModelProperties.getStrictTools()).parallelToolCalls(chatModelProperties.getParallelToolCalls()).timeout(chatModelProperties.getTimeout()).maxRetries(chatModelProperties.getMaxRetries()).proxy(ProxyProperties.convert(chatModelProperties.getProxy())).logRequests(chatModelProperties.getLogRequests()).logResponses(chatModelProperties.getLogResponses()).customHeaders(chatModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI流式聊天模型Bean
     * 当配置属性langchain4j.open-ai.streaming-chat-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiStreamingChatModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.streaming-chat-model.api-key"})
    OpenAiStreamingChatModel openAiStreamingChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getStreamingChatModel();
        return OpenAiStreamingChatModel.builder().baseUrl(chatModelProperties.getBaseUrl()).apiKey(chatModelProperties.getApiKey()).organizationId(chatModelProperties.getOrganizationId()).modelName(chatModelProperties.getModelName()).temperature(chatModelProperties.getTemperature()).topP(chatModelProperties.getTopP()).stop(chatModelProperties.getStop()).maxTokens(chatModelProperties.getMaxTokens()).maxCompletionTokens(chatModelProperties.getMaxCompletionTokens()).presencePenalty(chatModelProperties.getPresencePenalty()).frequencyPenalty(chatModelProperties.getFrequencyPenalty()).logitBias(chatModelProperties.getLogitBias()).responseFormat(chatModelProperties.getResponseFormat()).seed(chatModelProperties.getSeed()).user(chatModelProperties.getUser()).strictTools(chatModelProperties.getStrictTools()).parallelToolCalls(chatModelProperties.getParallelToolCalls()).timeout(chatModelProperties.getTimeout()).proxy(ProxyProperties.convert(chatModelProperties.getProxy())).logRequests(chatModelProperties.getLogRequests()).logResponses(chatModelProperties.getLogResponses()).customHeaders(chatModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI语言模型Bean
     * 当配置属性langchain4j.open-ai.language-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiLanguageModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.language-model.api-key"})
    OpenAiLanguageModel openAiLanguageModel(Properties properties) {
        LanguageModelProperties languageModelProperties = properties.getLanguageModel();
        return OpenAiLanguageModel.builder().baseUrl(languageModelProperties.getBaseUrl()).apiKey(languageModelProperties.getApiKey()).organizationId(languageModelProperties.getOrganizationId()).modelName(languageModelProperties.getModelName()).temperature(languageModelProperties.getTemperature()).timeout(languageModelProperties.getTimeout()).maxRetries(languageModelProperties.getMaxRetries()).proxy(ProxyProperties.convert(languageModelProperties.getProxy())).logRequests(languageModelProperties.getLogRequests()).logResponses(languageModelProperties.getLogResponses()).customHeaders(languageModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI流式语言模型Bean
     * 当配置属性langchain4j.open-ai.streaming-language-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiStreamingLanguageModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.streaming-language-model.api-key"})
    OpenAiStreamingLanguageModel openAiStreamingLanguageModel(Properties properties) {
        LanguageModelProperties languageModelProperties = properties.getStreamingLanguageModel();
        return OpenAiStreamingLanguageModel.builder().baseUrl(languageModelProperties.getBaseUrl()).apiKey(languageModelProperties.getApiKey()).organizationId(languageModelProperties.getOrganizationId()).modelName(languageModelProperties.getModelName()).temperature(languageModelProperties.getTemperature()).timeout(languageModelProperties.getTimeout()).proxy(ProxyProperties.convert(languageModelProperties.getProxy())).logRequests(languageModelProperties.getLogRequests()).logResponses(languageModelProperties.getLogResponses()).customHeaders(languageModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI嵌入模型Bean
     * 当配置属性langchain4j.open-ai.embedding-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiEmbeddingModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.embedding-model.api-key"})
    OpenAiEmbeddingModel openAiEmbeddingModel(Properties properties) {
        EmbeddingModelProperties embeddingModelProperties = properties.getEmbeddingModel();
        return OpenAiEmbeddingModel.builder().baseUrl(embeddingModelProperties.getBaseUrl()).apiKey(embeddingModelProperties.getApiKey()).organizationId(embeddingModelProperties.getOrganizationId()).modelName(embeddingModelProperties.getModelName()).dimensions(embeddingModelProperties.getDimensions()).user(embeddingModelProperties.getUser()).timeout(embeddingModelProperties.getTimeout()).maxRetries(embeddingModelProperties.getMaxRetries()).proxy(ProxyProperties.convert(embeddingModelProperties.getProxy())).logRequests(embeddingModelProperties.getLogRequests()).logResponses(embeddingModelProperties.getLogResponses()).customHeaders(embeddingModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI内容审核模型Bean
     * 当配置属性langchain4j.open-ai.moderation-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiModerationModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.moderation-model.api-key"})
    OpenAiModerationModel openAiModerationModel(Properties properties) {
        ModerationModelProperties moderationModelProperties = properties.getModerationModel();
        return OpenAiModerationModel.builder().baseUrl(moderationModelProperties.getBaseUrl()).apiKey(moderationModelProperties.getApiKey()).organizationId(moderationModelProperties.getOrganizationId()).modelName(moderationModelProperties.getModelName()).timeout(moderationModelProperties.getTimeout()).maxRetries(moderationModelProperties.getMaxRetries()).proxy(ProxyProperties.convert(moderationModelProperties.getProxy())).logRequests(moderationModelProperties.getLogRequests()).logResponses(moderationModelProperties.getLogResponses()).customHeaders(moderationModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI图像模型Bean
     * 当配置属性langchain4j.open-ai.image-model.api-key存在时创建此Bean
     *
     * @param properties 应用程序配置属性
     * @return OpenAiImageModel实例
     */
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.image-model.api-key"})
    OpenAiImageModel openAiImageModel(Properties properties) {
        ImageModelProperties imageModelProperties = properties.getImageModel();
        return OpenAiImageModel.builder().baseUrl(imageModelProperties.getBaseUrl()).apiKey(imageModelProperties.getApiKey()).organizationId(imageModelProperties.getOrganizationId()).modelName(imageModelProperties.getModelName()).size(imageModelProperties.getSize()).quality(imageModelProperties.getQuality()).style(imageModelProperties.getStyle()).user(imageModelProperties.getUser()).responseFormat(imageModelProperties.getResponseFormat()).timeout(imageModelProperties.getTimeout()).maxRetries(imageModelProperties.getMaxRetries()).proxy(ProxyProperties.convert(imageModelProperties.getProxy())).logRequests(imageModelProperties.getLogRequests()).logResponses(imageModelProperties.getLogResponses()).withPersisting(imageModelProperties.getWithPersisting()).persistTo(imageModelProperties.getPersistTo()).customHeaders(imageModelProperties.getCustomHeaders()).build();
    }

    /**
     * 创建OpenAI分词器Bean
     * 当容器中不存在OpenAiTokenizer类型的Bean时创建此Bean
     *
     * @return OpenAiTokenizer实例
     */
    @Bean
    @ConditionalOnMissingBean
    OpenAiTokenizer openAiTokenizer() {
        return new OpenAiTokenizer();
    }


}
