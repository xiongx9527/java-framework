package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProviderRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * content-retriever 连接测试：按 body 构建实例并真实检索一次，未保存的配置也能测。
 */
class Langchain4jContentRetrieverControllerTest {

    private final StubProvider stub = new StubProvider("stub", false);
    private final StubProvider strict = new StubProvider("strict", true);
    private Langchain4jContentRetrieverController controller;

    @BeforeEach
    void setUp() {
        ContentRetrieverProviderRegistry registry = new ContentRetrieverProviderRegistry();
        registry.register(stub);
        registry.register(strict);
        ContentRetrieverContext context = new ContentRetrieverContext(null, null, null,
                new MagicDynamicLangchain4jContentRetrieverDataSource(), null, null);
        controller = new Langchain4jContentRetrieverController(mock(MagicConfiguration.class), registry, context);
    }

    @Test
    void shouldRetrieveWithoutPersisting() {
        assertThat(controller.test(body("stub", null))).isEqualTo("ok");
        assertThat(stub.queries).containsExactly("ping");
    }

    @Test
    void shouldUseQueryFromBody() {
        assertThat(controller.test(body("stub", "框架怎么用 RAG"))).isEqualTo("ok");
        assertThat(stub.queries).containsExactly("框架怎么用 RAG");
    }

    @Test
    void shouldReportUnknownProvider() {
        assertThat(controller.test(body("no-such-provider", null))).contains("可用 provider");
    }

    @Test
    void shouldReportOptionPathWhenRequiredOptionMissing() {
        assertThat(controller.test(body("strict", null)))
                .contains("langchain4j-content-retriever[probe-main].options.required-option");
    }

    @Test
    void shouldReportRetrieveFailure() {
        assertThat(controller.test(body("stub", "boom"))).isEqualTo("测试失败: boom");
    }

    private static Map<String, Object> body(String provider, String query) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("key", "probe-main");
        body.put("provider", provider);
        body.put("options", new LinkedHashMap<String, Object>());
        if (query != null) {
            body.put("query", query);
        }
        return body;
    }

    /** 测试替身 provider：记录检索问题；问题为 boom 时抛运行时异常；strict 版本要求 options 必填项。 */
    private static final class StubProvider implements ContentRetrieverProvider {

        private final String name;
        private final boolean requireOption;
        private final List<String> queries = new ArrayList<>();

        private StubProvider(String name, boolean requireOption) {
            this.name = name;
            this.requireOption = requireOption;
        }

        @Override
        public String provider() {
            return name;
        }

        @Override
        public ContentRetriever build(Langchain4jOptionsReader options, ContentRetrieverContext context) {
            if (requireOption) {
                options.required("required-option");
            }
            return query -> {
                queries.add(query.text());
                if ("boom".equals(query.text())) {
                    throw new IllegalStateException("boom");
                }
                return Arrays.asList(Content.from("命中"));
            };
        }
    }
}
