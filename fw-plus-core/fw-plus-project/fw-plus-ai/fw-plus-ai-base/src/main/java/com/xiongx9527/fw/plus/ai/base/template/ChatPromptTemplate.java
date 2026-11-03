package com.xiongx9527.fw.plus.ai.base.template;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 多消息提示词模板：每条消息带角色，统一填充变量后产出 {@link ChatMessage} 列表。
 */
public final class ChatPromptTemplate {

    private final List<Entry> entries;

    private ChatPromptTemplate(List<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 只含一条 user 消息的快捷构造。 */
    public static ChatPromptTemplate ofUser(String template) {
        return builder().user(template).build();
    }

    public List<ChatMessage> apply(Map<String, Object> values) {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        for (Entry entry : entries) {
            messages.add(ChatMessage.of(entry.role, entry.template.apply(values)));
        }
        return messages;
    }

    /** 全部模板用到的变量并集。 */
    public List<String> variables() {
        java.util.Set<String> names = new java.util.LinkedHashSet<String>();
        for (Entry entry : entries) {
            names.addAll(entry.template.variables());
        }
        return new ArrayList<String>(names);
    }

    private static final class Entry {

        private final ChatRole role;

        private final PromptTemplate template;

        private Entry(ChatRole role, PromptTemplate template) {
            this.role = role;
            this.template = template;
        }
    }

    public static final class Builder {

        private final List<Entry> entries = new ArrayList<Entry>();

        private Builder() {
        }

        public Builder system(String template) {
            entries.add(new Entry(ChatRole.SYSTEM, PromptTemplate.from(template)));
            return this;
        }

        public Builder user(String template) {
            entries.add(new Entry(ChatRole.USER, PromptTemplate.from(template)));
            return this;
        }

        public Builder assistant(String template) {
            entries.add(new Entry(ChatRole.ASSISTANT, PromptTemplate.from(template)));
            return this;
        }

        public ChatPromptTemplate build() {
            return new ChatPromptTemplate(entries);
        }
    }
}
