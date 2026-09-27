package dev.langchain4j.data.message;

import dev.langchain4j.Experimental;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static dev.langchain4j.data.message.ChatMessageType.USER;
import static dev.langchain4j.internal.Exceptions.runtime;
import static dev.langchain4j.internal.Utils.quoted;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static java.util.Arrays.asList;

/** Represents a message from a user. */
public class UserMessage implements ChatMessage {

    private final String name;
    private final List<Content> contents;
    private final Map<String, Object> attributes;

    public UserMessage(Builder builder) {
        this.name = builder.name;
        this.contents = immutableContents(builder.contents);
        this.attributes = mutableAttributes(builder.attributes);
    }

    public UserMessage(String text) {
        this(TextContent.from(text));
    }

    public UserMessage(String name, String text) {
        this(name, TextContent.from(text));
    }

    public UserMessage(Content... contents) {
        this(asList(contents));
    }

    public UserMessage(String name, Content... contents) {
        this(name, asList(contents));
    }

    public UserMessage(List<Content> contents) {
        this.name = null;
        this.contents = immutableContents(contents);
        this.attributes = new LinkedHashMap<String, Object>();
    }

    public UserMessage(String name, List<Content> contents) {
        this.name = ensureNotBlank(name, "name");
        this.contents = immutableContents(contents);
        this.attributes = new LinkedHashMap<String, Object>();
    }

    public String name() {
        return name;
    }

    public List<Content> contents() {
        return contents;
    }

    public String singleText() {
        if (hasSingleText()) {
            return ((TextContent) contents.get(0)).text();
        }
        throw runtime("Expecting single text content, but got: " + contents);
    }

    public boolean hasSingleText() {
        return contents.size() == 1 && contents.get(0) instanceof TextContent;
    }

    /** @deprecated use {@link #singleText()} or {@link #contents()}. */
    @Deprecated
    public String text() {
        return singleText();
    }

    @Experimental
    public Map<String, Object> attributes() {
        return attributes;
    }

    @Experimental
    @SuppressWarnings("unchecked")
    public <T> T attribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }

    @Override
    public ChatMessageType type() {
        return USER;
    }

    public Builder toBuilder() {
        return builder().name(name).contents(contents).attributes(attributes);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        UserMessage that = (UserMessage) object;
        return Objects.equals(name, that.name)
                && Objects.equals(contents, that.contents)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contents, attributes);
    }

    @Override
    public String toString() {
        return "UserMessage { name = " + quoted(name) + ", contents = " + contents
                + ", attributes = " + attributes + " }";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private List<Content> contents;
        private Map<String, Object> attributes;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder contents(List<Content> contents) {
            this.contents = contents;
            return this;
        }

        public Builder addContent(Content content) {
            if (contents == null) {
                contents = new ArrayList<Content>();
            }
            contents.add(content);
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public UserMessage build() {
            return new UserMessage(this);
        }
    }

    public static UserMessage from(String text) { return new UserMessage(text); }
    public static UserMessage from(String name, String text) { return new UserMessage(name, text); }
    public static UserMessage from(Content... contents) { return new UserMessage(contents); }
    public static UserMessage from(String name, Content... contents) { return new UserMessage(name, contents); }
    public static UserMessage from(List<Content> contents) { return new UserMessage(contents); }
    public static UserMessage from(String name, List<Content> contents) { return new UserMessage(name, contents); }
    public static UserMessage userMessage(String text) { return from(text); }
    public static UserMessage userMessage(String name, String text) { return from(name, text); }
    public static UserMessage userMessage(Content... contents) { return from(contents); }
    public static UserMessage userMessage(String name, Content... contents) { return from(name, contents); }
    public static UserMessage userMessage(List<Content> contents) { return from(contents); }
    public static UserMessage userMessage(String name, List<Content> contents) { return from(name, contents); }

    public static Optional<UserMessage> findLast(Collection<ChatMessage> messages) {
        UserMessage last = null;
        for (ChatMessage message : messages) {
            if (message instanceof UserMessage) last = (UserMessage) message;
        }
        return Optional.ofNullable(last);
    }

    public static List<ChatMessage> replaceLast(List<ChatMessage> messages, UserMessage replacement) {
        if (replacement == null) return messages;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message instanceof UserMessage) {
                if (message.equals(replacement)) return messages;
                List<ChatMessage> result = new ArrayList<ChatMessage>(messages);
                result.set(i, replacement);
                return result;
            }
        }
        return messages;
    }

    private static List<Content> immutableContents(List<Content> contents) {
        return Collections.unmodifiableList(new ArrayList<Content>(ensureNotEmpty(contents, "contents")));
    }

    private static Map<String, Object> mutableAttributes(Map<String, Object> attributes) {
        return attributes == null ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(attributes);
    }
}
