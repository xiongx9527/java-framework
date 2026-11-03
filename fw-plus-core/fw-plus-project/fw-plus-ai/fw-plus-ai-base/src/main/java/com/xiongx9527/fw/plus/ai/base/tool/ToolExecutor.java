package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具执行器。
 */
public interface ToolExecutor {

    /**
     * 执行一次工具调用。
     *
     * @return 结果文本（回填给模型）；失败时抛 {@link ToolExecutionException}，
     *         由调用环转成 {@link ToolExecutionResult#failure} 再回填
     */
    String execute(ToolExecutionRequest request);
}
