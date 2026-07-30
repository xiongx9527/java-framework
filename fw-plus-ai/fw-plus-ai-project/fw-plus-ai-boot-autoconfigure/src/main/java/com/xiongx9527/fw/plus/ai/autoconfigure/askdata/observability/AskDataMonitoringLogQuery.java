package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

public class AskDataMonitoringLogQuery {

    private final String traceId;

    private AskDataMonitoringLogQuery(String traceId) { this.traceId = traceId; }
    public static AskDataMonitoringLogQuery byConversationId(String conversationId) { return new AskDataMonitoringLogQuery(conversationId); }
    public static AskDataMonitoringLogQuery byRequestId(String requestId) { return new AskDataMonitoringLogQuery(requestId); }
    public static AskDataMonitoringLogQuery all() { return new AskDataMonitoringLogQuery(null); }
    public String getTraceId() { return traceId; }
}
