package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import java.util.List;

/**
 * 由业务实现的中立观测传输 SPI。
 *
 * <p>实现负责把批量记录发送到 LangSmith、Langfuse、OTLP Gateway 或企业平台，并自行管理端点、凭据、超时和重试。
 * Starter 不读取或提供默认凭据。</p>
 */
public interface AskDataObservationTransport {
	void send(String exporterKey, List<AskDataObservationRecord> records);
}
