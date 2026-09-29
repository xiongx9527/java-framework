package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 七类实例持有者的类型覆盖与隔离性：每个数据源类型一个持有者，各自只持有本类型实例。
 */
class MagicDynamicLangchain4jDataSourcesTest {

    @Test
    void shouldCoverSevenDistinctDataSources() {
        List<AbstractMagicDynamicLangchain4jDataSource<?>> dataSources = Arrays.asList(
                new MagicDynamicLangchain4jChatModelDataSource(),
                new MagicDynamicLangchain4jStreamingChatModelDataSource(),
                new MagicDynamicLangchain4jLanguageModelDataSource(),
                new MagicDynamicLangchain4jStreamingLanguageModelDataSource(),
                new MagicDynamicLangchain4jEmbeddingModelDataSource(),
                new MagicDynamicLangchain4jTokenizerDataSource(),
                new MagicDynamicLangchain4jEmbeddingStoreDataSource());

        assertThat(dataSources).hasSize(Langchain4jModelTypes.values().length);
        assertThat(dataSources.stream().map(AbstractMagicDynamicLangchain4jDataSource::type))
                .containsExactlyInAnyOrder(Langchain4jModelTypes.values());
    }

    @Test
    void shouldKeepSameKeyIsolatedBetweenTypes() {
        MagicDynamicLangchain4jChatModelDataSource chatModels = new MagicDynamicLangchain4jChatModelDataSource();
        MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels = new MagicDynamicLangchain4jEmbeddingModelDataSource();

        chatModels.put("shared-key", mock(ChatLanguageModel.class));

        assertThat(embeddingModels.get("shared-key")).isNull();
        assertThat(embeddingModels.keys()).isEmpty();
    }

    @Test
    void shouldReplaceInstanceOnSameKey() {
        MagicDynamicLangchain4jEmbeddingModelDataSource dataSource = new MagicDynamicLangchain4jEmbeddingModelDataSource();
        EmbeddingModel first = mock(EmbeddingModel.class);
        EmbeddingModel second = mock(EmbeddingModel.class);

        dataSource.put("k", first);
        dataSource.put("k", second);

        assertThat(dataSource.get("k")).isSameAs(second);
        assertThat(dataSource.keys()).containsExactly("k");
    }
}
