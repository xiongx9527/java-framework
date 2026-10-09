package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import org.springframework.beans.factory.ObjectProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jTokenizerMagicResourceStorage;
import dev.langchain4j.model.TokenCountEstimator;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** tokenizer 动态注册器。 */
public class Langchain4jTokenizerMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jTokenizerInfo, TokenCountEstimator> {

    public Langchain4jTokenizerMagicDynamicRegistry(Langchain4jTokenizerMagicResourceStorage storage,
            MagicDynamicLangchain4jTokenizerDataSource holder, Langchain4jModelFactoryRegistry factories,
            ObjectProvider<TokenCountEstimator> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.TOKENIZER, defaultInstance);
    }

    @Override
    protected TokenCountEstimator buildInstance(Langchain4jTokenizerInfo entity) {
        return factories.buildTokenizer(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-tokenizer'")
    public void onFileEvent(FileEvent event) {
        safelyProcess(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-tokenizer'")
    public void onGroupEvent(GroupEvent event) {
        safelyProcess(event);
    }

    /** 仅返回无文件的 Spring 默认实例（key=default，只读展示）。控制台文件条目由控制器 listFiles 提供，这里不能再叠加 mappings()（会重复，且核心按 id/mappingKey 双键登记导致翻倍） */
    @Override
    public List<Langchain4jTokenizerInfo> defaultMappings() {
        List<Langchain4jTokenizerInfo> result = new ArrayList<>();
        if (defaultInstance.getIfAvailable() != null) {
            Langchain4jTokenizerInfo info = new Langchain4jTokenizerInfo();
            info.setKey(AbstractLangchain4jModelMagicDynamicRegistry.DEFAULT_KEY);
            info.setName("默认数据源");
            info.setProvider(defaultProvider());
            info.setEnabled(true);
            result.add(info);
        }
        return result;
    }

    @Override
    protected String defaultProvider() {
        return "openai";
    }
}
