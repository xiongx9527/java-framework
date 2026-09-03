package dev.langchain4j.agentic.agent;

import java.util.Objects;

public final class ErrorRecoveryResult {

    private final Type type;
    private final Object result;

    public ErrorRecoveryResult(Type type, Object result) {
        this.type = type;
        this.result = result;
    }

    public Type type() {
        return type;
    }

    public Object result() {
        return result;
    }

    public enum Type {
        THROW_EXCEPTION, RETURN_RESULT, RETRY
    }

    public static ErrorRecoveryResult throwException() {
        return new ErrorRecoveryResult(Type.THROW_EXCEPTION, null);
    }

    public static ErrorRecoveryResult retry() {
        return new ErrorRecoveryResult(Type.RETRY, null);
    }

    public static ErrorRecoveryResult result(Object result) {
        return new ErrorRecoveryResult(Type.RETURN_RESULT, result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorRecoveryResult)) return false;
        ErrorRecoveryResult that = (ErrorRecoveryResult) o;
        return Objects.equals(type, that.type) && Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (type == null ? 0 : type.hashCode());
        hash = 31 * hash + (result == null ? 0 : result.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "ErrorRecoveryResult[type=" + type + ", result=" + result + "]";
    }
}
