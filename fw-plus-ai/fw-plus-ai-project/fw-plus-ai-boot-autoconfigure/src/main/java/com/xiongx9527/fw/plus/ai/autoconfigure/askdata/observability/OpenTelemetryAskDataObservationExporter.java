package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

import java.util.Collections;

/** 将 AskData 事件映射为中立 OTLP trace/span/generation 记录。 */
public final class OpenTelemetryAskDataObservationExporter implements AskDataObservationExporter {
	private final AskDataObservationTransport transport;
	public OpenTelemetryAskDataObservationExporter(AskDataObservationTransport transport) {
		if (transport == null) { throw new IllegalArgumentException("transport 不能为空"); }
		this.transport = transport;
	}
	@Override public void export(AskDataEvent event) {
		transport.send("opentelemetry", Collections.singletonList(AskDataObservationRecord.from(event)));
	}
}
