package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * Tool argument-layer error: missing parameters, malformed JSON, type mismatch, missing/unknown
 * tool name, etc.
 *
 * <p>Distinct from ToolExecutionException (tool method execution error) so the calling loop can
 * route argument errors to ToolArgumentsErrorHandler and execution errors to
 * ToolExecutionErrorHandler, aligning with langchain4j 1.23's dual-handler design.</p>
 *
 * <p>Every argument error also carries a stable errorKey plus structured args. The message is the
 * model-facing English description (used verbatim when fed back to the LLM); the errorKey lets the
 * UI layer localize independently via I18nMessageResolver, following the backend-contract
 * convention of "return a key + raw reason, never hardcode a localized string in the backend".</p>
 */
public class ToolArgumentException extends ToolExecutionException {

    private static final long serialVersionUID = 1L;

    // Stable error keys for i18n. Register these in the consuming app's message bundle.
    public static final String KEY_REQUIRED = "tool.arg.required";
    public static final String KEY_PATTERN = "tool.arg.pattern";
    public static final String KEY_MIN_LENGTH = "tool.arg.minLength";
    public static final String KEY_MAX_LENGTH = "tool.arg.maxLength";
    public static final String KEY_MIN = "tool.arg.min";
    public static final String KEY_MAX = "tool.arg.max";
    public static final String KEY_JSON_OBJECT = "tool.arg.jsonObject";
    public static final String KEY_JSON_ARRAY = "tool.arg.jsonArray";
    public static final String KEY_DECODE = "tool.arg.decode";
    public static final String KEY_UNKNOWN_TOOL = "tool.arg.unknownTool";
    public static final String KEY_MISSING_NAME = "tool.arg.missingName";

    private final String errorKey;
    private final Object[] args;

    public ToolArgumentException(String message) {
        this(message, null, (Object[]) null);
    }

    public ToolArgumentException(String message, Throwable cause) {
        super(message, cause);
        this.errorKey = null;
        this.args = null;
    }

    public ToolArgumentException(String message, String errorKey, Object... args) {
        super(message);
        this.errorKey = errorKey;
        this.args = args;
    }

    public ToolArgumentException(String message, Throwable cause, String errorKey, Object... args) {
        super(message, cause);
        this.errorKey = errorKey;
        this.args = args;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
