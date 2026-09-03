package dev.langchain4j.exception;

/** Thrown when the current model or provider does not support a requested feature. */
public class UnsupportedFeatureException extends LangChain4jException {
    public UnsupportedFeatureException(String message) {
        super(message);
    }
}
