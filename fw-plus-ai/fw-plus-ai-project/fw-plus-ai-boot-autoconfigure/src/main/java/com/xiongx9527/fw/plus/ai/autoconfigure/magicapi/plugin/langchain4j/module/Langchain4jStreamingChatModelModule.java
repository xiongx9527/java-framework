package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 流式对话模型脚本模块：脚本用 {@code streamingChatModel['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jStreamingChatModel")
public class Langchain4jStreamingChatModelModule extends AbstractLangchain4jModelModule<StreamingChatModel>
        implements DynamicAttribute<StreamingChatModel, Langchain4jStreamingChatModelModule>, DynamicModule<StreamingChatModel> {

    public Langchain4jStreamingChatModelModule(MagicDynamicLangchain4jStreamingChatModelDataSource holder,
            ObjectProvider<StreamingChatModel> defaultInstance) {
        super(holder, Langchain4jModelTypes.STREAMING_CHAT_MODEL, defaultInstance);
    }

    @Override
    public Langchain4jStreamingChatModelModule setDynamicAttribute(String key, StreamingChatModel value) {
        throw new UnsupportedOperationException(
                "脚本不支持设置模型实例，请在控制台维护 langchain4j-streaming-chat-model 数据源");
    }

    @Override
    public List<StreamingChatModel> defaultMappings() {
        return super.defaultMappings();
    }
}
