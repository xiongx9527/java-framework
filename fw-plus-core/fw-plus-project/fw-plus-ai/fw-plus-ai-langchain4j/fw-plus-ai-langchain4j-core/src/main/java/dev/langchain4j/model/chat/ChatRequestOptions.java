package dev.langchain4j.model.chat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Per-invocation metadata that is not sent to the provider. */
public final class ChatRequestOptions {
    public static final ChatRequestOptions EMPTY = new ChatRequestOptions(Collections.emptyMap());
    private final Map<Object, Object> listenerAttributes;

    private ChatRequestOptions(Map<Object, Object> listenerAttributes) {
        this.listenerAttributes = listenerAttributes == null
                ? Collections.<Object, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(listenerAttributes));
    }

    public Map<Object, Object> listenerAttributes() {
        return listenerAttributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ChatRequestOptions)) return false;
        ChatRequestOptions that = (ChatRequestOptions) object;
        return Objects.equals(listenerAttributes, that.listenerAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listenerAttributes);
    }

    @Override
    public String toString() {
        return "ChatRequestOptions{listenerAttributes=" + listenerAttributes + "}";
    }

    public static final class Builder {
        private Map<Object, Object> listenerAttributes;

        public Builder listenerAttributes(Map<Object, Object> listenerAttributes) {
            this.listenerAttributes = listenerAttributes;
            return this;
        }

        public Builder addListenerAttribute(Object key, Object value) {
            if (listenerAttributes == null) listenerAttributes = new LinkedHashMap<Object, Object>();
            listenerAttributes.put(key, value);
            return this;
        }

        public ChatRequestOptions build() {
            return new ChatRequestOptions(listenerAttributes);
        }
    }
}
