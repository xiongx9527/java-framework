package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentClassifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentSource;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AgenticIntentRouting} 把意图分类接到条件编排：一次编排只判一次意图。
 *
 * <p>计数分类器断言 {@code is(...)} / {@code in(...)} 的多个分支共享同一份判定；
 * {@link AgenticIntentRouting#inputs(String)} 在边界判一次并注入，编排内零判定。</p>
 */
class AgenticIntentRoutingTest {

    enum SampleIntent { LOGISTICS, REFUND, CHITCHAT }

    /** 计数分类器：记录判定次数，便于断言"一次编排只判一次"。 */
    static final class CountingClassifier implements IntentClassifier<SampleIntent> {

        final AtomicInteger calls = new AtomicInteger();

        private final SampleIntent intent;

        CountingClassifier(SampleIntent intent) {
            this.intent = intent;
        }

        @Override
        public IntentResult<SampleIntent> classify(IntentContext context) {
            calls.incrementAndGet();
            return intent == null
                    ? IntentResult.none(IntentSource.FALLBACK)
                    : IntentResult.of(intent, 1.0d, IntentSource.RULE);
        }
    }

    public interface IntentConditionalAgent {
        @Agent
        String run(@V("request") String request);
    }

    public interface QueryConditionalAgent {
        @Agent
        String run(@V("query") String query);
    }

    public static class LogisticsBranch {
        @Agent(name = "logistics", description = "物流分支", outputKey = "answer")
        public String handle(@V("request") String request) {
            return "logistics-" + request;
        }
    }

    public static class RefundBranch {
        @Agent(name = "refund", description = "退款分支", outputKey = "answer")
        public String handle(@V("request") String request) {
            return "refund-" + request;
        }
    }

    public static class ChitchatBranch {
        @Agent(name = "chitchat", description = "闲聊分支", outputKey = "answer")
        public String handle(@V("request") String request) {
            return "chitchat-" + request;
        }
    }

    public static class QueryBranch {
        @Agent(name = "query", description = "按 query 键读取的分支", outputKey = "answer")
        public String handle(@V("query") String query) {
            return "query-" + query;
        }
    }

    @Test
    void isShouldClassifyOnceForAllBranches() {
        CountingClassifier classifier = new CountingClassifier(SampleIntent.LOGISTICS);
        AgenticIntentRouting<SampleIntent> routing = AgenticIntentRouting.of(classifier);

        IntentConditionalAgent agent = AgenticWorkflows.conditional(IntentConditionalAgent.class)
                .subAgents(routing.is(SampleIntent.LOGISTICS), new LogisticsBranch())
                .subAgents(routing.is(SampleIntent.REFUND), new RefundBranch())
                .subAgents(routing.is(SampleIntent.CHITCHAT), new ChitchatBranch())
                .outputKey("answer")
                .build();

        assertThat(agent.run("查物流")).isEqualTo("logistics-查物流");
        assertThat(classifier.calls.get()).isEqualTo(1);
    }

    @Test
    void inShouldMatchAnyListedIntentAndClassifyOnce() {
        CountingClassifier classifier = new CountingClassifier(SampleIntent.CHITCHAT);
        AgenticIntentRouting<SampleIntent> routing = AgenticIntentRouting.of(classifier);

        IntentConditionalAgent agent = AgenticWorkflows.conditional(IntentConditionalAgent.class)
                .subAgents(routing.in(SampleIntent.REFUND, SampleIntent.CHITCHAT), new ChitchatBranch())
                .outputKey("answer")
                .build();

        assertThat(agent.run("随便聊聊")).isEqualTo("chitchat-随便聊聊");
        assertThat(classifier.calls.get()).isEqualTo(1);
    }

    @Test
    void inputsShouldInjectIntentAndAvoidReclassificationInsideOrchestration() {
        CountingClassifier classifier = new CountingClassifier(SampleIntent.REFUND);
        AgenticIntentRouting<SampleIntent> routing = AgenticIntentRouting.of(classifier);

        Map<String, Object> inputs = routing.inputs("退款");
        assertThat(inputs).containsEntry("request", "退款").containsEntry("intent", "REFUND");
        assertThat(classifier.calls.get()).isEqualTo(1);

        UntypedAgent agent = AgenticWorkflows.conditional()
                .subAgents(routing.is(SampleIntent.LOGISTICS), new LogisticsBranch())
                .subAgents(routing.is(SampleIntent.REFUND), new RefundBranch())
                .outputKey("answer")
                .build();

        assertThat(agent.invoke(inputs)).isEqualTo("refund-退款");
        assertThat(classifier.calls.get()).isEqualTo(1);
    }

    @Test
    void workflowsFacadeShouldExposeIntentRoutingFactory() {
        CountingClassifier classifier = new CountingClassifier(SampleIntent.LOGISTICS);

        AgenticIntentRouting<SampleIntent> routing = AgenticWorkflows.intentRouting(classifier);
        IntentConditionalAgent agent = AgenticWorkflows.conditional(IntentConditionalAgent.class)
                .subAgents(routing.is(SampleIntent.LOGISTICS), new LogisticsBranch())
                .outputKey("answer")
                .build();

        assertThat(agent.run("查物流")).isEqualTo("logistics-查物流");
        assertThat(classifier.calls.get()).isEqualTo(1);
    }

    @Test
    void customTextKeyShouldBeHonored() {
        CountingClassifier classifier = new CountingClassifier(SampleIntent.LOGISTICS);
        AgenticIntentRouting<SampleIntent> routing = AgenticIntentRouting.of(classifier).textKey("query");

        QueryConditionalAgent agent = AgenticWorkflows.conditional(QueryConditionalAgent.class)
                .subAgents(routing.is(SampleIntent.LOGISTICS), new QueryBranch())
                .outputKey("answer")
                .build();

        assertThat(agent.run("查物流")).isEqualTo("query-查物流");
        assertThat(classifier.calls.get()).isEqualTo(1);
    }
}
