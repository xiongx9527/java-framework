package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认注入：把资料作为一条 user 消息插在原有消息之前，保留原有 system / 历史消息。
 *
 * <p>资料为空时原样返回消息列表——没有召回就不该改变提示词形状。</p>
 */
public class DefaultContentInjector implements ContentInjector {

    private final String header;

    public DefaultContentInjector() {
        this("Reference material:");
    }

    public DefaultContentInjector(String header) {
        this.header = header;
    }

    @Override
    public List<ChatMessage> inject(List<Content> contents, List<ChatMessage> messages) {
        List<ChatMessage> result = new ArrayList<ChatMessage>();
        if (messages != null) {
            result.addAll(messages);
        }
        if (contents == null || contents.isEmpty()) {
            return result;
        }
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isEmpty()) {
            sb.append(header).append('\n');
        }
        for (Content content : contents) {
            if (content == null || content.getText() == null) {
                continue;
            }
            sb.append("- ").append(content.getText()).append('\n');
        }
        if (sb.length() == 0) {
            return result;
        }
        int insertAt = 0;
        while (insertAt < result.size() && result.get(insertAt).getRole() == ChatRole.SYSTEM) {
            insertAt++;
        }
        result.add(insertAt, ChatMessage.of(ChatRole.USER, sb.toString().trim()));
        return result;
    }
}
