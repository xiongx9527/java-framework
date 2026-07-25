package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SequentialAskDataAgentDispatcherTest {

    @Test
    public void shouldExecuteSubAgentsSynchronouslyInOrderAndAggregateResults() {
        final List<String> executed = new ArrayList<String>();
        AskDataDispatchResult result = new SequentialAskDataAgentDispatcher().dispatch(request(Arrays.asList(
                agent("metadata", executed, AskDataSubAgentResult.success("metadata loaded")),
                agent("prompt", executed, AskDataSubAgentResult.success("prompt built"))),
                AskDataDispatchRequest.FailurePolicy.TERMINATE, 1000L, null));

        assertTrue(result.isSuccess());
        assertEquals(Arrays.asList("metadata", "prompt"), executed);
        assertEquals(2, result.getResults().size());
        assertEquals("metadata", result.getResults().get(0).getAgentName());
        assertEquals("metadata loaded", result.getResults().get(0).getOutputSummary());
        assertFalse(result.getResults().get(0).isTimedOut());
        assertTrue(result.getResults().get(0).getEndMillis() >= result.getResults().get(0).getStartMillis());
    }

    @Test
    public void shouldMarkResultTimedOutWhenSubAgentExceedsRequestTimeout() {
        AskDataDispatchResult result = new SequentialAskDataAgentDispatcher().dispatch(request(Arrays.asList(
                new AskDataSubAgent() {
                    @Override
                    public String getName() {
                        return "slow";
                    }

                    @Override
                    public AskDataStepResult execute(AskDataStepContext context) {
                        return AskDataStepResult.success("slow completed");
                    }

                    @Override
                    public AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request) {
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        return AskDataSubAgentResult.success("slow completed");
                    }
                }), AskDataDispatchRequest.FailurePolicy.CONTINUE, 1L, null));

        assertFalse(result.isSuccess());
        assertTrue(result.getResults().get(0).isTimedOut());
        assertTrue(result.getResults().get(0).getErrorMessage().contains("timeout"));
    }

    @Test
    public void shouldContinueAfterFailureAndKeepOriginalError() {
        final List<String> executed = new ArrayList<String>();
        AskDataDispatchResult result = new SequentialAskDataAgentDispatcher().dispatch(request(Arrays.asList(
                agent("failed", executed, AskDataSubAgentResult.failure("model unavailable")),
                agent("next", executed, AskDataSubAgentResult.success("continued"))),
                AskDataDispatchRequest.FailurePolicy.CONTINUE, 1000L, null));

        assertFalse(result.isSuccess());
        assertEquals(Arrays.asList("failed", "next"), executed);
        assertEquals("model unavailable", result.getResults().get(0).getErrorMessage());
    }

    @Test
    public void shouldUseDegradedOutputAfterFailure() {
        AskDataDispatchResult result = new SequentialAskDataAgentDispatcher().dispatch(request(Arrays.asList(
                agent("failed", new ArrayList<String>(), AskDataSubAgentResult.failure("model unavailable"))),
                AskDataDispatchRequest.FailurePolicy.DEGRADE, 1000L, "fallback response"));

        assertTrue(result.isSuccess());
        assertEquals("fallback response", result.getResults().get(0).getOutputSummary());
        assertEquals("model unavailable", result.getResults().get(0).getErrorMessage());
    }

    @Test
    public void shouldTerminateAfterFailure() {
        final List<String> executed = new ArrayList<String>();
        AskDataDispatchResult result = new SequentialAskDataAgentDispatcher().dispatch(request(Arrays.asList(
                agent("failed", executed, AskDataSubAgentResult.failure("model unavailable")),
                agent("next", executed, AskDataSubAgentResult.success("must not run"))),
                AskDataDispatchRequest.FailurePolicy.TERMINATE, 1000L, null));

        assertFalse(result.isSuccess());
        assertEquals(Arrays.asList("failed"), executed);
        assertEquals(1, result.getResults().size());
    }

    private AskDataDispatchRequest request(List<AskDataSubAgent> agents,
            AskDataDispatchRequest.FailurePolicy failurePolicy, long timeoutMillis, String degradedOutputSummary) {
        return new AskDataDispatchRequest(agents, new AskDataStepContext(new AskDataRequest()), timeoutMillis,
                failurePolicy, degradedOutputSummary);
    }

    private AskDataSubAgent agent(final String name, final List<String> executed,
            final AskDataSubAgentResult result) {
        return new AskDataSubAgent() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public AskDataStepResult execute(AskDataStepContext context) {
                return result.isSuccess() ? AskDataStepResult.success(result.getOutputSummary())
                        : AskDataStepResult.failure(result.getErrorMessage());
            }

            @Override
            public AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request) {
                executed.add(name);
                return result;
            }
        };
    }
}
