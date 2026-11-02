package com.xiongx9527.fw.plus.ai.rag.content.injector;

import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import java.util.List;

/** 内容注入端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.rag.content.injector.ContentInjector}。 */
public interface ContentInjector {
    ChatMessage inject(List<Content> contents, ChatMessage queryMessage);
}
