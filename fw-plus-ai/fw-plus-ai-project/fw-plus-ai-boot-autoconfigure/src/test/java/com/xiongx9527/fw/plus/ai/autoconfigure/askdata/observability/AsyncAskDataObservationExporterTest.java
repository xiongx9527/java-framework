package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AsyncAskDataObservationExporterTest {

    @Test
    public void shouldIsolateExporterFailureAndContinueOtherExporters() {
        Executor directExecutor = Runnable::run;
        final int[] exported = new int[1];
        AskDataObservationExporter failing = event -> { throw new IllegalStateException("offline"); };
        AskDataObservationExporter working = event -> exported[0]++;
        AsyncAskDataObservationExporter exporter = new AsyncAskDataObservationExporter(
                Arrays.asList(failing, working), directExecutor);

        exporter.publish(new AskDataEvent(new AskDataProjectContext("project-a", "operator-a"), "request-1",
                AskDataEvent.Type.CONVERSATION_COMPLETED, "sanitized"));

        assertEquals(1, exported[0]);
    }
}
