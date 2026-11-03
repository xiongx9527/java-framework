package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具执行结果，用于回填给模型。
 */
public final class ToolExecutionResult {

    private final String id;

    private final String name;

    private final String content;

    private final boolean error;

    public ToolExecutionResult(String id, String name, String content, boolean error) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.error = error;
    }

    public static ToolExecutionResult success(ToolExecutionRequest request, String content) {
        return new ToolExecutionResult(request == null ? null : request.getId(),
                request == null ? null : request.getName(), content, false);
    }

    public static ToolExecutionResult failure(ToolExecutionRequest request, String message) {
        return new ToolExecutionResult(request == null ? null : request.getId(),
                request == null ? null : request.getName(), message, true);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    /** 是否为失败结果（回填时模型需要知道「工具报错了」而不是「结果为空」）。 */
    public boolean isError() {
        return error;
    }
}
