package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * Tool error handler result (aligned with langchain4j 1.23 ToolErrorHandlerResult).
 *
 * <p>Decides whether the error is fed back to the model for self-correction, and whether the
 * calling loop should stop:
 * <ul>
 *   <li>stop=false (default): the message is returned to the model as a tool result so it can fix
 *   the arguments and retry - same as langchain4j default;</li>
 *   <li>stop=true: the loop terminates immediately and the error is handed back to the caller.</li>
 * </ul>
 */
public final class ToolErrorHandlerResult {

    private final String message;
    private final boolean stop;

    private ToolErrorHandlerResult(String message, boolean stop) {
        this.message = message;
        this.stop = stop;
    }

    public static ToolErrorHandlerResult retry(String message) {
        return new ToolErrorHandlerResult(message, false);
    }

    public static ToolErrorHandlerResult stop(String message) {
        return new ToolErrorHandlerResult(message, true);
    }

    public String getMessage() {
        return message;
    }

    public boolean isStop() {
        return stop;
    }
}
