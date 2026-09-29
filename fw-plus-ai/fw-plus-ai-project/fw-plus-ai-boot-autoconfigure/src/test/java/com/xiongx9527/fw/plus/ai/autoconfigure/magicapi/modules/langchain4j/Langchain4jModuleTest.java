package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.SimpleAssistant;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 快速构建助手：一次性对话、带记忆的多轮、系统提示、检索增强，以及缺少模型的提示。
 */
class Langchain4jModuleTest {

    private final Langchain4jModule module = new Langchain4jModule();

    @Test
    void shouldChatOnceWithSingleUserMessage() {
        RecordingChatModel model = new RecordingChatModel("杭州今天多云。");

        SimpleAssistant assistant = module.assistant().chatModel(model).build();
        String reply = assistant.chat("杭州天气怎么样");

        assertThat(reply).isEqualTo("杭州今天多云。");
        assertThat(model.requests).hasSize(1);
        assertThat(model.requests.get(0)).containsExactly(UserMessage.from("杭州天气怎么样"));
    }

    @Test
    void shouldChatOnceWithSystemPrompt() {
        RecordingChatModel model = new RecordingChatModel("好的。");

        SimpleAssistant assistant = module.assistant().chatModel(model)
                .systemMessage("你是助手").build();
        assistant.chat("s-1", "你好");

        assertThat(model.requests.get(0)).containsExactly(
                SystemMessage.from("你是助手"), UserMessage.from("你好"));
    }

    @Test
    void shouldChatOnceWithCustomMessages() {
        RecordingChatModel model = new RecordingChatModel("好的。");

        SimpleAssistant assistant = module.assistant().chatModel(model).build();
        assistant.generate(Arrays.<ChatMessage>asList(
                SystemMessage.from("你是助手"), UserMessage.from("上一句"), AiMessage.from("上一答"),
                UserMessage.from("这一句")));

        assertThat(model.requests.get(0)).hasSize(4);
    }

    @Test
    void shouldKeepHistoryForSameMemoryId() {
        RecordingChatModel model = new RecordingChatModel("收到。", "收到。");

        SimpleAssistant assistant = module.assistant().chatModel(model).memory(10).build();
        assistant.chat("session-1", "", "第一句");
        assistant.chat("session-1", "", "第二句");

        assertThat(model.requests.get(0)).hasSize(1);
        assertThat(model.requests.get(1)).as("第二轮带上第一轮的用户原话（EphemeralChatMemory 不保留 AI 回复）")
                .containsExactly(UserMessage.from("第一句"), UserMessage.from("第二句"));
    }

    @Test
    void shouldIsolateHistoryBetweenMemoryIds() {
        RecordingChatModel model = new RecordingChatModel("收到。", "收到。");

        SimpleAssistant assistant = module.assistant().chatModel(model).build();
        assistant.chat("s-1", "", "第一句");
        assistant.chat("s-2", "", "另一会话");

        assertThat(model.requests.get(1)).as("不同 memoryId 不共享历史").hasSize(1);
    }

    @Test
    void shouldApplySystemMessageProvider() {
        RecordingChatModel model = new RecordingChatModel("好的。");

        SimpleAssistant assistant = module.assistant().chatModel(model)
                .systemMessageProvider(memoryId -> "你是 fw-plus 助手").build();
        assistant.chat("s-1", "你好");

        assertThat(model.requests.get(0).get(0)).isEqualTo(SystemMessage.from("你是 fw-plus 助手"));
    }

    @Test
    void shouldEnableRetrievalWhenRetrieverGiven() {
        RecordingChatModel model = new RecordingChatModel("根据资料回答。");
        AtomicInteger retrievals = new AtomicInteger();
        ContentRetriever retriever = query -> {
            retrievals.incrementAndGet();
            return Arrays.asList(Content.from("西湖很适合傍晚散步"));
        };
        ContentInjector injector = (contents, chatMessage) -> UserMessage.from("参考资料后的问题");

        SimpleAssistant assistant = module.assistant().chatModel(model)
                .contentRetriever(retriever).contentInjector(injector).build();
        assistant.chat("s-1", "", "杭州有什么好玩的");

        assertThat(retrievals.get()).as("配置检索器后应发生一次检索").isEqualTo(1);
        assertThat(model.requests.get(0).get(0)).isEqualTo(UserMessage.from("参考资料后的问题"));
    }

                    @Test
    void shouldFailFastWithoutAnyModel() {
        assertThatThrownBy(() -> module.assistant().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少模型")
                .hasMessageContaining("至少调用 chatModel(...) 或 streamingModel(...)");
    }

    	/** 只配流式模型也能构建 */
	@Test
	void shouldBuildWithStreamingModelOnly() {
		StreamingChatModel streaming = new StreamingChatModel() {
		};

		SimpleAssistant assistant = module.assistant().streamingModel(streaming).build();

		assertThat(assistant).isNotNull();
	}

    /** 记录每次请求、按序回放的对话模型替身 */
    private static final class RecordingChatModel implements ChatModel {

        private final List<List<ChatMessage>> requests = new ArrayList<>();
        private final List<String> replies;

        private RecordingChatModel(String... replies) {
            this.replies = Arrays.asList(replies);
        }

        @Override
        public dev.langchain4j.model.chat.response.ChatResponse chat(java.util.List<ChatMessage> messages) {
            requests.add(new ArrayList<>(messages));
            int index = Math.min(requests.size() - 1, replies.size() - 1);
            return dev.langchain4j.model.chat.response.ChatResponse.builder()
                    .aiMessage(AiMessage.from(replies.get(index)))
                    .build();
        }
    }
}
