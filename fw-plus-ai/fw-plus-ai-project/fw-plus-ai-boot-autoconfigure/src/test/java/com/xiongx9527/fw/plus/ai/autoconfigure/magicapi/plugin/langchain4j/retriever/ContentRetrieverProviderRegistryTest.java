package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.module.Langchain4jContentRetrieverModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.EmbeddingStoreContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.FilteringContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.Neo4jContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.WebSearchContentRetrieverProvider;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * provider 分发表与 filtering provider 的行为测试。
 */
class ContentRetrieverProviderRegistryTest {

    private final ContentRetrieverProviderRegistry registry = new ContentRetrieverProviderRegistry();

    @Test
    void shouldRejectProviderOutsideTheTable() {
        registry.register(new FilteringContentRetrieverProvider());

        assertThatThrownBy(() -> registry.build(info("no-such-provider", new LinkedHashMap<String, Object>()), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("可用 provider");
    }

    @Test
    void shouldListRegisteredProviders() {
        registry.register(new EmbeddingStoreContentRetrieverProvider());
        registry.register(new Neo4jContentRetrieverProvider());
        registry.register(new WebSearchContentRetrieverProvider());
        registry.register(new FilteringContentRetrieverProvider());

        assertThat(registry.providers())
                .contains("embedding-store", "neo4j", "web-search", "filtering");
    }

    /** filtering 包在另一个 retriever 之外：按 score 阈值与 metadata 过滤，委托实例按 key 懒解析。 */
    @Test
    void shouldFilterRetrievedContentByScoreAndMetadata() {
        MagicDynamicLangchain4jContentRetrieverDataSource retrievers =
                new MagicDynamicLangchain4jContentRetrieverDataSource();
        retrievers.put("base", new ContentRetriever() {
            @Override
            public List<Content> retrieve(Query query) {
                return Arrays.asList(
                        content("命中", 0.9, "faq"),
                        content("低分", 0.1, "faq"),
                        content("分类不符", 0.9, "manual"));
            }
        });
        ContentRetrieverContext context = new ContentRetrieverContext(null, null, null,
                new Langchain4jContentRetrieverModule(retrievers, null), null, null);
        registry.register(new FilteringContentRetrieverProvider());

        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("content-retriever-key", "base");
        options.put("min-score", 0.5);
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("category", "faq");
        options.put("filters", filters);

        ContentRetriever retriever = registry.build(info("filtering", options), context);

        List<String> texts = new ArrayList<>();
        for (Content content : retriever.retrieve(Query.from("问题"))) {
            texts.add(content.textSegment().text());
        }
        assertThat(texts).containsExactly("命中");
    }

    private static Content content(String text, double score, String category) {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("score", score);
        metadata.put("category", category);
        return Content.from(TextSegment.from(text, new Metadata(metadata)));
    }

    private static Langchain4jContentRetrieverInfo info(String provider, Map<String, Object> options) {
        Langchain4jContentRetrieverInfo info = new Langchain4jContentRetrieverInfo();
        info.setKey("kb-main");
        info.setProvider(provider);
        info.setEnabled(true);
        info.setOptions(options);
        return info;
    }
}
