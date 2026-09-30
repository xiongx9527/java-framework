package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 函数过滤：返回值归一（Content 原样 / 文本项包装 / JSON 数组字符串 / null 不过滤）、
 * 入参内容，以及配置了函数后内置 score+metadata 规则让位给脚本。
 */
class MagicApiContentFilterTest {

    private static final Query QUERY = Query.from("杭州天气怎么样");

    @Test
    void shouldPassContentsTextsAndQueryToFunction() {
        RecordingInvoker invoker = new RecordingInvoker(Collections.emptyList());

        new MagicApiContentFilter(invoker, "filterContents").filter(texts("甲", "乙"), QUERY);

        assertThat(invoker.function).isEqualTo("filterContents");
        assertThat((java.util.List<String>) (List<?>) invoker.arguments.get("texts")).containsExactly("甲", "乙");
        assertThat((List<?>) invoker.arguments.get("contents")).hasSize(2);
        assertThat(invoker.arguments.get("query")).isEqualTo("杭州天气怎么样");
    }

    @Test
    void shouldWrapReturnedTextsIntoContents() {
        MagicApiContentFilter filter = new MagicApiContentFilter(
                new RecordingInvoker(Arrays.asList("改写后的正文")), "filterContents");

        List<Content> filtered = filter.filter(texts("甲", "乙"), QUERY);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).textSegment().text()).isEqualTo("改写后的正文");
    }

    @Test
    void shouldAcceptJsonArrayString() {
        MagicApiContentFilter filter = new MagicApiContentFilter(
                new RecordingInvoker("[\"甲\",\"丙\"]"), "filterContents");

        assertThat(filter.filter(texts("甲", "乙"), QUERY))
                .extracting(content -> content.textSegment().text())
                .containsExactly("甲", "丙");
    }

    @Test
    void shouldKeepReturnedContentInstancesAsIs() {
        Content content = Content.from("原样返回");
        MagicApiContentFilter filter = new MagicApiContentFilter(
                new RecordingInvoker(Collections.singletonList(content)), "filterContents");

        assertThat(filter.filter(texts("甲"), QUERY)).containsExactly(content);
    }

    @Test
    void shouldKeepAllContentsWhenFunctionReturnsNull() {
        MagicApiContentFilter filter = new MagicApiContentFilter(new RecordingInvoker(null), "filterContents");
        List<Content> contents = texts("甲", "乙");

        assertThat(filter.filter(contents, QUERY)).isSameAs(contents);
    }

    @Test
    void shouldLetFunctionDecideInsteadOfBuiltInRules() {
        // 内置规则会因 min-score=0.9 丢掉全部条目；配置了函数后由脚本决定（这里保留一条）
        FilteringContentRetriever retriever = new FilteringContentRetriever(
                query -> Collections.singletonList(Content.from("低分条目")),
                0.9, Collections.<String, String>emptyMap(),
                new MagicApiContentFilter(new RecordingInvoker(Arrays.asList("脚本保留")), "filterContents"));

        assertThat(retriever.retrieve(QUERY))
                .extracting(content -> content.textSegment().text())
                .containsExactly("脚本保留");
    }

    private static List<Content> texts(String... texts) {
        List<Content> contents = new ArrayList<>();
        for (String text : texts) {
            contents.add(Content.from(text));
        }
        return contents;
    }

    /** 记录调用参数、返回预设值的执行器替身 */
    private static final class RecordingInvoker implements MagicToolInvoker {

        private final Object result;
        private String function;
        private Map<String, Object> arguments;

        private RecordingInvoker(Object result) {
            this.result = result;
        }

        @Override
        public Object invokeFunction(String name, Map<String, Object> arguments) {
            this.function = name;
            this.arguments = arguments;
            return result;
        }

        @Override
        public Object invokeApi(ApiInfo info, Map<String, Object> arguments) {
            return AiMessage.from("unused");
        }
    }
}
