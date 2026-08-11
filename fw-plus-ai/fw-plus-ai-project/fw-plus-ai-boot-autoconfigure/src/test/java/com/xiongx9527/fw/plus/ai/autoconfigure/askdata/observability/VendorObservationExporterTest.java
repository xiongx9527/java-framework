package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VendorObservationExporterTest {

	@Test
	public void shouldMapLangSmithParentChildRuns() {
		RecordingTransport transport = new RecordingTransport();
		new LangSmithAskDataObservationExporter(transport).export(event(AskDataEvent.Type.AGENT_NODE_EXECUTED));

		assertEquals("langsmith", transport.exporterKey);
		assertEquals(AskDataObservationRecord.Kind.SPAN, transport.records.get(0).getKind());
		assertEquals("event-parent", transport.records.get(0).getParentEventId());
		assertEquals("request-a", transport.records.get(0).getRequestId());
	}

	@Test
	public void shouldBatchLangfuseTraceSpanAndGenerationRecords() {
		RecordingTransport transport = new RecordingTransport();
		LangfuseAskDataObservationExporter exporter = new LangfuseAskDataObservationExporter(transport, 2);
		exporter.export(event(AskDataEvent.Type.CONVERSATION_STARTED));
		exporter.export(event(AskDataEvent.Type.AGENT_NODE_EXECUTED));

		assertEquals("langfuse", transport.exporterKey);
		assertEquals(2, transport.records.size());
		assertEquals(AskDataObservationRecord.Kind.TRACE, transport.records.get(0).getKind());
		assertEquals(AskDataObservationRecord.Kind.SPAN, transport.records.get(1).getKind());
	}

	private static AskDataEvent event(AskDataEvent.Type type) {
		AskDataEventCorrelation correlation = AskDataEventCorrelation.builder("request-a", "conversation-a")
				.eventId(type == AskDataEvent.Type.CONVERSATION_STARTED ? "request-a:root" : "event-child")
				.parentEventId(type == AskDataEvent.Type.CONVERSATION_STARTED ? null : "event-parent")
				.graphNodeId("node-a").agentKey("agent-a").instanceId("instance-a").build();
		return AskDataEvent.correlated(new AskDataProjectContext("project-a", "operator-a"), "conversation-a",
				type, "redacted", correlation);
	}

	private static final class RecordingTransport implements AskDataObservationTransport {
		private String exporterKey;
		private List<AskDataObservationRecord> records = new ArrayList<AskDataObservationRecord>();
		@Override public void send(String exporterKey, List<AskDataObservationRecord> records) {
			this.exporterKey = exporterKey;
			this.records = new ArrayList<AskDataObservationRecord>(records);
		}
	}
}
