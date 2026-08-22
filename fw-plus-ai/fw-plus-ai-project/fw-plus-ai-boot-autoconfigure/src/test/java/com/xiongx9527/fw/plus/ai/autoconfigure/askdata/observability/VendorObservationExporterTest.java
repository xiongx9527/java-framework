package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

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
	public void shouldMapLangSmithModelTokenAndCostMetadata() {
		RecordingTransport transport = new RecordingTransport();
		AskDataEvent modelEvent = AskDataEvent.modelAttempt(
				new AskDataProjectContext("project-a", "operator-a"), "conversation-a",
				AskDataEvent.ModelType.CHAT, "chat-primary", AskDataModelRoutePolicy.REQUEST_MODEL_KEY,
				AskDataEvent.ModelState.SUCCEEDED, null, null, "provider-chat",
				AskDataEventCorrelation.builder("request-a", "conversation-a").build(),
				new AskDataModelUsage(11L, 5L, 16L, new BigDecimal("0.0025")));

		new LangSmithAskDataObservationExporter(transport).export(modelEvent);

		assertEquals("11", transport.records.get(0).getMetadata().get("inputTokens"));
		assertEquals("5", transport.records.get(0).getMetadata().get("outputTokens"));
		assertEquals("16", transport.records.get(0).getMetadata().get("totalTokens"));
		assertEquals("0.0025", transport.records.get(0).getMetadata().get("cost"));
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

	@Test
	public void shouldFlushLangfuseTailBatchWhenSpringContainerCloses() {
		RecordingTransport transport = new RecordingTransport();
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(AskDataObservationExporterProvider.class,
				() -> new AskDataObservationExporterProvider("langfuse", "test.langfuse",
						new LangfuseAskDataObservationExporter(transport, 10)));
		context.refresh();
		AskDataObservationExporterProvider provider = context.getBean(AskDataObservationExporterProvider.class);
		((AskDataObservationExporter) provider.getCapability()).export(
				event(AskDataEvent.Type.CONVERSATION_STARTED));

		assertEquals(0, transport.records.size());
		context.close();

		assertEquals("langfuse", transport.exporterKey);
		assertEquals(1, transport.records.size());
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
