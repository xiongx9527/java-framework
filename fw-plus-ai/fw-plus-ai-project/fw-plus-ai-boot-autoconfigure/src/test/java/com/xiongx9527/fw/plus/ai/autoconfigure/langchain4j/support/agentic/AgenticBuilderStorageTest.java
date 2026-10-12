package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentClassifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentSource;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AgenticBuilder} 的存储与意图识别扩展：
 * <ul>
 *   <li>助手侧记忆/存储（{@code memory} / {@code memoryStore}）接线到 agent 的 chatMemory；</li>
 *   <li>agentic 原生存储（{@code AgenticScopeStore} → {@code AgenticScopePersister}）；</li>
 *   <li>意图识别一站式入口 {@link AgenticBuilder#intent(IntentClassifier)}。</li>
 * </ul>
 */
class AgenticBuilderStorageTest {

    @AfterEach
    void resetGlobalScopePersister() {
        // AgenticScopePersister 是进程级静态单例，测试后必须复位，避免污染其它用例。
        AgenticScopePersister.setStore(null);
    }

    // ------------------------------------------------------------------
    // 助手侧记忆 / 存储
    // ------------------------------------------------------------------

    /** 记录 updateMessages 调用的 ChatMemoryStore。 */
    static final class RecordingChatMemoryStore implements ChatMemoryStore {
        final Map<Object, List<ChatMessage>> messages = new ConcurrentHashMap<>();
        final AtomicInteger updates = new AtomicInteger();

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return messages.getOrDefault(memoryId, List.of());
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            this.messages.put(memoryId, new ArrayList<>(messages));
            updates.incrementAndGet();
        }

        @Override
        public void deleteMessages(Object memoryId) {
            messages.remove(memoryId);
        }
    }

    @Test
    void memoryAndStoreShouldBeWiredIntoAgentChatMemory() {
        RecordingChatMemoryStore store = new RecordingChatMemoryStore();

        UntypedAgent agent = AgenticBuilder.agent()
                .chatModel(new ScriptedChatModel().agent("OK"))
                .userMessage("回答：{{q}}")
                .inputKey(String.class, "q")
                .memory(5)
                .memoryStore(store)
                .build();

        agent.invoke(Map.of("q", "hi"));

        assertThat(store.updates.get()).as("配置了 memoryStore 后，agent 的会话记忆应落盘到该 store")
                .isGreaterThan(0);
    }

    @Test
    void ephemeralConfigShouldStillWireAndPersistMemory() {
        RecordingChatMemoryStore store = new RecordingChatMemoryStore();

        UntypedAgent agent = AgenticBuilder.agent()
                .chatModel(new ScriptedChatModel().agent("OK"))
                .userMessage("回答：{{q}}")
                .inputKey(String.class, "q")
                .memoryStore(store)
                .ephemeralLimitAiLength(true)
                .ephemeralMaxAiCharsPerMessage(50)
                .ephemeralPersistToolResults(true)
                .ephemeralStripRagFromHistory(true)
                .build();

        agent.invoke(Map.of("q", "hi"));

        assertThat(store.updates.get())
                .as("仅配置 ephemeral/策略（未显式 memory()）也应启用记忆并落盘到 store")
                .isGreaterThan(0);
    }

    // ------------------------------------------------------------------
    // agentic 原生存储（AgenticScopeStore）
    // ------------------------------------------------------------------

    static final class RecordingScopeStore implements AgenticScopeStore {
        final Map<AgenticScopeKey, DefaultAgenticScope> saved = new ConcurrentHashMap<>();
        final AtomicInteger saves = new AtomicInteger();

        @Override
        public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
            saved.put(key, agenticScope);
            saves.incrementAndGet();
            return true;
        }

        @Override
        public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
            return Optional.ofNullable(saved.get(key));
        }

        @Override
        public boolean delete(AgenticScopeKey key) {
            return saved.remove(key) != null;
        }

        @Override
        public Set<AgenticScopeKey> getAllKeys() {
            return saved.keySet();
        }
    }

    @Test
    void scopeStoreShouldBeUsedWhenAgentRunsInSystem() {
        RecordingScopeStore scopeStore = new RecordingScopeStore();

        UntypedAgent agent = AgenticBuilder.agent()
                .chatModel(new ScriptedChatModel().agent("SCOPED"))
                .userMessage("回答：{{q}}")
                .inputKey(String.class, "q")
                .outputKey("answer")
                .scopeStore(scopeStore)
                .build();

        // agentic 原生存储只在 agent 作为「agentic 系统」的一部分运行时生效
        // （独立单 agent 走 invokeStandaloneAgent，使用 ephemeral scope，不经 AgenticScopeRegistry）。
        UntypedAgent pipeline = AgenticWorkflows.sequence()
                .subAgents(agent)
                .outputKey("answer")
                .build();

        pipeline.invoke(Map.of("q", "hi"));

        assertThat(scopeStore.saves.get())
                .as("scopeStore 应被 AgenticScopePersister 使用（scope 每次创建/更新都会 save）")
                .isGreaterThan(0);
    }

    // ------------------------------------------------------------------
    // 意图识别
    // ------------------------------------------------------------------

    enum MyIntent { GREETING, OTHER }

    static final class StubIntentClassifier implements IntentClassifier<MyIntent> {
        @Override
        public IntentResult<MyIntent> classify(IntentContext context) {
            return IntentResult.of(MyIntent.GREETING, 1.0, IntentSource.RULE);
        }
    }

    @Test
    void intentRoutingShouldWireConfiguredToolsIntoSameAgent() {
        StubIntentClassifier classifier = new StubIntentClassifier();

        AgenticBuilder<UntypedAgent> builder = AgenticBuilder.agent()
                .chatModel(new ScriptedChatModel().agent("ROUTED"))
                .userMessage("回答：{{q}}")
                .inputKey(String.class, "q");

        AgenticBuilder<UntypedAgent>.IntentRouting<MyIntent> routing = builder.intent(classifier);
        routing.tools()
                .source(request -> ToolProviderResult.builder().build())
                .fallbackNone();

        UntypedAgent agent = routing.done().build();

        // 接线后仍可正常构建与执行（不改变 agent 句柄语义）
        assertThat(agent.invoke(Map.of("q", "hi"))).isEqualTo("ROUTED");
    }

    @Test
    void intentRoutingShouldExposeRagAndToolsBuildersAndReturnOuterBuilder() {
        AgenticBuilder<UntypedAgent> builder = AgenticBuilder.agent();
        AgenticBuilder<UntypedAgent>.IntentRouting<MyIntent> routing =
                builder.intent(new StubIntentClassifier());

        assertThat(routing.rag()).isNotNull();
        assertThat(routing.tools()).isNotNull();
        assertThat(routing.done()).isSameAs(builder);
    }

    @Test
    void intentShouldRejectNullClassifier() {
        AgenticBuilder<UntypedAgent> builder = AgenticBuilder.agent();

        assertThatThrownBy(() -> builder.intent(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void memorySettersShouldReturnSameInstance() {
        AgenticBuilder<UntypedAgent> builder = AgenticBuilder.agent();

        assertThat(builder.memory(10)).isSameAs(builder);
        assertThat(builder.memoryStore(new RecordingChatMemoryStore())).isSameAs(builder);
        assertThat(builder.strictAiPersistenceStrategy(true)).isSameAs(builder);
        assertThat(builder.scopeStore(new RecordingScopeStore())).isSameAs(builder);
    }
}
