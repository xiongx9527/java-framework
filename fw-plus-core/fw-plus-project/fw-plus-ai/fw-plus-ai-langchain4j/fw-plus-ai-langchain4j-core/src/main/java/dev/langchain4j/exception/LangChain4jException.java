package dev.langchain4j.exception;

/** Root unchecked exception for LangChain4j-specific failures. */
public class LangChain4jException extends RuntimeException {

    public LangChain4jException(String message) {
        super(message);
    }

    public LangChain4jException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public LangChain4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
