package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.List;

/**
 * 内容注入：把检索到的资料写进提示词。
 */
public interface ContentInjector {

    List<ChatMessage> inject(List<Content> contents, List<ChatMessage> messages);
}
