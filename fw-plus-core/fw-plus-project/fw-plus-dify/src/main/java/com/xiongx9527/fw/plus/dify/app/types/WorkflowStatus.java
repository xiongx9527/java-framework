package com.xiongx9527.fw.plus.dify.app.types;

/**
 * 工作流运行状态。
 * <p>Dify 服务端状态枚举为 running/succeeded/failed/stopped/partial-succeeded/paused，
 * 其中 partial-succeeded 含连字符，无法直接作为 Java 枚举常量，故使用 partialSucceeded 常量并通过
 * {@link #getValue()} 还原服务端取值。</p>
 */
public enum WorkflowStatus {
    running("running"),
    succeeded("succeeded"),
    failed("failed"),
    stopped("stopped"),
    partialSucceeded("partial-succeeded"),
    paused("paused");

    private final String value;

    WorkflowStatus(String value) {
        this.value = value;
    }

    /**
     * 服务端使用的状态取值（如 partial-succeeded）
     */
    public String getValue() {
        return value;
    }
}
