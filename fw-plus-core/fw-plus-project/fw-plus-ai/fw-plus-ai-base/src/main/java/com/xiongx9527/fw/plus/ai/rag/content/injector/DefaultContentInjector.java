package com.xiongx9527.fw.plus.ai.rag.content.injector;

import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.data.document.Metadata;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.message.SystemMessage;
import com.xiongx9527.fw.plus.ai.message.UserMessage;
import com.xiongx9527.fw.plus.ai.template.Prompt;
import com.xiongx9527.fw.plus.ai.template.PromptTemplate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 默认内容注入器，镜像 langchain4j 1.23.0 的 {@code DefaultContentInjector}（把召回内容拼进用户消息）。 */
public class DefaultContentInjector implements ContentInjector {

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("{{contents}}\n\n{{userMessage}}");

    private final PromptTemplate promptTemplate;
    private final List<String> metadataKeys;

    public DefaultContentInjector() { this(DEFAULT_PROMPT_TEMPLATE, Collections.<String>emptyList()); }
    public DefaultContentInjector(List<String> metadataKeys) { this(DEFAULT_PROMPT_TEMPLATE, metadataKeys); }
    public DefaultContentInjector(PromptTemplate promptTemplate) { this(promptTemplate, Collections.<String>emptyList()); }
    public DefaultContentInjector(PromptTemplate promptTemplate, List<String> metadataKeys) {
        this.promptTemplate = promptTemplate == null ? DEFAULT_PROMPT_TEMPLATE : promptTemplate;
        this.metadataKeys = metadataKeys == null ? Collections.<String>emptyList() : metadataKeys;
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage queryMessage) {
        if (contents == null || contents.isEmpty()) return queryMessage;
        Prompt prompt = createPrompt(queryMessage, contents);
        return queryMessage instanceof SystemMessage ? SystemMessage.from(prompt.text()) : UserMessage.from(prompt.text());
    }

    protected Prompt createPrompt(ChatMessage queryMessage, List<Content> contents) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("userMessage", queryMessage.text());
        variables.put("contents", format(contents));
        return promptTemplate.render(variables);
    }

    protected String format(List<Content> contents) {
        StringBuilder sb = new StringBuilder();
        for (Content c : contents) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(format(c));
        }
        return sb.toString();
    }

    protected String format(Content content) {
        return content == null || content.textSegment() == null ? "" : content.textSegment().text();
    }

    protected String format(Metadata metadata) {
        if (metadata == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String key : metadataKeys) {
            String value = metadata.getString(key);
            if (value != null) { if (sb.length() > 0) sb.append('\n'); sb.append(format(key, value)); }
        }
        return sb.toString();
    }

    protected String format(String key, String value) { return key + ": " + value; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private PromptTemplate promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        private List<String> metadataKeys = Collections.emptyList();
        public Builder promptTemplate(PromptTemplate promptTemplate) { this.promptTemplate = promptTemplate; return this; }
        public Builder metadataKeys(List<String> metadataKeys) { this.metadataKeys = metadataKeys; return this; }
        public DefaultContentInjector build() { return new DefaultContentInjector(promptTemplate, metadataKeys); }
    }
}
