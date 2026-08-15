package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 将 AskData 事件批量映射为 Langfuse trace、span 和 generation。 */
public final class LangfuseAskDataObservationExporter implements AskDataObservationExporter {
	private final AskDataObservationTransport transport;
	private final int batchSize;
	private final List<AskDataObservationRecord> pending = new ArrayList<AskDataObservationRecord>();
	public LangfuseAskDataObservationExporter(AskDataObservationTransport transport) { this(transport, 10); }
	public LangfuseAskDataObservationExporter(AskDataObservationTransport transport, int batchSize) {
		if (transport == null || batchSize < 1) { throw new IllegalArgumentException("transport 和 batchSize 必须有效"); }
		this.transport = transport;
		this.batchSize = batchSize;
	}
	@Override public synchronized void export(AskDataEvent event) {
		pending.add(AskDataObservationRecord.from(event));
		if (pending.size() >= batchSize) { flush(); }
	}
	public synchronized void flush() {
		if (pending.isEmpty()) { return; }
		List<AskDataObservationRecord> batch = Collections.unmodifiableList(
				new ArrayList<AskDataObservationRecord>(pending));
		transport.send("langfuse", batch);
		pending.clear();
	}
}
