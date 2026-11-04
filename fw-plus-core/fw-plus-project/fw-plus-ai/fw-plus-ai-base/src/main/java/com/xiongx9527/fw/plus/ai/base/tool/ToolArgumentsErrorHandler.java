package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * Tool arguments error handler (aligned with langchain4j 1.23 ToolArgumentsErrorHandler).
 *
 * <p>Invoked by the calling loop for argument-layer errors - missing parameters, malformed JSON,
 * type mismatch, missing/unknown tool name. The default implementation feeds the error back to the
 * model so it can fix the arguments and retry (retry), matching langchain4j default behaviour;
 * it can be customised to stop the loop or to rewrite the message.
 */
public interface ToolArgumentsErrorHandler {

    ToolErrorHandlerResult handle(ToolExecutionRequest request, String rawArguments, Throwable error);

    static ToolArgumentsErrorHandler defaultHandler() {
        return (request, rawArguments, error) -> ToolErrorHandlerResult.retry(
                "tool arguments error: " + (error == null ? "unknown"
                        : (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage())));
    }
}
