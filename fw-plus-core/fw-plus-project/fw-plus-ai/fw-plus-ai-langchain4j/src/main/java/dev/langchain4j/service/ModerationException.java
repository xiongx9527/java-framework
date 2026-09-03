package dev.langchain4j.service;

import dev.langchain4j.model.moderation.Moderation;

public class ModerationException extends RuntimeException {
    private final Moderation moderation;

    public ModerationException(String message) {
        this(message, null);
    }

    public ModerationException(String message, Moderation moderation) {
        super(message);
        this.moderation = moderation;
    }

    public Moderation moderation() { return moderation; }
}
