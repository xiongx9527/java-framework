package dev.langchain4j.memory;

import dev.langchain4j.data.message.ChatMessage;
import java.util.Arrays;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/** Represents the memory of a chat conversation. */
public interface ChatMemory {

    Object id();
    void add(ChatMessage message);

    default void add(ChatMessage... messages) {
        if (messages != null && messages.length > 0) add(Arrays.asList(messages));
    }

    default void add(Iterable<ChatMessage> messages) {
        if (messages != null) {
            for (ChatMessage message : messages) add(message);
        }
    }

    default void set(ChatMessage... messages) {
        if (messages == null || messages.length == 0) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        set(Arrays.asList(messages));
    }

    default void set(Iterable<ChatMessage> messages) {
        ensureNotNull(messages, "messages");
        if (!messages.iterator().hasNext()) throw new IllegalArgumentException("messages must not be empty");
        clear();
        add(messages);
    }

    List<ChatMessage> messages();
    void clear();
}
