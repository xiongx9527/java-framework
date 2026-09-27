package dev.langchain4j.azure.aisearch.spring;

import com.azure.search.documents.indexes.models.SearchIndex;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchContentRetriever;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

import static dev.langchain4j.azure.aisearch.spring.Properties.PREFIX;

@AutoConfiguration
@EnableConfigurationProperties(Properties.class)
public class AutoConfig {
    @Bean
    @ConditionalOnProperty(PREFIX + ".content-retriever.api-key")
    public AzureAiSearchContentRetriever azureAiSearchContentRetriever(Properties properties, @Nullable EmbeddingModel embeddingModel, @Nullable SearchIndex index) {
        Properties.NestedProperties nestedProperties = properties.getContentRetriever();
        return AzureAiSearchContentRetriever.builder()
                .endpoint(nestedProperties.getEndpoint())
                .apiKey(nestedProperties.getApiKey())
                .createOrUpdateIndex(nestedProperties.getCreateOrUpdateIndex())
                .embeddingModel(embeddingModel)
                .dimensions(nestedProperties.getDimensions() == null ? 0 : nestedProperties.getDimensions())
                .index(index)
                .maxResults(nestedProperties.getMaxResults())
                .minScore(nestedProperties.getMinScore() == null ? 0.0 : nestedProperties.getMinScore())
                .queryType(nestedProperties.getQueryType())
                .build();
    }
}