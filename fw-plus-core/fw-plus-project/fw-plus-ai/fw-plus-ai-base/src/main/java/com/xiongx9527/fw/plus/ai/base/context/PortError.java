package com.xiongx9527.fw.plus.ai.base.context;

/**
 * 端口错误：把「哪个端口、什么码、能否重试」结构化，避免上层靠异常文本判断。
 */
public final class PortError {

    private final String port;

    private final int code;

    private final String message;

    private final boolean retryable;

    public PortError(String port, int code, String message, boolean retryable) {
        this.port = port;
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }

    public static PortError retryable(String port, String message) {
        return new PortError(port, 0, message, true);
    }

    public static PortError fatal(String port, String message) {
        return new PortError(port, 0, message, false);
    }

    public String getPort() {
        return port;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return "PortError{port=" + port + ", code=" + code + ", retryable=" + retryable + ", message=" + message + '}';
    }
}
