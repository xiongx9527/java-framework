package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

public interface AskDataObservationExporter {
    void export(AskDataEvent event);
}
