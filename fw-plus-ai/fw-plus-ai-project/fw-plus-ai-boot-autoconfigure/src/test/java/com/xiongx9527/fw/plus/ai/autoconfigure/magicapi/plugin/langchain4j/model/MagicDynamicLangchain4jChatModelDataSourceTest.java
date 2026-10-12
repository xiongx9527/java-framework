package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * {@link MagicDynamicLangchain4jChatModelDataSource} 行为测试：以 chat-model 类型为代表，
 * 覆盖共享持有逻辑（按 key 持有/驱逐、驱逐即关闭、关闭异常不阻断、destroy 清空）。
 */
class MagicDynamicLangchain4jChatModelDataSourceTest {

    @Test
    void shouldHoldInstanceByKey() {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();
        ChatModel model = mock(ChatModel.class);

        dataSource.put("qwen-main", model);

        assertThat(dataSource.get("qwen-main")).isSameAs(model);
        assertThat(dataSource.keys()).containsExactly("qwen-main");
        assertThat(dataSource.snapshot()).containsEntry("qwen-main", model);
    }

    @Test
    void shouldReportItsOwnType() {
        assertThat(new MagicDynamicLangchain4jChatModelDataSource().type())
                .isEqualTo(Langchain4jModelTypes.CHAT_MODEL);
    }

    @Test
    void shouldReturnNullForUnknownKey() {
        assertThat(new MagicDynamicLangchain4jChatModelDataSource().get("absent")).isNull();
    }

    @Test
    void shouldRejectSnapshotAndKeyViewMutation() {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();
        dataSource.put("a", mock(ChatModel.class));

        assertThatCode(() -> dataSource.snapshot().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> dataSource.keys().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(dataSource.keys()).containsExactly("a");
    }

    @Test
    void shouldCloseInstanceOnEvict() throws Exception {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();
        AtomicInteger closed = new AtomicInteger();
        ChatModel model = closeable(closed);
        dataSource.put("k", model);

        dataSource.evict("k");

        assertThat(closed.get()).isEqualTo(1);
        assertThat(dataSource.get("k")).isNull();
    }

    @Test
    void shouldNotBreakWhenCloseThrows() throws Exception {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();
        ChatModel bad = mock(ChatModel.class, withSettings().extraInterfaces(Closeable.class));
        doThrow(new IOException("模拟关闭失败")).when((Closeable) bad).close();
        ChatModel good = mock(ChatModel.class);
        dataSource.put("bad", bad);
        dataSource.put("good", good);

        assertThatCode(() -> dataSource.evict("bad")).doesNotThrowAnyException();

        assertThat(dataSource.get("bad")).isNull();
        assertThat(dataSource.get("good")).isSameAs(good);
    }

    @Test
    void shouldIgnoreEvictOfUnknownKey() {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();

        assertThatCode(() -> dataSource.evict("absent")).doesNotThrowAnyException();
    }

    @Test
    void shouldClearAndCloseEverythingOnDestroy() throws Exception {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        dataSource.put("first", closeable(first));
        dataSource.put("second", closeable(second));

        dataSource.destroy();

        assertThat(first.get()).isEqualTo(1);
        assertThat(second.get()).isEqualTo(1);
        assertThat(dataSource.keys()).isEmpty();
    }

    @Test
    void shouldNotCloseNonCloseableInstanceOnEvict() {
        MagicDynamicLangchain4jChatModelDataSource dataSource = new MagicDynamicLangchain4jChatModelDataSource();
        dataSource.put("plain", mock(ChatModel.class));

        assertThatCode(() -> dataSource.evict("plain")).doesNotThrowAnyException();

        assertThat(dataSource.get("plain")).isNull();
    }

    /** 同时是 ChatModel 与 Closeable 的替身，close 时计数。 */
    private static ChatModel closeable(AtomicInteger counter) throws IOException {
        ChatModel model = mock(ChatModel.class, withSettings().extraInterfaces(Closeable.class));
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when((Closeable) model).close();
        return model;
    }
}
