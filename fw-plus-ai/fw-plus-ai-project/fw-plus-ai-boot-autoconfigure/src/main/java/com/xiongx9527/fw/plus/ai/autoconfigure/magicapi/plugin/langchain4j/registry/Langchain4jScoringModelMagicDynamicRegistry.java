package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import java.util.ArrayList;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import java.util.Collections;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jScoringModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jScoringModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jScoringModelMagicResourceStorage;
import dev.langchain4j.model.scoring.ScoringModel;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.springframework.context.event.EventListener;

/** scoring-model 动态注册器：资源变更即重建 ScoringModel 实例。 */
public class Langchain4jScoringModelMagicDynamicRegistry
        extends AbstractLangchain4jModelMagicDynamicRegistry<Langchain4jScoringModelInfo, ScoringModel> {

    public Langchain4jScoringModelMagicDynamicRegistry(Langchain4jScoringModelMagicResourceStorage storage,
            MagicDynamicLangchain4jScoringModelDataSource holder, Langchain4jModelFactoryRegistry factories,
            ObjectProvider<ScoringModel> defaultInstance) {
        super(storage, holder, factories, Langchain4jModelTypes.SCORING_MODEL, defaultInstance);
    }

    @Override
    protected ScoringModel buildInstance(Langchain4jScoringModelInfo entity) {
        return factories.buildScoringModel(entity);
    }

    @EventListener(condition = "#event.type == 'langchain4j-scoring-model'")
    public void onFileEvent(FileEvent event) {
        safelyProcess(event);
    }

    @EventListener(condition = "#event.type == 'langchain4j-scoring-model'")
    public void onGroupEvent(GroupEvent event) {
        safelyProcess(event);
    }

    /** 默认实例（Spring 注入，key=default）+ 控制台配置的 datasource，一并暴露给控制台与脚本 */
    @Override
    public List<Langchain4jScoringModelInfo> defaultMappings() {
        List<Langchain4jScoringModelInfo> result = new ArrayList<>();
        if (defaultInstance.getIfAvailable() != null) {
            Langchain4jScoringModelInfo info = new Langchain4jScoringModelInfo();
            info.setKey(AbstractLangchain4jModelMagicDynamicRegistry.DEFAULT_KEY);
            info.setName("默认数据源");
            info.setProvider(defaultProvider());
            info.setEnabled(true);
            result.add(info);
        }
        result.addAll(mappings());
        return result;
    }

    @Override
    protected String defaultProvider() {
        return "jina";
    }
}
