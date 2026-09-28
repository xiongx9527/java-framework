package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jTokenizerMagicResourceStorage;
import dev.langchain4j.model.Tokenizer;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** tokenizer 动态注册器。 */
public class Langchain4jTokenizerMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jTokenizerInfo, Tokenizer> {

    public Langchain4jTokenizerMagicDynamicRegistry(Langchain4jTokenizerMagicResourceStorage storage,
            MagicDynamicLangchain4jTokenizerDataSource holder, Langchain4jModelFactoryRegistry factories) {
        super(storage, holder, factories, Langchain4jModelTypes.TOKENIZER);
    }

    @Override
    protected Tokenizer buildInstance(Langchain4jTokenizerInfo entity) {
        return factories.buildTokenizer(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-tokenizer'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-tokenizer'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }
}
