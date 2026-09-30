package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module.Langchain4jContentInjectorModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import java.util.List;

/**
 * {@link ContentInjector} 的动态代理：持有脚本模块与资源 path，每次调用实时解析当前注入器，
 * magic 函数热更新/注销后不持有失效引用。解析失败回落容器中的注入器 bean
 * （path 空取默认、非空按 bean 名），与 {@code MagicDynamicChatMemoryStore} 的兜底语义一致。
 */
public class MagicDynamicContentInjector implements ContentInjector {

    private final Langchain4jContentInjectorModule contentInjectorModule;

    private final String path;

    public MagicDynamicContentInjector(Langchain4jContentInjectorModule contentInjectorModule, String path) {
        this.contentInjectorModule = contentInjectorModule;
        this.path = path;
    }

    private ContentInjector getContentInjector() {
        try {
            return contentInjectorModule.get(path);
        } catch (Exception e) {
            if (StringUtils.isEmpty(path)) {
                return SpringUtils.getBean(ContentInjector.class);
            }
            return SpringUtils.getBean(path, ContentInjector.class);
        }
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
        return getContentInjector().inject(contents, chatMessage);
    }
}
