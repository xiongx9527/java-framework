package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AsyncAskDataObservationExporterTest {

    @Test
    public void shouldIsolateExporterFailureAndContinueOtherExporters() {
        Executor directExecutor = Runnable::run;
        final int[] exported = new int[1];
        final int[] diagnostics = new int[1];
        AskDataObservationExporter failing = event -> { throw new IllegalStateException("offline"); };
        AskDataObservationExporter working = event -> exported[0]++;
        AsyncAskDataObservationExporter exporter = new AsyncAskDataObservationExporter(
                Arrays.asList(failing, working), directExecutor, event -> {
                    assertEquals(AskDataEvent.Type.OBSERVABILITY_EXPORT_DEGRADED, event.getType());
                    diagnostics[0]++;
                });

        exporter.publish(new AskDataEvent(new AskDataProjectContext("project-a", "operator-a"), "request-1",
                AskDataEvent.Type.CONVERSATION_COMPLETED, "sanitized"));

        assertEquals(1, exported[0]);
        assertEquals(1, diagnostics[0]);
    }

    @Test
    public void shouldReportSanitizedDiagnosticWhenExecutorRejectsExport() {
        final int[] diagnostics = new int[1];
        AskDataEventPublisher diagnosticPublisher = event -> {
            assertEquals(AskDataEvent.Type.OBSERVABILITY_EXPORT_DEGRADED, event.getType());
            diagnostics[0]++;
        };
        AsyncAskDataObservationExporter exporter = new AsyncAskDataObservationExporter(
                Arrays.asList(event -> { }), command -> { throw new IllegalStateException("rejected"); },
                diagnosticPublisher);

        assertDoesNotThrow(() -> exporter.publish(new AskDataEvent(
                new AskDataProjectContext("project-a", "operator-a"), "request-1",
                AskDataEvent.Type.CONVERSATION_COMPLETED, "sanitized")));
        assertEquals(1, diagnostics[0]);
    }
}
