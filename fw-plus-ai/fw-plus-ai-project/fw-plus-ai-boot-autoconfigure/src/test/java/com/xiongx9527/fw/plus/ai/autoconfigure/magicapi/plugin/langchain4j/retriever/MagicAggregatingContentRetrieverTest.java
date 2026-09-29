package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 组合检索器：多路结果合并、去重、条数上限与单路失败（抛错 / 返回 null）跳过。
 */
class MagicAggregatingContentRetrieverTest {

    private static final Query QUERY = Query.from("问题");

    /** 并行扇出：两路必须同时在执行，若串行执行则第一路等待超时 */
    @Test
    void shouldRetrieveDelegatesInParallel() {
        java.util.concurrent.CountDownLatch firstStarted = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch secondStarted = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean firstSawSecond = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean secondSawFirst = new java.util.concurrent.atomic.AtomicBoolean();

        ContentRetriever first = query -> {
            firstStarted.countDown();
            firstSawSecond.set(awaitQuietly(secondStarted));
            return texts("a");
        };
        ContentRetriever second = query -> {
            secondStarted.countDown();
            secondSawFirst.set(awaitQuietly(firstStarted));
            return texts("b");
        };
        MagicAggregatingContentRetriever retriever = new MagicAggregatingContentRetriever(
                Arrays.asList(first, second), Arrays.asList("first", "second"), 0, true);

        try {
            assertThat(textsOf(retriever)).containsExactly("a", "b");
            assertThat(firstSawSecond.get()).as("两路子检索器应并行执行").isTrue();
            assertThat(secondSawFirst.get()).as("两路子检索器应并行执行").isTrue();
        } finally {
            retriever.close();
        }
    }

    /** datasource 下线时释放并行线程池 */
    @Test
    void shouldShutdownExecutorOnClose() {
        MagicAggregatingContentRetriever retriever = retriever(0, true, texts("a"), texts("b"));

        retriever.close();

        assertThat(retriever.retrieve(QUERY)).isEmpty();
    }

    @Test
    void shouldMergeDelegatesInDeclarationOrder() {
        MagicAggregatingContentRetriever retriever = retriever(2, true, texts("a"), texts("b"));

        assertThat(textsOf(retriever)).containsExactly("a", "b");
    }

    @Test
    void shouldDeduplicateByTextWhenEnabled() {
        assertThat(textsOf(retriever(0, true, texts("a", "b"), texts("b", "c"))))
                .containsExactly("a", "b", "c");
        assertThat(textsOf(retriever(0, false, texts("a"), texts("a")))).containsExactly("a", "a");
    }

    @Test
    void shouldStopAtMaxResults() {
        assertThat(textsOf(retriever(2, true, texts("a", "b"), texts("c")))).containsExactly("a", "b");
    }

    @Test
    void shouldSkipBrokenDelegateAndKeepGoing() {
        List<ContentRetriever> delegates = Arrays.asList(
                query -> texts("a"),
                query -> {
                    throw new IllegalStateException("boom");
                },
                query -> texts("c"));

        MagicAggregatingContentRetriever retriever =
                new MagicAggregatingContentRetriever(delegates, Arrays.asList("a", "broken", "c"), 0, true);

        assertThat(textsOf(retriever)).containsExactly("a", "c");
    }

    @Test
    void shouldTolerateNullResultOfDelegate() {
        List<ContentRetriever> delegates = Arrays.asList(query -> null, query -> texts("c"));

        MagicAggregatingContentRetriever retriever =
                new MagicAggregatingContentRetriever(delegates, Arrays.asList("nil", "c"), 0, true);

        assertThat(textsOf(retriever)).containsExactly("c");
    }

    @Test
    void shouldTolerateNullDelegateList() {
        assertThat(new MagicAggregatingContentRetriever(null, null, 0, true).retrieve(QUERY))
                .isEqualTo(Collections.<Content>emptyList());
    }

    private static MagicAggregatingContentRetriever retriever(int maxResults, boolean deduplicate,
            List<Content> first) {
        return retriever(maxResults, deduplicate, first, Collections.<Content>emptyList());
    }

    private static MagicAggregatingContentRetriever retriever(int maxResults, boolean deduplicate,
            List<Content> first, List<Content> second) {
        List<ContentRetriever> delegates = new ArrayList<>();
        delegates.add(query -> first);
        delegates.add(query -> second);
        return new MagicAggregatingContentRetriever(delegates, Arrays.asList("first", "second"),
                maxResults, deduplicate);
    }

    private static boolean awaitQuietly(java.util.concurrent.CountDownLatch latch) {
        try {
            return latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static List<Content> texts(String... values) {
        List<Content> contents = new ArrayList<>();
        for (String value : values) {
            contents.add(Content.from(value));
        }
        return contents;
    }

    private static List<String> textsOf(MagicAggregatingContentRetriever retriever) {
        List<String> texts = new ArrayList<>();
        for (Content content : retriever.retrieve(QUERY)) {
            texts.add(content.textSegment().text());
        }
        return texts;
    }
}
