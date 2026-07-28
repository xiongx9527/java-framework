package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

/** Langfuse 通用占位 exporter；不引入厂商 SDK。 */
public class LangfuseAskDataObservationExporter implements AskDataObservationExporter {
    @Override public void export(AskDataEvent event) { }
}
