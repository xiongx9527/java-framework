package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

import java.util.Collections;

/** 将 AskData 事件映射为 LangSmith 父子 run/trace 记录。 */
public final class LangSmithAskDataObservationExporter implements AskDataObservationExporter {
	private final AskDataObservationTransport transport;
	public LangSmithAskDataObservationExporter(AskDataObservationTransport transport) {
		if (transport == null) { throw new IllegalArgumentException("transport 不能为空"); }
		this.transport = transport;
	}
	@Override public void export(AskDataEvent event) {
		transport.send("langsmith", Collections.singletonList(AskDataObservationRecord.from(event)));
	}
}
