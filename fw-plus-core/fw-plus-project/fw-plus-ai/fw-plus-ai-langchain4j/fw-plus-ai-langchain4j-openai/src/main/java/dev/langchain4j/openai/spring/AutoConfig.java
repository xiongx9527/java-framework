//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.openai.spring;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.model.openai.OpenAiModerationModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingLanguageModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties({Properties.class})
public class AutoConfig {
    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.chat-model.api-key"})
    OpenAiChatModel openAiChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getChatModel();
        return OpenAiChatModel.builder().baseUrl(chatModelProperties.getBaseUrl()).apiKey(chatModelProperties.getApiKey()).organizationId(chatModelProperties.getOrganizationId()).modelName(chatModelProperties.getModelName()).temperature(chatModelProperties.getTemperature()).topP(chatModelProperties.getTopP()).stop(chatModelProperties.getStop()).maxTokens(chatModelProperties.getMaxTokens()).maxCompletionTokens(chatModelProperties.getMaxCompletionTokens()).presencePenalty(chatModelProperties.getPresencePenalty()).frequencyPenalty(chatModelProperties.getFrequencyPenalty()).logitBias(chatModelProperties.getLogitBias()).responseFormat(chatModelProperties.getResponseFormat()).strictJsonSchema(chatModelProperties.getStrictJsonSchema()).seed(chatModelProperties.getSeed()).user(chatModelProperties.getUser()).strictTools(chatModelProperties.getStrictTools()).parallelToolCalls(chatModelProperties.getParallelToolCalls()).timeout(chatModelProperties.getTimeout()).maxRetries(chatModelProperties.getMaxRetries()).logRequests(chatModelProperties.getLogRequests()).logResponses(chatModelProperties.getLogResponses()).customHeaders(chatModelProperties.getCustomHeaders()).build();
    }

    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.streaming-chat-model.api-key"})
    OpenAiStreamingChatModel openAiStreamingChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getStreamingChatModel();
        return OpenAiStreamingChatModel.builder().baseUrl(chatModelProperties.getBaseUrl()).apiKey(chatModelProperties.getApiKey()).organizationId(chatModelProperties.getOrganizationId()).modelName(chatModelProperties.getModelName()).temperature(chatModelProperties.getTemperature()).topP(chatModelProperties.getTopP()).stop(chatModelProperties.getStop()).maxTokens(chatModelProperties.getMaxTokens()).maxCompletionTokens(chatModelProperties.getMaxCompletionTokens()).presencePenalty(chatModelProperties.getPresencePenalty()).frequencyPenalty(chatModelProperties.getFrequencyPenalty()).logitBias(chatModelProperties.getLogitBias()).responseFormat(chatModelProperties.getResponseFormat()).seed(chatModelProperties.getSeed()).user(chatModelProperties.getUser()).strictTools(chatModelProperties.getStrictTools()).parallelToolCalls(chatModelProperties.getParallelToolCalls()).timeout(chatModelProperties.getTimeout()).logRequests(chatModelProperties.getLogRequests()).logResponses(chatModelProperties.getLogResponses()).customHeaders(chatModelProperties.getCustomHeaders()).build();
    }

    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.language-model.api-key"})
    OpenAiLanguageModel openAiLanguageModel(Properties properties) {
        LanguageModelProperties languageModelProperties = properties.getLanguageModel();
        return OpenAiLanguageModel.builder().baseUrl(languageModelProperties.getBaseUrl()).apiKey(languageModelProperties.getApiKey()).organizationId(languageModelProperties.getOrganizationId()).modelName(languageModelProperties.getModelName()).temperature(languageModelProperties.getTemperature()).timeout(languageModelProperties.getTimeout()).maxRetries(languageModelProperties.getMaxRetries()).logRequests(languageModelProperties.getLogRequests()).logResponses(languageModelProperties.getLogResponses()).customHeaders(languageModelProperties.getCustomHeaders()).build();
    }

    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.streaming-language-model.api-key"})
    OpenAiStreamingLanguageModel openAiStreamingLanguageModel(Properties properties) {
        LanguageModelProperties languageModelProperties = properties.getStreamingLanguageModel();
        return OpenAiStreamingLanguageModel.builder().baseUrl(languageModelProperties.getBaseUrl()).apiKey(languageModelProperties.getApiKey()).organizationId(languageModelProperties.getOrganizationId()).modelName(languageModelProperties.getModelName()).temperature(languageModelProperties.getTemperature()).timeout(languageModelProperties.getTimeout()).logRequests(languageModelProperties.getLogRequests()).logResponses(languageModelProperties.getLogResponses()).customHeaders(languageModelProperties.getCustomHeaders()).build();
    }

    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.embedding-model.api-key"})
    OpenAiEmbeddingModel openAiEmbeddingModel(Properties properties) {
        EmbeddingModelProperties embeddingModelProperties = properties.getEmbeddingModel();
        return OpenAiEmbeddingModel.builder().baseUrl(embeddingModelProperties.getBaseUrl()).apiKey(embeddingModelProperties.getApiKey()).organizationId(embeddingModelProperties.getOrganizationId()).modelName(embeddingModelProperties.getModelName()).dimensions(embeddingModelProperties.getDimensions()).user(embeddingModelProperties.getUser()).timeout(embeddingModelProperties.getTimeout()).maxRetries(embeddingModelProperties.getMaxRetries()).logRequests(embeddingModelProperties.getLogRequests()).logResponses(embeddingModelProperties.getLogResponses()).customHeaders(embeddingModelProperties.getCustomHeaders()).build();
    }

    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.moderation-model.api-key"})
    OpenAiModerationModel openAiModerationModel(Properties properties) {
        ModerationModelProperties moderationModelProperties = properties.getModerationModel();
        return OpenAiModerationModel.builder().baseUrl(moderationModelProperties.getBaseUrl()).apiKey(moderationModelProperties.getApiKey()).organizationId(moderationModelProperties.getOrganizationId()).modelName(moderationModelProperties.getModelName()).timeout(moderationModelProperties.getTimeout()).maxRetries(moderationModelProperties.getMaxRetries()).logRequests(moderationModelProperties.getLogRequests()).logResponses(moderationModelProperties.getLogResponses()).customHeaders(moderationModelProperties.getCustomHeaders()).build();
    }

    @Bean
    @ConditionalOnProperty({"langchain4j.open-ai.image-model.api-key"})
    OpenAiImageModel openAiImageModel(Properties properties) {
        ImageModelProperties imageModelProperties = properties.getImageModel();
        return OpenAiImageModel.builder().baseUrl(imageModelProperties.getBaseUrl()).apiKey(imageModelProperties.getApiKey()).organizationId(imageModelProperties.getOrganizationId()).modelName(imageModelProperties.getModelName()).size(imageModelProperties.getSize()).quality(imageModelProperties.getQuality()).user(imageModelProperties.getUser()).timeout(imageModelProperties.getTimeout()).maxRetries(imageModelProperties.getMaxRetries()).logRequests(imageModelProperties.getLogRequests()).logResponses(imageModelProperties.getLogResponses()).customHeaders(imageModelProperties.getCustomHeaders()).build();
    }

}
