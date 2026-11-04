package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * Tool argument-layer error: missing parameters, malformed JSON, type mismatch, missing/unknown
 * tool name, etc.
 *
 * <p>Distinct from ToolExecutionException (tool method execution error) so the calling loop can
 * route argument errors to ToolArgumentsErrorHandler and execution errors to
 * ToolExecutionErrorHandler, aligning with langchain4j 1.23's dual-handler design.
 */
public class ToolArgumentException extends ToolExecutionException {

    private static final long serialVersionUID = 1L;

    public ToolArgumentException(String message) {
        super(message);
    }

    public ToolArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
