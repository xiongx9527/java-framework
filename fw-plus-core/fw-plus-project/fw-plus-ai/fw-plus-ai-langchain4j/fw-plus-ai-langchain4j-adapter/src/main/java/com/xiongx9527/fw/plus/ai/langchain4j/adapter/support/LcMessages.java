package com.xiongx9527.fw.plus.ai.langchain4j.adapter.support;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.content.ContentPart;
import com.xiongx9527.fw.plus.ai.base.model.content.ContentType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * base 的 {@link ChatMessage} 与 LangChain4j 0.35 的 {@code dev.langchain4j.data.message.ChatMessage} 互转。
 *
 * <p>多模态边界：TEXT 与 IMAGE（URL / base64）可映射；AUDIO / FILE 在 0.35 没有与 base
 * 语义对应的通用 Content 类型，转换时跳过，需要时由业务侧用厂商 SDK 自行封装。</p>
 */
public final class LcMessages {

    private LcMessages() {
    }

    public static List<dev.langchain4j.data.message.ChatMessage> toUpstream(List<ChatMessage> messages) {
        List<dev.langchain4j.data.message.ChatMessage> out =
                new ArrayList<dev.langchain4j.data.message.ChatMessage>();
        if (messages == null) {
            return out;
        }
        for (ChatMessage message : messages) {
            dev.langchain4j.data.message.ChatMessage converted = toUpstream(message);
            if (converted != null) {
                out.add(converted);
            }
        }
        return out;
    }

    public static dev.langchain4j.data.message.ChatMessage toUpstream(ChatMessage message) {
        if (message == null) {
            return null;
        }
        String text = message.getContent() == null ? "" : message.getContent();
        ChatRole role = message.getRole();
        if (role == ChatRole.SYSTEM) {
            return SystemMessage.from(text);
        }
        if (role == ChatRole.TOOL) {
            String id = message.getToolCallId() == null ? "" : message.getToolCallId();
            return ToolExecutionResultMessage.from(id, message.getName(), text);
        }
        if (role == ChatRole.ASSISTANT) {
            if (message.hasToolCalls()) {
                return AiMessage.from(text, LcTools.toUpstreamCalls(message.getToolCalls()));
            }
            return AiMessage.from(text);
        }
        List<Content> contents = toUpstreamContents(message.getParts());
        if (contents.isEmpty()) {
            return UserMessage.from(text);
        }
        return UserMessage.from(contents);
    }

    private static List<Content> toUpstreamContents(List<ContentPart> parts) {
        List<Content> out = new ArrayList<Content>();
        if (parts == null) {
            return out;
        }
        for (ContentPart part : parts) {
            if (part == null) {
                continue;
            }
            if (part.type() == ContentType.TEXT) {
                if (part.text() != null) {
                    out.add(dev.langchain4j.data.message.TextContent.from(part.text()));
                }
            } else if (part.type() == ContentType.IMAGE
                    && part instanceof com.xiongx9527.fw.plus.ai.base.model.content.ImageContent) {
                com.xiongx9527.fw.plus.ai.base.model.content.ImageContent image =
                        (com.xiongx9527.fw.plus.ai.base.model.content.ImageContent) part;
                if (image.isUrl()) {
                    out.add(dev.langchain4j.data.message.ImageContent.from(image.getUrl()));
                } else if (image.getData() != null) {
                    String base64 = Base64.getEncoder().encodeToString(image.getData());
                    String mime = image.getMimeType() == null ? "image/png" : image.getMimeType();
                    out.add(new dev.langchain4j.data.message.ImageContent(base64, mime));
                }
            }
            // AUDIO / FILE：0.35 无通用对应类型，跳过
        }
        return out;
    }
}
