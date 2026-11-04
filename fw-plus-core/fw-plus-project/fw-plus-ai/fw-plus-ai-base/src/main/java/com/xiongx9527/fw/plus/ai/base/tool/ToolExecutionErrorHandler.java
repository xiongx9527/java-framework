package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * Tool execution error handler (aligned with langchain4j 1.23 ToolExecutionErrorHandler).
 *
 * <p>Invoked by the calling loop when a tool method throws at execution time. The default
 * implementation feeds the exception message back to the model (retry), matching langchain4j
 * default behaviour; it can be customised to stop the loop or to rewrite the message
 * (e.g. redact, translate to English).
 */
public interface ToolExecutionErrorHandler {

    ToolErrorHandlerResult handle(ToolExecutionRequest request, Throwable error);

    static ToolExecutionErrorHandler defaultHandler() {
        return (request, error) -> ToolErrorHandlerResult.retry(
                "tool error: " + (error == null ? "unknown"
                        : (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage())));
    }
}
